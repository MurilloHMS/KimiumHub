package com.proautokimium.api.Infrastructure.services.partner;

import com.proautokimium.api.Application.DTOs.partners.EmployeeSiteAccess;
import com.proautokimium.api.Application.DTOs.partners.PendingSiteAccessRowDTO;
import com.proautokimium.api.Infrastructure.repositories.EmployeeRepository;
import com.proautokimium.api.Infrastructure.repositories.humanResources.CareerHistoryRepository;
import com.proautokimium.api.domain.entities.Employee;
import com.proautokimium.api.domain.enums.SiteAccess;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * O relatório dos pendentes: quem entra, em que ordem, com que texto, e que os
 * dois arquivos saem de verdade.
 */
class PendingSiteAccessReportServiceTest {

    private static final ZoneId SP = ZoneId.of("America/Sao_Paulo");

    private final EmployeeRepository employees = mock(EmployeeRepository.class);
    private final CareerHistoryRepository careerHistories = mock(CareerHistoryRepository.class);
    private final SiteAccessResolver resolver = mock(SiteAccessResolver.class);
    private final PendingSiteAccessReportService service = new PendingSiteAccessReportService(
            employees, careerHistories, resolver, new PendingSiteAccessExcelWriter(),
            Clock.fixed(Instant.parse("2026-10-08T15:00:00Z"), SP));

    private final Map<UUID, EmployeeSiteAccess> situacoes = new HashMap<>();

    private Employee funcionario(String nome, boolean ativo, SiteAccess situacao, LocalDateTime pedido) {
        Employee employee = new Employee();
        employee.id = UUID.randomUUID();
        employee.setName(nome);
        employee.setCodParceiro(nome.substring(0, 3).toUpperCase());
        employee.setAtivo(ativo);
        situacoes.put(employee.id, new EmployeeSiteAccess(situacao, null, pedido));
        return employee;
    }

    private void equipe(Employee... pessoas) {
        when(employees.findAll()).thenReturn(List.of(pessoas));
        when(careerHistories.findLatestPerEmployee()).thenReturn(List.of());
        when(resolver.resolve(anyList())).thenReturn(situacoes);
    }

    @Test
    @DisplayName("só entram os ativos pendentes, em ordem de nome")
    void soAtivosPendentes() {
        equipe(
                funcionario("Diego Martins", true, SiteAccess.PENDING, null),
                funcionario("Ricardo Lima", true, SiteAccess.ACTIVE, null),
                funcionario("Carlos Dias", true, SiteAccess.BLOCKED, null),
                funcionario("João Antigo", false, SiteAccess.PENDING, null),
                funcionario("Bruna Teixeira", true, SiteAccess.PENDING, null));

        assertThat(service.rows()).extracting(PendingSiteAccessRowDTO::getName)
                .containsExactly("Bruna Teixeira", "Diego Martins");
    }

    @Test
    @DisplayName("o detalhe diz se a pessoa pediu o código ou nunca entrou")
    void detalhe() {
        equipe(
                funcionario("Bruna Teixeira", true, SiteAccess.PENDING, LocalDateTime.of(2026, 10, 3, 9, 12)),
                funcionario("Diego Martins", true, SiteAccess.PENDING, null));

        assertThat(service.rows()).extracting(PendingSiteAccessRowDTO::getDetail)
                .containsExactly("pediu o código em 03/10/2026, não concluiu", "nunca entrou");
    }

    @Test
    @DisplayName("a planilha sai com o cabeçalho e uma linha por pessoa")
    void planilha() throws Exception {
        equipe(funcionario("Bruna Teixeira", true, SiteAccess.PENDING, null),
               funcionario("Diego Martins", true, SiteAccess.PENDING, null));

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(service.excel()))) {
            Sheet sheet = workbook.getSheetAt(0);
            assertThat(sheet.getRow(0).getCell(1).getStringCellValue()).isEqualTo("Nome");
            assertThat(sheet.getRow(1).getCell(1).getStringCellValue()).isEqualTo("Bruna Teixeira");
            assertThat(sheet.getLastRowNum()).isEqualTo(2);
        }
    }

    /** Carrega o `.jasper` commitado, como em produção, e preenche de verdade. */
    @Test
    @DisplayName("o PDF sai, com gente e sem ninguém pendente")
    void pdf() {
        equipe(funcionario("Bruna Teixeira", true, SiteAccess.PENDING, null));
        assertThat(new String(service.pdf(), 0, 5, StandardCharsets.US_ASCII)).isEqualTo("%PDF-");

        situacoes.clear();
        equipe();
        assertThat(new String(service.pdf(), 0, 5, StandardCharsets.US_ASCII)).isEqualTo("%PDF-");
    }

    @Test
    @DisplayName("o nome do arquivo leva a data de hoje, em São Paulo")
    void nomeDoArquivo() {
        assertThat(service.fileName("xlsx")).isEqualTo("funcionarios-sem-acesso-2026-10-08.xlsx");
    }
}
