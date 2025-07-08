package com.easymed.html2pdf.service;

import com.github.jhonnymertz.wkhtmltopdf.wrapper.Pdf;
import com.github.jhonnymertz.wkhtmltopdf.wrapper.params.Param;
import com.easymed.html2pdf.model.PdfRequest;
import com.github.jhonnymertz.wkhtmltopdf.wrapper.configurations.WrapperConfig;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Map;

@Service
public class Html2PdfService {
    public byte[] generatePdf(PdfRequest request) {
        try {
            String executable = WrapperConfig.findExecutable();
            Pdf pdf = new Pdf(new WrapperConfig(executable));
            // Add global options
            if (request.getGlobalOptions() != null) {
                for (Map.Entry<String, String> entry : request.getGlobalOptions().entrySet()) {
                    pdf.addParam(new Param(entry.getKey(), entry.getValue()));
                }
            }
            // Add pages
            List<String> htmlPages = request.getHtmlPages();
            List<Map<String, String>> pageOptions = request.getPageOptions();
            for (int i = 0; i < htmlPages.size(); i++) {
                var page = pdf.addPageFromString(htmlPages.get(i));
                if (pageOptions != null && i < pageOptions.size() && pageOptions.get(i) != null) {
                    for (Map.Entry<String, String> entry : pageOptions.get(i).entrySet()) {
                        page.addParam(new Param(entry.getKey(), entry.getValue()));
                    }
                }
            }
            return pdf.getPDF();
        } catch (Exception e) {
            throw new RuntimeException("PDF generation failed: " + e.getMessage(), e);
        }
    }
} 