package com.proautokimium.api.Infrastructure.services.email.newsletter.reader;

import com.proautokimium.api.Infrastructure.abstractions.excel.ExcelReader;
import com.proautokimium.api.domain.entities.Newsletter;
import com.proautokimium.api.domain.enums.EmailStatus;
import org.apache.poi.ss.usermodel.Row;
import org.springframework.stereotype.Service;

@Service
public class NewsletterOneFileReaderService extends ExcelReader<Newsletter> {

    @Override
    protected int getFirstDataRow(){ return 3;}

    @Override
    protected Newsletter mapRow(Row row){
        Newsletter newsletter = new Newsletter();

        newsletter.setCodigoCliente(getString(row, 0));
        newsletter.setMatrizCode(getString(row, 1));
        newsletter.setNomeDoCliente(getString(row, 2));
        newsletter.setMatrizName(getString(row, 3));
        newsletter.setData(getDate(row, 4));
        newsletter.setMes(getString(row, 5));
        newsletter.setQuantidadeDeProdutos(getInteger(row, 6));
        newsletter.setQuantidadeDeLitros(getDouble(row, 7));
        newsletter.setQuantidadeDeVisitas(getInteger(row, 8));
        newsletter.setQuantidadeNotasEmitidas(getInteger(row, 9));
        newsletter.setMediaDiasAtendimento(getInteger(row, 10));
        newsletter.setProdutoEmDestaque(getString(row, 11));
        newsletter.setFaturamentoTotal(getDouble(row, 12));
        newsletter.setValorDePecasTrocadas(getDouble(row, 13));
        newsletter.setValorTotalDeHoras(getDouble(row, 14));
        newsletter.setValorTotalCobradoHoras(getDouble(row, 15));
        newsletter.setValorTotalCobradoHorasMauUso(getDouble(row, 17));
        newsletter.setValorTotalDeHorasMauUso(getDouble(row, 18));
        newsletter.setEmailCliente(getString(row, 19));
        newsletter.setStatus(EmailStatus.PENDING);

        return newsletter;
    }
}
