package com.proautokimium.api.Infrastructure.services.reports.guide;

import com.proautokimium.api.Application.DTOs.guide.GuideReportRequestDTO;
import com.proautokimium.api.Application.DTOs.guide.GuideReportRowDTO;
import com.proautokimium.api.Infrastructure.exceptions.product.ProductNotFoundException;
import com.proautokimium.api.Infrastructure.factories.ReportFactory;
import com.proautokimium.api.Infrastructure.repositories.ProductWebSiteRepository;
import com.proautokimium.api.Infrastructure.services.storage.EquipmentImageStorageService;
import com.proautokimium.api.Infrastructure.services.storage.ProductImageStorageService;
import com.proautokimium.api.Infrastructure.utils.ColorCircleRenderer;
import com.proautokimium.api.domain.entities.EquipmentGuide;
import com.proautokimium.api.domain.entities.ProductWebsite;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Serviço responsável por gerar o "Guia de Utilização" em PDF.
 *
 * <p>
 * Fluxo:
 * <ol>
 *   <li>Recebe a lista de IDs dos produtos selecionados pelo usuário.</li>
 *   <li>Busca cada {@link ProductWebsite} pelo ID, respeitando a ordem enviada.</li>
 *   <li>Converte cada produto em um {@link GuideReportRowDTO}
 *       resolvendo as imagens via {@link ProductImageStorageService}
 *       e {@link EquipmentImageStorageService}.</li>
 *   <li>Monta os parâmetros do relatório (logos, título).</li>
 *   <li>Delega a geração do PDF à {@link ReportFactory}.</li>
 * </ol>
 * </p>
 */
@Service
public class GuideReportService {
    private final Logger logger = LoggerFactory.getLogger(GuideReportService.class);

    private static final String REPORT_LOCATION = "guide/guia_utilizacao.jasper";

    private final ProductWebSiteRepository productRepository;
    private final ProductImageStorageService productImageStorage;
    private final EquipmentImageStorageService equipmentImageStorage;
    private final ReportFactory reportFactory;

    @Value("${report.logo.empresa:classpath:/static/images/logo_empresa.png}")
    private String logoEmpresaPath;

    public GuideReportService(
            ProductWebSiteRepository productRepository,
            ProductImageStorageService productImageStorage,
            EquipmentImageStorageService equipmentImageStorage,
            ReportFactory reportFactory
    ) {
        this.productRepository     = productRepository;
        this.productImageStorage   = productImageStorage;
        this.equipmentImageStorage = equipmentImageStorage;
        this.reportFactory         = reportFactory;
    }

    /**
     * Gera o Guia de Utilização em PDF com os produtos selecionados pelo usuário.
     *
     * @param request     DTO com título e lista de IDs dos produtos na ordem desejada
     * @param logoCliente Stream do logo do cliente (pode ser null)
     * @return PDF como array de bytes
     * @throws ProductNotFoundException se algum ID não for encontrado
     */
    public byte[] gerarGuia(GuideReportRequestDTO request, InputStream logoCliente) {
        List<GuideReportRowDTO> rows = request.productIds().stream()
                .map(id -> productRepository.findById(id)
                        .orElseThrow(ProductNotFoundException::new))
                .map(this::toRow)
                .toList();

        JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(rows);

        InputStream aventalImage = getClass().getResourceAsStream("/templates/images/icones-guia/avental.png");
        InputStream botaImage = getClass().getResourceAsStream("/templates/images/icones-guia/bota.png");
        InputStream luvaImage = getClass().getResourceAsStream("/templates/images/icones-guia/luva.png");
        InputStream mascaraImage = getClass().getResourceAsStream("/templates/images/icones-guia/mascara.png");
        InputStream oculosImage = getClass().getResourceAsStream("/templates/images/icones-guia/oculos.png");
        InputStream toucaImage = getClass().getResourceAsStream("/templates/images/icones-guia/touca.png");

        Map<String, Object> params = new HashMap<>();
        params.put("TITULO_GUIA",  request.tituloGuia().toUpperCase());
        params.put("LOGO_CLIENTE", logoCliente);
        params.put("LOGO_EMPRESA", resolveLogoEmpresa());
        params.put("LOGO_AVENTAL", aventalImage);
        params.put("LOGO_LUVA", luvaImage);
        params.put("LOGO_BOTA", botaImage);
        params.put("LOGO_MASCARA", mascaraImage);
        params.put("LOGO_OCULOS", oculosImage);
        params.put("LOGO_TOUCA", toucaImage);

        return reportFactory.generatePdf(params, dataSource, REPORT_LOCATION);
    }

    // ── Conversão produto → DTO ─────────────────────────────────────────────

    private GuideReportRowDTO toRow(ProductWebsite p) {
        String primeiraCorHex = (p.getCores() != null && !p.getCores().isEmpty())
                ? p.getCores().get(0)
                : null;
        return new GuideReportRowDTO(
                p.getName(),
                p.getSystemCode(),
                resolveProductImage(p.getImagem()),
                buildCoresHex(p.getCores()),
                p.getFinalidade(),
                p.getDescricao(),
                p.getDiluicao(),
                p.getConcentracao(),
                p.getLocalUso(),
                buildEquipNomes(p.getEquipmentGuides()),
                resolveEquipImagens(p.getEquipmentGuides()),
                ColorCircleRenderer.render(primeiraCorHex)
        );
    }

    // ── Resolução de imagens ────────────────────────────────────────────────

    private InputStream resolveProductImage(String filename) {
        if (filename == null || filename.isBlank()) return null;
        try {
            Path path = productImageStorage.searchFile(extractFilename(filename));
            if (Files.exists(path)) return Files.newInputStream(path);
        } catch (IOException ex) {
            logger.error("Ocorreu um erro ao obter a imagem do produto: {}", ex.getMessage(), ex);
        }
        return null;
    }

    private List<InputStream> resolveEquipImagens(List<EquipmentGuide> equipamentos) {
        if (equipamentos == null || equipamentos.isEmpty()) return List.of();
        List<InputStream> streams = new ArrayList<>();
        for (EquipmentGuide eq : equipamentos) {
            if (eq.getImagem() == null || eq.getImagem().isBlank()) continue;
            try {
                Path path = equipmentImageStorage.searchFile(extractFilename(eq.getImagem()));
                if (Files.exists(path)) streams.add(Files.newInputStream(path));
            } catch (IOException ex) {
                logger.error("Ocorreu um erro ao obter a imagem do equipamento: {}", ex.getMessage(), ex);
            }
        }
        return streams;
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

    private String buildCoresHex(List<String> cores) {
        if (cores == null || cores.isEmpty()) return null;
        return String.join(",", cores);
    }

    private String buildEquipNomes(List<EquipmentGuide> equipamentos) {
        if (equipamentos == null || equipamentos.isEmpty()) return null;
        return equipamentos.stream()
                .map(EquipmentGuide::getNome)
                .reduce((a, b) -> a + "\n" + b)
                .orElse(null);
    }

    private String extractFilename(String path) {
        if (path == null) return null;
        int idx = path.lastIndexOf('/');
        return idx >= 0 ? path.substring(idx + 1) : path;
    }

    private InputStream resolveLogoEmpresa() {
        try {
            if (logoEmpresaPath.startsWith("classpath:")) {
                return getClass().getResourceAsStream(
                        logoEmpresaPath.substring("classpath:".length()));
            }
            Path path = Path.of(logoEmpresaPath);
            if (Files.exists(path)) return Files.newInputStream(path);
        } catch (IOException ex) {
            logger.error("Ocorreu um erro obter a logo da empresa: {}", ex.getMessage(), ex);
        }
        return null;
    }
}