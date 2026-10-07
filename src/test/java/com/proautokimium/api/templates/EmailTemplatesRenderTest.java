package com.proautokimium.api.templates;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Renderiza cada e-mail com dados de exemplo, pelo mesmo motor do Spring
 * (SpEL), sem subir a aplicação.
 *
 * O que isto pega: variável com nome errado (o Thymeleaf põe vazio e não dá
 * erro), peça que não encaixou no casco, e texto que sumiu na troca de layout.
 */
class EmailTemplatesRenderTest {

    private static SpringTemplateEngine engine;

    @BeforeAll
    static void engine() {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode("HTML");
        resolver.setCharacterEncoding("UTF-8");
        engine = new SpringTemplateEngine();
        engine.setTemplateResolver(resolver);
    }

    /**
     * Também grava o e-mail em target/email-previews/, para abrir no navegador e
     * ver como ficou sem precisar enviar. O último caso de cada template fica.
     */
    static String render(String template, Map<String, Object> vars) {
        Context ctx = new Context();
        ctx.setVariables(vars);
        String html = engine.process(template, ctx);
        try {
            Path out = Path.of("target", "email-previews", template.replace("html/", "") + ".html");
            Files.createDirectories(out.getParent());
            Files.writeString(out, html);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return html;
    }

    /** O que todo e-mail tem: o casco inteiro, uma vez só, e nenhum texto de exemplo do molde. */
    static void assertCasco(String html, String area) {
        assertThat(html).startsWith("<!DOCTYPE html>");
        assertThat(html.split("<html", -1)).hasSize(2);
        assertThat(html).contains("Proauto <span style=\"color:#57c1ab;\">Kimium</span>");
        assertThat(html).contains(">" + area + "</td>");
        assertThat(html).contains("Este é um e-mail automático do Portal Proauto Kimium.");
        // Os textos de molde das peças não podem vazar para o e-mail.
        assertThat(html).doesNotContain(">Título<").doesNotContain(">Texto<").doesNotContain(">Rótulo<")
                .doesNotContain("ABC123").doesNotContain(">Abrir<");
    }

    @Test
    @DisplayName("primeiro acesso: código, prazo, botão e link de reserva com o endereço")
    void primeiroAcesso() {
        String html = render("html/first-access-token", Map.of(
                "token", "482913",
                "ttlMinutes", 30,
                "actionUrl", "https://portal.proautokimium.com.br/login/first-access?token=482913&email=ana%40x.com"));

        assertCasco(html, "Primeiro acesso");
        assertThat(html).contains(">482913</span>");
        assertThat(html).contains("O código expira em 30 minutos.");
        assertThat(html).contains("Use 482913 para criar sua senha. Vale por 30 minutos.");
        assertThat(html).contains("href=\"https://portal.proautokimium.com.br/login/first-access?token=482913&amp;email=ana%40x.com\"");
        assertThat(html).contains(">Continuar cadastro</a>");
    }

    @Test
    @DisplayName("redefinição de senha: mesmo desenho do primeiro acesso, com o próprio botão")
    void redefinicao() {
        String html = render("html/reset-access-token", Map.of("token", "715204", "ttlMinutes", 30,
                "actionUrl", "https://portal/login/reset?token=715204"));

        assertCasco(html, "Redefinição de senha");
        assertThat(html).contains(">715204</span>").contains(">Redefinir senha</a>").contains("O código expira em 30 minutos.");
    }

    @Test
    @DisplayName("convite do cliente: o nome da empresa e o prazo do convite")
    void conviteDoCliente() {
        String html = render("html/client-invite", Map.of("customerName", "Transportes Rápido Sul",
                "actionUrl", "https://portal/cliente/convite?t=1", "ttlHours", 72));

        assertCasco(html, "Área do Cliente");
        assertThat(html).contains("Transportes Rápido Sul").contains("O convite expira em 72 horas.").contains(">Definir minha senha</a>");
    }

    @Test
    @DisplayName("banco de talentos: com primeiro nome e sem ele, o cumprimento não quebra")
    void bancoDeTalentosAcesso() {
        Map<String, Object> vars = new HashMap<>(Map.of("ttlHoras", 24, "actionUrl", "https://site/meu-curriculo?t=1"));
        vars.put("primeiroNome", "Camila");
        String comNome = render("html/talent-bank-access", vars);
        vars.put("primeiroNome", null);
        String semNome = render("html/talent-bank-access", vars);

        assertCasco(comNome, "Banco de Talentos");
        assertThat(comNome).contains(">Olá, Camila</p>").contains("O link expira em 24 horas.");
        assertThat(semNome).contains(">Olá</p>").doesNotContain("null");
    }

    @Test
    @DisplayName("banco de talentos vencendo: o aviso do link muda com e sem token")
    void bancoDeTalentosVencendo() {
        Map<String, Object> vars = new HashMap<>(Map.of("dataDeExpiracao", "11/09/2028", "ttlHoras", 24,
                "actionUrl", "https://site/meu-curriculo"));
        vars.put("primeiroNome", "Maria");
        vars.put("comToken", true);
        String comToken = render("html/talent-bank-expiring", vars);
        vars.put("comToken", false);
        String semToken = render("html/talent-bank-expiring", vars);

        assertCasco(comToken, "Banco de Talentos");
        assertThat(comToken).contains("até 11/09/2028").contains("O link expira em 24 horas.")
                .doesNotContain("informe seu e-mail para receber o link");
        assertThat(semToken).contains("informe seu e-mail para receber o link").doesNotContain("O link expira em");
    }

    @Test
    @DisplayName("relatório de reembolsos: o quadro e o anexo, sem nome nem valor no corpo")
    void relatorioDeReembolsos() {
        String html = render("html/hr-report", Map.of("periodo", "01/09/2026 a 30/09/2026",
                "escopo", "de todos os funcionários", "status", "Aprovado, Pago", "emitidoPor", "Carla Mendes",
                "emitidoEm", "07/10/2026 10:02", "arquivo", "reembolsos-setembro.pdf"));

        assertCasco(html, "Recursos Humanos");
        assertThat(html).contains("de todos os funcionários, período de 01/09/2026 a 30/09/2026.")
                .contains(">Status incluídos</td>").contains(">Aprovado, Pago</td>")
                .contains(">Emitido por</td>").contains(">Carla Mendes</td>")
                .contains("reembolsos-setembro.pdf");
    }

    @Test
    @DisplayName("documento a vencer: alerta vermelho só no dia; antes disso, sem alerta")
    void documentoAVencer() {
        Map<String, Object> vars = new HashMap<>(Map.of("chamada", "Vence em 30 dias", "funcionario", "Bruno Lima",
                "documento", "ASO periódico", "tipo", "ASO", "vencimento", "05/11/2026",
                "link", "https://portal/rh/employee-documents?status=EXPIRING"));
        vars.put("hoje", false);
        String antes = render("html/employee-document-alert", vars);
        vars.put("hoje", true);
        vars.put("chamada", "Vence hoje");
        String hoje = render("html/employee-document-alert", vars);

        assertCasco(antes, "Documentos");
        assertThat(antes).contains(">Vence em 30 dias</h1>").contains(">Bruno Lima</td>").contains(">05/11/2026</td>")
                .doesNotContain("#fdecea");
        assertThat(hoje).contains("#fdecea").contains(">Último dia</span>");
        assertThat(antes).contains("ASO periódico de Bruno Lima: vence em 30 dias (05/11/2026).");
    }

    @Test
    @DisplayName("resumo da programação: atrasadas em vermelho, próximas em azul, e lista vazia some")
    void resumoDaProgramacao() {
        Map<String, Object> atrasada = new HashMap<>(Map.of("maquina", "Lavadora LX-300", "cliente", "Frigorífico Bom Corte",
                "previsao", "01/10/2026", "chamada", "6 dias de atraso", "pessoas", "Carmen · Fábio"));
        atrasada.put("regiao", "SP");
        Map<String, Object> proxima = new HashMap<>(Map.of("maquina", "Calandra CL-1200", "cliente", "Lavanderia Central",
                "previsao", "09/10/2026", "chamada", "em 2 dias"));
        proxima.put("regiao", null);
        proxima.put("pessoas", null);

        String html = render("html/machine-alert-digest", Map.of("assunto", "x", "resumo", "1 atrasada · 1 saída próxima",
                "totalAtrasadas", 1, "totalProximas", 1, "atrasadas", List.of(atrasada), "proximas", List.of(proxima),
                "link", "https://portal/stock/programacao"));
        String soProximas = render("html/machine-alert-digest", Map.of("assunto", "x", "resumo", "1 saída próxima",
                "totalAtrasadas", 0, "totalProximas", 1, "atrasadas", List.of(), "proximas", List.of(proxima),
                "link", "https://portal/stock/programacao"));

        assertCasco(html, "Programação");
        assertThat(html).contains(">Atrasadas (1)</p>").contains(">Lavadora LX-300</span>")
                .contains("Frigorífico Bom Corte · SP").contains("previsão 01/10/2026 · Carmen · Fábio")
                .contains(">6 dias de atraso</td>").contains(">Saídas próximas (1)</p>")
                .contains(">Calandra CL-1200</span>").contains(">em 2 dias · 09/10/2026</td>");
        assertThat(soProximas).doesNotContain("Atrasadas").contains(">Calandra CL-1200</span>").doesNotContain("null");
    }

    @Test
    @DisplayName("candidaturas: as quatro respostas, com o nome e a vaga")
    void candidaturas() {
        Map<String, Object> vars = new HashMap<>(Map.of("nome", "Camila", "vaga", "Técnico de Manutenção"));
        vars.put("aviso", "");
        String recebidaSemAviso = render("html/candidatura-recebida", vars);
        vars.put("aviso", "<p style=\"background:#f4f4f4\">Usamos o currículo que você já tinha enviado. <a href=\"https://site/meu-curriculo\">acessar meu cadastro</a></p>");
        String recebidaComAviso = render("html/candidatura-recebida", vars);

        assertCasco(recebidaSemAviso, "Trabalhe conosco");
        assertThat(recebidaSemAviso).contains(">Olá, Camila</p>").contains("vaga de Técnico de Manutenção")
                .doesNotContain("Aviso sobre o currículo");
        // O aviso é HTML montado pelo código: entra como HTML, com o link funcionando.
        assertThat(recebidaComAviso).contains("<a href=\"https://site/meu-curriculo\">acessar meu cadastro</a>");

        for (String t : List.of("html/candidatura-avancou", "html/candidatura-reprovada", "html/candidatura-aprovada")) {
            String html = render(t, Map.of("nome", "Camila", "vaga", "Técnico de Manutenção"));
            assertCasco(html, "Trabalhe conosco");
            assertThat(html).as(t).contains("Camila").contains("Técnico de Manutenção");
        }
    }

    @Test
    @DisplayName("checklist: título e mensagem escapados, como o HtmlUtils fazia")
    void checklist() {
        String html = render("html/checklist-controladoria", Map.of("titulo", "Pedido de alteração em checklist",
                "mensagem", "Rafael pediu para alterar o nº 0042 — Lavanderia <Central>: \"preço errado\"",
                "link", "https://portal/vendas/checklists"));

        assertCasco(html, "Checklist de vendas");
        assertThat(html).contains(">Pedido de alteração em checklist</h1>")
                .contains("Lavanderia &lt;Central&gt;").doesNotContain("<Central>")
                .contains(">Abrir no KimiumHub</a>");
    }

    private static Map<String, Object> newsletter(int visitas, double horas) {
        Map<String, Object> v = new HashMap<>();
        v.put("mes", "Setembro");
        v.put("nomeDoCliente", "Lavanderia Central");
        v.put("produtoEmDestaque", "Detergente Alcalino KX");
        v.put("quantidadeDeProdutos", 1234);
        v.put("quantidadeDeLitros", 5320.0);
        v.put("quantidadeDeVisitas", visitas);
        v.put("valorDePecasTrocadas", 1850.5);
        v.put("mediaDiasAtendimento", 3);
        v.put("valorTotalDeHoras", horas);
        v.put("valorTotalCobradoHoras", 12345.6);
        v.put("horasNormais", 100.0);
        v.put("valorHorasNormais", 9000.0);
        v.put("horasMauUso", 28.5);
        v.put("valorHorasMauUso", 3345.6);
        v.put("faturamentoTotal", 48250.75);
        return v;
    }

    @Test
    @DisplayName("newsletter: dinheiro no formato do Brasil (1.234,56), nunca o americano")
    void newsletterDinheiro() {
        String html = render("html/newsletter_v2", newsletter(14, 128.5));

        assertCasco(html, "Newsletter · Setembro");
        assertThat(html).contains(">Lavanderia Central</p>").contains("Detergente Alcalino KX")
                .contains(">R$ 48.250,75</td>").contains(">1.234</td>").contains(">5.320</td>")
                .contains(">R$ 1.850,50</td>").contains(">128,50 h</td>").contains(">R$ 12.345,60</td>")
                .contains(">28,50 h · R$ 3.345,60</td>")
                .doesNotContain("48,250.75").doesNotContain("3345.6");
    }

    @Test
    @DisplayName("newsletter: sem visitas e sem horas, as duas seções somem")
    void newsletterSemManutencao() {
        String html = render("html/newsletter_v2", newsletter(0, 0.0));

        assertThat(html).doesNotContain("Manutenções").doesNotContain("Horas de manutenção")
                .contains(">R$ 48.250,75</td>");
    }
}
