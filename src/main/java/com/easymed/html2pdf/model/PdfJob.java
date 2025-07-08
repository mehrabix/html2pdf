package com.easymed.html2pdf.model;

import java.time.Instant;
import java.util.UUID;

public class PdfJob {
    private UUID id;
    private PdfRequest request;
    private PdfJobStatus status;
    private byte[] pdf;
    private String error;
    private Instant scheduledTime;
    private Instant startedAt;
    private Instant finishedAt;

    public PdfJob(UUID id, PdfRequest request, PdfJobStatus status, Instant scheduledTime) {
        this.id = id;
        this.request = request;
        this.status = status;
        this.scheduledTime = scheduledTime;
    }
    public UUID getId() { return id; }
    public PdfRequest getRequest() { return request; }
    public PdfJobStatus getStatus() { return status; }
    public void setStatus(PdfJobStatus status) { this.status = status; }
    public byte[] getPdf() { return pdf; }
    public void setPdf(byte[] pdf) { this.pdf = pdf; }
    public String getError() { return error; }
    public void setError(String error) { this.error = error; }
    public Instant getScheduledTime() { return scheduledTime; }
    public Instant getStartedAt() { return startedAt; }
    public void setStartedAt(Instant startedAt) { this.startedAt = startedAt; }
    public Instant getFinishedAt() { return finishedAt; }
    public void setFinishedAt(Instant finishedAt) { this.finishedAt = finishedAt; }
} 