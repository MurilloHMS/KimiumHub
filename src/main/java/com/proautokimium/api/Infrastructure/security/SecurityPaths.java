package com.proautokimium.api.Infrastructure.security;

public final class SecurityPaths {

    public static final String[] SWAGGER = {
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html"
    };

    public static final String[] PUBLIC_GET ={
            "/api/vaga/publicadas",
            "/api/public-secrets/**",
            "/api/product/website/active",
            "/upload/images/**",
            "/upload/equipment/images/**",
            "/upload/profile/images/**",
            "/api/faq/public",
            "/api/vaga/areas",
            // Um segmento, nunca `/**`: o token e a chave do dossie pessoal de
            // alguem, e um caminho novo pendurado aqui embaixo nasceria aberto.
            "/api/talent-bank/public/*",
            "/api/talent-bank/public/*/curriculo",
            "/api/profile/public/**",
            "/ws/**",

            // O health que o Docker consulta a cada 15s para saber se o
            // container esta de pe. Sem isto ele levava 401, o healthcheck
            // falhava com saida vazia, e o deploy era revertido mesmo com a
            // API no ar.
            //
            // **`/actuator/health`, e nao `/actuator/**`.** O actuator pode
            // expor `/env`, `/beans` e `/heapdump`; hoje so o health esta
            // ligado, mas liberar o caminho inteiro deixaria a porta aberta
            // no dia em que alguem ligar outro endpoint sem lembrar disto.
            "/actuator/health"
    };

    public static final String[] PUBLIC_POST ={
            "/api/auth/login",
            "/api/contact",
            "/api/certificate",
            "/api/certificate/no-validation",
            // Renovação de sessão. Quem chama está com o access token vencido,
            // então exigir autenticação aqui tornaria o endpoint inalcançável
            // no único momento em que ele serve. Quem protege é o refresh token
            // ser válido, não usado e não revogado.
            "/api/auth/refresh",

            // Sair também é público, e pelo mesmo motivo: quem aperta "Sair"
            // pode estar com o access token vencido, e exigir autenticação
            // impediria de sair justamente quem mais precisa.
            "/api/auth/logout",
            "/api/auth/forgot-password",
            "/api/auth/reset-password",
            "/api/candidatura",
            "/api/talent-bank/public",
            "/api/talent-bank/public/access-link",
            // As três do fluxo de primeiro acesso, e só elas.
            //
            // O fluxo é auto-atendimento, não convite: o funcionário informa o
            // próprio CPF, recebe o token por e-mail e escolhe a senha. As três
            // etapas acontecem antes de existir usuário, então nenhuma delas
            // pode exigir login — fechar a primeira torna as outras duas
            // inalcançáveis.
            //
            // Listadas uma a uma, e não como "/api/auth/first-access/**",
            // porque esse padrão casaria qualquer caminho novo pendurado aí
            // embaixo. O `/**` já abriu um endpoint sem querer neste projeto.
            //
            // O que protege a primeira etapa é o CPF precisar existir em
            // `employees`. É pouco: CPF não é segredo, e o token vai para o
            // e-mail que o CHAMADOR digita, não para um endereço em ficha —
            // `Employee` não tem coluna de e-mail. Mandar para o endereço
            // cadastrado é o conserto certo e depende de migration.
            "/api/auth/first-access",
            "/api/auth/first-access/*/is-valid",
            "/api/auth/first-access/*/sign-in"
    };

    /**
     * PUT publico, e ele nao existia neste projeto ate 2026-09-11.
     *
     * <p>So havia lista para GET e POST; qualquer outro verbo caia no
     * {@code anyRequest()}. Acrescentar a lista sozinha nao basta: o
     * {@code PublicPathsHaveNoPreAuthorizeTest} varre por verbo, e o
     * {@code publicasDoVerbo()} dele so conhecia GET e POST. Sem estender o
     * teste junto, anotar por engano um destes caminhos com
     * {@code @PreAuthorize} deixaria a suite <b>verde</b> e quebraria o
     * formulario no deploy -- o incidente de 2026-08-27 de novo, com o mesmo
     * teste, num buraco novo.
     */
    public static final String[] PUBLIC_PUT = {
            "/api/talent-bank/public/*"
    };

    /** Ver {@link #PUBLIC_PUT}: a mesma armadilha, o mesmo cuidado. */
    public static final String[] PUBLIC_DELETE = {
            "/api/talent-bank/public/*"
    };

    private SecurityPaths(){}
}
