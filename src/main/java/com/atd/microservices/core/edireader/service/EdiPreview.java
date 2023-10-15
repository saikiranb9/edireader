package com.atd.microservices.core.edireader.service;

import java.io.IOException;
import java.io.Reader;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.berryworks.edireader.DefaultXMLTags;
import com.berryworks.edireader.EDIReader;
import com.berryworks.edireader.EDIStandard;
import com.berryworks.edireader.util.sax.EDIReaderSAXAdapter;

public class EdiPreview {

    private EDIStandard standard;
    private String documentType;
    private String version;
    private String ackRequested;

    public EdiPreview(Reader edi) throws SAXException, IOException {
        EDIReader ediReader = new EDIReader();
        ediReader.setContentHandler(new PreviewHandler(this));
        ediReader.setSyntaxExceptionHandler(e -> {
            return true; // Ignore
        });
        ediReader.parse(new InputSource(edi));
    }

    public EDIStandard getStandard() {
        return standard;
    }

    private void setStandard(EDIStandard standard) {
        this.standard = standard;
    }

    public String getDocumentType() {
        return documentType;
    }

    private void setDocumentType(String documentType) {
        this.documentType = documentType;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }
    
    public String getAcknowledgementRequested() {
        return ackRequested;
    }

    public void setAcknowledgementRequested(String ackRequested) {
        this.ackRequested = ackRequested;
    }
    private static class PreviewHandler extends EDIReaderSAXAdapter {

        private final EdiPreview ediPreview;

        public PreviewHandler(EdiPreview ediPreview) {
            super(new DefaultXMLTags());
            this.ediPreview = ediPreview;
        }
        
        @Override
        protected void beginInterchange(int charCount, int segmentCharCount, Attributes attributes) {
            String standardAsString = attributes.getValue("Standard");
            ediPreview.setAcknowledgementRequested(attributes.getValue("AckRequest"));
            switch (standardAsString) {
                case "ANSI X.12":
                    ediPreview.setStandard(EDIStandard.ANSI);
                    break;
                case "EDIFACT":
                    ediPreview.setStandard(EDIStandard.EDIFACT);
                    break;
            }
        }

        @Override
        protected void beginExplicitGroup(int charCount, int segmentCharCount, Attributes attributes) {
            String version = attributes.getValue("StandardVersion");
            if (ediPreview.getStandard() == EDIStandard.ANSI) {
                if (version.length() > 6) {
                    version = version.substring(0, 6);
                }
            }
            ediPreview.setVersion(version);
        }

        @Override
        protected void beginDocument(int charCount, int segmentCharCount, Attributes attributes) {
            String documentType = attributes.getValue("DocType");
            ediPreview.setDocumentType(documentType);
            if (ediPreview.getStandard() == EDIStandard.EDIFACT) {
                String v = attributes.getValue("Version");
                String r = attributes.getValue("Release");
                ediPreview.setVersion(v + r);
            }
        }
    }
}