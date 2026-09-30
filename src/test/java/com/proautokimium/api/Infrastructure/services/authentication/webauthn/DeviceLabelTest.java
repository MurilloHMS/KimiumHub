package com.proautokimium.api.Infrastructure.services.authentication.webauthn;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

/** O nome que o Perfil e o RH mostram. A ordem importa: o Edge e o Samsung também dizem "Chrome". */
class DeviceLabelTest {

    @ParameterizedTest(name = "{1}")
    @CsvSource(delimiter = '|', value = {
            "Mozilla/5.0 (Linux; Android 14; SM-A546E) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/129.0 Mobile Safari/537.36 | Android · Chrome",
            "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) SamsungBrowser/25.0 Chrome/121.0 Mobile Safari/537.36 | Android · Samsung Internet",
            "Mozilla/5.0 (iPhone; CPU iPhone OS 18_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/18.0 Mobile/15E148 Safari/604.1 | iPhone · Safari",
            "Mozilla/5.0 (iPhone; CPU iPhone OS 18_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) CriOS/129.0 Mobile/15E148 Safari/604.1 | iPhone · Chrome",
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/129.0 Safari/537.36 Edg/129.0 | Windows · Edge",
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:131.0) Gecko/20100101 Firefox/131.0 | Windows · Firefox",
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 14_6) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/18.0 Safari/605.1.15 | Mac · Safari",
    })
    void labels(String userAgent, String expected) {
        assertThat(DeviceLabel.from(userAgent)).isEqualTo(expected);
    }

    @org.junit.jupiter.api.Test
    void withoutUserAgent() {
        assertThat(DeviceLabel.from(null)).isEqualTo("Aparelho");
    }
}
