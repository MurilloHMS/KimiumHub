package com.proautokimium.api.Infrastructure.services.email.newsletter.reader;

import com.proautokimium.api.Infrastructure.abstractions.excel.SheetRow;
import com.proautokimium.api.domain.entities.Newsletter;
import com.proautokimium.api.domain.enums.EmailStatus;
import org.springframework.stereotype.Service;

@Service
public class NewsletterOneFileReaderService{


    public Newsletter mapRow(SheetRow row){
        Newsletter newsletter = new Newsletter();

        newsletter.setCodigoCliente(row.string(0));
        newsletter.setMatrizCode(row.string(1));
        newsletter.setNomeDoCliente(row.string(2));
        newsletter.setMatrizName(row.string(3));
        newsletter.setData(row.date(4));
        newsletter.setMes(row.string(5));
        newsletter.setQuantidadeDeProdutos(row.integer(6));
        newsletter.setQuantidadeDeLitros(row.number(7));
        newsletter.setQuantidadeDeVisitas(row.integer(8));
        newsletter.setQuantidadeNotasEmitidas(row.integer(9));
        newsletter.setMediaDiasAtendimento(row.integer(10));
        newsletter.setProdutoEmDestaque(row.string(11));
        newsletter.setFaturamentoTotal(row.number(12));
        newsletter.setValorDePecasTrocadas(row.number(13));
        newsletter.setValorTotalDeHoras(row.number(14));
        newsletter.setValorTotalCobradoHoras(row.number(15));
        newsletter.setMauUso(row.integer(16) == 1);
        newsletter.setValorTotalCobradoHorasMauUso(row.number(17));
        newsletter.setValorTotalDeHorasMauUso(row.number(18));
        newsletter.setEmailCliente(row.string(19));
        newsletter.setStatus(EmailStatus.PENDING);

        return newsletter;
    }
}
