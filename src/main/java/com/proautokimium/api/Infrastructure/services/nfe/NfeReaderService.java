package com.proautokimium.api.Infrastructure.services.nfe;

import com.proautokimium.api.Infrastructure.interfaces.nfe.INfeReader;
import com.proautokimium.api.domain.models.NfeIcmsInfo;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;

@Service
public class NfeReaderService implements INfeReader {

    @Override
    public NfeIcmsInfo getIcmsByXml(InputStream stream) throws Exception {
        Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(stream);

        doc.getDocumentElement().normalize();

        NfeIcmsInfo dados = new NfeIcmsInfo();

        NodeList idelist = doc.getElementsByTagName("ide");

        if(idelist.getLength() > 0){
            Element ide = (Element) idelist.item(0);
            dados.setNNF(getTagValue("nNF", ide));
        }

        NodeList icmsTotList = doc.getElementsByTagName("ICMSTot");
        if(icmsTotList.getLength() > 0){
            Element icms = (Element) icmsTotList.item(0);

            dados.setVIcms(parseDouble(getTagValue("vICMS", icms)));
            dados.setVPis(parseDouble(getTagValue("vPIS", icms)));
            dados.setVCofins(parseDouble(getTagValue("vCOFINS", icms)));
        }
        return dados;
    }

    private String getTagValue(String tag, Element element){
        NodeList list = element.getElementsByTagName(tag);
        if(list.getLength() > 0){
            return list.item(0).getTextContent();
        }
        return null;
    }

    private double parseDouble(String value){
        try{
            return value != null ? Double.parseDouble(value) : 0.0;
        } catch (Exception e){
            return 0.0;
        }
    }
}
