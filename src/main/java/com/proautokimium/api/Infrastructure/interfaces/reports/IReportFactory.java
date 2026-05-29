package com.proautokimium.api.Infrastructure.interfaces.reports;

import net.sf.jasperreports.engine.JRDataSource;

import java.util.Map;

public interface IReportFactory {
    byte[] generatePdf(Map<String, Object> params, JRDataSource dataSource, String reportLocation);

    byte[] generateImage(Map<String, Object> params, String reportLocation);
    byte [] generatePdf(Map<String, Object> params, String reportLocation);
}
