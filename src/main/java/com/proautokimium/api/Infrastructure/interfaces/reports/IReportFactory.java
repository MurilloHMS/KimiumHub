package com.proautokimium.api.Infrastructure.interfaces.reports;

import java.util.Map;

public interface IReportFactory {
    byte[] generateImage(Map<String, Object> params, String reportLocation);
    byte [] generatePdf(Map<String, Object> params, String reportLocation);
}
