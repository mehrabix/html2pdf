package com.easymed.html2pdf.model;

public class PdfJobResult {
    private byte[] pdf;
    private String error;
    public PdfJobResult(byte[] pdf, String error) {
        this.pdf = pdf;
        this.error = error;
    }
    public byte[] getPdf() { return pdf; }
    public String getError() { return error; }
} 