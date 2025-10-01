package com.proautokimium.api.Infrastructure.services.nfe;

import com.proautokimium.api.Infrastructure.interfaces.nfe.INfeReader;
import com.proautokimium.api.domain.models.NfeDataInfo;
import com.proautokimium.api.domain.models.NfeIcmsInfo;
import org.springframework.cglib.core.Local;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

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

    @Override
    public List<NfeDataInfo> getNfeDataByXml(InputStream stream) throws Exception {
        Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(stream);

        doc.getDocumentElement().normalize();

        List<NfeDataInfo> infoList = new ArrayList<>();

        NodeList prodList = doc.getElementsByTagName("prod");

        for(int i = 0; i < prodList.getLength(); i++){
            Element prod = (Element) prodList.item(i);
            NfeDataInfo dataInfo = new NfeDataInfo();

            dataInfo.setProduct(getTagValue("xProd", prod));
            dataInfo.setUnitValue(getTagValue("vUnCom", prod));
            dataInfo.setTotalValue(getTagValue("vProd", prod));
            dataInfo.setCfop(getTagValue("CFOP", prod));

            NodeList ideList = doc.getElementsByTagName("ide");
            if(ideList.getLength() > 0){
                Element ide = (Element) ideList.item(0);
                dataInfo.setNfeNum(getTagValue("nNF", ide));
                dataInfo.setNfeDate(parseDate(getTagValue("dhEmi", ide)));
            }

            NodeList emitList = doc.getElementsByTagName("emit");
            if(emitList.getLength() > 0){
                Element emit = (Element) emitList.item(0);
                dataInfo.setPartner(getTagValue("xNome", emit));
            }

            infoList.add(dataInfo);
        }
        return infoList;
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

    private Date parseDate(String value){
        try{
            if(value != null){
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
                return sdf.parse(value);
            }
        }catch (Exception e){
            return null;
        }
        return null;
    }
}
