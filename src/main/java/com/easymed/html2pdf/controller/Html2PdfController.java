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
import org.springframework.web.bind.annotation.RestController;

import com.easymed.html2pdf.model.AsyncResponse;
import com.easymed.html2pdf.model.PdfJobResult;
import com.easymed.html2pdf.model.PdfJobStatus;
import com.easymed.html2pdf.model.PdfRequest;
import com.easymed.html2pdf.service.AsyncPdfJobService;
import com.easymed.html2pdf.service.Html2PdfService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "تبدیل HTML به PDF / HTML to PDF Conversion", description = "مجموعه ای از اندپوینت ها برای تبدیل HTML به PDF به صورت همزمان و غیرهمزمان. / A set of endpoints for converting HTML to PDF synchronously and asynchronously.")
@RestController
@RequestMapping("/api/pdf")
public class Html2PdfController {
    @Autowired
    private Html2PdfService html2PdfService;
    @Autowired
    private AsyncPdfJobService asyncPdfJobService;

    @Operation(summary = "تولید PDF به صورت همزمان / Generate PDF synchronously", description = "یک یا چند صفحه HTML را به فایل PDF تبدیل کرده و نتیجه را بلافاصله برمی‌گرداند. / Converts one or more HTML pages to a PDF file and returns the result immediately.")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
        description = "پارامترهای درخواست برای تولید PDF / Request parameters for PDF generation",
        required = true,
        content = @Content(
            schema = @Schema(implementation = PdfRequest.class),
            examples = @ExampleObject(
                name = "نمونه کامل / Full Example",
                value = "{\n  \"htmlPages\": [\n    \"<html><head></head><body><h1>سلام دنیا!/Hello World!</h1><h2>بخش اول/Section 1</h2><p>این یک صفحه تست راست به چپ است./This is a sample right-to-left test page.</p><h2>بخش دوم/Section 2</h2><p>ادامه تست.../Continued test...</p></body></html>\"\n  ],\n  \"globalOptions\": {\n    \"--page-size\": \"A4\",\n    \"--orientation\": \"Portrait\",\n    \"--margin-top\": \"20mm\",\n    \"--margin-bottom\": \"20mm\"\n  },\n  \"pageOptions\": [\n    {}\n  ],\n  \"scheduledTime\": \"2025-12-31T23:59:59Z\",\n  \"rtl\": true,\n  \"headerHtml\": \"<html><head><meta charset='UTF-8'></head><body><div style='text-align:center; font-family:Vazirmatn,tahoma; font-size:10pt;'>سربرگ سفارشی/Custom Header</div></body></html>\",\n  \"footerHtml\": \"<html><head><meta charset='UTF-8'></head><body style='font-family:Vazirmatn,tahoma; font-size:10pt; text-align:center;'>پاصفحه سفارشی/Custom Footer</body></html>\",\n  \"addPageNumbering\": true,\n  \"addToc\": true,\n  \"outline\": true,\n  \"dumpOutline\": \"outline.xml\",\n  \"grayscale\": false,\n  \"lowQuality\": false,\n  \"logLevel\": \"info\",\n  \"copies\": 1,\n  \"title\": \"سند تست فارسی/Sample Persian Document\",\n  \"printMediaType\": true\n}"
            )
        )
    )
    @PostMapping("/sync")
    public ResponseEntity<ByteArrayResource> generatePdfSync(@RequestBody PdfRequest request) {
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
                value = "{\n  \"htmlPages\": [\n    \"<html><head></head><body><h1>سلام دنیا!/Hello World!</h1><h2>بخش اول/Section 1</h2><p>این یک صفحه تست راست به چپ است./This is a sample right-to-left test page.</p><h2>بخش دوم/Section 2</h2><p>ادامه تست.../Continued test...</p></body></html>\"\n  ],\n  \"globalOptions\": {\n    \"--page-size\": \"A4\",\n    \"--orientation\": \"Portrait\",\n    \"--margin-top\": \"20mm\",\n    \"--margin-bottom\": \"20mm\"\n  },\n  \"pageOptions\": [\n    {}\n  ],\n  \"scheduledTime\": \"2025-12-31T23:59:59Z\",\n  \"rtl\": true,\n  \"headerHtml\": \"<html><head><meta charset='UTF-8'></head><body><div style='text-align:center; font-family:Vazirmatn,tahoma; font-size:10pt;'>سربرگ سفارشی/Custom Header</div></body></html>\",\n  \"footerHtml\": \"<html><head><meta charset='UTF-8'></head><body style='font-family:Vazirmatn,tahoma; font-size:10pt; text-align:center;'>پاصفحه سفارشی/Custom Footer</body></html>\",\n  \"addPageNumbering\": true,\n  \"addToc\": true,\n  \"outline\": true,\n  \"dumpOutline\": \"outline.xml\",\n  \"grayscale\": false,\n  \"lowQuality\": false,\n  \"logLevel\": \"info\",\n  \"copies\": 1,\n  \"title\": \"سند تست فارسی/Sample Persian Document\",\n  \"printMediaType\": true\n}"
            )
        )
    )
    @PostMapping("/async")
    public ResponseEntity<AsyncResponse> generatePdfAsync(@RequestBody PdfRequest request) {
        UUID jobId = asyncPdfJobService.submitJob(request);
        return ResponseEntity.ok(new AsyncResponse(jobId));
    }

    @Operation(summary = "بررسی وضعیت کار / Check job status", description = "وضعیت یک کار تولید PDF را با استفاده از شناسه آن برمی‌گرداند. / Returns the status of a PDF generation job using its ID.")
    @GetMapping("/job/{id}/status")
    public ResponseEntity<PdfJobStatus> getJobStatus(
        @Parameter(description = "شناسه کار (Job ID) که پس از ثبت کار غیرهمزمان دریافت شده است / The job ID received after submitting an async job") @PathVariable UUID id) {
        return ResponseEntity.ok(asyncPdfJobService.getJobStatus(id));
    }

    @Operation(summary = "دریافت نتیجه کار / Get job result", description = "نتیجه PDF یک کار تکمیل شده را برمی‌گرداند. پس از دانلود، کار از سیستم حذف خواهد شد. / Returns the PDF result of a completed job. After download, the job is removed from the system.")
    @GetMapping("/job/{id}/result")
    public ResponseEntity<?> getJobResult(
        @Parameter(description = "شناسه کار (Job ID) برای دریافت فایل PDF نهایی / The job ID to retrieve the final PDF file") @PathVariable UUID id) {
        PdfJobResult result = asyncPdfJobService.getJobResultAndClear(id);
        if (result == null || result.getPdf() == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=output.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(new ByteArrayResource(result.getPdf()));
    }
} 