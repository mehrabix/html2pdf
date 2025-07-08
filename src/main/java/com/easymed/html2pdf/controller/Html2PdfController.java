package com.easymed.html2pdf.controller;

import com.easymed.html2pdf.model.PdfRequest;
import com.easymed.html2pdf.model.PdfJob;
import com.easymed.html2pdf.model.PdfJobResult;
import com.easymed.html2pdf.model.PdfJobStatus;
import com.easymed.html2pdf.service.Html2PdfService;
import com.easymed.html2pdf.service.AsyncPdfJobService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "HTML to PDF", description = "Endpoints for converting HTML to PDF synchronously and asynchronously.")
@RestController
@RequestMapping("/api/pdf")
public class Html2PdfController {
    @Autowired
    private Html2PdfService html2PdfService;
    @Autowired
    private AsyncPdfJobService asyncPdfJobService;

    @Operation(summary = "Generate PDF synchronously", description = "Converts HTML to PDF and returns the PDF file immediately.")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
        description = "PDF generation request",
        required = true,
        content = @Content(
            schema = @Schema(implementation = PdfRequest.class),
            examples = @ExampleObject(
                value = "{\n  \"htmlPages\": [\"<html><body><h1>Hello PDF</h1></body></html>\"],\n  \"globalOptions\": {\"--page-size\": \"A4\", \"--orientation\": \"Portrait\"},\n  \"pageOptions\": [{\"--footer-center\": \"Page Footer\"}]\n}"
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

    @Operation(summary = "Generate PDF asynchronously", description = "Submits an HTML-to-PDF job and returns a job ID. Use the job ID to check status and download the result.")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
        description = "PDF generation request (with optional scheduledTime for delayed jobs)",
        required = true,
        content = @Content(
            schema = @Schema(implementation = PdfRequest.class),
            examples = @ExampleObject(
                value = "{\n  \"htmlPages\": [\"<html><body><h1>Async PDF</h1></body></html>\"],\n  \"globalOptions\": {\"--page-size\": \"A4\"},\n  \"scheduledTime\": \"2025-07-08T15:00:00Z\"\n}"
            )
        )
    )
    @PostMapping("/async")
    public ResponseEntity<String> generatePdfAsync(@RequestBody PdfRequest request) {
        UUID jobId = asyncPdfJobService.submitJob(request);
        return ResponseEntity.ok(jobId.toString());
    }

    @GetMapping("/job/{id}/status")
    public ResponseEntity<PdfJobStatus> getJobStatus(@PathVariable UUID id) {
        return ResponseEntity.ok(asyncPdfJobService.getJobStatus(id));
    }

    @GetMapping("/job/{id}/result")
    public ResponseEntity<?> getJobResult(@PathVariable UUID id) {
        PdfJobResult result = asyncPdfJobService.getJobResult(id);
        if (result == null || result.getPdf() == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=output.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(new ByteArrayResource(result.getPdf()));
    }
} 