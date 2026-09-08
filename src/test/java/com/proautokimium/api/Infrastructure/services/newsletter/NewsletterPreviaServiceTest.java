package com.proautokimium.api.Infrastructure.services.newsletter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.proautokimium.api.Application.DTOs.newsletter.ClienteDaNewsletterDTO;
import com.proautokimium.api.Application.DTOs.newsletter.CorrecaoDeHoraDTO;
import com.proautokimium.api.Application.DTOs.newsletter.PendenciaDeHoraDTO;
import com.proautokimium.api.Application.DTOs.newsletter.PreviaResponseDTO;
import com.proautokimium.api.Infrastructure.exceptions.newsletter.NewsletterMesJaConfirmadoException;
import com.proautokimium.api.Infrastructure.repositories.CustomerRepository;
import com.proautokimium.api.Infrastructure.repositories.NewsletterPreviaClienteRepository;
import com.proautokimium.api.Infrastructure.repositories.NewsletterPreviaOsRepository;
import com.proautokimium.api.Infrastructure.repositories.NewsletterPreviaRepository;
import com.proautokimium.api.Infrastructure.repositories.NewsletterRepository;
import com.proautokimium.api.Infrastructure.services.newsletter.sankhya.NewsletterSankhyaQueryService;
import com.proautokimium.api.Infrastructure.utils.LinhaSankhya;
import com.proautokimium.api.Infrastructure.utils.SankhyaRows;
import com.proautokimium.api.domain.entities.Newsletter;
import com.proautokimium.api.domain.entities.NewsletterPrevia;
import com.proautokimium.api.domain.entities.NewsletterPreviaCliente;
import com.proautokimium.api.domain.entities.NewsletterPreviaOs;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

/**
 * A junção das seis consultas.
 *
 * **O que estes testes protegem é o defeito que a migração revelou.** A hora da
 * OS é texto livre no ERP e vem em duas convenções — `13:00` e `14h20`. O
 * `TRY_CAST` do SQL antigo devolvia `NULL` para a segunda, e o
 * `WHERE hora_inicio IS NOT NULL` descartava a linha em silêncio: 82 das 350 OS
 * de junho, com o valor de horas saindo subestimado e nada indicando.
 *
 * Nenhuma dessas falhas aparece na tela. A newsletter sai, os números parecem
 * plausíveis, e o cliente recebe um valor menor do que o que foi trabalhado.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class NewsletterPreviaServiceTest {

    @Mock NewsletterSankhyaQueryService sankhya;
    @Mock NewsletterPreviaRepository previaRepository;
    @Mock NewsletterPreviaClienteRepository clienteRepository;
    @Mock NewsletterPreviaOsRepository osRepository;
    @Mock NewsletterRepository newsletterRepository;
    @Mock CustomerRepository customerRepository;

    private NewsletterPreviaService service;
    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        service = new NewsletterPreviaService(sankhya, previaRepository, clienteRepository,
                osRepository, newsletterRepository, customerRepository);

        when(previaRepository.findByMesAndAno(anyInt(), anyInt())).thenReturn(Optional.empty());
        when(previaRepository.save(any(NewsletterPrevia.class))).thenAnswer(i -> i.getArgument(0));
        when(customerRepository.findByCodParceiroIn(any())).thenReturn(List.of());
    }

    // ── Ajuda ─────────────────────────────────────────────────────────────────

    /** Monta a resposta do ERP no formato real: colunas de um lado, valores do outro. */
    private List<LinhaSankhya> linhas(String colunas, String... valores) {
        StringBuilder json = new StringBuilder("{\"fieldsMetadata\":[");
        String[] nomes = colunas.split(",");
        for (int i = 0; i < nomes.length; i++) {
            json.append(i > 0 ? "," : "").append("{\"name\":\"").append(nomes[i].trim()).append("\"}");
        }
        json.append("],\"rows\":[");
        for (int i = 0; i < valores.length; i++) {
            json.append(i > 0 ? "," : "").append("[").append(valores[i]).append("]");
        }
        json.append("]}");

        return SankhyaRows.emLinhas(json.toString(), mapper);
    }

    private void erpDevolve(Map<String, List<LinhaSankhya>> consultas) {
        when(sankhya.buscarTudo(any(LocalDate.class), any(LocalDate.class))).thenReturn(consultas);
    }

    /** O cenário mínimo: um cliente, e só o que cada teste precisa em cima. */
    private Map<String, List<LinhaSankhya>> comUmCliente() {
        Map<String, List<LinhaSankhya>> consultas = new HashMap<>();
        consultas.put(NewsletterSankhyaQueryService.CLIENTES,
                linhas("codigo_cliente,nome_do_cliente,email_cliente,codigo_matriz,nome_matriz",
                        "8805,\"EXAL VESUVIUS\",\"exal@x.com\",null,null"));
        consultas.put(NewsletterSankhyaQueryService.FATURAMENTO, List.of());
        consultas.put(NewsletterSankhyaQueryService.VISITAS, List.of());
        consultas.put(NewsletterSankhyaQueryService.PECAS, List.of());
        consultas.put(NewsletterSankhyaQueryService.HORAS, List.of());
        consultas.put(NewsletterSankhyaQueryService.PRODUTO, List.of());
        return consultas;
    }

    /** Devolve a prévia que o serviço mandou salvar. */
    private NewsletterPrevia previaSalva() {
        ArgumentCaptor<NewsletterPrevia> captor = ArgumentCaptor.forClass(NewsletterPrevia.class);
        verify(previaRepository).save(captor.capture());
        return captor.getValue();
    }

    private void devolveDoBanco(NewsletterPrevia previa) {
        when(clienteRepository.findByPreviaIdOrderByFaturamentoTotalDesc(any()))
                .thenReturn(previa.getClientes());
        when(osRepository.findByPreviaId(any())).thenReturn(previa.getOrdensDeServico());
    }

    // ── A junção ──────────────────────────────────────────────────────────────

    /**
     * A lista de clientes manda; as outras cinco completam.
     *
     * **A maioria dos clientes não está na maioria dos mapas** — 913 clientes
     * para 855 com faturamento e 142 com peças. Quem falta vira zero, que é o
     * `ISNULL` do SQL escrito noutro lugar; qualquer outra coisa (pular a linha,
     * estourar) perderia clientes que compraram.
     */
    @Test
    @DisplayName("cliente ausente das outras consultas entra com zero, e não some")
    void clienteSemFaturamentoEntraComZero() {
        erpDevolve(comUmCliente());

        service.buscarOuCriar(6, 2026);
        NewsletterPrevia previa = previaSalva();

        assertThat(previa.getClientes()).hasSize(1);
        NewsletterPreviaCliente cliente = previa.getClientes().get(0);

        assertThat(cliente.getCodigoCliente()).isEqualTo("8805");
        assertThat(cliente.getFaturamentoTotal()).isZero();
        assertThat(cliente.getQuantidadeDeVisitas()).isZero();
        assertThat(cliente.getValorDePecasTrocadas()).isZero();
        assertThat(cliente.getProdutoEmDestaque()).isNull();
    }

    @Test
    @DisplayName("junta as cinco consultas de apoio pelo código do cliente")
    void juntaPeloCodigo() {
        Map<String, List<LinhaSankhya>> consultas = comUmCliente();
        consultas.put(NewsletterSankhyaQueryService.FATURAMENTO,
                linhas("codigo_cliente,quantidade_notas_emitidas,quantidade_de_produtos,quantidade_de_litros,faturamento_total",
                        "8805,3,7,120.5,4861.07"));
        consultas.put(NewsletterSankhyaQueryService.VISITAS,
                linhas("codigo_cliente,quantidade_de_visitas,media_dias_atendimento", "8805,2,4"));
        consultas.put(NewsletterSankhyaQueryService.PECAS,
                linhas("codigo_cliente,valor_de_pecas_trocadas", "8805,318.9"));
        consultas.put(NewsletterSankhyaQueryService.PRODUTO,
                linhas("codigo_cliente,produto_em_destaque", "8805,\"PRO GRUN\""));
        erpDevolve(consultas);

        service.buscarOuCriar(6, 2026);
        NewsletterPreviaCliente cliente = previaSalva().getClientes().get(0);

        assertThat(cliente.getQuantidadeNotasEmitidas()).isEqualTo(3);
        assertThat(cliente.getQuantidadeDeLitros()).isEqualTo(120.5);
        assertThat(cliente.getFaturamentoTotal()).isEqualTo(4861.07);
        assertThat(cliente.getQuantidadeDeVisitas()).isEqualTo(2);
        assertThat(cliente.getValorDePecasTrocadas()).isEqualTo(318.9);
        assertThat(cliente.getProdutoEmDestaque()).isEqualTo("PRO GRUN");
    }

    // ── As horas, que é onde estava o defeito ─────────────────────────────────

    /**
     * Os dois formatos entram na conta.
     *
     * `13:00` sempre entrou. **`14h20` é o que sumia** — e são 79 das 350 OS de
     * junho. Duas horas a 150 dá 300; se este teste passar a devolver 150, é
     * porque uma das duas voltou a ser descartada.
     */
    @Test
    @DisplayName("hora escrita com h entra na soma, do mesmo jeito que a escrita com dois-pontos")
    void osDosDoisFormatosEntram() {
        Map<String, List<LinhaSankhya>> consultas = comUmCliente();
        consultas.put(NewsletterSankhyaQueryService.HORAS,
                linhas("codigo_cliente,numero_os,mau_uso,hora_inicio,hora_fim",
                        "8805,101,0,\"13:00\",\"14:00\"",
                        "8805,102,0,\"14h20\",\"15h20\""));
        erpDevolve(consultas);

        service.buscarOuCriar(6, 2026);
        NewsletterPreviaCliente cliente = previaSalva().getClientes().get(0);

        assertThat(cliente.getValorTotalDeHoras())
                .as("uma hora de cada OS")
                .isEqualTo(2d);
        assertThat(cliente.getValorTotalCobradoHoras())
                .as("duas horas a 150")
                .isEqualTo(300d);
    }

    /**
     * O que não deu para ler vira pendência **e continua existindo**.
     *
     * Este é o teste que separa o comportamento novo do antigo: antes a linha
     * desaparecia, e o valor menor não tinha explicação em lugar nenhum.
     */
    @Test
    @DisplayName("OS ilegível vira pendência com o texto original, em vez de sumir")
    void osIlegivelViraPendencia() {
        Map<String, List<LinhaSankhya>> consultas = comUmCliente();
        consultas.put(NewsletterSankhyaQueryService.HORAS,
                linhas("codigo_cliente,numero_os,mau_uso,hora_inicio,hora_fim",
                        "8805,20044,0,\"1603\",null",
                        "8805,20045,0,\"9:40 8/5\",\"11:00\""));
        erpDevolve(consultas);

        PreviaResponseDTO resposta = service.buscarOuCriar(6, 2026);
        NewsletterPrevia previa = previaSalva();
        devolveDoBanco(previa);

        assertThat(previa.getOrdensDeServico())
                .as("as duas continuam guardadas, mesmo sem hora")
                .hasSize(2);
        assertThat(previa.getOrdensDeServico()).allMatch(NewsletterPreviaOs::estaPendente);

        // A resposta é montada a partir do banco; com os mocks devolvendo a
        // prévia salva, as pendências saem completas.
        PreviaResponseDTO comBanco = service.buscarOuCriar(6, 2026);
        assertThat(comBanco.pendencias()).extracting(PendenciaDeHoraDTO::numeroOs)
                .contains(20044, 20045);
        assertThat(comBanco.pendencias()).extracting(PendenciaDeHoraDTO::horaInicio)
                .as("o texto cru, e não uma interpretação dele")
                .contains("1603", "9:40 8/5");

        assertThat(resposta.mes()).isEqualTo(6);
    }

    @Test
    @DisplayName("OS pendente não entra na conta, e não derruba as que entram")
    void pendenteNaoContaMasNaoAtrapalha() {
        Map<String, List<LinhaSankhya>> consultas = comUmCliente();
        consultas.put(NewsletterSankhyaQueryService.HORAS,
                linhas("codigo_cliente,numero_os,mau_uso,hora_inicio,hora_fim",
                        "8805,101,0,\"08:00\",\"10:00\"",
                        "8805,102,0,\"1603\",null"));
        erpDevolve(consultas);

        service.buscarOuCriar(6, 2026);
        NewsletterPreviaCliente cliente = previaSalva().getClientes().get(0);

        assertThat(cliente.getValorTotalDeHoras()).isEqualTo(2d);
        assertThat(cliente.getValorTotalCobradoHoras()).isEqualTo(300d);
    }

    /**
     * Mau uso vai para colunas próprias.
     *
     * A newsletter mostra os dois separados: o que foi atendimento e o que foi
     * mau uso do equipamento. Somados no mesmo lugar, o cliente veria como
     * serviço prestado o que é cobrança por dano.
     */
    @Test
    @DisplayName("mau uso soma em colunas separadas e liga a marca")
    void mauUsoVaiParaOutraColuna() {
        Map<String, List<LinhaSankhya>> consultas = comUmCliente();
        consultas.put(NewsletterSankhyaQueryService.HORAS,
                linhas("codigo_cliente,numero_os,mau_uso,hora_inicio,hora_fim",
                        "8805,101,0,\"08:00\",\"10:00\"",
                        "8805,102,1,\"14:00\",\"15:00\""));
        erpDevolve(consultas);

        service.buscarOuCriar(6, 2026);
        NewsletterPreviaCliente cliente = previaSalva().getClientes().get(0);

        assertThat(cliente.getValorTotalDeHoras()).isEqualTo(2d);
        assertThat(cliente.getValorTotalCobradoHoras()).isEqualTo(300d);
        assertThat(cliente.getValorTotalDeHorasMauUso()).isEqualTo(1d);
        assertThat(cliente.getValorTotalCobradoHorasMauUso()).isEqualTo(150d);
        assertThat(cliente.isMauUso()).isTrue();
    }

    // ── Mês já confirmado ─────────────────────────────────────────────────────

    /**
     * **Refazer um mês manda e-mail duas vezes para a base inteira.** É o tipo
     * de erro que se descobre na caixa do cliente, e a recusa precisa dizer
     * quando foi e com quantos — senão quem recebeu o "não" não sabe o que
     * aconteceu.
     */
    @Test
    @DisplayName("mês já confirmado é recusado, com a data e a quantidade")
    void mesConfirmadoRecusa() {
        NewsletterPrevia confirmada = new NewsletterPrevia(5, 2026);
        confirmada.setConfirmadoEm(LocalDateTime.of(2026, 6, 2, 9, 30));
        confirmada.setQuantidadeDeClientes(887);
        when(previaRepository.findByMesAndAno(5, 2026)).thenReturn(Optional.of(confirmada));

        assertThatThrownBy(() -> service.buscarOuCriar(5, 2026))
                .isInstanceOf(NewsletterMesJaConfirmadoException.class)
                .hasMessageContaining("Maio")
                .hasMessageContaining("02/06/2026")
                .hasMessageContaining("887");

        verify(sankhya, never()).buscarTudo(any(), any());
    }

    /**
     * Mês em revisão devolve o rascunho.
     *
     * É o que deixa sair da tela no meio de 913 clientes e voltar depois sem
     * perder as correções — e sem gastar seis consultas no ERP de novo.
     */
    @Test
    @DisplayName("mês em revisão devolve o rascunho existente, sem consultar o ERP")
    void mesEmRevisaoDevolveRascunho() {
        NewsletterPrevia rascunho = new NewsletterPrevia(6, 2026);
        when(previaRepository.findByMesAndAno(6, 2026)).thenReturn(Optional.of(rascunho));
        devolveDoBanco(rascunho);

        PreviaResponseDTO resposta = service.buscarOuCriar(6, 2026);

        assertThat(resposta.nomeDoMes()).isEqualTo("Junho");
        verify(sankhya, never()).buscarTudo(any(), any());
    }

    // ── Correção ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("corrigir a hora refaz a conta do cliente")
    void corrigirRefazAConta() {
        NewsletterPrevia previa = new NewsletterPrevia(6, 2026);

        NewsletterPreviaOs os = new NewsletterPreviaOs();
        os.setCodigoCliente("8805");
        os.setNumeroOs(20044);
        os.setHoraInicioTexto("1603");

        NewsletterPreviaCliente cliente = new NewsletterPreviaCliente();
        cliente.setCodigoCliente("8805");
        cliente.setNomeDoCliente("EXAL VESUVIUS");

        when(previaRepository.findById(any())).thenReturn(Optional.of(previa));
        when(osRepository.findByPreviaIdAndNumeroOs(any(), eq(20044))).thenReturn(Optional.of(os));
        when(clienteRepository.findByPreviaIdAndCodigoCliente(any(), eq("8805")))
                .thenReturn(Optional.of(cliente));
        when(osRepository.findByPreviaIdAndCodigoCliente(any(), eq("8805"))).thenReturn(List.of(os));
        when(clienteRepository.save(any(NewsletterPreviaCliente.class))).thenAnswer(i -> i.getArgument(0));

        ClienteDaNewsletterDTO recalculado = service.corrigirHora(UUID.randomUUID(), 20044,
                new CorrecaoDeHoraDTO(LocalTime.of(16, 3), LocalTime.of(17, 3)));

        assertThat(os.estaPendente()).isFalse();
        assertThat(recalculado.valorTotalDeHoras()).isEqualTo(1d);
        assertThat(recalculado.valorTotalCobradoHoras()).isEqualTo(150d);
    }

    // ── Confirmação ───────────────────────────────────────────────────────────

    /**
     * Quem pediu para não receber **fica na prévia e fora da fila**.
     *
     * As duas metades importam: tirar da fila é o que respeita o pedido; manter
     * na prévia é o que impede o total de clientes do mês de mudar sem
     * explicação.
     */
    @Test
    @DisplayName("confirmar enfileira só quem tem e-mail e aceita receber")
    void confirmarRespeitaODescadastro() {
        NewsletterPrevia previa = new NewsletterPrevia(6, 2026);

        NewsletterPreviaCliente aceita = cliente("8805", "EXAL", "exal@x.com", true);
        NewsletterPreviaCliente recusou = cliente("8781", "TEMPERO", "tempero@x.com", false);
        NewsletterPreviaCliente semEmail = cliente("8632", "SECTOR", null, true);

        when(previaRepository.findById(any())).thenReturn(Optional.of(previa));
        when(clienteRepository.findByPreviaIdOrderByFaturamentoTotalDesc(any()))
                .thenReturn(List.of(aceita, recusou, semEmail));

        service.confirmar(UUID.randomUUID());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Newsletter>> captor = ArgumentCaptor.forClass(List.class);
        verify(newsletterRepository).saveAll(captor.capture());

        assertThat(captor.getValue()).extracting(Newsletter::getCodigoCliente)
                .containsExactly("8805");
        assertThat(previa.getQuantidadeDeClientes()).isEqualTo(1);
        assertThat(previa.estaConfirmada()).isTrue();
    }

    @Test
    @DisplayName("confirmar duas vezes é recusado")
    void confirmarDuasVezesRecusa() {
        NewsletterPrevia previa = new NewsletterPrevia(6, 2026);
        previa.setConfirmadoEm(LocalDateTime.of(2026, 7, 1, 8, 0));
        previa.setQuantidadeDeClientes(900);
        when(previaRepository.findById(any())).thenReturn(Optional.of(previa));

        UUID id = UUID.randomUUID();
        assertThatThrownBy(() -> service.confirmar(id))
                .isInstanceOf(NewsletterMesJaConfirmadoException.class);

        verify(newsletterRepository, never()).saveAll(any());
    }

    private NewsletterPreviaCliente cliente(String codigo, String nome, String email, boolean recebe) {
        NewsletterPreviaCliente c = new NewsletterPreviaCliente();
        c.setCodigoCliente(codigo);
        c.setNomeDoCliente(nome);
        c.setEmailCliente(email);
        c.setRecebeEmail(recebe);
        return c;
    }
}
