package com.proautokimium.api.Infrastructure.services.humanResources;

import com.proautokimium.api.Application.DTOs.humanResources.EmployeeDocument.EmployeeDocumentResponseDTO;
import com.proautokimium.api.Infrastructure.repositories.EmployeeRepository;
import com.proautokimium.api.Infrastructure.repositories.UserRepository;
import com.proautokimium.api.Infrastructure.repositories.humanResources.EmployeeDocumentRepository;
import com.proautokimium.api.Infrastructure.repositories.humanResources.EmployeeDocumentTypeRepository;
import com.proautokimium.api.Infrastructure.services.notification.NotificationService;
import com.proautokimium.api.Infrastructure.services.storage.EmployeeDocumentStorageService;
import com.proautokimium.api.domain.entities.Employee;
import com.proautokimium.api.domain.entities.auth.User;
import com.proautokimium.api.domain.entities.humanResources.EmployeeDocument;
import com.proautokimium.api.domain.entities.humanResources.EmployeeDocumentType;
import com.proautokimium.api.domain.enums.humanResources.EmployeeDocumentStatus;
import com.proautokimium.api.domain.exceptions.humanResources.InvalidRequestDataException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * O vínculo de documento pelo RH.
 *
 * O que estes testes protegem, em ordem de gravidade: o disco nunca fica com
 * arquivo órfão; um documento não substitui o de outro funcionário; e o
 * funcionário é avisado com um link que chega à tela dele.
 */
@ExtendWith(MockitoExtension.class)
class EmployeeDocumentServiceTest {

    private static final ZoneId ZONE = ZoneId.of("America/Sao_Paulo");
    private static final LocalDate TODAY = LocalDate.of(2026, 9, 29);

    @Mock private EmployeeDocumentRepository repository;
    @Mock private EmployeeDocumentTypeRepository typeRepository;
    @Mock private EmployeeRepository employeeRepository;
    @Mock private UserRepository userRepository;
    @Mock private EmployeeDocumentStorageService storage;
    @Mock private NotificationService notificationService;

    private EmployeeDocumentService service;

    private Employee employee;
    private EmployeeDocumentType aso;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(TODAY.atTime(10, 0).atZone(ZONE).toInstant(), ZONE);
        service = new EmployeeDocumentService(repository, typeRepository, employeeRepository, userRepository,
                storage, notificationService, clock);

        employee = new Employee();
        employee.id = UUID.randomUUID();
        employee.setCodParceiro("EMP001");
        employee.setName("Ana Souza");

        aso = EmployeeDocumentType.create("ASO", LocalDateTime.of(2026, 1, 1, 0, 0));
        aso.id = UUID.randomUUID();
    }

    private static MockMultipartFile pdf(String name) {
        return new MockMultipartFile("file", name, "application/pdf", "conteudo".getBytes());
    }

    /** O caminho feliz do vínculo, com tudo que ele precisa encontrar. */
    private void employeeAndTypeExist() throws Exception {
        when(employeeRepository.findById(employee.getId())).thenReturn(Optional.of(employee));
        when(typeRepository.findById(aso.getId())).thenReturn(Optional.of(aso));
        when(storage.save(any(), eq("EMP001"), anyString())).thenReturn("EMP001/uuid-aso.pdf");
    }

    private EmployeeDocument stored(Employee owner) {
        EmployeeDocument document = new EmployeeDocument();
        document.id = UUID.randomUUID();
        document.setEmployee(owner);
        document.setTitle("ASO 2025");
        document.setStoragePath("EMP001/antigo.pdf");
        return document;
    }

    @Nested
    @DisplayName("vincular")
    class Link {

        @Test
        @DisplayName("grava tipo, vencimento, quem enviou e o tipo do arquivo, e avisa o funcionário")
        void gravaEAvisa() throws Exception {
            employeeAndTypeExist();
            when(repository.save(any(EmployeeDocument.class))).thenAnswer(inv -> inv.getArgument(0));
            User linked = mock(User.class);
            when(linked.getLogin()).thenReturn("ana.login");
            when(userRepository.findByEmployee_Id(employee.getId())).thenReturn(Optional.of(linked));

            EmployeeDocumentResponseDTO response = service.link(employee.getId(), aso.getId(), "ASO periódico",
                    TODAY.plusYears(1), null, pdf("aso.pdf"), "rh.maria");

            ArgumentCaptor<EmployeeDocument> saved = ArgumentCaptor.forClass(EmployeeDocument.class);
            verify(repository).save(saved.capture());
            assertThat(saved.getValue().getType()).isSameAs(aso);
            assertThat(saved.getValue().getDueDate()).isEqualTo(TODAY.plusYears(1));
            assertThat(saved.getValue().getUploadedBy()).isEqualTo("rh.maria");
            assertThat(saved.getValue().getContentType()).isEqualTo("application/pdf");
            assertThat(response.status()).isEqualTo(EmployeeDocumentStatus.VALID);
            assertThat(response.employeeName()).isEqualTo("Ana Souza");

            // O link leva à tela do funcionário, e não ao hub de Documentos.
            verify(notificationService).notify(eq("ana.login"), any(), any(), any(), eq("/documentos/rh/documents"));
        }

        @Test
        @DisplayName("sem título, usa o nome do tipo")
        void tituloPadraoEOTipo() throws Exception {
            employeeAndTypeExist();
            when(repository.save(any(EmployeeDocument.class))).thenAnswer(inv -> inv.getArgument(0));

            EmployeeDocumentResponseDTO response = service.link(employee.getId(), aso.getId(), "  ",
                    null, null, pdf("aso.pdf"), "rh.maria");

            assertThat(response.title()).isEqualTo("ASO");
        }

        /**
         * **Nada no disco antes de tudo estar conferido.** Arquivo recusado não
         * pode nem chegar ao `save` — senão cada tentativa errada deixa um arquivo.
         */
        @ParameterizedTest
        @ValueSource(strings = {"virus.exe", "planilha.xlsx", "sem-extensao"})
        @DisplayName("recusa o que não é PDF, JPG ou PNG, sem gravar nada")
        void recusaTipoDeArquivo(String name) throws Exception {
            assertThatThrownBy(() -> service.link(employee.getId(), aso.getId(), null, null, null,
                    pdf(name), "rh.maria"))
                    .isInstanceOf(InvalidRequestDataException.class);
            verify(storage, never()).save(any(), anyString(), anyString());
        }

        @Test
        @DisplayName("aceita JPG e PNG, em qualquer caixa")
        void aceitaImagem() throws Exception {
            employeeAndTypeExist();
            when(repository.save(any(EmployeeDocument.class))).thenAnswer(inv -> inv.getArgument(0));

            EmployeeDocumentResponseDTO response = service.link(employee.getId(), aso.getId(), null, null, null,
                    new MockMultipartFile("file", "FOTO.JPG", "image/jpeg", new byte[]{1}), "rh.maria");

            assertThat(response.contentType()).isEqualTo("image/jpeg");
        }

        @Test
        @DisplayName("recusa arquivo vazio e acima de 10 MB")
        void recusaVazioEGrande() {
            MockMultipartFile vazio = new MockMultipartFile("file", "a.pdf", "application/pdf", new byte[0]);
            MockMultipartFile grande = new MockMultipartFile("file", "a.pdf", "application/pdf",
                    new byte[(int) EmployeeDocumentService.MAX_FILE_BYTES + 1]);

            assertThatThrownBy(() -> service.link(employee.getId(), aso.getId(), null, null, null, vazio, "rh"))
                    .isInstanceOf(InvalidRequestDataException.class);
            assertThatThrownBy(() -> service.link(employee.getId(), aso.getId(), null, null, null, grande, "rh"))
                    .isInstanceOf(InvalidRequestDataException.class);
        }

        @Test
        @DisplayName("tipo desativado não recebe documento novo")
        void tipoInativo() throws Exception {
            aso.deactivate();
            when(employeeRepository.findById(employee.getId())).thenReturn(Optional.of(employee));
            when(typeRepository.findById(aso.getId())).thenReturn(Optional.of(aso));

            assertThatThrownBy(() -> service.link(employee.getId(), aso.getId(), null, null, null,
                    pdf("aso.pdf"), "rh"))
                    .isInstanceOf(InvalidRequestDataException.class);
            verify(storage, never()).save(any(), anyString(), anyString());
        }

        @Test
        @DisplayName("substituir marca o antigo com o novo")
        void substitui() throws Exception {
            employeeAndTypeExist();
            EmployeeDocument old = stored(employee);
            when(repository.findById(old.getId())).thenReturn(Optional.of(old));
            when(repository.save(any(EmployeeDocument.class))).thenAnswer(inv -> inv.getArgument(0));

            EmployeeDocumentResponseDTO response = service.link(employee.getId(), aso.getId(), null, null,
                    old.getId(), pdf("aso.pdf"), "rh");

            assertThat(old.getReplacedBy()).isNotNull();
            assertThat(old.getReplacedBy().getTitle()).isEqualTo(response.title());
            assertThat(old.statusOn(TODAY)).isEqualTo(EmployeeDocumentStatus.REPLACED);
        }

        /**
         * **O teste que justifica a compensação.** A recusa vem DEPOIS do arquivo
         * gravado (a entidade só confere o dono quando o novo tem id); sem o
         * `catch`, o arquivo ficaria no disco sem linha nenhuma apontando para ele.
         */
        @Test
        @DisplayName("substituir documento de outro funcionário é recusado, e o arquivo gravado é apagado")
        void substituirDeOutroApagaOArquivo() throws Exception {
            employeeAndTypeExist();
            Employee other = new Employee();
            other.id = UUID.randomUUID();
            EmployeeDocument othersDocument = stored(other);
            when(repository.findById(othersDocument.getId())).thenReturn(Optional.of(othersDocument));
            when(repository.save(any(EmployeeDocument.class))).thenAnswer(inv -> inv.getArgument(0));

            assertThatThrownBy(() -> service.link(employee.getId(), aso.getId(), null, null,
                    othersDocument.getId(), pdf("aso.pdf"), "rh"))
                    .isInstanceOf(InvalidRequestDataException.class);

            verify(storage).delete("EMP001/uuid-aso.pdf");
            assertThat(othersDocument.getReplacedBy()).isNull();
            verify(notificationService, never()).notify(any(), any(), any(), any(), any());
        }
    }

    @Test
    @DisplayName("o filtro de situação usa a regra calculada no dia")
    void filtraPorSituacao() {
        EmployeeDocument expired = stored(employee);
        expired.setDueDate(TODAY.minusDays(1));
        EmployeeDocument valid = stored(employee);
        valid.setDueDate(TODAY.plusYears(1));
        when(repository.search(null, null)).thenReturn(List.of(expired, valid));

        List<EmployeeDocumentResponseDTO> result = service.search(null, null, EmployeeDocumentStatus.EXPIRED);

        assertThat(result).extracting(EmployeeDocumentResponseDTO::id).containsExactly(expired.getId());
        assertThat(result.get(0).daysUntilDue()).isEqualTo(-1L);
    }

    /** Fora de transação (o teste), o arquivo sai na hora; dentro, só depois do commit. */
    @Test
    @DisplayName("excluir apaga a linha e o arquivo")
    void exclui() throws Exception {
        EmployeeDocument document = stored(employee);
        when(repository.findById(document.getId())).thenReturn(Optional.of(document));

        service.delete(document.getId());

        verify(repository).delete(document);
        verify(storage).delete("EMP001/antigo.pdf");
    }

    @Nested
    @DisplayName("quem acessa")
    class Access {

        @Test
        void rhSempreAcessa() {
            assertThat(service.canAccess(stored(employee), "qualquer", true)).isTrue();
        }

        @Test
        void donoAcessa() {
            when(userRepository.findByLoginWithEmployee("ana.login")).thenReturn(Optional.empty());
            when(employeeRepository.findByUsername("ana.login")).thenReturn(Optional.of(employee));

            assertThat(service.canAccess(stored(employee), "ana.login", false)).isTrue();
        }

        @Test
        void terceiroNaoAcessa() {
            Employee other = new Employee();
            other.id = UUID.randomUUID();
            when(userRepository.findByLoginWithEmployee("joao.login")).thenReturn(Optional.empty());
            when(employeeRepository.findByUsername("joao.login")).thenReturn(Optional.of(other));

            assertThat(service.canAccess(stored(employee), "joao.login", false)).isFalse();
        }
    }
}
