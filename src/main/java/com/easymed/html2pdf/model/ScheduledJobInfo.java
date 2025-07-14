package com.easymed.html2pdf.model;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ScheduledJobInfo {
    private UUID id;
    private PdfJobStatus jobStatus;
    private LocalDateTime scheduledTime;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String errorMessage;
    
    // PDF Request Details
    private String title;
    private int pageCount;
    private boolean rtl;
    private boolean addToc;
    private boolean outline;
    private boolean grayscale;
    private boolean lowQuality;
    private int copies;
    private boolean printMediaType;
    private boolean addPageNumbering;
    private String pageNumberingText;
    private Map<String, String> globalOptions;
    private String logLevel;
    private boolean normalizeColors;
    private String headerHtml;
    private String footerHtml;
    private String dumpOutline;
    private String pageSize;
    private String orientation;
    private String margins;
    private boolean hasCustomHeader;
    private boolean hasCustomFooter;
} 