package com.easymed.html2pdf.model;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class ScheduledJobsQuery {
    private int skip = 0;
    private int take = 10;
    
    private String sortBy = "scheduledTime";
    private String sortDirection = "asc";
    
    private String titleFilter;
    private LocalDateTime scheduledAfter;
    private LocalDateTime scheduledBefore;
    private Integer minPageCount;
    private Integer maxPageCount;
    private Boolean rtl;
    private Boolean addToc;
    private Boolean outline;
    private Boolean grayscale;
    private Boolean lowQuality;
    private Boolean printMediaType;
    private Boolean addPageNumbering;
    private String pageSize;
    private String orientation;
    private Boolean hasCustomHeader;
    private Boolean hasCustomFooter;
    
    // Validation
    public void validate() {
        if (skip < 0) skip = 0;
        if (take < 1) take = 1;
        if (take > 100) take = 100;
        
        if (!isValidSortBy(sortBy)) {
            sortBy = "scheduledTime";
        }
        
        if (!"asc".equalsIgnoreCase(sortDirection) && !"desc".equalsIgnoreCase(sortDirection)) {
            sortDirection = "asc";
        }
    }
    
    private boolean isValidSortBy(String field) {
        return field != null && (
            field.equals("scheduledTime") ||
            field.equals("createdAt") ||
            field.equals("title") ||
            field.equals("pageCount") ||
            field.equals("copies")
        );
    }
} 