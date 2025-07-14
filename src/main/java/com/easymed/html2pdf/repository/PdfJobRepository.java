package com.easymed.html2pdf.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.easymed.html2pdf.entity.PdfJob;
import com.easymed.html2pdf.model.PdfJobStatus;

@Repository
public interface PdfJobRepository extends JpaRepository<PdfJob, UUID> {
    Optional<PdfJob> findFirstByJobStatus(PdfJobStatus status);
    
    List<PdfJob> findByJobStatusAndUpdatedAtBefore(PdfJobStatus status, LocalDateTime time);
    
    @Query("SELECT j FROM PdfJob j WHERE (j.jobStatus = :pendingStatus OR (j.jobStatus = :scheduledStatus AND j.scheduledTime <= :currentTime)) ORDER BY j.scheduledTime ASC")
    Optional<PdfJob> findFirstReadyJob(@Param("pendingStatus") PdfJobStatus pendingStatus, 
                                       @Param("scheduledStatus") PdfJobStatus scheduledStatus, 
                                       @Param("currentTime") LocalDateTime currentTime);
    
    @Query("SELECT j FROM PdfJob j WHERE j.jobStatus = :scheduledStatus AND j.scheduledTime <= :currentTime")
    List<PdfJob> findScheduledJobsReadyForExecution(@Param("scheduledStatus") PdfJobStatus scheduledStatus, 
                                                     @Param("currentTime") LocalDateTime currentTime);
    
    List<PdfJob> findByJobStatusOrderByScheduledTimeAsc(PdfJobStatus status);
    
    List<PdfJob> findByJobStatusAndSessionIdOrderByScheduledTimeAsc(PdfJobStatus status, String sessionId);
    
    Optional<PdfJob> findByIdAndSessionId(UUID id, String sessionId);
    
    Page<PdfJob> findByJobStatusAndSessionId(PdfJobStatus status, String sessionId, Pageable pageable);
    
    @Query("SELECT COUNT(j) FROM PdfJob j WHERE j.jobStatus = :status AND j.sessionId = :sessionId")
    long countByJobStatusAndSessionId(@Param("status") PdfJobStatus status, @Param("sessionId") String sessionId);
} 