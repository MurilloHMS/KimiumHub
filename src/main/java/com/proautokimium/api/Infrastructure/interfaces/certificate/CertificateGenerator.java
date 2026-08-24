package com.proautokimium.api.Infrastructure.interfaces.certificate;

import java.util.List;

public interface CertificateGenerator {

    byte[] generateCertificate(String name);
    byte[] generateCertificatesZip(List<String> names);
}
