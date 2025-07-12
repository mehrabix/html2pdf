package com.easymed.html2pdf.service;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

import com.easymed.html2pdf.model.PdfRequest;
import com.github.jhonnymertz.wkhtmltopdf.wrapper.Pdf;
import com.github.jhonnymertz.wkhtmltopdf.wrapper.configurations.WrapperConfig;
import com.github.jhonnymertz.wkhtmltopdf.wrapper.params.Param;

@Service
public class Html2PdfService {

    private static final Logger log = LoggerFactory.getLogger(Html2PdfService.class);
    private final String rtlCss;

    public Html2PdfService() {
        String regularFont = loadFontAsBase64("static/fonts/Vazirmatn-Regular.woff2");
        String boldFont = loadFontAsBase64("static/fonts/Vazirmatn-Bold.woff2");

        this.rtlCss = String.format(
            "@font-face { font-family: 'Vazirmatn'; src: url(data:font/woff2;base64,%s) format('woff2'); font-weight: normal; font-style: normal; }" +
            "@font-face { font-family: 'Vazirmatn'; src: url(data:font/woff2;base64,%s) format('woff2'); font-weight: bold; font-style: normal; }" +
            "* { font-family: 'Vazirmatn', Tahoma, Arial, 'Arial Unicode MS', sans-serif !important; }" +
            "body { direction: rtl; }",
            regularFont, boldFont
        );
    }

    private String loadFontAsBase64(String path) {
        try {
            ClassPathResource fontResource = new ClassPathResource(path);
            byte[] fontBytes = StreamUtils.copyToByteArray(fontResource.getInputStream());
            return Base64.getEncoder().encodeToString(fontBytes);
        } catch (IOException e) {
            log.error("Failed to load font from path: {}", path, e);
            return "";
        }
    }

    public byte[] generatePdf(PdfRequest request) {
        // Normalization CSS for ink saving
        String normalizationCss = "html, body, div, span, applet, object, iframe, h1, h2, h3, h4, h5, h6, p, blockquote, pre, a, abbr, acronym, address, big, cite, code, del, dfn, em, img, ins, kbd, q, s, samp, small, strike, strong, sub, sup, tt, var, b, u, i, center, dl, dt, dd, ol, ul, li, fieldset, form, label, legend, table, caption, tbody, tfoot, thead, tr, th, td, article, aside, canvas, details, embed, figure, figcaption, footer, header, hgroup, menu, nav, output, ruby, section, summary, time, mark, audio, video, input, textarea, select, button, * { background: #fff !important; background-color: #fff !important; background-image: none !important; background-repeat: no-repeat !important; background-attachment: scroll !important; background-position: 0% 0% !important; color: #222 !important; box-shadow: none !important; border-color: #ccc !important; text-shadow: none !important; filter: brightness(1.2) !important; } *::before, *::after { background: #fff !important; background-color: #fff !important; background-image: none !important; } [style*=\"background\"] { background: #fff !important; background-color: #fff !important; background-image: none !important; } [style*=\"Background\"] { background: #fff !important; background-color: #fff !important; background-image: none !important; } [style*=\"BACKGROUND\"] { background: #fff !important; background-color: #fff !important; background-image: none !important; } [class*=\"bg-\"], [class*=\"background-\"], [class*=\"dark\"], [class*=\"black\"], [class*=\"gray\"], [class*=\"grey\"] { background: #fff !important; background-color: #fff !important; background-image: none !important; } [id*=\"bg-\"], [id*=\"background-\"], [id*=\"dark\"], [id*=\"black\"], [id*=\"gray\"], [id*=\"grey\"] { background: #fff !important; background-color: #fff !important; background-image: none !important; } [style*=\"#7e7e7e\"], [style*=\"#7E7E7E\"], [style*=\"rgb(126,126,126)\"], [style*=\"rgb(126, 126, 126)\"] { background: #fff !important; background-color: #fff !important; color: #222 !important; } [style*=\"#666\"], [style*=\"#777\"], [style*=\"#888\"], [style*=\"#999\"], [style*=\"#aaa\"], [style*=\"#bbb\"] { background: #fff !important; background-color: #fff !important; color: #222 !important; }";
        String normalizationScript = "<script>window.addEventListener('load', function() { document.querySelectorAll('*').forEach(function(el) { el.style.setProperty('background', '#fff', 'important'); el.style.setProperty('background-color', '#fff', 'important'); el.style.setProperty('background-image', 'none', 'important'); el.style.setProperty('color', '#222', 'important'); el.style.setProperty('box-shadow', 'none', 'important'); }); });</script>";
        File tempStyleSheet = null;
        File tempHeaderFile = null;
        File tempFooterFile = null;
        try {
            String executable = WrapperConfig.findExecutable();
            if (executable == null) {
                return errorPdf("برنامه wkhtmltopdf پیدا نشد. لطفاً نصب بودن آن را بررسی کنید.");
            }
            Pdf pdf = new Pdf(new WrapperConfig(executable));

            if (request.isRtl() && this.rtlCss != null && !this.rtlCss.isEmpty()) {
                tempStyleSheet = File.createTempFile("style-", ".css");
                try (FileWriter writer = new FileWriter(tempStyleSheet, StandardCharsets.UTF_8)) {
                    writer.write(this.rtlCss);
                }
                pdf.addParam(new Param("--user-style-sheet", tempStyleSheet.getAbsolutePath()));
            }

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
            if (request.isPrintMediaType()) {
                pdf.addParam(new Param("--print-media-type"));
            }
            
            boolean addPageNumbering = request.isAddPageNumbering();
            String footerHtml = request.getFooterHtml();
            boolean hasCustomFooter = footerHtml != null && !footerHtml.trim().isEmpty();

            if (addPageNumbering || hasCustomFooter) {
                String script = "<script>" +
                    "window.onload = function() {" +
                    "  var vars = {};" +
                    "  var x = window.location.search.substring(1).split('&');" +
                    "  for (var i = 0; i < x.length; i++) {" +
                    "    var z = x[i].split('=', 2);" +
                    "    if (z.length === 2) {" +
                    "      vars[z[0]] = decodeURIComponent(z[1]);" +
                    "    }" +
                    "  }" +
                    "  var body = document.body.innerHTML;" +
                    "  for (var key in vars) {" +
                    "    var pattern = new RegExp('\\\\[' + key + '\\\\]', 'g');" +
                    "    body = body.replace(pattern, vars[key]);" +
                    "  }" +
                    "  document.body.innerHTML = body;" +
                    "};" +
                    "</script>";
                
                String footerContent;

                if (addPageNumbering) {
                    String pageText = request.getPageNumberingText() != null ? request.getPageNumberingText() : "صفحه [page] از [topage]";
                    footerContent = "<body><div style='text-align: center; font-size: 10px;'>" + pageText + "</div></body>";
                } else {
                    if (!footerHtml.toLowerCase().contains("<html") || !footerHtml.toLowerCase().contains("<body")) {
                        return errorPdf("مقدار footerHtml باید یک HTML کامل باشد.");
                    }
                    footerContent = footerHtml.substring(footerHtml.indexOf("<body"));
                }
                
                String fullFooterHtml = "<html><head><meta charset='UTF-8'><style>" +
                (request.isRtl() ? rtlCss : "* { font-family: Arial, sans-serif !important; } body { direction: ltr; }") +
                (request.isNormalizeColors() ? normalizationCss : "") +
                "</style>" + (request.isNormalizeColors() ? normalizationScript : "") + script + "</head>" + footerContent + "</html>";
                    
                tempFooterFile = File.createTempFile("footer", ".html");
                try (FileWriter writer = new FileWriter(tempFooterFile, StandardCharsets.UTF_8)) {
                    writer.write(fullFooterHtml);
                }
                pdf.addParam(new Param("--footer-html", tempFooterFile.getAbsolutePath()));
            }

            if (request.getHeaderHtml() != null && !request.getHeaderHtml().trim().isEmpty()) {
                String headerHtml = request.getHeaderHtml().trim();
                if (!headerHtml.toLowerCase().contains("<html") || !headerHtml.toLowerCase().contains("<body")) {
                    return errorPdf("مقدار headerHtml باید یک HTML کامل باشد.");
                }
            
                String fullHeaderHtml = "<html><head><meta charset='UTF-8'><style>" +
                (request.isRtl() ? rtlCss : "* { font-family: Arial, sans-serif !important; } body { direction: ltr; }") +
                (request.isNormalizeColors() ? normalizationCss : "") +
                "</style>" + (request.isNormalizeColors() ? normalizationScript : "") + "</head>" + headerHtml.substring(headerHtml.indexOf("<body")) + "</html>";
                    
                tempHeaderFile = File.createTempFile("header", ".html");
                try (FileWriter writer = new FileWriter(tempHeaderFile, StandardCharsets.UTF_8)) {
                    writer.write(fullHeaderHtml);
                }
                pdf.addParam(new Param("--header-html", tempHeaderFile.getAbsolutePath()));
            }
            
            
            List<String> htmlPages = request.getHtmlPages();
            if (htmlPages == null || htmlPages.isEmpty()) {
                return errorPdf("هیچ صفحه‌ای برای تولید PDF ارسال نشده است.");
            }
            List<Map<String, String>> pageOptions = request.getPageOptions();
            
            for (int i = 0; i < htmlPages.size(); i++) {
                String html = htmlPages.get(i);
                if (html == null || html.trim().isEmpty()) {
                    return errorPdf("صفحه HTML شماره " + (i+1) + " خالی است.");
                }
                
                StringBuilder headContent = new StringBuilder();
                headContent.append("<meta charset='UTF-8'>");
                
                if (i == 0 && request.getTitle() != null && !request.getTitle().trim().isEmpty()) {
                    headContent.append("<title>").append(request.getTitle().trim()).append("</title>");
                }
                
                String style;
                if (request.isRtl()) {
                    style = this.rtlCss;
                } else {
                    style = "* { font-family: Arial, sans-serif !important; } body { direction: ltr; }";
                }
                if (request.isNormalizeColors()) {
                    style += normalizationCss;
                    headContent.append(normalizationScript);
                }
                headContent.append("<style>").append(style).append("</style>");
                
                if (html.contains("<head>")) {
                    html = html.replaceFirst("<head>", "<head>" + headContent);
                } else {
                    html = "<head>" + headContent + "</head>" + html;
                }
                
                
                var page = pdf.addPageFromString(html);
                if (pageOptions != null && i < pageOptions.size() && pageOptions.get(i) != null) {
                    for (Map.Entry<String, String> entry : pageOptions.get(i).entrySet()) {
                        String key = entry.getKey();
                        String value = entry.getValue();
                        if (key != null && !key.trim().isEmpty() && value != null && !value.trim().isEmpty()) {
                            page.addParam(new Param(key.trim(), value.trim()));
                        }
                    }
                }
            }
            
            // Check if content has headings for TOC generation
            boolean hasHeadings = false;
            if (request.isAddToc()) {
                for (String html : htmlPages) {
                    if (html != null && (html.toLowerCase().contains("<h1") || 
                                        html.toLowerCase().contains("<h2") || 
                                        html.toLowerCase().contains("<h3") || 
                                        html.toLowerCase().contains("<h4") || 
                                        html.toLowerCase().contains("<h5") || 
                                        html.toLowerCase().contains("<h6"))) {
                        hasHeadings = true;
                        break;
                    }
                }
                
                if (hasHeadings) {
                    pdf.addToc();
                }
            }
            
            byte[] pdfBytes = null;
            try {
                pdfBytes = pdf.getPDF();
            } catch (Exception e) {
                return errorPdf("خطا در تولید PDF: " + e.getMessage());
            }
            
            return pdfBytes;
        } catch (Exception e) {
            return errorPdf("خطای ناشناخته در تولید PDF: " + e.getMessage());
        } finally {
            if (tempStyleSheet != null && tempStyleSheet.exists()) {
                tempStyleSheet.delete();
            }
            if (tempHeaderFile != null && tempHeaderFile.exists()) {
                tempHeaderFile.delete();
            }
            if (tempFooterFile != null && tempFooterFile.exists()) {
                tempFooterFile.delete();
            }
        }
    }

    private byte[] errorPdf(String message) {
        String html = "<html><head><meta charset='UTF-8'><style>" + this.rtlCss + "</style></head>" +
                      "<body><p>" + message + "</p></body></html>";
        try {
            String executable = WrapperConfig.findExecutable();
            if (executable == null) {
                throw new RuntimeException("wkhtmltopdf not found.");
            }
            Pdf pdf = new Pdf(new WrapperConfig(executable));
            pdf.addPageFromString(html);
            return pdf.getPDF();
        } catch (Exception e) {
            log.error("Unable to generate error PDF", e);
            return new byte[0];
        }
    }
    
} 