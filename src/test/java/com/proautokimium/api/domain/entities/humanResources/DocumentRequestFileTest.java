package com.proautokimium.api.domain.entities.humanResources;

import com.proautokimium.api.domain.entities.Employee;
import com.proautokimium.api.domain.exceptions.humanResources.InvalidRequestDataException;
import com.proautokimium.api.domain.exceptions.humanResources.InvalidStatusTransitionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * O arquivo anexado a uma resposta. Nunca é apagado: quando chega um novo,
 * o antigo ganha a data em que foi substituído.
 */
class DocumentRequestFileTest {

    private static final LocalDateTime AGORA = LocalDateTime.of(2026, 10, 5, 9, 0);
    private static final LocalDateTime DEPOIS = AGORA.plusDays(1);

    private DocumentRequestRecipient recipient;

    @BeforeEach
    void setUp() {
        DocumentRequest request = DocumentRequest.draft("Envie seu RG", "rita", AGORA);
        recipient = DocumentRequestRecipient.create(request, new Employee(), AGORA);
    }

    private DocumentRequestFile rg() {
        return DocumentRequestFile.create(recipient, "rg", "rg-frente.pdf", "0042/abc-rg-frente.pdf", AGORA);
    }

    @Test
    @DisplayName("criar guarda a resposta, o campo, os nomes e a hora, e nasce como o arquivo atual")
    void createStoresEverything() {
        DocumentRequestFile file = rg();

        assertThat(file.getDocumentRequestRecipient()).isSameAs(recipient);
        assertThat(file.getFieldKey()).isEqualTo("rg");
        assertThat(file.getOriginalFilename()).isEqualTo("rg-frente.pdf");
        assertThat(file.getStoragePath()).isEqualTo("0042/abc-rg-frente.pdf");
        assertThat(file.getUploadedAt()).isEqualTo(AGORA);
        assertThat(file.getReplacedAt()).isNull();
        assertThat(file.getEmployeeDocument()).isNull();
    }

    @Test
    @DisplayName("sem resposta, sem campo, sem nome ou sem caminho: recusado antes do banco")
    void createRefusesMissingData() {
        assertThrows(InvalidRequestDataException.class,
                () -> DocumentRequestFile.create(null, "rg", "rg.pdf", "0042/rg.pdf", AGORA));
        assertThrows(InvalidRequestDataException.class,
                () -> DocumentRequestFile.create(recipient, "  ", "rg.pdf", "0042/rg.pdf", AGORA));
        assertThrows(InvalidRequestDataException.class,
                () -> DocumentRequestFile.create(recipient, "rg", "", "0042/rg.pdf", AGORA));
        assertThrows(InvalidRequestDataException.class,
                () -> DocumentRequestFile.create(recipient, "rg", "rg.pdf", null, AGORA));
    }

    @Test
    @DisplayName("substituir marca a data e deixa de ser o atual")
    void replaceMarksDate() {
        DocumentRequestFile file = rg();

        file.replace(DEPOIS);

        assertThat(file.getReplacedAt()).isEqualTo(DEPOIS);
    }

    @Test
    @DisplayName("substituir duas vezes é recusado, e a primeira data fica")
    void replaceTwiceRefused() {
        DocumentRequestFile file = rg();
        file.replace(AGORA);

        assertThrows(InvalidStatusTransitionException.class, () -> file.replace(DEPOIS));

        assertThat(file.getReplacedAt()).isEqualTo(AGORA);
    }

    @Test
    @DisplayName("vincular guarda o documento do funcionário criado na aprovação")
    void linkToStoresDocument() {
        DocumentRequestFile file = rg();
        EmployeeDocument document = new EmployeeDocument();

        file.linkTo(document);

        assertThat(file.getEmployeeDocument()).isSameAs(document);
    }

    @Test
    @DisplayName("vincular sem documento é recusado")
    void linkToNullRefused() {
        DocumentRequestFile file = rg();

        assertThrows(InvalidRequestDataException.class, () -> file.linkTo(null));
        assertThat(file.getEmployeeDocument()).isNull();
    }

    @Test
    @DisplayName("vincular duas vezes é recusado, e o primeiro vínculo fica")
    void linkToTwiceRefused() {
        DocumentRequestFile file = rg();
        EmployeeDocument first = new EmployeeDocument();
        file.linkTo(first);

        assertThrows(InvalidStatusTransitionException.class, () -> file.linkTo(new EmployeeDocument()));
        assertThat(file.getEmployeeDocument()).isSameAs(first);
    }
}
