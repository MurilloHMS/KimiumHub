package com.proautokimium.api.Infrastructure.services.reports.machine;

import com.proautokimium.api.Infrastructure.services.reports.machine.dtos.MaquinaDTO;
import com.proautokimium.api.Infrastructure.services.reports.machine.dtos.MatrizDTO;
import com.proautokimium.api.Infrastructure.services.reports.machine.dtos.ReciboLocacaoDTO;
import com.proautokimium.api.Infrastructure.services.reports.machine.dtos.UnidadeDTO;
import com.proautokimium.api.domain.models.MachineContract;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class MachineContractReportService {

    public ReciboLocacaoDTO build(
            List<MachineContract> contracts,
            String mesReferencia,
            String vencimento
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

                List<MaquinaDTO> maquinas =
                        unidadeContracts.stream()
                                .map(item -> {
                                    MaquinaDTO maquina = new MaquinaDTO();

                                    maquina.setCodProd(item.getCodigoProduto());
                                    maquina.setDescrprod(item.getDescricaoProduto());
                                    maquina.setVlrunit(
                                            BigDecimal.valueOf(item.getVlrUnitario())
                                    );
                                    maquina.setObservacao(item.getObservacao());

                                    return maquina;
                                })
                                .toList();

                UnidadeDTO unidade = new UnidadeDTO();

                unidade.setNumnota(firstUnidade.getNumeroNota());
                unidade.setNomeparc(firstUnidade.getNomeParceiro());
                unidade.setCgcCpf(firstUnidade.getDocumento());
                unidade.setEntrega(firstUnidade.getEnderecoEntrega());

                unidade.setVlrDesdob(
                        BigDecimal.valueOf(firstUnidade.getVlrDesdobramento())
                );

                unidade.setMaquinas(maquinas);

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

        BigDecimal totalGeral =
                matrizes.stream()
                        .map(MatrizDTO::getTotalMatriz)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

        ReciboLocacaoDTO dto = new ReciboLocacaoDTO();

        dto.setMesReferencia(mesReferencia);
        dto.setVencimento(vencimento);

        dto.setDataEmissao(
                LocalDate.now()
                        .format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
        );

        dto.setTotalGeral(totalGeral);
        dto.setMatrizes(matrizes);

        return dto;
    }
}
