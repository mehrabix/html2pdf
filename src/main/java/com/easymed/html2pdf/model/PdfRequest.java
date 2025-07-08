package com.easymed.html2pdf.model;

import java.util.List;
import java.util.Map;

public class PdfRequest {
    private List<String> htmlPages;
    private Map<String, String> globalOptions;
    private List<Map<String, String>> pageOptions;
    private String scheduledTime;

    public PdfRequest() {}

    public List<String> getHtmlPages() {
        return htmlPages;
    }

    public void setHtmlPages(List<String> htmlPages) {
        this.htmlPages = htmlPages;
    }

    public Map<String, String> getGlobalOptions() {
        return globalOptions;
    }

    public void setGlobalOptions(Map<String, String> globalOptions) {
        this.globalOptions = globalOptions;
    }

    public List<Map<String, String>> getPageOptions() {
        return pageOptions;
    }

    public void setPageOptions(List<Map<String, String>> pageOptions) {
        this.pageOptions = pageOptions;
    }

    public String getScheduledTime() {
        return scheduledTime;
    }

    public void setScheduledTime(String scheduledTime) {
        this.scheduledTime = scheduledTime;
    }
} 