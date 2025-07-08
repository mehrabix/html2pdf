package com.easymed.html2pdf.service;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.easymed.html2pdf.model.PdfRequest;
import com.github.jhonnymertz.wkhtmltopdf.wrapper.Pdf;
import com.github.jhonnymertz.wkhtmltopdf.wrapper.configurations.WrapperConfig;
import com.github.jhonnymertz.wkhtmltopdf.wrapper.params.Param;

@Service
public class Html2PdfService {
    public byte[] generatePdf(PdfRequest request) {
        try {
            String executable = WrapperConfig.findExecutable();
            Pdf pdf = new Pdf(new WrapperConfig(executable));
            if (request.getGlobalOptions() != null) {
                for (Map.Entry<String, String> entry : request.getGlobalOptions().entrySet()) {
                    pdf.addParam(new Param(entry.getKey(), entry.getValue()));
                }
            }
            List<String> htmlPages = request.getHtmlPages();
            List<Map<String, String>> pageOptions = request.getPageOptions();
            boolean rtl = request.isRtl();
            for (int i = 0; i < htmlPages.size(); i++) {
                String html = htmlPages.get(i);
                String direction = rtl ? "rtl" : "ltr";
                String font = rtl ? "Tahoma, 'Vazir', Arial, 'Arial Unicode MS', sans-serif" : "Arial, sans-serif";
                String meta = "<meta charset='UTF-8'>";
                String style = String.format("<style>body { font-family: %s; direction: %s; }</style>", font, direction);
                if (html.contains("<head>")) {
                    html = html.replaceFirst("<head>", "<head>" + meta + style);
                } else {
                    html = "<head>" + meta + style + "</head>" + html;
                }
                var page = pdf.addPageFromString(html);
                if (pageOptions != null && i < pageOptions.size() && pageOptions.get(i) != null) {
                    for (Map.Entry<String, String> entry : pageOptions.get(i).entrySet()) {
                        page.addParam(new Param(entry.getKey(), entry.getValue()));
                    }
                }
            }
            byte[] pdfBytes = pdf.getPDF();
            return pdfBytes;
        } catch (Exception e) {
            throw new RuntimeException("PDF generation failed: " + e.getMessage(), e);
        }
    }
} 