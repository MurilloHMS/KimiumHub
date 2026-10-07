package com.proautokimium.api.Infrastructure.services.email;

import com.proautokimium.api.Infrastructure.services.processoSeletivo.TalentBankAccessTokenService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * O e-mail do banco de talentos.
 *
 * <p>Separado do {@code AuthEmailService} porque aquele é de autenticação: tem
 * o {@code FROM} dele e usa {@code sendNow}. Este é do módulo de recrutamento,
 * e é também o lugar natural para os outros e-mails do processo seletivo
 * saírem do HTML cravado em {@code EmailTemplates}, quando for a hora.
 */
@Service
public class TalentBankEmailService {

    private static final String FROM = "noreply@envios.proautokimium.com.br";
    private static final String ACCESS_TEMPLATE = "html/talent-bank-access";
    private static final String EXPIRING_TEMPLATE = "html/talent-bank-expiring";
    private static final DateTimeFormatter DATA_BR = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final String ROTA_DO_SITE = "/meu-curriculo";

    private final EmailRenderer renderer;
    private final EmailQueueService emailQueueService;
    private final String websiteBaseUrl;

    public TalentBankEmailService(EmailRenderer renderer,
                                  EmailQueueService emailQueueService,
                                  @Value("${app.base-url}") String websiteBaseUrl) {
        this.renderer = renderer;
        this.emailQueueService = emailQueueService;
        this.websiteBaseUrl = websiteBaseUrl;
    }

    /**
     * Manda o link de acesso.
     *
     * <p><b>Vai para a fila, nunca {@code sendNow}</b>, e isso é decisão de
     * segurança e não de desempenho. O endpoint que chama isto responde a mesma
     * coisa exista ou não o e-mail; se o caminho conhecido fizesse SMTP inline
     * (centenas de milissegundos) e o desconhecido respondesse na hora, a
     * <b>diferença de latência</b> seria o oráculo que o corpo da resposta
     * cuidadosamente não é.
     *
     * <p>O preço é o link chegar em até 60s — o mesmo atraso que a confirmação
     * de candidatura já tem hoje.
     */
    public void enviarLinkDeAcesso(String destinatario, String nomeCompleto, String token) {
        Map<String, Object> vars = comPrimeiroNome(nomeCompleto);
        vars.put("ttlHoras", TalentBankAccessTokenService.TOKEN_TTL_HORAS);
        vars.put("actionUrl", linkPara(token));
        String html = renderer.render(ACCESS_TEMPLATE, vars);

        emailQueueService.sendEmail(destinatario, FROM,
                "Seus dados no Banco de Talentos da Proauto Kimium", html);
    }

    /**
     * O aviso de que o prazo está acabando.
     *
     * <p>O link já entra renovável: abrir e marcar a autorização de novo é a
     * renovação inteira. Sem token — quando o cooldown recusou emitir um —, o
     * botão leva para {@code /meu-curriculo}, onde a pessoa pede o link com o
     * próprio e-mail. O aviso sai do mesmo jeito: pular a pessoa por causa do
     * cooldown a deixaria sem aviso nenhum, porque a coluna de controle só é
     * gravada depois do envio.
     *
     * @param token vazio quando não houve token novo
     */
    public void enviarAvisoDeExpiracao(String destinatario, String nomeCompleto,
                                       LocalDateTime expiraEm, Optional<String> token) {

        Map<String, Object> vars = comPrimeiroNome(nomeCompleto);
        vars.put("dataDeExpiracao", expiraEm.format(DATA_BR));
        vars.put("ttlHoras", TalentBankAccessTokenService.TOKEN_TTL_HORAS);
        vars.put("comToken", token.isPresent());
        vars.put("actionUrl", token.map(this::linkPara).orElse(websiteBaseUrl + ROTA_DO_SITE));
        String html = renderer.render(EXPIRING_TEMPLATE, vars);

        emailQueueService.sendEmail(destinatario, FROM,
                "Seus dados no Banco de Talentos vencem em " + expiraEm.format(DATA_BR), html);
    }

    /**
     * <b>Sem {@code &email=} na URL</b>, ao contrário do primeiro acesso.
     *
     * <p>Lá o endereço precisa viajar porque o sign-in cria o usuário com ele.
     * Aqui o servidor já sabe de quem é o token, e pôr o endereço na URL o joga
     * no histórico do navegador, no {@code Referer} de qualquer recurso de
     * terceiro que a página carregar, e na barra de endereço de uma tela
     * compartilhada.
     */
    private String linkPara(String token) {
        return websiteBaseUrl + ROTA_DO_SITE
                + "/" + URLEncoder.encode(token, StandardCharsets.UTF_8);
    }

    /**
     * HashMap, e não Map.of: o primeiro nome é null quando o cadastro não tem
     * nome, e o Map.of recusa null com NullPointerException. O template já
     * cumprimenta só com "Olá" nesse caso.
     */
    private Map<String, Object> comPrimeiroNome(String nomeCompleto) {
        Map<String, Object> vars = new HashMap<>();
        vars.put("primeiroNome", primeiroNomeDe(nomeCompleto));
        return vars;
    }

    private static String primeiroNomeDe(String nomeCompleto) {
        if (nomeCompleto == null || nomeCompleto.isBlank()) {
            return null;
        }
        return nomeCompleto.trim().split("\\s+")[0];
    }
}
