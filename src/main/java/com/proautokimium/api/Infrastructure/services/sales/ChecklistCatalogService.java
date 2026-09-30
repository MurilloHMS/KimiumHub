package com.proautokimium.api.Infrastructure.services.sales;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.proautokimium.api.Application.DTOs.sales.ChecklistCatalogDTO;
import com.proautokimium.api.Infrastructure.repositories.sales.ChecklistComodatoItemRepository;
import com.proautokimium.api.Infrastructure.repositories.sales.ChecklistVisualItemRepository;
import com.proautokimium.api.Infrastructure.utils.LinhaSankhya;
import com.proautokimium.api.domain.valueObjects.BrazilianDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * O catálogo que o celular guarda para o vendedor trabalhar sem internet.
 *
 * <p><b>O Sankhya é lido no máximo uma vez por hora</b>, e não a cada celular:
 * a leitura inteira leva uns 8 s (clientes, produtos e preços, paginados), e
 * cada consulta é um login no ERP. O retrato fica em memória; vencido, quem
 * pede recebe o retrato antigo na hora e a renovação corre por trás — o
 * vendedor não espera o ERP. Só o primeiro pedido depois de subir a API espera.
 *
 * <p>Os cadastros daqui (comodato, comunicação visual) são lidos a cada pedido:
 * são duas listas pequenas, e a Controladoria espera ver a mudança na hora.
 */
@Service
public class ChecklistCatalogService {

    private static final Logger log = LoggerFactory.getLogger(ChecklistCatalogService.class);

    /** A tabela geral: vale para quem não tem tabela, está na 0 ou é cliente novo. */
    public static final int GENERAL_PRICE_TABLE = 80;

    static final Duration MAX_AGE = Duration.ofHours(1);

    private final ChecklistSankhyaQueryService sankhya;
    private final ChecklistComodatoItemRepository comodatoRepository;
    private final ChecklistVisualItemRepository visualRepository;
    private final ObjectMapper mapper;
    private final Clock clock;

    private volatile ErpSnapshot snapshot;
    private final AtomicBoolean refreshing = new AtomicBoolean(false);

    public ChecklistCatalogService(ChecklistSankhyaQueryService sankhya,
                                   ChecklistComodatoItemRepository comodatoRepository,
                                   ChecklistVisualItemRepository visualRepository,
                                   ObjectMapper mapper, Clock clock) {
        this.sankhya = sankhya;
        this.comodatoRepository = comodatoRepository;
        this.visualRepository = visualRepository;
        this.mapper = mapper;
        this.clock = clock;
    }

    /** O retrato do ERP, já filtrado e com o hash que entra na versão. */
    record ErpSnapshot(List<ChecklistCatalogDTO.Customer> customers,
                       List<ChecklistCatalogDTO.Product> products,
                       List<ChecklistCatalogDTO.Price> prices,
                       LocalDateTime fetchedAt,
                       String hash) {}

    public ChecklistCatalogDTO catalog() {
        ErpSnapshot erp = currentSnapshot();

        Map<Integer, ChecklistCatalogDTO.Product> byCode = new HashMap<>();
        erp.products().forEach(p -> byCode.put(p.code(), p));

        List<ChecklistCatalogDTO.ComodatoItem> comodato = comodatoRepository.findAllByOrderBySortOrderAsc().stream()
                .filter(c -> c.isActive())
                .map(c -> {
                    ChecklistCatalogDTO.Product product = byCode.get(c.getProductCode());
                    return new ChecklistCatalogDTO.ComodatoItem(c.getId(), c.getProductCode(),
                            product == null ? "Produto " + c.getProductCode() : product.name(),
                            c.getPopularName(), product != null);
                })
                .toList();

        List<ChecklistCatalogDTO.VisualItem> visual = visualRepository.findAllByOrderBySortOrderAscNameAsc().stream()
                .filter(v -> v.isActive())
                .map(v -> new ChecklistCatalogDTO.VisualItem(v.getId(), v.getName()))
                .toList();

        String version = sha256(erp.hash() + "|" + json(comodato) + "|" + json(visual)).substring(0, 16);
        return new ChecklistCatalogDTO(version, erp.fetchedAt(), erp.customers(), erp.products(), erp.prices(),
                comodato, visual);
    }

    /** Os produtos do ERP (com os de comodato), para a Controladoria escolher. */
    public List<ChecklistCatalogDTO.Product> products() {
        return currentSnapshot().products();
    }

    private ErpSnapshot currentSnapshot() {
        ErpSnapshot current = snapshot;
        if (current == null) {
            return refreshNow();
        }
        if (Duration.between(current.fetchedAt(), LocalDateTime.now(clock)).compareTo(MAX_AGE) > 0) {
            refreshInBackground();
        }
        return current;
    }

    private synchronized ErpSnapshot refreshNow() {
        if (snapshot == null) {
            snapshot = load();
        }
        return snapshot;
    }

    private void refreshInBackground() {
        if (!refreshing.compareAndSet(false, true)) {
            return;
        }
        CompletableFuture.runAsync(() -> {
            try {
                snapshot = load();
            } catch (RuntimeException e) {
                // O retrato antigo continua servindo; o próximo pedido tenta de novo.
                log.warn("Não foi possível renovar o catálogo do checklist: {}", e.getMessage());
            } finally {
                refreshing.set(false);
            }
        });
    }

    ErpSnapshot load() {
        List<ChecklistCatalogDTO.Customer> customers = sankhya.customers().stream().map(this::customer).toList();
        List<ChecklistCatalogDTO.Product> products = sankhya.products().stream().map(ChecklistCatalogService::product).toList();

        Set<Integer> tables = new HashSet<>();
        tables.add(GENERAL_PRICE_TABLE);
        customers.forEach(c -> {
            if (c.priceTable() != null) tables.add(c.priceTable());
        });
        Set<Integer> sellable = new HashSet<>();
        products.stream().filter(p -> "V".equals(p.usage()) || "R".equals(p.usage()))
                .forEach(p -> sellable.add(p.code()));

        List<ChecklistCatalogDTO.Price> prices = sankhya.prices().stream()
                .filter(r -> tables.contains(r.inteiro("CODTAB")) && sellable.contains(r.inteiro("CODPROD")))
                .map(r -> new ChecklistCatalogDTO.Price(r.inteiro("CODTAB"), r.inteiro("CODPROD"),
                        BigDecimal.valueOf(r.decimal("VLRVENDA"))))
                .toList();

        LocalDateTime now = LocalDateTime.now(clock);
        String hash = sha256(json(customers) + "|" + json(products) + "|" + json(prices));
        log.info("Catálogo do checklist lido do Sankhya: {} clientes, {} produtos, {} preços",
                customers.size(), products.size(), prices.size());
        return new ErpSnapshot(customers, products, prices, now, hash);
    }

    private ChecklistCatalogDTO.Customer customer(LinhaSankhya r) {
        String street = join(r.texto("TIPOLOGRADOURO"), r.texto("LOGRADOURO"));
        int table = r.inteiro("CODTAB");
        return new ChecklistCatalogDTO.Customer(
                r.inteiro("CODPARC"),
                r.texto("NOMEPARC"),
                r.texto("RAZAOSOCIAL"),
                BrazilianDocument.digits(r.texto("CGC_CPF")),
                r.texto("TIPPESSOA"),
                r.texto("IE"),
                r.texto("TELEFONE"),
                r.texto("EMAILNFE"),
                r.texto("CEP"),
                street,
                r.texto("NUMERO"),
                r.texto("COMPLEMENTO"),
                r.texto("BAIRRO"),
                r.texto("CIDADE"),
                r.texto("UF"),
                // Tabela 0 é "sem tabela" na prática (82 clientes, 5 itens): vai
                // para a geral, como quem não tem nenhuma.
                table > 0 ? table : null);
    }

    static ChecklistCatalogDTO.Product product(LinhaSankhya r) {
        String name = r.texto("DESCRPROD");
        String unit = r.texto("CODVOL");
        String erpLabel = r.texto("EMBALAGEM");
        double erpSize = r.decimal("QTDEMBALAGEM");

        String label;
        BigDecimal size;
        boolean fromName = false;
        if (erpLabel != null && erpSize > 0) {
            size = BigDecimal.valueOf(erpSize).stripTrailingZeros();
            label = size.toPlainString().replace('.', ',') + " " + (unit == null ? "" : unit) + " " + erpLabel;
        } else {
            PackageGuess guess = PackageGuess.fromName(name, unit);
            size = guess == null ? null : guess.size();
            label = guess == null ? null : guess.label();
            fromName = guess != null;
        }
        return new ChecklistCatalogDTO.Product(r.inteiro("CODPROD"), name, r.texto("USOPROD"),
                (long) r.decimal("CODGRUPOPROD"), unit, BigDecimal.valueOf(r.decimal("IPI")).stripTrailingZeros(),
                label == null ? null : label.trim(), size, fromName);
    }

    /**
     * A embalagem escrita no nome do produto: "KIMI AB200 - 5 LT GL NATURAL" é
     * 5 LT; "POSEIDON - 7,5 KG GL" é 7,5 KG. Só vale quando a unidade do nome é
     * a unidade de preço do produto (LT com LT, KG com KG) — senão o total
     * multiplicaria litros por preço de quilo. Medido em 2026-09-30: só 46
     * produtos de venda têm a embalagem no TGFVOA; os outros, só no nome.
     */
    record PackageGuess(BigDecimal size, String label) {

        private static final Pattern SIZE = Pattern.compile(
                "(\\d+(?:[.,]\\d+)?)\\s*(LTS?|L|KGS?)\\b", Pattern.CASE_INSENSITIVE);

        static PackageGuess fromName(String name, String unit) {
            if (name == null || unit == null) {
                return null;
            }
            Matcher m = SIZE.matcher(name);
            while (m.find()) {
                String nameUnit = m.group(2).toUpperCase().startsWith("K") ? "KG" : "LT";
                if (!nameUnit.equals(unit.toUpperCase())) {
                    continue;
                }
                BigDecimal size = new BigDecimal(m.group(1).replace(',', '.')).stripTrailingZeros();
                if (size.signum() <= 0) {
                    continue;
                }
                return new PackageGuess(size, size.toPlainString().replace('.', ',') + " " + nameUnit);
            }
            return null;
        }
    }

    private static String join(String a, String b) {
        if (a == null) return b;
        if (b == null) return a;
        return a + " " + b;
    }

    private String json(Object value) {
        try {
            return mapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String sha256(String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(text.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
