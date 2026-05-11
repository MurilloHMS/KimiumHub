package com.proautokimium.api.domain.models;

public class EmailTemplates {

    public static class Subjects {

        public static final String CONFIRMACAO_CANDIDATURA =
                "Recebemos sua candidatura";

        public static final String AVANCO_ETAPA =
                "Você avançou para a próxima etapa";

        public static final String REPROVACAO =
                "Atualização sobre seu processo seletivo";

        public static final String APROVACAO =
                "Parabéns! Você foi aprovado(a)";

        public static final String PROPOSTA =
                "Proposta - Proauto Kimium";

        public static final String BOAS_VINDAS =
                "Bem-vindo(a) à Proauto Kimium";
    }

    public static String confirmacaoCandidatura(
            String nome,
            String vaga
    ) {
        return """
            <div style="font-family: Arial, sans-serif; font-size: 14px; line-height: 1.6; color: #333;">

                <p>Olá, <strong>%s</strong>!</p>

                <p>
                    Recebemos sua candidatura para a vaga de
                    <strong>%s</strong>.
                </p>

                <p>
                    Nosso time irá analisar seu perfil e,
                    caso avance para as próximas etapas,
                    entraremos em contato.
                </p>

                <br>

                <p>
                    Atenciosamente,<br>
                    <strong>Proauto Kimium</strong>
                </p>

            </div>
            """.formatted(nome, vaga);
    }

    public static String avancouEtapa(
            String nome,
            String vaga
    ) {
        return """
            <div style="font-family: Arial, sans-serif; font-size: 14px; line-height: 1.6; color: #333;">

                <p>Olá, <strong>%s</strong>!</p>

                <p>
                    Parabéns! Você avançou para a próxima etapa
                    da vaga de <strong>%s</strong>.
                </p>

                <p>
                    Em breve enviaremos mais detalhes
                    sobre as próximas etapas do processo seletivo.
                </p>

                <br>

                <p>
                    Atenciosamente,<br>
                    <strong>Proauto Kimium</strong>
                </p>

            </div>
            """.formatted(nome, vaga);
    }

    public static String reprovacao(
            String nome,
            String vaga
    ) {
        return """
            <div style="font-family: Arial, sans-serif; font-size: 14px; line-height: 1.6; color: #333;">

                <p>Olá, <strong>%s</strong>!</p>

                <p>
                    Agradecemos sua participação no processo seletivo
                    para a vaga de <strong>%s</strong>.
                </p>

                <p>
                    Após análise do seu perfil, decidimos seguir
                    com outros candidatos neste momento.
                </p>

                <p>
                    Agradecemos seu interesse na Proauto Kimium
                    e desejamos sucesso na sua trajetória profissional.
                </p>

                <br>

                <p>
                    Atenciosamente,<br>
                    <strong>Proauto Kimium</strong>
                </p>

            </div>
            """.formatted(nome, vaga);
    }

    public static String aprovacao(
            String nome,
            String vaga
    ) {
        return """
            <div style="font-family: Arial, sans-serif; font-size: 14px; line-height: 1.6; color: #333;">

                <p>Olá, <strong>%s</strong>!</p>

                <p>
                    Temos o prazer de informar que você foi aprovado(a)
                    no processo seletivo para a vaga de
                    <strong>%s</strong>.
                </p>

                <p>
                    Nosso time entrará em contato em breve
                    com os próximos passos e orientações.
                </p>

                <p>
                    Parabéns e seja bem-vindo(a)!
                </p>

                <br>

                <p>
                    Atenciosamente,<br>
                    <strong>Proauto Kimium</strong>
                </p>

            </div>
            """.formatted(nome, vaga);
    }
}