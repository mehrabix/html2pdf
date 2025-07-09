package com.easymed.html2pdf.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.easymed.html2pdf.entity.PdfJob;
import com.easymed.html2pdf.model.PdfJobStatus;

@Repository
public interface PdfJobRepository extends JpaRepository<PdfJob, UUID> {
    Optional<PdfJob> findFirstByJobStatus(PdfJobStatus status);
    List<PdfJob> findByJobStatusAndUpdatedAtBefore(PdfJobStatus status, LocalDateTime time);
} 