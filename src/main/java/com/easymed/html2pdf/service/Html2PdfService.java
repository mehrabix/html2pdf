package com.easymed.html2pdf.service;

import java.util.List;
import java.util.Map;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

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
            if (executable == null) {
                return errorPdf("برنامه wkhtmltopdf پیدا نشد. لطفاً نصب بودن آن را بررسی کنید.");
            }
            Pdf pdf = new Pdf(new WrapperConfig(executable));
            if (request.getGlobalOptions() != null) {
                for (Map.Entry<String, String> entry : request.getGlobalOptions().entrySet()) {
                    String key = entry.getKey();
                    String value = entry.getValue();
                    if (key != null && !key.trim().isEmpty() && value != null && !value.trim().isEmpty()) {
                        pdf.addParam(new Param(key.trim(), value.trim()));
                    }
                }
            }
            if (request.isOutline()) {
                pdf.addParam(new Param("--outline"));
            }
            if (request.getDumpOutline() != null && !request.getDumpOutline().trim().isEmpty()) {
                String outlineName = request.getDumpOutline().trim();
                if (!outlineName.toLowerCase().endsWith(".xml")) {
                    return errorPdf("نام فایل dump-outline باید با .xml تمام شود.");
                }
                File dumpFile = new File(outlineName);
                if (!dumpFile.isAbsolute()) {
                    dumpFile = new File(System.getProperty("java.io.tmpdir"), outlineName);
                }
                try {
                    if (!dumpFile.getParentFile().exists()) {
                        dumpFile.getParentFile().mkdirs();
                    }
                    if (!dumpFile.exists()) {
                        dumpFile.createNewFile();
                    }
                    if (!dumpFile.canWrite()) {
                        return errorPdf("فایل dump-outline قابل نوشتن نیست: " + dumpFile.getAbsolutePath());
                    }
                } catch (IOException e) {
                    return errorPdf("امکان ایجاد یا نوشتن فایل dump-outline وجود ندارد: " + dumpFile.getAbsolutePath());
                }
                pdf.addParam(new Param("--dump-outline", dumpFile.getAbsolutePath()));
            }
            if (request.isGrayscale()) {
                pdf.addParam(new Param("--grayscale"));
            }
            if (request.isLowQuality()) {
                pdf.addParam(new Param("--lowquality"));
            }
            if (request.getLogLevel() != null && !request.getLogLevel().trim().isEmpty()) {
                String level = request.getLogLevel().trim();
                if (!level.equals("none") && !level.equals("error") && !level.equals("warn") && !level.equals("info")) {
                    return errorPdf("مقدار log-level نامعتبر است. فقط یکی از none, error, warn, info مجاز است.");
                }
                pdf.addParam(new Param("--log-level", level));
            }
            if (request.getCopies() > 1) {
                pdf.addParam(new Param("--copies", String.valueOf(request.getCopies())));
            }
            if (request.getTitle() != null && !request.getTitle().trim().isEmpty()) {
                pdf.addParam(new Param("--title", request.getTitle().trim()));
            }
            if (request.isPrintMediaType()) {
                pdf.addParam(new Param("--print-media-type"));
            }
            File tempHeaderFile = null;
            if (request.getHeaderHtml() != null && !request.getHeaderHtml().trim().isEmpty()) {
                String headerHtml = request.getHeaderHtml().trim();
                if (!headerHtml.toLowerCase().contains("<html") || !headerHtml.toLowerCase().contains("<body")) {
                    return errorPdf("مقدار headerHtml باید یک HTML کامل باشد.");
                }
                tempHeaderFile = File.createTempFile("header", ".html");
                try (FileWriter writer = new FileWriter(tempHeaderFile, java.nio.charset.StandardCharsets.UTF_8)) {
                    writer.write(headerHtml);
                }
                pdf.addParam(new Param("--header-html", tempHeaderFile.getAbsolutePath()));
            }
            File tempFooterFile = null;
            boolean useFooterHtml = request.getFooterHtml() != null && !request.getFooterHtml().trim().isEmpty();
            if (useFooterHtml) {
                String footerHtml = request.getFooterHtml().trim();
                if (!footerHtml.toLowerCase().contains("<html") || !footerHtml.toLowerCase().contains("<body")) {
                    return errorPdf("مقدار footerHtml باید یک HTML کامل باشد.");
                }
                tempFooterFile = File.createTempFile("footer", ".html");
                try (FileWriter writer = new FileWriter(tempFooterFile, java.nio.charset.StandardCharsets.UTF_8)) {
                    writer.write(footerHtml);
                }
            }
            List<String> htmlPages = request.getHtmlPages();
            if (htmlPages == null || htmlPages.isEmpty()) {
                return errorPdf("هیچ صفحه‌ای برای تولید PDF ارسال نشده است.");
            }
            List<Map<String, String>> pageOptions = request.getPageOptions();
            boolean rtl = request.isRtl();
            for (int i = 0; i < htmlPages.size(); i++) {
                String html = htmlPages.get(i);
                if (html == null || html.trim().isEmpty()) {
                    return errorPdf("صفحه HTML شماره " + (i+1) + " خالی است.");
                }
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
                if (useFooterHtml && tempFooterFile != null) {
                    page.addParam(new Param("--footer-html", tempFooterFile.getAbsolutePath()));
                } else if (pageOptions != null && i < pageOptions.size() && pageOptions.get(i) != null) {
                    for (Map.Entry<String, String> entry : pageOptions.get(i).entrySet()) {
                        String key = entry.getKey();
                        String value = entry.getValue();
                        if (key != null && !key.trim().isEmpty() && value != null && !value.trim().isEmpty()) {
                            page.addParam(new Param(key.trim(), value.trim()));
                        }
                    }
                }
            }
            if (request.isAddToc()) {
                pdf.addToc();
            }
            byte[] pdfBytes = null;
            try {
                pdfBytes = pdf.getPDF();
            } catch (Exception e) {
                return errorPdf("خطا در تولید PDF: " + e.getMessage());
            }
            if (tempHeaderFile != null && tempHeaderFile.exists()) {
                tempHeaderFile.delete();
            }
            if (tempFooterFile != null && tempFooterFile.exists()) {
                tempFooterFile.delete();
            }
            return pdfBytes;
        } catch (Exception e) {
            return errorPdf("خطای ناشناخته در تولید PDF: " + e.getMessage());
        }
    }

    private byte[] errorPdf(String message) {
        String html = "<html><head><meta charset='UTF-8'></head><body style='font-family:tahoma;direction:rtl;color:red;'><h2>خطا</h2><p>" + message + "</p></body></html>";
        try {
            String executable = WrapperConfig.findExecutable();
            if (executable == null) {
                return html.getBytes(StandardCharsets.UTF_8);
            }
            Pdf pdf = new Pdf(new WrapperConfig(executable));
            pdf.addPageFromString(html);
            return pdf.getPDF();
        } catch (Exception e) {
            return html.getBytes(StandardCharsets.UTF_8);
        }
    }
} 