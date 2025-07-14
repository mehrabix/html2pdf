package com.easymed.html2pdf.controller;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.easymed.html2pdf.model.AsyncResponse;
import com.easymed.html2pdf.model.PagedResponse;
import com.easymed.html2pdf.model.PdfJobResult;
import com.easymed.html2pdf.model.PdfJobStatus;
import com.easymed.html2pdf.model.PdfRequest;
import com.easymed.html2pdf.model.ScheduledJobInfo;
import com.easymed.html2pdf.model.ScheduledJobsQuery;
import com.easymed.html2pdf.service.AsyncPdfJobService;
import com.easymed.html2pdf.service.Html2PdfService;
import com.easymed.html2pdf.service.SessionService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Tag(name = "تبدیل HTML به PDF / HTML to PDF Conversion", description = "مجموعه ای از اندپوینت ها برای تبدیل HTML به PDF به صورت همزمان و غیرهمزمان. / A set of endpoints for converting HTML to PDF synchronously and asynchronously.")
@RestController
@RequestMapping("/api/pdf")
public class Html2PdfController {
    @Autowired
    private Html2PdfService html2PdfService;
    @Autowired
    private AsyncPdfJobService asyncPdfJobService;
    @Autowired
    private SessionService sessionService;

    @Operation(summary = "تولید PDF به صورت همزمان / Generate PDF synchronously", description = "یک یا چند صفحه HTML را به فایل PDF تبدیل کرده و نتیجه را بلافاصله برمی‌گرداند. / Converts one or more HTML pages to a PDF file and returns the result immediately.")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
        description = "پارامترهای درخواست برای تولید PDF / Request parameters for PDF generation",
        required = true,
        content = @Content(
            schema = @Schema(implementation = PdfRequest.class),
            examples = @ExampleObject(
                name = "نمونه کامل / Full Example",
                value = "{\n  \"htmlPages\": [\n    \"<html><head></head><body><h1>سلام دنیا!/Hello World!</h1><h2>بخش اول/Section 1</h2><p>این یک صفحه تست راست به چپ است./This is a sample right-to-left test page.</p><h2>بخش دوم/Section 2</h2><p>ادامه تست.../Continued test...</p></body></html>\"\n  ],\n  \"globalOptions\": {\n    \"--page-size\": \"A4\",\n    \"--orientation\": \"Portrait\",\n    \"--margin-top\": \"20mm\",\n    \"--margin-bottom\": \"20mm\"\n  },\n  \"pageOptions\": [\n    {}\n  ],\n  \"scheduledTime\": \"2025-12-31T23:59:59Z\",\n  \"rtl\": true,\n  \"headerHtml\": \"<html><head><meta charset='UTF-8'></head><body><div style='text-align:center; font-family:Vazirmatn,tahoma; font-size:10pt;'>سربرگ سفارشی/Custom Header</div></body></html>\",\n  \"footerHtml\": \"<html><head><meta charset='UTF-8'></head><body style='font-family:Vazirmatn,tahoma; font-size:10pt; text-align:center;'>پاصفحه سفارشی/Custom Footer</body></html>\",\n  \"addPageNumbering\": true,\n  \"pageNumberingText\": \"صفحه [page] از [topage]\",\n  \"addToc\": true,\n  \"outline\": true,\n  \"dumpOutline\": \"outline.xml\",\n  \"grayscale\": false,\n  \"lowQuality\": false,\n  \"logLevel\": \"info\",\n  \"copies\": 1,\n  \"title\": \"سند تست فارسی/Sample Persian Document\",\n  \"printMediaType\": true\n}"
            )
        )
    )
    @PostMapping("/sync")
    public ResponseEntity<ByteArrayResource> generatePdfSync(@RequestBody PdfRequest request, HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        sessionService.getOrCreateSessionId(httpRequest, httpResponse);
        byte[] pdf = html2PdfService.generatePdf(request);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=output.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(new ByteArrayResource(pdf));
    }

    @Operation(
        summary = "تولید PDF به صورت غیرهمزمان / Generate PDF asynchronously", 
        description = "یک کار (job) برای تبدیل HTML به PDF ثبت کرده و یک شناسه کار برمی‌گرداند. از این شناسه برای بررسی وضعیت و دریافت نتیجه استفاده کنید. / Submits a job to convert HTML to PDF and returns a job ID. Use this ID to check status and retrieve the result."
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
        description = "پارامترهای درخواست برای تولید PDF (می‌تواند شامل زمانبندی برای اجرای تاخیری باشد) / Request parameters for PDF generation (can include scheduling for delayed execution)",
        required = true,
        content = @Content(
            schema = @Schema(implementation = PdfRequest.class),
            examples = @ExampleObject(
                name = "نمونه کامل با زمانبندی / Full Example with Scheduling",
                value = "{\n  \"htmlPages\": [\n    \"<html><head></head><body><h1>سلام دنیا!/Hello World!</h1><h2>بخش اول/Section 1</h2><p>این یک صفحه تست راست به چپ است./This is a sample right-to-left test page.</p><h2>بخش دوم/Section 2</h2><p>ادامه تست.../Continued test...</p></body></html>\"\n  ],\n  \"globalOptions\": {\n    \"--page-size\": \"A4\",\n    \"--orientation\": \"Portrait\",\n    \"--margin-top\": \"20mm\",\n    \"--margin-bottom\": \"20mm\"\n  },\n  \"pageOptions\": [\n    {}\n  ],\n  \"scheduledTime\": \"2025-12-31T23:59:59Z\",\n  \"rtl\": true,\n  \"headerHtml\": \"<html><head><meta charset='UTF-8'></head><body><div style='text-align:center; font-family:Vazirmatn,tahoma; font-size:10pt;'>سربرگ سفارشی/Custom Header</div></body></html>\",\n  \"footerHtml\": \"<html><head><meta charset='UTF-8'></head><body style='font-family:Vazirmatn,tahoma; font-size:10pt; text-align:center;'>پاصفحه سفارشی/Custom Footer</body></html>\",\n  \"addPageNumbering\": true,\n  \"pageNumberingText\": \"صفحه [page] از [topage]\",\n  \"addToc\": true,\n  \"outline\": true,\n  \"dumpOutline\": \"outline.xml\",\n  \"grayscale\": false,\n  \"lowQuality\": false,\n  \"logLevel\": \"info\",\n  \"copies\": 1,\n  \"title\": \"سند تست فارسی/Sample Persian Document\",\n  \"printMediaType\": true\n}"
            )
        )
    )
    @PostMapping("/async")
    public ResponseEntity<AsyncResponse> generatePdfAsync(@RequestBody PdfRequest request, HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        String sessionId = sessionService.getOrCreateSessionId(httpRequest, httpResponse);
        UUID jobId = asyncPdfJobService.submitJob(request, sessionId);
        return ResponseEntity.ok(new AsyncResponse(jobId));
    }

    @Operation(summary = "بررسی وضعیت کار / Check job status", description = "وضعیت یک کار تولید PDF را با استفاده از شناسه آن برمی‌گرداند. / Returns the status of a PDF generation job using its ID.")
    @GetMapping("/job/{id}/status")
    public ResponseEntity<PdfJobStatus> getJobStatus(
        @Parameter(description = "شناسه کار (Job ID) که پس از ثبت کار غیرهمزمان دریافت شده است / The job ID received after submitting an async job") @PathVariable UUID id, HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        String sessionId = sessionService.getOrCreateSessionId(httpRequest, httpResponse);
        PdfJobStatus status = asyncPdfJobService.getJobStatus(id, sessionId);
        if (status == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(status);
    }

    @Operation(summary = "دریافت نتیجه کار / Get job result", description = "نتیجه PDF یک کار تکمیل شده را برمی‌گرداند. پس از دانلود، کار از سیستم حذف خواهد شد. / Returns the PDF result of a completed job. After download, the job is removed from the system.")
    @GetMapping("/job/{id}/result")
    public ResponseEntity<?> getJobResult(
        @Parameter(description = "شناسه کار (Job ID) برای دریافت فایل PDF نهایی / The job ID to retrieve the final PDF file") @PathVariable UUID id, HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        String sessionId = sessionService.getOrCreateSessionId(httpRequest, httpResponse);
        PdfJobResult result = asyncPdfJobService.getJobResultAndClear(id, sessionId);
        if (result == null || result.getPdf() == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=output.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(new ByteArrayResource(result.getPdf()));
    }

    @Operation(
        summary = "دریافت لیست کارهای زمانبندی شده / Get list of scheduled jobs", 
        description = "لیست تمام کارهای زمانبندی شده که هنوز اجرا نشده‌اند را با پشتیبانی از صفحه‌بندی، مرتب‌سازی و فیلترینگ برمی‌گرداند. / Returns a paginated, sortable, and filterable list of all scheduled jobs that have not been executed yet."
    )
    @GetMapping("/jobs/scheduled")
    public ResponseEntity<PagedResponse<ScheduledJobInfo>> getScheduledJobs(
            @RequestParam(defaultValue = "0") int skip,
            @RequestParam(defaultValue = "10") int take,
            @RequestParam(defaultValue = "scheduledTime") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDirection,
            @RequestParam(required = false) String titleFilter,
            @RequestParam(required = false) String scheduledAfter,
            @RequestParam(required = false) String scheduledBefore,
            @RequestParam(required = false) Integer minPageCount,
            @RequestParam(required = false) Integer maxPageCount,
            @RequestParam(required = false) Boolean rtl,
            @RequestParam(required = false) Boolean addToc,
            @RequestParam(required = false) Boolean outline,
            @RequestParam(required = false) Boolean grayscale,
            @RequestParam(required = false) Boolean lowQuality,
            @RequestParam(required = false) Boolean printMediaType,
            @RequestParam(required = false) Boolean addPageNumbering,
            @RequestParam(required = false) String pageSize,
            @RequestParam(required = false) String orientation,
            @RequestParam(required = false) Boolean hasCustomHeader,
            @RequestParam(required = false) Boolean hasCustomFooter,
            HttpServletRequest httpRequest, 
            HttpServletResponse httpResponse) {
        
        String sessionId = sessionService.getOrCreateSessionId(httpRequest, httpResponse);
        
        ScheduledJobsQuery query = new ScheduledJobsQuery();
        query.setSkip(skip);
        query.setTake(take);
        query.setSortBy(sortBy);
        query.setSortDirection(sortDirection);
        query.setTitleFilter(titleFilter);
        
        try {
            if (scheduledAfter != null && !scheduledAfter.trim().isEmpty()) {
                query.setScheduledAfter(java.time.LocalDateTime.parse(scheduledAfter));
            }
            if (scheduledBefore != null && !scheduledBefore.trim().isEmpty()) {
                query.setScheduledBefore(java.time.LocalDateTime.parse(scheduledBefore));
            }
        } catch (Exception e) {
        }
        
        query.setMinPageCount(minPageCount);
        query.setMaxPageCount(maxPageCount);
        query.setRtl(rtl);
        query.setAddToc(addToc);
        query.setOutline(outline);
        query.setGrayscale(grayscale);
        query.setLowQuality(lowQuality);
        query.setPrintMediaType(printMediaType);
        query.setAddPageNumbering(addPageNumbering);
        query.setPageSize(pageSize);
        query.setOrientation(orientation);
        query.setHasCustomHeader(hasCustomHeader);
        query.setHasCustomFooter(hasCustomFooter);
        
        PagedResponse<ScheduledJobInfo> response = asyncPdfJobService.getScheduledJobs(sessionId, query);
        return ResponseEntity.ok(response);
    }
} 