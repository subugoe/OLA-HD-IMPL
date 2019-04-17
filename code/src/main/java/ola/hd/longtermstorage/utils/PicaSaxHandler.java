package ola.hd.longtermstorage.utils;

import ola.hd.longtermstorage.domain.SearchResult;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

import java.util.ArrayList;
import java.util.List;

public class PicaSaxHandler extends DefaultHandler {

    private List<SearchResult> extractedData;

    private boolean inRecord = false;
    private boolean inIdDatafield = false;
    private boolean inIdSubfield = false;

    private boolean inTitleDatafield = false;
    private boolean inTitleSubfield = false;

    private String tempPPN = "";
    private String tempTitle = "";

    public PicaSaxHandler() {
        extractedData = new ArrayList<>();
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) {
        if (qName.equalsIgnoreCase("zs:record")) {
            inRecord = true;
        }

        if (qName.equalsIgnoreCase("datafield") && attributes.getValue("tag").equalsIgnoreCase("006Y")) {
            inIdDatafield = true;
        }

        if (qName.equalsIgnoreCase("subfield") && attributes.getValue("code").equalsIgnoreCase("0")) {
            inIdSubfield = true;
        }

        if (qName.equalsIgnoreCase("datafield") && attributes.getValue("tag").equalsIgnoreCase("021A")) {
            inTitleDatafield = true;
        }

        if (qName.equalsIgnoreCase("subfield") && attributes.getValue("code").equalsIgnoreCase("a")) {
            inTitleSubfield = true;
        }
    }

    @Override
    public void endElement(String uri, String localName, String qName) {
        if (qName.equalsIgnoreCase("zs:record")) {
            inRecord = false;
            tempPPN = "";
            tempTitle = "";
        }

        if (qName.equalsIgnoreCase("datafield")) {
            inIdDatafield = false;
            inTitleDatafield = false;
        }

        if (qName.equalsIgnoreCase("subfield")) {
            inIdSubfield = false;
            inTitleSubfield = false;
        }
    }

    @Override
    public void characters(char[] ch, int start, int length) {
        if (inRecord && inIdDatafield && inIdSubfield) {
            String gvk = new String(ch, start, length);
            tempPPN = gvk.replace("gvk", "PPN");

            if (!tempTitle.isEmpty()) {
                extractedData.add(new SearchResult(tempPPN, tempTitle));
            }
        }

        if (inRecord && inTitleDatafield && inTitleSubfield) {
            tempTitle = new String(ch, start, length);

            if (!tempPPN.isEmpty()) {
                extractedData.add(new SearchResult(tempPPN, tempTitle));
            }
        }
    }

    public List<SearchResult> getExtractedData() {
        return extractedData;
    }
}
