package com.proautokimium.api.Infrastructure.services.reports.machine;

import com.proautokimium.api.Application.DTOs.machine.*;
import com.proautokimium.api.Infrastructure.utils.ValorExtensoUtil;
import com.proautokimium.api.domain.models.MachineContract;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class MachineContractReportService {

    private static final DecimalFormat BRL_FORMAT = new DecimalFormat(
            "R$ #,##0.00", new DecimalFormatSymbols(new Locale("pt", "BR"))
    );
    private static final DateTimeFormatter DATA_EMISSAO_FMT =
            DateTimeFormatter.ofPattern("dd 'de' MMMM 'de' yyyy", new Locale("pt", "BR"));

    public List<MatrizPreviewDTO> buildPreview(List<MachineContract> contracts) {
        return agruparPorMatriz(contracts).entrySet().stream()
                .map(entry -> {
                    List<MachineContract> linhas = entry.getValue();

                    long totalUnidades = linhas.stream()
                            .map(MachineContract::getNumeroNota)
                            .distinct().count();

                    BigDecimal total = linhas.stream()
                            .collect(Collectors.groupingBy(MachineContract::getNumeroNota))
                            .values().stream()
                            .map(u -> BigDecimal.valueOf(u.get(0).getVlrDesdobramento()))
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    MatrizPreviewDTO p = new MatrizPreviewDTO();
                    p.setCodMatriz(entry.getKey());
                    p.setNomeMatriz(linhas.get(0).getNomeMatriz());
                    p.setTotalUnidades((int) totalUnidades);
                    p.setTotalMaquinas(linhas.size());
                    p.setTotalMatriz(total);
                    return p;
                })
                .collect(Collectors.toList());
    }

    public ReciboLocacaoDTO build(
            List<MachineContract> contracts,
            String mesReferencia,
            Map<String, String> vencimentos
    ) {
        List<MatrizDTO> matrizes = new ArrayList<>();

        for (Map.Entry<String, List<MachineContract>> entry : agruparPorMatriz(contracts).entrySet()) {

            String codMatriz = entry.getKey();
            List<MachineContract> linhas = entry.getValue();

            String vencimento = vencimentos.getOrDefault(codMatriz, "Não informado");
            List<UnidadeDTO> unidades = buildUnidades(linhas);

            BigDecimal totalMatriz = unidades.stream()
                    .map(UnidadeDTO::getVlrDesdob)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            MatrizDTO matriz = new MatrizDTO();
            matriz.setCodMatriz(codMatriz);
            matriz.setNomeMatriz(linhas.get(0).getNomeMatriz());
            matriz.setTotalMatriz(totalMatriz);
            matriz.setVencimento(vencimento);
            matriz.setUnidades(unidades);

            matrizes.add(matriz);
        }

        BigDecimal totalGeral = matrizes.stream()
                .map(MatrizDTO::getTotalMatriz)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        ReciboLocacaoDTO dto = new ReciboLocacaoDTO();
        dto.setMesReferencia(mesReferencia);
        dto.setDataEmissao(LocalDate.now().format(DATA_EMISSAO_FMT));
        dto.setTotalGeral(totalGeral);
        dto.setMatrizes(matrizes);
        return dto;
    }

    private LinkedHashMap<String, List<MachineContract>> agruparPorMatriz(List<MachineContract> contracts) {
        return contracts.stream()
                .collect(Collectors.groupingBy(
                        MachineContract::getCodigoMatriz,
                        LinkedHashMap::new,
                        Collectors.toList()
                ));
    }

    private List<UnidadeDTO> buildUnidades(List<MachineContract> matrizContracts) {

        Map<String, List<MachineContract>> porNota = matrizContracts.stream()
                .collect(Collectors.groupingBy(
                        MachineContract::getNumeroNota,
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        List<UnidadeDTO> unidades = new ArrayList<>();

        for (List<MachineContract> linhas : porNota.values()) {

            MachineContract first = linhas.get(0);
            boolean plural = linhas.size() > 1;

            List<MaquinaDTO> maquinas = linhas.stream()
                    .map(c -> {
                        MaquinaDTO m = new MaquinaDTO();
                        m.setCodProd(c.getCodigoProduto());
                        m.setDescrprod(c.getDescricaoProduto());
                        m.setVlrunit(BigDecimal.valueOf(c.getVlrUnitario()));
                        m.setObservacao(c.getObservacao());
                        return m;
                    })
                    .toList();

            BigDecimal vlrDesdob  = BigDecimal.valueOf(first.getVlrDesdobramento());
            String vlrFormatado   = BRL_FORMAT.format(vlrDesdob);
            String resumoMaquinas = maquinas.stream()
                    .map(MaquinaDTO::getDescrprod)
                    .collect(Collectors.joining(", "));

            String textoIntro = String.format(
                    "Refere-se à locação de %d (%s) %s – %s - " +
                            "locad%s pela empresa PROAUTO INDUSTRIA QUIMICA EIRELI.",
                    linhas.size(),
                    plural ? ValorExtensoUtil.UNIDADES_ESPECIAL[linhas.size()] : "Uma",
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
                    first.getNomeParceiro(),
                    first.getEnderecoEntrega()
            );

            String textoValor = String.format(
                    "O valor da locação é de %s (%s) " +
                            " ",
                    vlrFormatado, ValorExtensoUtil.converter(vlrDesdob)
            );

            UnidadeDTO u = new UnidadeDTO();
            u.setNumnota(first.getNumeroNota());
            u.setNomeparc(first.getNomeParceiro());
            u.setCgcCpf(first.getDocumento());
            u.setEntrega(first.getEnderecoEntrega());
            u.setVlrDesdob(vlrDesdob);
            u.setVlrFormatado(vlrFormatado);
            u.setMaquinas(maquinas);
            u.setQuantidadeMaquinas(linhas.size());
            u.setTextoIntro(textoIntro);
            u.setTextoInstalacao(textoInstalacao);
            u.setTextoValor(textoValor);
            unidades.add(u);
        }

        return unidades;
    }
}