package com.easymed.html2pdf.scheduler;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.easymed.html2pdf.entity.PdfJob;
import com.easymed.html2pdf.model.PdfJobStatus;
import com.easymed.html2pdf.repository.PdfJobRepository;
import com.easymed.html2pdf.service.AsyncPdfJobService;

@Component
public class PdfJobScheduler {
    private static final Logger log = LoggerFactory.getLogger(PdfJobScheduler.class);

    @Autowired
    private PdfJobRepository pdfJobRepository;

    @Autowired
    private AsyncPdfJobService asyncPdfJobService;

    @Scheduled(fixedDelay = 10000)
    @Transactional("transactionManager")
    public void checkPendingJobs() {
        LocalDateTime currentTime = LocalDateTime.now();
        
        List<PdfJob> scheduledJobs = pdfJobRepository.findScheduledJobsReadyForExecution(PdfJobStatus.SCHEDULED, currentTime);
        if (!scheduledJobs.isEmpty()) {
            log.info("Found {} scheduled jobs ready for execution", scheduledJobs.size());
            for (PdfJob job : scheduledJobs) {
                job.setJobStatus(PdfJobStatus.PENDING);
                pdfJobRepository.save(job);
                log.info("Updated job {} status from SCHEDULED to PENDING", job.getId());
            }
        }
        
        Optional<PdfJob> nextJob = pdfJobRepository.findFirstReadyJob(PdfJobStatus.PENDING, PdfJobStatus.SCHEDULED, currentTime);
        if (nextJob.isPresent()) {
            PdfJob job = nextJob.get();
            log.info("Processing job {} with status {}", job.getId(), job.getJobStatus());
            asyncPdfJobService.processJob(job);
        }
    }

    @Scheduled(fixedDelay = 60000)
    @Transactional("transactionManager")
    public void cleanupOldCompletedJobs() {
        LocalDateTime fiveMinutesAgo = LocalDateTime.now().minusMinutes(5);
        List<PdfJob> oldJobs = pdfJobRepository.findByJobStatusAndUpdatedAtBefore(PdfJobStatus.SUCCESS, fiveMinutesAgo);
        if (!oldJobs.isEmpty()) {
            pdfJobRepository.deleteAll(oldJobs);
            log.info("Cleaned up {} old completed jobs.", oldJobs.size());
        }
    }
} 