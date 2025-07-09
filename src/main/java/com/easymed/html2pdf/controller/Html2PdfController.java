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

@Tag(name = "تبدیل HTML به PDF", description = "مجموعه ای از اندپوینت ها برای تبدیل HTML به PDF به صورت همزمان و غیرهمزمان.")
@RestController
@RequestMapping("/api/pdf")
public class Html2PdfController {
    @Autowired
    private Html2PdfService html2PdfService;
    @Autowired
    private AsyncPdfJobService asyncPdfJobService;

    @Operation(summary = "تولید PDF به صورت همزمان", description = "یک یا چند صفحه HTML را به فایل PDF تبدیل کرده و نتیجه را بلافاصله برمی‌گرداند.")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
        description = "پارامترهای درخواست برای تولید PDF",
        required = true,
        content = @Content(
            schema = @Schema(implementation = PdfRequest.class),
            examples = @ExampleObject(
                name = "نمونه کامل",
                value = "{\n  \"htmlPages\": [\n    \"<html><head></head><body><h1>سلام دنیا!</h1><h2>بخش اول</h2><p>این یک صفحه تست راست به چپ است.</p><h2>بخش دوم</h2><p>ادامه تست...</p></body></html>\"\n  ],\n  \"globalOptions\": {\n    \"--page-size\": \"A4\",\n    \"--orientation\": \"Portrait\",\n    \"--margin-top\": \"20mm\",\n    \"--margin-bottom\": \"20mm\"\n  },\n  \"pageOptions\": [\n    {}\n  ],\n  \"scheduledTime\": \"2025-12-31T23:59:59Z\",\n  \"rtl\": true,\n  \"headerHtml\": \"<html><head><meta charset='UTF-8'></head><body><div style='text-align:center; font-family:Vazirmatn,tahoma; font-size:10pt;'>سربرگ سفارشی</div></body></html>\",\n  \"footerHtml\": \"<html><head><meta charset='UTF-8'></head><body style='font-family:Vazirmatn,tahoma; font-size:10pt; text-align:center;'>پاصفحه سفارشی</body></html>\",\n  \"addPageNumbering\": true,\n  \"addToc\": true,\n  \"outline\": true,\n  \"dumpOutline\": \"outline.xml\",\n  \"grayscale\": false,\n  \"lowQuality\": false,\n  \"logLevel\": \"info\",\n  \"copies\": 1,\n  \"title\": \"سند تست فارسی\",\n  \"printMediaType\": true\n}"
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
        summary = "تولید PDF به صورت غیرهمزمان", 
        description = "یک کار (job) برای تبدیل HTML به PDF ثبت کرده و یک شناسه کار برمی‌گرداند. از این شناسه برای بررسی وضعیت و دریافت نتیجه استفاده کنید.",
        responses = {
            @ApiResponse(
                responseCode = "200", 
                description = "کار با موفقیت ثبت شد", 
                content = @Content(schema = @Schema(implementation = AsyncResponse.class))
            )
        }
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
        description = "پارامترهای درخواست برای تولید PDF (می‌تواند شامل زمانبندی برای اجرای تاخیری باشد)",
        required = true,
        content = @Content(
            schema = @Schema(implementation = PdfRequest.class),
            examples = @ExampleObject(
                name = "نمونه کامل با زمانبندی",
                value = "{\n  \"htmlPages\": [\n    \"<html><head></head><body><h1>سلام دنیا!</h1><h2>بخش اول</h2><p>این یک صفحه تست راست به چپ است.</p><h2>بخش دوم</h2><p>ادامه تست...</p></body></html>\"\n  ],\n  \"globalOptions\": {\n    \"--page-size\": \"A4\",\n    \"--orientation\": \"Portrait\",\n    \"--margin-top\": \"20mm\",\n    \"--margin-bottom\": \"20mm\"\n  },\n  \"pageOptions\": [\n    {}\n  ],\n  \"scheduledTime\": \"2025-12-31T23:59:59Z\",\n  \"rtl\": true,\n  \"headerHtml\": \"<html><head><meta charset='UTF-8'></head><body><div style='text-align:center; font-family:Vazirmatn,tahoma; font-size:10pt;'>سربرگ سفارشی</div></body></html>\",\n  \"footerHtml\": \"<html><head><meta charset='UTF-8'></head><body style='font-family:Vazirmatn,tahoma; font-size:10pt; text-align:center;'>پاصفحه سفارشی</body></html>\",\n  \"addPageNumbering\": true,\n  \"addToc\": true,\n  \"outline\": true,\n  \"dumpOutline\": \"outline.xml\",\n  \"grayscale\": false,\n  \"lowQuality\": false,\n  \"logLevel\": \"info\",\n  \"copies\": 1,\n  \"title\": \"سند تست فارسی\",\n  \"printMediaType\": true\n}"
            )
        )
    )
    @PostMapping("/async")
    public ResponseEntity<AsyncResponse> generatePdfAsync(@RequestBody PdfRequest request) {
        UUID jobId = asyncPdfJobService.submitJob(request);
        return ResponseEntity.ok(new AsyncResponse(jobId));
    }

    @Operation(summary = "بررسی وضعیت کار", description = "وضعیت یک کار تولید PDF را با استفاده از شناسه آن برمی‌گرداند.")
    @GetMapping("/job/{id}/status")
    public ResponseEntity<PdfJobStatus> getJobStatus(
        @Parameter(description = "شناسه کار (Job ID) که پس از ثبت کار غیرهمزمان دریافت شده است") @PathVariable UUID id) {
        return ResponseEntity.ok(asyncPdfJobService.getJobStatus(id));
    }

    @Operation(summary = "دریافت نتیجه کار", description = "نتیجه PDF یک کار تکمیل شده را برمی‌گرداند. پس از دانلود، کار از سیستم حذف خواهد شد.")
    @GetMapping("/job/{id}/result")
    public ResponseEntity<?> getJobResult(
        @Parameter(description = "شناسه کار (Job ID) برای دریافت فایل PDF نهایی") @PathVariable UUID id) {
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