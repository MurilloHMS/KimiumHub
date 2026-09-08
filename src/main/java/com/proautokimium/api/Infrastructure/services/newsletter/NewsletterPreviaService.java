package com.proautokimium.api.Infrastructure.services.newsletter;

import com.proautokimium.api.Application.DTOs.newsletter.*;
import com.proautokimium.api.Infrastructure.exceptions.newsletter.NewsletterMesJaConfirmadoException;
import com.proautokimium.api.Infrastructure.exceptions.newsletter.NewsletterPreviaNaoEncontradaException;
import com.proautokimium.api.Infrastructure.repositories.CustomerRepository;
import com.proautokimium.api.Infrastructure.repositories.NewsletterPreviaClienteRepository;
import com.proautokimium.api.Infrastructure.repositories.NewsletterPreviaOsRepository;
import com.proautokimium.api.Infrastructure.repositories.NewsletterPreviaRepository;
import com.proautokimium.api.Infrastructure.repositories.NewsletterRepository;
import com.proautokimium.api.Infrastructure.services.newsletter.sankhya.NewsletterSankhyaQueryService;
import com.proautokimium.api.Infrastructure.utils.LinhaSankhya;
import com.proautokimium.api.Infrastructure.utils.TimeParserUtil;
import com.proautokimium.api.domain.entities.Customer;
import com.proautokimium.api.domain.entities.Newsletter;
import com.proautokimium.api.domain.entities.NewsletterPrevia;
import com.proautokimium.api.domain.entities.NewsletterPreviaCliente;
import com.proautokimium.api.domain.entities.NewsletterPreviaOs;
import com.proautokimium.api.domain.enums.EmailStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * A prévia da newsletter: busca no ERP, revisão, confirmação.
 *
 * O caminho antigo passava por planilha — consulta à mão no Sankhya, exporta
 * Excel, sobe no sistema. Aqui a API consulta, junta e guarda um rascunho; a
 * tela confere; a confirmação vira fila de envio.
 */
@Service
public class NewsletterPreviaService {

    private static final Locale BRASIL = Locale.forLanguageTag("pt-BR");
    private static final DateTimeFormatter DIA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final NewsletterSankhyaQueryService sankhya;
    private final NewsletterPreviaRepository previaRepository;
    private final NewsletterPreviaClienteRepository clienteRepository;
    private final NewsletterPreviaOsRepository osRepository;
    private final NewsletterRepository newsletterRepository;
    private final CustomerRepository customerRepository;

    public NewsletterPreviaService(
            NewsletterSankhyaQueryService sankhya,
            NewsletterPreviaRepository previaRepository,
            NewsletterPreviaClienteRepository clienteRepository,
            NewsletterPreviaOsRepository osRepository,
            NewsletterRepository newsletterRepository,
            CustomerRepository customerRepository) {
        this.sankhya = sankhya;
        this.previaRepository = previaRepository;
        this.clienteRepository = clienteRepository;
        this.osRepository = osRepository;
        this.newsletterRepository = newsletterRepository;
        this.customerRepository = customerRepository;
    }

    // ── Buscar ────────────────────────────────────────────────────────────────

    /**
     * O rascunho do mês: o que existe, ou um novo vindo do ERP.
     *
     * **Mês já confirmado é recusado.** A newsletter dispara e-mail, e refazer
     * um mês é o tipo de coisa que se descobre tarde — na caixa do cliente.
     *
     * **Mês em revisão devolve o rascunho existente**, e não uma consulta nova:
     * é o que deixa sair da tela no meio de 913 clientes e voltar depois sem
     * perder as correções.
     */
    @Transactional
    public PreviaResponseDTO buscarOuCriar(int mes, int ano) {
        Optional<NewsletterPrevia> existente = previaRepository.findByMesAndAno(mes, ano);

        if (existente.isPresent()) {
            NewsletterPrevia previa = existente.get();

            if (previa.estaConfirmada()) {
                throw new NewsletterMesJaConfirmadoException(
                        nomeDoMes(mes) + " de " + ano + " já foi confirmado em "
                                + previa.getConfirmadoEm().format(DIA) + ", com "
                                + previa.getQuantidadeDeClientes() + " clientes.");
            }

            return montarResposta(previa);
        }

        return montarResposta(criar(mes, ano));
    }

    private NewsletterPrevia criar(int mes, int ano) {
        LocalDate de = LocalDate.of(ano, mes, 1);
        LocalDate ate = de.withDayOfMonth(de.lengthOfMonth());

        Map<String, List<LinhaSankhya>> consultas = sankhya.buscarTudo(de, ate);

        NewsletterPrevia previa = new NewsletterPrevia(mes, ano);

        // As cinco consultas de apoio viram MAPA por código de cliente. Com 913
        // clientes e cinco fontes, procurar na lista a cada volta é o mesmo
        // trabalho feito 4.500 vezes.
        Map<String, LinhaSankhya> faturamento = porCodigo(consultas.get(NewsletterSankhyaQueryService.FATURAMENTO));
        Map<String, LinhaSankhya> visitas = porCodigo(consultas.get(NewsletterSankhyaQueryService.VISITAS));
        Map<String, LinhaSankhya> pecas = porCodigo(consultas.get(NewsletterSankhyaQueryService.PECAS));
        Map<String, LinhaSankhya> produto = porCodigo(consultas.get(NewsletterSankhyaQueryService.PRODUTO));

        // As OS entram inteiras, uma linha por ordem: a correção é por OS, e o
        // total do cliente é calculado a partir delas.
        List<LinhaSankhya> horas = consultas.getOrDefault(NewsletterSankhyaQueryService.HORAS, List.of());
        Map<String, List<NewsletterPreviaOs>> osPorCliente = new HashMap<>();

        for (LinhaSankhya linha : horas) {
            NewsletterPreviaOs os = new NewsletterPreviaOs();
            os.setCodigoCliente(String.valueOf(linha.inteiro("codigo_cliente")));
            os.setNumeroOs(linha.inteiro("numero_os"));
            os.setMauUso(linha.booleano("mau_uso"));
            os.setHoraInicioTexto(linha.texto("hora_inicio"));
            os.setHoraFimTexto(linha.texto("hora_fim"));

            // O que o parser entende vira hora; o que não entende fica pendente
            // e aparece na tela. Antes, o TRY_CAST do SQL devolvia NULL e o
            // WHERE descartava a linha — 82 das 350 OS de junho, em silêncio.
            TimeParserUtil.interpret(os.getHoraInicioTexto()).ifPresent(os::setHoraInicio);
            TimeParserUtil.interpret(os.getHoraFimTexto()).ifPresent(os::setHoraFim);

            previa.adicionar(os);
            osPorCliente.computeIfAbsent(os.getCodigoCliente(), c -> new ArrayList<>()).add(os);
        }

        // O cadastro de clientes é a fonte do e-mail; o ERP é a reserva. É a
        // única fonte que sabe quem aceita receber, que é o que uma newsletter
        // precisa saber.
        List<LinhaSankhya> clientes = consultas.getOrDefault(NewsletterSankhyaQueryService.CLIENTES, List.of());
        Map<String, Customer> cadastro = cadastroDe(clientes);

        for (LinhaSankhya linha : clientes) {
            String codigo = String.valueOf(linha.inteiro("codigo_cliente"));

            NewsletterPreviaCliente cliente = new NewsletterPreviaCliente();
            cliente.setCodigoCliente(codigo);
            cliente.setNomeDoCliente(ouVazio(linha.texto("nome_do_cliente")));
            cliente.setCodigoMatriz(linha.texto("codigo_matriz"));
            cliente.setNomeMatriz(linha.texto("nome_matriz"));

            aplicarEmail(cliente, cadastro.get(codigo), linha.texto("email_cliente"));

            // Cliente ausente de um mapa vira zero — é o ISNULL do SQL, escrito
            // noutro lugar. E é o caso comum: 855 dos 913 têm faturamento, 142
            // têm peças.
            LinhaSankhya f = faturamento.get(codigo);
            if (f != null) {
                cliente.setQuantidadeNotasEmitidas(f.inteiro("quantidade_notas_emitidas"));
                cliente.setQuantidadeDeProdutos(f.inteiro("quantidade_de_produtos"));
                cliente.setQuantidadeDeLitros(f.decimal("quantidade_de_litros"));
                cliente.setFaturamentoTotal(f.decimal("faturamento_total"));
            }

            LinhaSankhya v = visitas.get(codigo);
            if (v != null) {
                cliente.setQuantidadeDeVisitas(v.inteiro("quantidade_de_visitas"));
                cliente.setMediaDiasAtendimento(v.inteiro("media_dias_atendimento"));
            }

            LinhaSankhya p = pecas.get(codigo);
            if (p != null) {
                cliente.setValorDePecasTrocadas(p.decimal("valor_de_pecas_trocadas"));
            }

            LinhaSankhya d = produto.get(codigo);
            if (d != null) {
                cliente.setProdutoEmDestaque(d.texto("produto_em_destaque"));
            }

            somarHoras(cliente, osPorCliente.getOrDefault(codigo, List.of()));

            previa.adicionar(cliente);
        }

        return previaRepository.save(previa);
    }

    // ── Corrigir uma hora ─────────────────────────────────────────────────────

    /**
     * Grava a hora digitada e refaz a conta do cliente.
     *
     * Devolve **o cliente recalculado**, e não um "ok": a tela mostra o número
     * que a API calculou, em vez de refazer a conta por conta própria. Duas
     * contas para o mesmo valor é como elas passam a discordar.
     */
    @Transactional
    public ClienteDaNewsletterDTO corrigirHora(UUID previaId, int numeroOs, CorrecaoDeHoraDTO correcao) {
        NewsletterPrevia previa = emRevisao(previaId);

        NewsletterPreviaOs os = osRepository.findByPreviaIdAndNumeroOs(previaId, numeroOs)
                .orElseThrow(() -> new NewsletterPreviaNaoEncontradaException(
                        "A OS " + numeroOs + " não está nesta prévia."));

        os.setHoraInicio(correcao.horaInicio());
        os.setHoraFim(correcao.horaFim());
        osRepository.save(os);

        NewsletterPreviaCliente cliente = clienteRepository
                .findByPreviaIdAndCodigoCliente(previa.getId(), os.getCodigoCliente())
                .orElseThrow(() -> new NewsletterPreviaNaoEncontradaException(
                        "A OS " + numeroOs + " aponta para um cliente que não está na prévia."));

        somarHoras(cliente, osRepository.findByPreviaIdAndCodigoCliente(previaId, os.getCodigoCliente()));

        return ClienteDaNewsletterDTO.de(clienteRepository.save(cliente));
    }

    // ── Preencher e-mails ─────────────────────────────────────────────────────

    /** Vários de uma vez: 31 requisições para um trabalho só seria desperdício. */
    @Transactional
    public List<ClienteDaNewsletterDTO> preencherEmails(UUID previaId, List<EmailPreenchidoDTO> emails) {
        NewsletterPrevia previa = emRevisao(previaId);

        Map<String, String> porCodigo = emails.stream()
                .collect(Collectors.toMap(EmailPreenchidoDTO::codigoCliente, EmailPreenchidoDTO::email, (a, b) -> b));

        List<NewsletterPreviaCliente> alvos = clienteRepository
                .findByPreviaIdAndCodigoClienteIn(previa.getId(), new ArrayList<>(porCodigo.keySet()));

        for (NewsletterPreviaCliente cliente : alvos) {
            cliente.setEmailCliente(porCodigo.get(cliente.getCodigoCliente()).trim());

            // Preencher o e-mail à mão é dizer "mande para este": quem estava
            // fora por não ter endereço volta para a fila. Quem pediu para não
            // receber continua fora — isso não se desfaz digitando um e-mail.
            if (cliente.isRecebeEmail()) {
                cliente.setRecebeEmail(true);
            }
        }

        return clienteRepository.saveAll(alvos).stream()
                .map(ClienteDaNewsletterDTO::de)
                .toList();
    }

    // ── Confirmar ─────────────────────────────────────────────────────────────

    /**
     * Fecha o mês e enche a fila de envio.
     *
     * Entram só os que têm e-mail e aceitam receber. Os demais ficam guardados
     * na prévia, marcados — o total de clientes do mês não muda por causa disso.
     *
     * Status `PENDING`, que é onde a importação por planilha deixava as linhas.
     * O agendamento continua sendo um passo à parte.
     */
    @Transactional
    public void confirmar(UUID previaId) {
        NewsletterPrevia previa = emRevisao(previaId);

        LocalDate data = LocalDate.of(previa.getAno(), previa.getMes(), 1);
        data = data.withDayOfMonth(data.lengthOfMonth());

        List<Newsletter> fila = new ArrayList<>();

        for (NewsletterPreviaCliente cliente : clienteRepository.findByPreviaIdOrderByFaturamentoTotalDesc(previaId)) {
            if (!cliente.isRecebeEmail() || cliente.getEmailCliente() == null || cliente.getEmailCliente().isBlank()) {
                continue;
            }

            Newsletter n = new Newsletter();
            n.setCodigoCliente(cliente.getCodigoCliente());
            n.setNomeDoCliente(cliente.getNomeDoCliente());
            n.setEmailCliente(cliente.getEmailCliente());
            n.setMatrizCode(cliente.getCodigoMatriz());
            n.setMatrizName(cliente.getNomeMatriz());
            n.setData(data);
            n.setMes(nomeDoMes(previa.getMes()));
            n.setQuantidadeNotasEmitidas(cliente.getQuantidadeNotasEmitidas());
            n.setQuantidadeDeProdutos(cliente.getQuantidadeDeProdutos());
            n.setQuantidadeDeLitros(cliente.getQuantidadeDeLitros());
            n.setQuantidadeDeVisitas(cliente.getQuantidadeDeVisitas());
            n.setMediaDiasAtendimento(cliente.getMediaDiasAtendimento());
            n.setProdutoEmDestaque(cliente.getProdutoEmDestaque());
            n.setFaturamentoTotal(cliente.getFaturamentoTotal());
            n.setValorDePecasTrocadas(cliente.getValorDePecasTrocadas());
            n.setValorTotalDeHoras(cliente.getValorTotalDeHoras());
            n.setValorTotalCobradoHoras(cliente.getValorTotalCobradoHoras());
            n.setMauUso(cliente.isMauUso());
            n.setValorTotalDeHorasMauUso(cliente.getValorTotalDeHorasMauUso());
            n.setValorTotalCobradoHorasMauUso(cliente.getValorTotalCobradoHorasMauUso());
            n.setStatus(EmailStatus.PENDING);

            fila.add(n);
        }

        newsletterRepository.saveAll(fila);

        previa.setConfirmadoEm(LocalDateTime.now());
        previa.setQuantidadeDeClientes(fila.size());
        previaRepository.save(previa);
    }

    // ── Bastidores ────────────────────────────────────────────────────────────

    private NewsletterPrevia emRevisao(UUID previaId) {
        NewsletterPrevia previa = previaRepository.findById(previaId)
                .orElseThrow(() -> new NewsletterPreviaNaoEncontradaException("Prévia não encontrada."));

        if (previa.estaConfirmada()) {
            throw new NewsletterMesJaConfirmadoException(
                    nomeDoMes(previa.getMes()) + " de " + previa.getAno() + " já foi confirmado em "
                            + previa.getConfirmadoEm().format(DIA) + ", com "
                            + previa.getQuantidadeDeClientes() + " clientes.");
        }

        return previa;
    }

    private Map<String, LinhaSankhya> porCodigo(List<LinhaSankhya> linhas) {
        if (linhas == null) {
            return Map.of();
        }

        // `(a, b) -> b` em vez de deixar estourar: as consultas agrupam por
        // CODPARC, então repetição não deveria acontecer — e se acontecer, o
        // lote do mês inteiro não pode cair por causa de uma linha.
        return linhas.stream().collect(Collectors.toMap(
                l -> String.valueOf(l.inteiro("codigo_cliente")),
                Function.identity(),
                (a, b) -> b));
    }

    private Map<String, Customer> cadastroDe(List<LinhaSankhya> clientes) {
        List<String> codigos = clientes.stream()
                .map(l -> String.valueOf(l.inteiro("codigo_cliente")))
                .distinct()
                .toList();

        if (codigos.isEmpty()) {
            return Map.of();
        }

        return customerRepository.findByCodParceiroIn(codigos).stream()
                .collect(Collectors.toMap(Customer::getCodParceiro, Function.identity(), (a, b) -> a));
    }

    /**
     * Cadastro primeiro, ERP como reserva.
     *
     * Quem está com `recebe_email = false` fica marcado e não entra na fila —
     * mas continua na prévia, com os números, senão o total de clientes do mês
     * mudaria sem explicação.
     */
    private void aplicarEmail(NewsletterPreviaCliente cliente, Customer cadastrado, String emailDoErp) {
        if (cadastrado != null) {
            cliente.setRecebeEmail(cadastrado.isRecebeEmail());

            String doCadastro = cadastrado.getEmail() == null ? null : cadastrado.getEmail().getAddress();
            if (doCadastro != null && !doCadastro.isBlank()) {
                cliente.setEmailCliente(doCadastro.trim());
                return;
            }
        }

        cliente.setEmailCliente(emailDoErp);
    }

    /**
     * Soma as OS do cliente em quatro totais.
     *
     * Mau uso vai para colunas próprias porque a newsletter mostra os dois
     * separados: o que foi atendimento e o que foi mau uso do equipamento.
     *
     * OS pendente entra com zero — fica de fora da conta, que é o que já
     * acontecia antes; a diferença é que agora ela aparece na tela.
     */
    private void somarHoras(NewsletterPreviaCliente cliente, List<NewsletterPreviaOs> ordens) {
        double horas = 0, cobrado = 0, horasMauUso = 0, cobradoMauUso = 0;

        for (NewsletterPreviaOs os : ordens) {
            if (os.isMauUso()) {
                horasMauUso += os.horas();
                cobradoMauUso += os.valorCobrado();
            } else {
                horas += os.horas();
                cobrado += os.valorCobrado();
            }
        }

        cliente.setValorTotalDeHoras(horas);
        cliente.setValorTotalCobradoHoras(cobrado);
        cliente.setValorTotalDeHorasMauUso(horasMauUso);
        cliente.setValorTotalCobradoHorasMauUso(cobradoMauUso);
        cliente.setMauUso(horasMauUso > 0);
    }

    private PreviaResponseDTO montarResposta(NewsletterPrevia previa) {
        List<ClienteDaNewsletterDTO> clientes =
                clienteRepository.findByPreviaIdOrderByFaturamentoTotalDesc(previa.getId()).stream()
                        .map(ClienteDaNewsletterDTO::de)
                        .toList();

        Map<String, String> nomes = clienteRepository.findByPreviaIdOrderByFaturamentoTotalDesc(previa.getId()).stream()
                .collect(Collectors.toMap(NewsletterPreviaCliente::getCodigoCliente,
                        NewsletterPreviaCliente::getNomeDoCliente, (a, b) -> a));

        List<PendenciaDeHoraDTO> pendencias = osRepository.findByPreviaId(previa.getId()).stream()
                .filter(NewsletterPreviaOs::estaPendente)
                .sorted(Comparator.comparingInt(NewsletterPreviaOs::getNumeroOs))
                .map(os -> new PendenciaDeHoraDTO(
                        os.getNumeroOs(),
                        os.getCodigoCliente(),
                        nomes.getOrDefault(os.getCodigoCliente(), ""),
                        os.getHoraInicioTexto(),
                        os.getHoraFimTexto()))
                .toList();

        return new PreviaResponseDTO(previa.getId(), previa.getMes(), previa.getAno(),
                nomeDoMes(previa.getMes()), clientes, pendencias);
    }

    private String nomeDoMes(int mes) {
        String nome = java.time.Month.of(mes).getDisplayName(TextStyle.FULL, BRASIL);
        return nome.substring(0, 1).toUpperCase(BRASIL) + nome.substring(1);
    }

    private String ouVazio(String valor) {
        return valor == null ? "" : valor;
    }
}
