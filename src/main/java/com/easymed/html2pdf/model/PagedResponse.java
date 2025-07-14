package com.easymed.html2pdf.model;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PagedResponse<T> {
    private List<T> data;
    private PaginationInfo pagination;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaginationInfo {
        private int skip;
        private int take;
        private long totalCount;
        private int totalPages;
        private boolean hasNext;
        private boolean hasPrevious;
        private String sortBy;
        private String sortDirection;
        
        public static PaginationInfo create(int skip, int take, long totalCount, String sortBy, String sortDirection) {
            int totalPages = (int) Math.ceil((double) totalCount / take);
            boolean hasNext = skip + take < totalCount;
            boolean hasPrevious = skip > 0;
            
            return new PaginationInfo(skip, take, totalCount, totalPages, hasNext, hasPrevious, sortBy, sortDirection);
        }
    }
} 