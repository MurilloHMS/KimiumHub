package com.proautokimium.api.Infrastructure.services.holerite;

import com.proautokimium.api.Infrastructure.exceptions.holerite.InvalidPayslipTypeException;
import com.proautokimium.api.Infrastructure.repositories.EmployeeRepository;
import com.proautokimium.api.Infrastructure.repositories.HoleriteDocumentoRepository;
import com.proautokimium.api.Infrastructure.repositories.PayslipTypeRepository;
import com.proautokimium.api.Infrastructure.repositories.UserRepository;
import com.proautokimium.api.Infrastructure.services.notification.NotificationService;
import com.proautokimium.api.Infrastructure.services.storage.HoleriteStorageService;
import com.proautokimium.api.Infrastructure.services.pdf.holerith.HolerithExtractorService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** Tipo desconhecido é recusado antes de qualquer página virar holerite gravado. */
class HoleriteServiceTipoTest {

    @Test
    @DisplayName("enviar com tipo que não está no cadastro: 400 com o código, e nada é lido nem gravado")
    void unknownTypeRefusedBeforeAnything() {
        PayslipTypeRepository types = mock(PayslipTypeRepository.class);
        when(types.findByCode(anyString())).thenReturn(Optional.empty());
        HolerithExtractorService extractor = mock(HolerithExtractorService.class);
        HoleriteStorageService storage = mock(HoleriteStorageService.class);
        HoleriteDocumentoRepository repository = mock(HoleriteDocumentoRepository.class);
        HoleriteService service = new HoleriteService(extractor, storage, mock(EmployeeRepository.class), repository,
                mock(UserRepository.class), mock(NotificationService.class),
                new PayslipTypeService(types, Clock.systemDefaultZone()), Clock.systemDefaultZone());
        MockMultipartFile pdf = new MockMultipartFile("file", "plr.pdf", "application/pdf", new byte[]{'%', 'P', 'D', 'F'});

        assertThatThrownBy(() -> service.vincular(pdf, LocalDate.of(2026, 9, 1), "BONUS_X"))
                .isInstanceOf(InvalidPayslipTypeException.class).hasMessageContaining("BONUS_X");
        assertThatThrownBy(() -> service.preview(pdf, LocalDate.of(2026, 9, 1), "BONUS_X"))
                .isInstanceOf(InvalidPayslipTypeException.class);
        verifyNoInteractions(extractor, storage, repository);
    }
}
