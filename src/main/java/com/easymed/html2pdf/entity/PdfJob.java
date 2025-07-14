package com.easymed.html2pdf.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import com.easymed.html2pdf.model.PdfJobStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import lombok.Data;

@Entity
@Data
public class PdfJob {

    @Id
    private UUID id;

    @Enumerated(EnumType.STRING)
    private PdfJobStatus jobStatus;

    @Column(columnDefinition = "TEXT")
    private String pdfRequestJson;

    @Lob
    @Column(columnDefinition = "BLOB")
    private byte[] pdfData;

    private String errorMessage;

    private String sessionId;

    private LocalDateTime scheduledTime;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
} 