package com.easymed.html2pdf.service;

import com.easymed.html2pdf.model.*;
import com.easymed.html2pdf.repository.PdfJobRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.UUID;
import java.util.concurrent.*;

@Service
@EnableAsync
public class AsyncPdfJobService {
    @Autowired
    private Html2PdfService html2PdfService;
    @Autowired
    private PdfJobRepository jobRepository;
    private ScheduledExecutorService scheduler;

    public AsyncPdfJobService() {
        scheduler = Executors.newScheduledThreadPool(2);
    }

    public UUID submitJob(PdfRequest request) {
        UUID jobId = UUID.randomUUID();
        Instant scheduledTime = null;
        if (request.getScheduledTime() != null) {
            try {
                scheduledTime = Instant.parse(request.getScheduledTime());
            } catch (DateTimeParseException e) {
                throw new IllegalArgumentException("Invalid scheduledTime format. Use ISO8601.");
            }
        }
        PdfJob job = new PdfJob(jobId, request, PdfJobStatus.PENDING, scheduledTime);
        jobRepository.save(job);
        if (scheduledTime != null && scheduledTime.isAfter(Instant.now())) {
            long delay = scheduledTime.toEpochMilli() - Instant.now().toEpochMilli();
            scheduler.schedule(() -> runJob(jobId), delay, TimeUnit.MILLISECONDS);
        } else {
            runJobAsync(jobId);
        }
        return jobId;
    }

    @Async
    public void runJobAsync(UUID jobId) {
        runJob(jobId);
    }

    public void runJob(UUID jobId) {
        PdfJob job = jobRepository.findById(jobId);
        if (job == null) return;
        job.setStatus(PdfJobStatus.RUNNING);
        job.setStartedAt(Instant.now());
        try {
            byte[] pdf = html2PdfService.generatePdf(job.getRequest());
            job.setPdf(pdf);
            job.setStatus(PdfJobStatus.SUCCESS);
        } catch (Exception e) {
            job.setError(e.getMessage());
            job.setStatus(PdfJobStatus.FAILED);
        }
        job.setFinishedAt(Instant.now());
        jobRepository.save(job);
    }

    public PdfJobStatus getJobStatus(UUID jobId) {
        PdfJob job = jobRepository.findById(jobId);
        return job != null ? job.getStatus() : null;
    }

    public PdfJobResult getJobResult(UUID jobId) {
        PdfJob job = jobRepository.findById(jobId);
        if (job == null || job.getStatus() != PdfJobStatus.SUCCESS) return null;
        return new PdfJobResult(job.getPdf(), job.getError());
    }
} 