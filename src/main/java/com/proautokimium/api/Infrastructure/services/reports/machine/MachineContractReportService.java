package com.proautokimium.api.Infrastructure.services.reports.machine;

import com.proautokimium.api.Infrastructure.services.reports.machine.dtos.MaquinaDTO;
import com.proautokimium.api.Infrastructure.services.reports.machine.dtos.MatrizDTO;
import com.proautokimium.api.Infrastructure.services.reports.machine.dtos.ReciboLocacaoDTO;
import com.proautokimium.api.Infrastructure.services.reports.machine.dtos.UnidadeDTO;
import com.proautokimium.api.Infrastructure.utils.ValorExtensoUtil;
import com.proautokimium.api.domain.models.MachineContract;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class MachineContractReportService {

    private static final DecimalFormat BRL_FORMAT = new DecimalFormat(
            "R$ #,##0.00", new DecimalFormatSymbols(new Locale("pt", "BR"))
    );

    public ReciboLocacaoDTO build(
            List<MachineContract> contracts,
            LocalDate vencimento
    ) {
        Map<String, List<MachineContract>> matrizMap =
                contracts.stream()
                        .collect(Collectors.groupingBy(
                                MachineContract::getCodigoMatriz,
                                LinkedHashMap::new,
                                Collectors.toList()
                        ));

        List<MatrizDTO> matrizes = new ArrayList<>();

        for (Map.Entry<String, List<MachineContract>> matrizEntry : matrizMap.entrySet()) {

            List<MachineContract> matrizContracts = matrizEntry.getValue();
            MachineContract firstMatriz = matrizContracts.get(0);

            Map<String, List<MachineContract>> unidadeMap =
                    matrizContracts.stream()
                            .collect(Collectors.groupingBy(
                                    MachineContract::getNumeroNota,
                                    LinkedHashMap::new,
                                    Collectors.toList()
                            ));

            List<UnidadeDTO> unidades = new ArrayList<>();

            for (Map.Entry<String, List<MachineContract>> unidadeEntry : unidadeMap.entrySet()) {

                List<MachineContract> unidadeContracts = unidadeEntry.getValue();
                MachineContract firstUnidade = unidadeContracts.get(0);

                // ── Máquinas ──────────────────────────────────────────────
                List<MaquinaDTO> maquinas =
                        unidadeContracts.stream()
                                .map(item -> {
                                    MaquinaDTO m = new MaquinaDTO();
                                    m.setCodProd(item.getCodigoProduto());
                                    m.setDescrprod(item.getDescricaoProduto());
                                    m.setVlrunit(BigDecimal.valueOf(item.getVlrUnitario()));
                                    m.setObservacao(item.getObservacao());
                                    return m;
                                })
                                .toList();

                BigDecimal vlrDesdob = BigDecimal.valueOf(firstUnidade.getVlrDesdobramento());
                int qtd = maquinas.size();

                // ── Textos do recibo ──────────────────────────────────────
                String resumoMaquinas = maquinas.stream()
                        .map(MaquinaDTO::getDescrprod)
                        .collect(Collectors.joining(", "));

                boolean plural = qtd > 1;
                String qtdExtenso = qtd == 1 ? "Uma" : String.valueOf(qtd);

                String textoIntro = String.format(
                        "Refere-se à locação de %d (%s) %s de Lavar Louça – %s - " +
                                "locad%s pela empresa PROAUTO INDÚSTRIA QUÍMICA EIRELI."
                                ,
                        qtd,
                        qtdExtenso,
                        plural ? "Máquinas" : "Máquina",
                        resumoMaquinas,
                        plural ? "as" : "a"
                );

                String textoInstalacao = String.format(
                        "%s máquin%s está%s instalad%s na empresa %s com " +
                                "endereço de entrega: %s.",
                        plural ? "As" : "A",
                        plural ? "as" : "a",
                        plural ? "o" : "",
                        plural ? "as" : "a",
                        firstUnidade.getNomeParceiro(),
                        firstUnidade.getEnderecoEntrega()
                );

                String vlrFormatado  = BRL_FORMAT.format(vlrDesdob);
                String vlrExtenso    = ValorExtensoUtil.converter(vlrDesdob);

                String textoValor = String.format(
                        "O valor da locação é de %s (%s) com vencimento em " +
                                " %s ",
                        vlrFormatado,
                        vlrExtenso,
                        vencimento
                );

                // ── Monta DTO ─────────────────────────────────────────────
                UnidadeDTO unidade = new UnidadeDTO();
                unidade.setNumnota(firstUnidade.getNumeroNota());
                unidade.setNomeparc(firstUnidade.getNomeParceiro());
                unidade.setCgcCpf(firstUnidade.getDocumento());
                unidade.setEntrega(firstUnidade.getEnderecoEntrega());
                unidade.setVlrDesdob(vlrDesdob);
                unidade.setMaquinas(maquinas);
                unidade.setQuantidadeMaquinas(qtd);
                unidade.setVlrFormatado(vlrFormatado);
                unidade.setTextoIntro(textoIntro);
                unidade.setTextoInstalacao(textoInstalacao);
                unidade.setTextoValor(textoValor);

                unidades.add(unidade);
            }

            BigDecimal totalMatriz =
                    unidades.stream()
                            .map(UnidadeDTO::getVlrDesdob)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

            MatrizDTO matriz = new MatrizDTO();
            matriz.setCodMatriz(firstMatriz.getCodigoMatriz());
            matriz.setNomeMatriz(firstMatriz.getNomeMatriz());
            matriz.setTotalMatriz(totalMatriz);
            matriz.setUnidades(unidades);

            matrizes.add(matriz);
        }

        String mesReferencia = vencimento.getMonth().getDisplayName(TextStyle.FULL, new Locale("pt", "BR"));

        BigDecimal totalGeral =
                matrizes.stream()
                        .map(MatrizDTO::getTotalMatriz)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

        ReciboLocacaoDTO dto = new ReciboLocacaoDTO();
        dto.setMesReferencia(mesReferencia);
        dto.setVencimento(vencimento.format(DateTimeFormatter.ofPattern("dd 'de' MMMM 'de' yyyy", new Locale("pt", "BR"))));
        dto.setDataEmissao(
                LocalDate.now().format(DateTimeFormatter.ofPattern("dd 'de' MMMM 'de' yyyy", new Locale("pt", "BR")))
        );
        dto.setTotalGeral(totalGeral);
        dto.setMatrizes(matrizes);

        return dto;
    }
}