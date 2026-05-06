package com.proautokimium.api.Infrastructure.converters;

import com.proautokimium.api.Application.DTOs.email.NewsletterResponseDTO;
import com.proautokimium.api.domain.entities.Newsletter;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class NewsletterConverter{

    public NewsletterResponseDTO toDto(Newsletter entity){
        return new NewsletterResponseDTO(
                entity.getCodigoCliente(),
                entity.getNomeDoCliente(),
                entity.getData(),
                entity.getMes(),
                entity.getQuantidadeDeProdutos(),
                entity.getQuantidadeDeLitros(),
                entity.getQuantidadeNotasEmitidas(),
                entity.getMediaDiasAtendimento(),
                entity.getProdutoEmDestaque(),
                entity.getFaturamentoTotal(),
                entity.getValorDePecasTrocadas(),
                entity.getValorTotalDeHoras(),
                entity.getValorTotalCobradoHoras(),
                entity.isMauUso(),
                entity.getValorTotalDeHorasMauUso(),
                entity.getValorTotalCobradoHorasMauUso(),
                entity.getStatus(),
                entity.getEmailCliente(),
                entity.getMatrizCode(),
                entity.getMatrizName()
        );
    }
}
