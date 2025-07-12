package com.easymed.html2pdf.model;

import java.util.List;
import java.util.Map;

public class PdfRequest {
    private List<String> htmlPages;
    private Map<String, String> globalOptions;
    private List<Map<String, String>> pageOptions;
    private String scheduledTime;
    private boolean rtl = true;
    private String headerHtml;
    private boolean addToc;
    private boolean outline;
    private String dumpOutline;
    private boolean grayscale;
    private boolean lowQuality;
    private String logLevel;
    private int copies = 1;
    private String title;
    private boolean printMediaType;
    private String footerHtml;
    private boolean addPageNumbering;
    private String pageNumberingText;

    public PdfRequest() {}

    public List<String> getHtmlPages() { return htmlPages; }
    public void setHtmlPages(List<String> htmlPages) { this.htmlPages = htmlPages; }
    public Map<String, String> getGlobalOptions() { return globalOptions; }
    public void setGlobalOptions(Map<String, String> globalOptions) { this.globalOptions = globalOptions; }
    public List<Map<String, String>> getPageOptions() { return pageOptions; }
    public void setPageOptions(List<Map<String, String>> pageOptions) { this.pageOptions = pageOptions; }
    public String getScheduledTime() { return scheduledTime; }
    public void setScheduledTime(String scheduledTime) { this.scheduledTime = scheduledTime; }
    public boolean isRtl() { return rtl; }
    public void setRtl(boolean rtl) { this.rtl = rtl; }
    public String getHeaderHtml() { return headerHtml; }
    public void setHeaderHtml(String headerHtml) { this.headerHtml = headerHtml; }
    public boolean isAddToc() { return addToc; }
    public void setAddToc(boolean addToc) { this.addToc = addToc; }
    public boolean isOutline() { return outline; }
    public void setOutline(boolean outline) { this.outline = outline; }
    public String getDumpOutline() { return dumpOutline; }
    public void setDumpOutline(String dumpOutline) { this.dumpOutline = dumpOutline; }
    public boolean isGrayscale() { return grayscale; }
    public void setGrayscale(boolean grayscale) { this.grayscale = grayscale; }
    public boolean isLowQuality() { return lowQuality; }
    public void setLowQuality(boolean lowQuality) { this.lowQuality = lowQuality; }
    public String getLogLevel() { return logLevel; }
    public void setLogLevel(String logLevel) { this.logLevel = logLevel; }
    public int getCopies() { return copies; }
    public void setCopies(int copies) { this.copies = copies; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public boolean isPrintMediaType() { return printMediaType; }
    public void setPrintMediaType(boolean printMediaType) { this.printMediaType = printMediaType; }
    public String getFooterHtml() { return footerHtml; }
    public void setFooterHtml(String footerHtml) { this.footerHtml = footerHtml; }
    public boolean isAddPageNumbering() { return addPageNumbering; }
    public void setAddPageNumbering(boolean addPageNumbering) { this.addPageNumbering = addPageNumbering; }
    public String getPageNumberingText() { return pageNumberingText; }
    public void setPageNumberingText(String pageNumberingText) { this.pageNumberingText = pageNumberingText; }
} 