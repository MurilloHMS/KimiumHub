package com.proautokimium.api.Application.DTOs.newsletter;

import com.proautokimium.api.domain.entities.NewsletterPreviaCliente;

/** Um cliente da prévia, como a tela de revisão o lê. */
public record ClienteDaNewsletterDTO(
        String codigoCliente,
        String nomeDoCliente,
        String emailCliente,
        boolean recebeEmail,
        String codigoMatriz,
        String nomeMatriz,

        int quantidadeNotasEmitidas,
        int quantidadeDeProdutos,
        double quantidadeDeLitros,
        int quantidadeDeVisitas,
        int mediaDiasAtendimento,
        String produtoEmDestaque,

        double faturamentoTotal,
        double valorDePecasTrocadas,
        double valorTotalDeHoras,
        double valorTotalCobradoHoras,
        boolean mauUso,
        double valorTotalDeHorasMauUso,
        double valorTotalCobradoHorasMauUso
) {
    public static ClienteDaNewsletterDTO de(NewsletterPreviaCliente c) {
        return new ClienteDaNewsletterDTO(
                c.getCodigoCliente(),
                c.getNomeDoCliente(),
                c.getEmailCliente(),
                c.isRecebeEmail(),
                c.getCodigoMatriz(),
                c.getNomeMatriz(),
                c.getQuantidadeNotasEmitidas(),
                c.getQuantidadeDeProdutos(),
                c.getQuantidadeDeLitros(),
                c.getQuantidadeDeVisitas(),
                c.getMediaDiasAtendimento(),
                c.getProdutoEmDestaque(),
                c.getFaturamentoTotal(),
                c.getValorDePecasTrocadas(),
                c.getValorTotalDeHoras(),
                c.getValorTotalCobradoHoras(),
                c.isMauUso(),
                c.getValorTotalDeHorasMauUso(),
                c.getValorTotalCobradoHorasMauUso()
        );
    }
}
