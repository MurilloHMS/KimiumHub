package com.proautokimium.api.Infrastructure.services.reports.guide;

import com.proautokimium.api.Application.DTOs.guide.GuideLayoutPreviewRequestDTO;
import com.proautokimium.api.Application.DTOs.guide.GuideReportRequestDTO;
import com.proautokimium.api.Application.DTOs.guide.GuideReportRowDTO;
import com.proautokimium.api.Infrastructure.exceptions.product.ProductNotFoundException;
import com.proautokimium.api.Infrastructure.repositories.ProductWebSiteRepository;
import com.proautokimium.api.Infrastructure.services.storage.EquipmentImageStorageService;
import com.proautokimium.api.Infrastructure.services.storage.ProductImageStorageService;
import com.proautokimium.api.Infrastructure.utils.ColorCircleRenderer;
import com.proautokimium.api.Infrastructure.utils.ColorNameUtil;
import com.proautokimium.api.domain.entities.EquipmentGuide;
import com.proautokimium.api.domain.entities.ProductWebsite;
import com.proautokimium.api.domain.enums.guide.GuideImageSource;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Gera o "Guia de Utilização" em PDF.
 *
 * <p>
 * O desenho vem do layout publicado ({@link GuideLayoutService}), montado pelo
 * {@link GuideJasperDesignBuilder}. A prévia do designer passa pelo mesmo
 * caminho com o layout que está na tela dele — por isso a prévia É o arquivo:
 * não existe um segundo desenho para discordar do primeiro.
 * </p>
 *
 * <p>
 * Fluxo:
 * <ol>
 *   <li>Busca cada {@link ProductWebsite} pelo ID, respeitando a ordem enviada.</li>
 *   <li>Converte cada produto em um {@link GuideReportRowDTO}, resolvendo as
 *       imagens do disco.</li>
 *   <li>Monta os parâmetros (título e imagens do cabeçalho e do rodapé).</li>
 *   <li>Compila o layout (com cache) e preenche.</li>
 * </ol>
 * </p>
 */
@Service
public class GuideReportService {
    private final Logger logger = LoggerFactory.getLogger(GuideReportService.class);

    private final ProductWebSiteRepository productRepository;
    private final ProductImageStorageService productImageStorage;
    private final EquipmentImageStorageService equipmentImageStorage;
    private final GuideLayoutService layoutService;
    private final GuideReportCompiler compiler;

    @Value("${report.logo.empresa:classpath:/static/images/logo_empresa.png}")
    private String logoEmpresaPath;

    public GuideReportService(
            ProductWebSiteRepository productRepository,
            ProductImageStorageService productImageStorage,
            EquipmentImageStorageService equipmentImageStorage,
            GuideLayoutService layoutService,
            GuideReportCompiler compiler
    ) {
        this.productRepository     = productRepository;
        this.productImageStorage   = productImageStorage;
        this.equipmentImageStorage = equipmentImageStorage;
        this.layoutService         = layoutService;
        this.compiler              = compiler;
    }

    /**
     * O guia de Contratos, sempre com o layout publicado.
     *
     * @param customerLogo bytes do logo do cliente (pode ser null)
     * @throws ProductNotFoundException se algum ID não for encontrado
     */
    @Transactional(readOnly = true)
    public byte[] generate(GuideReportRequestDTO request, byte[] customerLogo) {
        GuideLayoutService.ParsedLayout layout = layoutService.publishedLayout();
        return render(layout, request.productIds(), request.tituloGuia(), customerLogo);
    }

    /** A prévia do designer: o layout da tela dele, salvo ou não. Nada é gravado. */
    @Transactional(readOnly = true)
    public byte[] preview(GuideLayoutPreviewRequestDTO request) {
        GuideLayoutService.ParsedLayout layout = layoutService.parseAndValidate(request.document());
        String title = request.title() == null || request.title().isBlank() ? "Exemplo" : request.title();
        return render(layout, request.productIds(), title, null);
    }

    private byte[] render(GuideLayoutService.ParsedLayout layout, List<UUID> productIds, String title, byte[] customerLogo) {
        List<GuideReportRowDTO> rows = productIds.stream()
                .map(id -> productRepository.findById(id)
                        .orElseThrow(ProductNotFoundException::new))
                .map(this::toRow)
                .toList();

        Map<String, Object> params = new HashMap<>();
        params.put(GuideJasperDesignBuilder.PARAM_TITLE, title == null ? null : title.toUpperCase());
        for (GuideImageSource source : GuideImageSource.values()) {
            if (source.getClasspathResource() != null) {
                params.put(GuideJasperDesignBuilder.imageParameter(source), readClasspath(source.getClasspathResource()));
            }
        }
        params.put(GuideJasperDesignBuilder.imageParameter(GuideImageSource.COMPANY_LOGO), resolveLogoEmpresa());
        params.put(GuideJasperDesignBuilder.imageParameter(GuideImageSource.CUSTOMER_LOGO),
                customerLogo == null || customerLogo.length == 0 ? null : customerLogo);
        for (UUID id : GuideJasperDesignBuilder.uploadedImageIds(layout.layout())) {
            params.put(GuideJasperDesignBuilder.uploadedImageParameter(id), layoutService.image(id).getContent());
        }

        try {
            JasperReport report = compiler.compile(layout.text(), layout.layout());
            JasperPrint print = JasperFillManager.fillReport(report, params, new JRBeanCollectionDataSource(rows));
            return JasperExportManager.exportReportToPdf(print);
        } catch (JRException e) {
            throw new IllegalStateException("Erro ao gerar o guia de utilização", e);
        }
    }

    // ── Conversão produto → DTO ─────────────────────────────────────────────

    private GuideReportRowDTO toRow(ProductWebsite p) {
        String coresHex = buildCoresHex(p.getCores());

        // Equipamentos: imagens e nomes em paralelo (apenas os que têm imagem),
        // para o template exibir cada ícone com o nome logo abaixo.
        List<byte[]> equipImagens = new ArrayList<>();
        List<String> equipNomes = new ArrayList<>();
        List<EquipmentGuide> equipamentos = p.getEquipmentGuides();
        if (equipamentos != null) {
            for (EquipmentGuide eq : equipamentos) {
                if (eq.getImagem() == null || eq.getImagem().isBlank()) continue;
                byte[] bytes = readStoredImage(equipmentImageStorage.searchFile(extractFilename(eq.getImagem())), "equipamento");
                if (bytes != null) {
                    equipImagens.add(bytes);
                    equipNomes.add(eq.getNome());
                }
            }
        }

        String corNome = ColorNameUtil.toNames(coresHex);
        return new GuideReportRowDTO(
                p.getName(),
                p.getSystemCode(),
                resolveProductImage(p.getImagem()),
                coresHex,
                p.getFinalidade(),
                p.getDescricaoGuia() != null && !p.getDescricaoGuia().isBlank()
                        ? p.getDescricaoGuia()
                        : p.getDescricao(),
                p.getDiluicao(),
                p.getConcentracao(),
                p.getLocalUso(),
                buildEquipNomes(equipamentos),
                equipImagens,
                readAll(ColorCircleRenderer.render(coresHex)),   // renderiza todas as cores
                corNome != null ? corNome : coresHex,             // nome básico; o hex cru se não der para nomear
                equipNomes
        );
    }

    // ── Resolução de imagens ────────────────────────────────────────────────

    private byte[] resolveProductImage(String filename) {
        if (filename == null || filename.isBlank()) return null;
        return readStoredImage(productImageStorage.searchFile(extractFilename(filename)), "produto");
    }

    private byte[] readStoredImage(Path path, String what) {
        try {
            if (path != null && Files.exists(path)) return Files.readAllBytes(path);
        } catch (IOException ex) {
            logger.error("Ocorreu um erro ao obter a imagem do {}: {}", what, ex.getMessage(), ex);
        }
        return null;
    }

    private byte[] readClasspath(String resource) {
        try (InputStream in = getClass().getResourceAsStream(resource)) {
            return in == null ? null : in.readAllBytes();
        } catch (IOException ex) {
            logger.error("Ocorreu um erro ao ler {}: {}", resource, ex.getMessage(), ex);
            return null;
        }
    }

    private static byte[] readAll(InputStream in) {
        if (in == null) return null;
        try (in) {
            return in.readAllBytes();
        } catch (IOException ex) {
            return null;
        }
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

    private byte[] resolveLogoEmpresa() {
        try {
            if (logoEmpresaPath.startsWith("classpath:")) {
                return readClasspath(logoEmpresaPath.substring("classpath:".length()));
            }
            Path path = Path.of(logoEmpresaPath);
            if (Files.exists(path)) return Files.readAllBytes(path);
        } catch (IOException ex) {
            logger.error("Ocorreu um erro obter a logo da empresa: {}", ex.getMessage(), ex);
        }
        return null;
    }
}
