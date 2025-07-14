package com.easymed.html2pdf.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.easymed.html2pdf.entity.PdfJob;
import com.easymed.html2pdf.model.PagedResponse;
import com.easymed.html2pdf.model.PdfJobResult;
import com.easymed.html2pdf.model.PdfJobStatus;
import com.easymed.html2pdf.model.PdfRequest;
import com.easymed.html2pdf.model.ScheduledJobInfo;
import com.easymed.html2pdf.model.ScheduledJobsQuery;
import com.easymed.html2pdf.repository.PdfJobRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class AsyncPdfJobService {

    private static final Logger log = LoggerFactory.getLogger(AsyncPdfJobService.class);

    @Autowired
    private PdfJobRepository pdfJobRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private Html2PdfService html2PdfService;

    public UUID submitJob(PdfRequest request, String sessionId) {
        PdfJob pdfJob = new PdfJob();
        pdfJob.setId(UUID.randomUUID());
        pdfJob.setJobStatus(PdfJobStatus.PENDING);
        pdfJob.setSessionId(sessionId);
        
        if (request.getScheduledTime() != null && !request.getScheduledTime().trim().isEmpty()) {
            try {
                String scheduledTimeStr = request.getScheduledTime().trim();
                LocalDateTime scheduledDateTime;
                
                if (scheduledTimeStr.endsWith("Z")) {
                    scheduledDateTime = LocalDateTime.parse(scheduledTimeStr.substring(0, scheduledTimeStr.length() - 1), DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                } else {
                    scheduledDateTime = LocalDateTime.parse(scheduledTimeStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                }
                
                LocalDateTime now = LocalDateTime.now();
                LocalDateTime maxFutureTime = now.plusDays(30);
                
                if (scheduledDateTime.isBefore(now)) {
                    log.warn("Scheduled time is in the past: {}. Job will be processed immediately.", scheduledTimeStr);
                    scheduledDateTime = now;
                } else if (scheduledDateTime.isAfter(maxFutureTime)) {
                    log.warn("Scheduled time is too far in future (max 30 days): {}. Setting to 30 days from now.", scheduledTimeStr);
                    scheduledDateTime = maxFutureTime;
                }
                
                pdfJob.setScheduledTime(scheduledDateTime);
                
                if (scheduledDateTime.isAfter(now)) {
                    pdfJob.setJobStatus(PdfJobStatus.SCHEDULED);
                }
                
            } catch (DateTimeParseException e) {
                log.warn("Invalid scheduled time format: {}. Job will be processed immediately.", request.getScheduledTime());
                pdfJob.setScheduledTime(LocalDateTime.now());
            }
        } else {
            pdfJob.setScheduledTime(LocalDateTime.now());
        }
        
        try {
            pdfJob.setPdfRequestJson(objectMapper.writeValueAsString(request));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Could not serialize PDF request", e);
        }
        
        pdfJobRepository.save(pdfJob);
        return pdfJob.getId();
    }

    @Async("pdfExecutor")
    @Transactional("transactionManager")
    public void processJob(PdfJob job) {
        job.setJobStatus(PdfJobStatus.RUNNING);
        pdfJobRepository.save(job);
        try {
            PdfRequest request = objectMapper.readValue(job.getPdfRequestJson(), PdfRequest.class);
            byte[] pdf = html2PdfService.generatePdf(request);
            job.setPdfData(pdf);
            job.setJobStatus(PdfJobStatus.SUCCESS);
        } catch (Exception e) {
            job.setErrorMessage(e.getMessage());
            job.setJobStatus(PdfJobStatus.FAILED);
        }
        pdfJobRepository.save(job);
    }

    public PdfJobStatus getJobStatus(UUID jobId, String sessionId) {
        return pdfJobRepository.findByIdAndSessionId(jobId, sessionId).map(PdfJob::getJobStatus).orElse(null);
    }

    public PdfJobResult getJobResult(UUID jobId, String sessionId) {
        return pdfJobRepository.findByIdAndSessionId(jobId, sessionId)
                .filter(job -> job.getJobStatus() == PdfJobStatus.SUCCESS)
                .map(job -> new PdfJobResult(job.getPdfData(), job.getErrorMessage()))
                .orElse(null);
    }

   @Transactional("transactionManager")
    public PdfJobResult getJobResultAndClear(UUID jobId, String sessionId) {
        Optional<PdfJob> jobOptional = pdfJobRepository.findByIdAndSessionId(jobId, sessionId)
                .filter(job -> job.getJobStatus() == PdfJobStatus.SUCCESS);

        if (jobOptional.isPresent()) {
            PdfJob job = jobOptional.get();
            PdfJobResult result = new PdfJobResult(job.getPdfData(), job.getErrorMessage());
            pdfJobRepository.delete(job);
            return result;
        }
        return null;
    }
    
    public List<ScheduledJobInfo> getScheduledJobs(String sessionId) {
        List<PdfJob> jobs = pdfJobRepository.findByJobStatusAndSessionIdOrderByScheduledTimeAsc(PdfJobStatus.SCHEDULED, sessionId);
        return jobs.stream()
                .map(this::mapToScheduledJobInfo)
                .toList();
    }
    
    public PagedResponse<ScheduledJobInfo> getScheduledJobs(String sessionId, ScheduledJobsQuery query) {
        query.validate();
        
        Sort sort = createSort(query.getSortBy(), query.getSortDirection());
        Pageable pageable = PageRequest.of(query.getSkip() / query.getTake(), query.getTake(), sort);
        
        Page<PdfJob> jobsPage = pdfJobRepository.findByJobStatusAndSessionId(PdfJobStatus.SCHEDULED, sessionId, pageable);
        
        List<ScheduledJobInfo> scheduledJobInfos = jobsPage.getContent().stream()
                .map(this::mapToScheduledJobInfo)
                .filter(info -> matchesFilters(info, query))
                .toList();
        
        PagedResponse.PaginationInfo paginationInfo = PagedResponse.PaginationInfo.create(
            query.getSkip(),
            query.getTake(),
            jobsPage.getTotalElements(),
            query.getSortBy(),
            query.getSortDirection()
        );
        
        return new PagedResponse<>(scheduledJobInfos, paginationInfo);
    }
    
    private Sort createSort(String sortBy, String sortDirection) {
        Sort.Direction direction = "desc".equalsIgnoreCase(sortDirection) ? 
            Sort.Direction.DESC : Sort.Direction.ASC;
        
        String entityField = switch (sortBy) {
            case "scheduledTime" -> "scheduledTime";
            case "createdAt" -> "createdAt";
            case "title" -> "pdfRequestJson";
            case "pageCount" -> "pdfRequestJson";
            case "copies" -> "pdfRequestJson";
            default -> "scheduledTime";
        };
        
        return Sort.by(direction, entityField);
    }
    
    private boolean matchesFilters(ScheduledJobInfo info, ScheduledJobsQuery query) {
        if (query.getTitleFilter() != null && !query.getTitleFilter().trim().isEmpty()) {
            String title = info.getTitle() != null ? info.getTitle().toLowerCase() : "";
            if (!title.contains(query.getTitleFilter().toLowerCase())) {
                return false;
            }
        }
        
        if (query.getScheduledAfter() != null && info.getScheduledTime().isBefore(query.getScheduledAfter())) {
            return false;
        }
        if (query.getScheduledBefore() != null && info.getScheduledTime().isAfter(query.getScheduledBefore())) {
            return false;
        }
        
        if (query.getMinPageCount() != null && info.getPageCount() < query.getMinPageCount()) {
            return false;
        }
        if (query.getMaxPageCount() != null && info.getPageCount() > query.getMaxPageCount()) {
            return false;
        }
        
        if (query.getRtl() != null && info.isRtl() != query.getRtl()) {
            return false;
        }
        if (query.getAddToc() != null && info.isAddToc() != query.getAddToc()) {
            return false;
        }
        if (query.getOutline() != null && info.isOutline() != query.getOutline()) {
            return false;
        }
        if (query.getGrayscale() != null && info.isGrayscale() != query.getGrayscale()) {
            return false;
        }
        if (query.getLowQuality() != null && info.isLowQuality() != query.getLowQuality()) {
            return false;
        }
        if (query.getPrintMediaType() != null && info.isPrintMediaType() != query.getPrintMediaType()) {
            return false;
        }
        if (query.getAddPageNumbering() != null && info.isAddPageNumbering() != query.getAddPageNumbering()) {
            return false;
        }
        if (query.getHasCustomHeader() != null && info.isHasCustomHeader() != query.getHasCustomHeader()) {
            return false;
        }
        if (query.getHasCustomFooter() != null && info.isHasCustomFooter() != query.getHasCustomFooter()) {
            return false;
        }
        
        // String filters
        if (query.getPageSize() != null && !query.getPageSize().trim().isEmpty()) {
            String pageSize = info.getPageSize() != null ? info.getPageSize().toLowerCase() : "";
            if (!pageSize.contains(query.getPageSize().toLowerCase())) {
                return false;
            }
        }
        if (query.getOrientation() != null && !query.getOrientation().trim().isEmpty()) {
            String orientation = info.getOrientation() != null ? info.getOrientation().toLowerCase() : "";
            if (!orientation.contains(query.getOrientation().toLowerCase())) {
                return false;
            }
        }
        
        return true;
    }
    
    private ScheduledJobInfo mapToScheduledJobInfo(PdfJob job) {
        ScheduledJobInfo info = new ScheduledJobInfo();
        
        info.setId(job.getId());
        info.setJobStatus(job.getJobStatus());
        info.setScheduledTime(job.getScheduledTime());
        info.setCreatedAt(job.getCreatedAt());
        info.setUpdatedAt(job.getUpdatedAt());
        info.setErrorMessage(job.getErrorMessage());
        
        try {
            PdfRequest request = objectMapper.readValue(job.getPdfRequestJson(), PdfRequest.class);
            
            info.setTitle(request.getTitle() != null ? request.getTitle() : "Untitled PDF");
            info.setPageCount(request.getHtmlPages() != null ? request.getHtmlPages().size() : 0);
            info.setRtl(request.isRtl());
            info.setAddToc(request.isAddToc());
            info.setOutline(request.isOutline());
            info.setGrayscale(request.isGrayscale());
            info.setLowQuality(request.isLowQuality());
            info.setCopies(request.getCopies());
            info.setPrintMediaType(request.isPrintMediaType());
            info.setAddPageNumbering(request.isAddPageNumbering());
            info.setPageNumberingText(request.getPageNumberingText());
            info.setLogLevel(request.getLogLevel() != null ? request.getLogLevel() : "info");
            info.setNormalizeColors(request.isNormalizeColors());
            info.setHeaderHtml(request.getHeaderHtml());
            info.setFooterHtml(request.getFooterHtml());
            info.setDumpOutline(request.getDumpOutline());
            
            info.setGlobalOptions(request.getGlobalOptions());
            
            if (request.getGlobalOptions() != null) {
                Map<String, String> options = request.getGlobalOptions();
                info.setPageSize(options.getOrDefault("--page-size", "A4"));
                info.setOrientation(options.getOrDefault("--orientation", "Portrait"));
                
                String marginTop = options.get("--margin-top");
                String marginBottom = options.get("--margin-bottom");
                String marginLeft = options.get("--margin-left");
                String marginRight = options.get("--margin-right");
                
                StringBuilder margins = new StringBuilder();
                if (marginTop != null) margins.append("Top: ").append(marginTop);
                if (marginBottom != null) {
                    if (margins.length() > 0) margins.append(", ");
                    margins.append("Bottom: ").append(marginBottom);
                }
                if (marginLeft != null) {
                    if (margins.length() > 0) margins.append(", ");
                    margins.append("Left: ").append(marginLeft);
                }
                if (marginRight != null) {
                    if (margins.length() > 0) margins.append(", ");
                    margins.append("Right: ").append(marginRight);
                }
                
                info.setMargins(margins.length() > 0 ? margins.toString() : "Default");
            } else {
                info.setPageSize("A4");
                info.setOrientation("Portrait");
                info.setMargins("Default");
            }
            
            info.setHasCustomHeader(request.getHeaderHtml() != null && !request.getHeaderHtml().trim().isEmpty());
            info.setHasCustomFooter(request.getFooterHtml() != null && !request.getFooterHtml().trim().isEmpty());
            
        } catch (Exception e) {
            log.warn("Failed to parse PDF request JSON for job {}: {}", job.getId(), e.getMessage());
            info.setTitle("Error parsing request");
            info.setPageCount(0);
            info.setRtl(true);
            info.setCopies(1);
            info.setLogLevel("info");
            info.setPageSize("A4");
            info.setOrientation("Portrait");
            info.setMargins("Default");
            info.setHasCustomHeader(false);
            info.setHasCustomFooter(false);
        }
        
        return info;
    }
} 