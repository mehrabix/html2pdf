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
    private final String ltrCss;

    public Html2PdfService() {
        String regularFont = loadFontAsBase64("static/fonts/Vazirmatn-Regular.woff2");
        String boldFont = loadFontAsBase64("static/fonts/Vazirmatn-Bold.woff2");

        this.rtlCss = String.format(
            "@font-face { font-family: 'Vazirmatn'; src: url(data:font/woff2;base64,%s) format('woff2'); font-weight: normal; font-style: normal; }" +
            "@font-face { font-family: 'Vazirmatn'; src: url(data:font/woff2;base64,%s) format('woff2'); font-weight: bold; font-style: normal; }" +
            "* { font-family: 'Vazirmatn', Tahoma, Arial, 'Arial Unicode MS', sans-serif !important; }" +
            "body { direction: rtl !important; text-align: right !important; }" +
            "p, div, span, h1, h2, h3, h4, h5, h6, td, th, li { direction: rtl !important; text-align: right !important; }" +
            "table { direction: rtl !important; }" +
            "ul, ol { direction: rtl !important; text-align: right !important; }" +
            "input, textarea, select { direction: rtl !important; text-align: right !important; }",
            regularFont, boldFont
        );

        this.ltrCss = "* { font-family: Arial, sans-serif !important; } body { direction: ltr !important; text-align: left !important; }";
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
        if (request == null) {
            return errorPdf("درخواست نامعتبر است.");
        }

        String normalizationCss = request.isNormalizeColors() ? "html, body, div, span, applet, object, iframe, h1, h2, h3, h4, h5, h6, p, blockquote, pre, a, abbr, acronym, address, big, cite, code, del, dfn, em, img, ins, kbd, q, s, samp, small, strike, strong, sub, sup, tt, var, b, u, i, center, dl, dt, dd, ol, ul, li, fieldset, form, label, legend, table, caption, tbody, tfoot, thead, tr, th, td, article, aside, canvas, details, embed, figure, figcaption, footer, header, hgroup, menu, nav, output, ruby, section, summary, time, mark, audio, video, input, textarea, select, button, * { background: #fff !important; background-color: #fff !important; background-image: none !important; background-repeat: no-repeat !important; background-attachment: scroll !important; background-position: 0% 0% !important; color: #222 !important; box-shadow: none !important; border-color: #ccc !important; text-shadow: none !important; filter: brightness(1.2) !important; } *::before, *::after { background: #fff !important; background-color: #fff !important; background-image: none !important; } [style*=\"background\"] { background: #fff !important; background-color: #fff !important; background-image: none !important; } [style*=\"Background\"] { background: #fff !important; background-color: #fff !important; background-image: none !important; } [style*=\"BACKGROUND\"] { background: #fff !important; background-color: #fff !important; background-image: none !important; } [class*=\"bg-\"], [class*=\"background-\"], [class*=\"dark\"], [class*=\"black\"], [class*=\"gray\"], [class*=\"grey\"] { background: #fff !important; background-color: #fff !important; background-image: none !important; } [id*=\"bg-\"], [id*=\"background-\"], [id*=\"dark\"], [id*=\"black\"], [id*=\"gray\"], [id*=\"grey\"] { background: #fff !important; background-color: #fff !important; background-image: none !important; } [style*=\"#7e7e7e\"], [style*=\"#7E7E7E\"], [style*=\"rgb(126,126,126)\"], [style*=\"rgb(126, 126, 126)\"] { background: #fff !important; background-color: #fff !important; color: #222 !important; } [style*=\"#666\"], [style*=\"#777\"], [style*=\"#888\"], [style*=\"#999\"], [style*=\"#aaa\"], [style*=\"#bbb\"] { background: #fff !important; background-color: #fff !important; color: #222 !important; }" : "";
        
        String normalizationScript = request.isNormalizeColors() ? "<script>window.addEventListener('load', function() { document.querySelectorAll('*').forEach(function(el) { el.style.setProperty('background', '#fff', 'important'); el.style.setProperty('background-color', '#fff', 'important'); el.style.setProperty('background-image', 'none', 'important'); el.style.setProperty('color', '#222', 'important'); el.style.setProperty('box-shadow', 'none', 'important'); }); });</script>" : "";

        File tempHeaderFile = null;
        File tempFooterFile = null;

        try {
            String executable = WrapperConfig.findExecutable();
            if (executable == null) {
                return errorPdf("برنامه wkhtmltopdf پیدا نشد. لطفاً نصب بودن آن را بررسی کنید.");
            }
            
            WrapperConfig config = new WrapperConfig(executable);
            Pdf pdf = new Pdf(config);
            
            pdf.setTimeout(60);
            
            pdf.addParam(new Param("--disable-smart-shrinking"));
            pdf.addParam(new Param("--load-error-handling", "ignore"));
            pdf.addParam(new Param("--load-media-error-handling", "ignore"));
            pdf.addParam(new Param("--disable-external-links"));
            pdf.addParam(new Param("--disable-forms"));
            pdf.addParam(new Param("--disable-plugins"));
            pdf.addParam(new Param("--no-stop-slow-scripts"));
            pdf.addParam(new Param("--javascript-delay", "200"));
            
            pdf.addParam(new Param("--encoding", "UTF-8"));
            pdf.addParam(new Param("--enable-local-file-access"));

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
                log.info("Processing footer - pageNumbering: {}, hasCustomFooter: {}", addPageNumbering, hasCustomFooter);
                
                String footerContent = createFooterContent(request, addPageNumbering, footerHtml, hasCustomFooter, normalizationCss, normalizationScript);
                if (footerContent.startsWith("ERROR:")) {
                    log.error("Footer content creation failed: {}", footerContent);
                    return errorPdf(footerContent.substring(6));
                }
                
                try {
                    tempFooterFile = File.createTempFile("footer", ".html");
                    tempFooterFile.deleteOnExit();
                    
                    try (FileWriter writer = new FileWriter(tempFooterFile, StandardCharsets.UTF_8)) {
                        writer.write(footerContent);
                        writer.flush();
                    }
                    
                    if (!tempFooterFile.exists() || tempFooterFile.length() == 0) {
                        log.error("Footer file was not created properly: {}", tempFooterFile.getAbsolutePath());
                        return errorPdf("خطا در ایجاد فایل فوتر موقت");
                    }
                    
                    String footerFilePath = tempFooterFile.getAbsolutePath();
                    pdf.addParam(new Param("--footer-html", footerFilePath));
                    
                    log.info("Footer configured successfully: file={}, size={} bytes", footerFilePath, tempFooterFile.length());
                    
                } catch (IOException e) {
                    log.error("Failed to create footer temp file", e);
                    return errorPdf("خطا در ایجاد فایل موقت برای فوتر: " + e.getMessage());
                }
            }

            if (request.getHeaderHtml() != null && !request.getHeaderHtml().trim().isEmpty()) {
                log.info("Processing header HTML (length: {})", request.getHeaderHtml().length());
                
                String headerContent = createHeaderContent(request, request.getHeaderHtml().trim(), normalizationCss, normalizationScript);
                if (headerContent.startsWith("ERROR:")) {
                    log.error("Header content creation failed: {}", headerContent);
                    return errorPdf(headerContent.substring(6));
                }
                
                try {
                    tempHeaderFile = File.createTempFile("header", ".html");
                    tempHeaderFile.deleteOnExit();
                    
                    try (FileWriter writer = new FileWriter(tempHeaderFile, StandardCharsets.UTF_8)) {
                        writer.write(headerContent);
                        writer.flush();
                    }
                    
                    if (!tempHeaderFile.exists() || tempHeaderFile.length() == 0) {
                        log.error("Header file was not created properly: {}", tempHeaderFile.getAbsolutePath());
                        return errorPdf("خطا در ایجاد فایل هدر موقت");
                    }
                    
                    String headerFilePath = tempHeaderFile.getAbsolutePath();
                    pdf.addParam(new Param("--header-html", headerFilePath));
                    
                    pdf.addParam(new Param("--header-spacing", "5"));
                    pdf.addParam(new Param("--header-line"));
                    
                    log.info("Header configured successfully: file={}, size={} bytes", headerFilePath, tempHeaderFile.length());
                    
                } catch (IOException e) {
                    log.error("Failed to create header temp file", e);
                    return errorPdf("خطا در ایجاد فایل موقت برای هدر: " + e.getMessage());
                }
                
                boolean hasCustomTopMargin = false;
                String currentTopMargin = "10mm";
                
                if (request.getGlobalOptions() != null) {
                    String topMargin = request.getGlobalOptions().get("--margin-top");
                    if (topMargin != null && !topMargin.trim().isEmpty()) {
                        hasCustomTopMargin = true;
                        currentTopMargin = topMargin.trim();
                        
                        try {
                            String numericPart = currentTopMargin.replaceAll("[^0-9.]", "");
                            double marginValue = Double.parseDouble(numericPart);
                            String unit = currentTopMargin.replaceAll("[0-9.]", "").toLowerCase();
                            
                            double marginInMm = marginValue;
                            if (unit.equals("in")) {
                                marginInMm = marginValue * 25.4;
                            } else if (unit.equals("pt")) {
                                marginInMm = marginValue * 0.352778;
                            } else if (unit.equals("cm")) {
                                marginInMm = marginValue * 10;
                            }
                            
                            if (marginInMm < 15) {
                                log.warn("Top margin {} is too small for header. Increasing to 20mm.", currentTopMargin);
                                pdf.addParam(new Param("--margin-top", "20mm"));
                            }
                        } catch (NumberFormatException e) {
                            log.warn("Could not parse top margin value: {}. Using 20mm for header.", currentTopMargin);
                            pdf.addParam(new Param("--margin-top", "20mm"));
                        }
                    }
                }
                
                if (!hasCustomTopMargin) {
                    pdf.addParam(new Param("--margin-top", "20mm"));
                }
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
                
                String enhancedHtml = createEnhancedHtml(html, request, i, normalizationCss, normalizationScript);
                
                var page = pdf.addPageFromString(enhancedHtml);
                
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
            
            if (request.isAddToc()) {
                boolean hasHeadings = hasHeadingsInPages(htmlPages);
                if (hasHeadings) {
                    pdf.addToc();
                }
            }
            
            log.info("Starting PDF generation with {} pages", htmlPages.size());
            byte[] pdfBytes = pdf.getPDF();
            log.info("PDF generation completed successfully, size: {} bytes", pdfBytes.length);
            return pdfBytes;
            
        } catch (Exception e) {
            log.error("Error generating PDF: {}", e.getMessage(), e);
            
            String errorMessage;
            if (e.getMessage() != null && e.getMessage().contains("timeout")) {
                errorMessage = "تولید PDF بیش از حد انتظار طول کشید. لطفاً محتوای هدر و فوتر را ساده‌تر کنید یا سعی مجدد نمایید.";
            } else if (e.getMessage() != null && e.getMessage().contains("not found")) {
                errorMessage = "برنامه wkhtmltopdf پیدا نشد یا در دسترس نیست.";
            } else if (e.getMessage() != null && e.getMessage().contains("permission")) {
                errorMessage = "دسترسی کافی برای تولید PDF وجود ندارد.";
            } else {
                errorMessage = "خطای ناشناخته در تولید PDF: " + (e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName());
            }
            
            return errorPdf(errorMessage);
        } finally {
            if (tempHeaderFile != null && tempHeaderFile.exists()) {
                try {
                    if (!tempHeaderFile.delete()) {
                        log.warn("Failed to delete temp header file: {}", tempHeaderFile.getAbsolutePath());
                    }
                } catch (Exception e) {
                    log.warn("Error deleting temp header file: {}", tempHeaderFile.getAbsolutePath(), e);
                }
            }
            if (tempFooterFile != null && tempFooterFile.exists()) {
                try {
                    if (!tempFooterFile.delete()) {
                        log.warn("Failed to delete temp footer file: {}", tempFooterFile.getAbsolutePath());
                    }
                } catch (Exception e) {
                    log.warn("Error deleting temp footer file: {}", tempFooterFile.getAbsolutePath(), e);
                }
            }
        }
    }

    private String createEnhancedHtml(String html, PdfRequest request, int pageIndex, String normalizationCss, String normalizationScript) {
        StringBuilder headContent = new StringBuilder();
        headContent.append("<meta charset='UTF-8'>");
        
        if (pageIndex == 0 && request.getTitle() != null && !request.getTitle().trim().isEmpty()) {
            headContent.append("<title>").append(escapeHtml(request.getTitle().trim())).append("</title>");
        }
        
        StringBuilder cssContent = new StringBuilder();
        
        if (request.isRtl()) {
            cssContent.append(this.rtlCss);
        } else {
            cssContent.append(this.ltrCss);
        }
        
        if (request.isNormalizeColors()) {
            cssContent.append(normalizationCss);
        }
        
        headContent.append("<style>").append(cssContent).append("</style>");
        
        if (request.isNormalizeColors()) {
            headContent.append(normalizationScript);
        }
        
        if (html.toLowerCase().contains("<head>")) {
            html = html.replaceFirst("(?i)<head>", "<head>" + headContent.toString());
        } else if (html.toLowerCase().contains("<html>")) {
            html = html.replaceFirst("(?i)<html>", "<html><head>" + headContent.toString() + "</head>");
        } else {
            html = "<html><head>" + headContent.toString() + "</head><body>" + html + "</body></html>";
        }
        
        return html;
    }

    private String createFooterContent(PdfRequest request, boolean addPageNumbering, String footerHtml, boolean hasCustomFooter, String normalizationCss, String normalizationScript) {
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
            footerContent = "<body><div style='text-align: center; font-size: 10px;'>" + escapeHtml(pageText) + "</div></body>";
        } else {
            if (!footerHtml.toLowerCase().contains("<html") || !footerHtml.toLowerCase().contains("<body")) {
                return "ERROR:مقدار footerHtml باید یک HTML کامل باشد.";
            }
            footerContent = footerHtml.substring(footerHtml.indexOf("<body"));
        }
        
        String directionCss = request.isRtl() ? rtlCss : ltrCss;
        
        String footerSpecificCss = "body { margin: 0; padding: 5px; font-size: 10pt; overflow: hidden; } " +
                                  "div, p, span { margin: 0; padding: 2px; } " +
                                  "* { box-sizing: border-box; max-width: 100%; }";
        
        String safeNormalizationScript = request.isNormalizeColors() ? 
            "<script>window.onload=function(){document.body.style.background='#fff';};</script>" : "";
        
        return "<!DOCTYPE html><html><head><meta charset='UTF-8'>" +
               "<meta name='viewport' content='width=device-width, initial-scale=1'>" +
               "<style>" + directionCss + normalizationCss + footerSpecificCss + "</style>" + 
               safeNormalizationScript + script + "</head>" + footerContent + "</html>";
    }

    private String createHeaderContent(PdfRequest request, String headerHtml, String normalizationCss, String normalizationScript) {
        log.debug("Creating header content for header HTML: {}", headerHtml.substring(0, Math.min(100, headerHtml.length())));
        
        if (!headerHtml.toLowerCase().contains("<html") || !headerHtml.toLowerCase().contains("<body")) {
            return "ERROR:مقدار headerHtml باید یک HTML کامل باشد که شامل تگ‌های <html> و <body> باشد.";
        }
        
        String directionCss = request.isRtl() ? rtlCss : ltrCss;
        
        String bodyContent;
        int bodyIndex = headerHtml.toLowerCase().indexOf("<body");
        if (bodyIndex == -1) {
            return "ERROR:تگ <body> در headerHtml پیدا نشد.";
        }
        
        bodyContent = headerHtml.substring(bodyIndex);
        
        String headerSpecificCss = "body { margin: 0; padding: 5px; font-size: 10pt; overflow: hidden; } " +
                                  "div, p, span { margin: 0; padding: 2px; } " +
                                  "* { box-sizing: border-box; max-width: 100%; } " +
                                  "img { max-width: 100%; height: auto; } " +
                                  "table { table-layout: fixed; width: 100%; }";
        
        String safeNormalizationScript = request.isNormalizeColors() ? 
            "<script>window.onload=function(){document.body.style.background='#fff';};</script>" : "";
        
        String fullHeaderContent = "<!DOCTYPE html><html><head><meta charset='UTF-8'>" +
                                 "<meta name='viewport' content='width=device-width, initial-scale=1'>" +
                                 "<style>" + directionCss + normalizationCss + headerSpecificCss + "</style>" + 
                                 safeNormalizationScript + "</head>" + bodyContent + "</html>";
        
        log.debug("Generated header content length: {}", fullHeaderContent.length());
        return fullHeaderContent;
    }

    private boolean hasHeadingsInPages(List<String> htmlPages) {
        for (String html : htmlPages) {
            if (html != null && (html.toLowerCase().contains("<h1") || 
                                html.toLowerCase().contains("<h2") || 
                                html.toLowerCase().contains("<h3") || 
                                html.toLowerCase().contains("<h4") || 
                                html.toLowerCase().contains("<h5") || 
                                html.toLowerCase().contains("<h6"))) {
                return true;
            }
        }
        return false;
    }

    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&#39;");
    }

    private byte[] errorPdf(String message) {
        String html = "<html><head><meta charset='UTF-8'><style>" + this.rtlCss + "</style></head>" +
                      "<body><p>" + escapeHtml(message) + "</p></body></html>";
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