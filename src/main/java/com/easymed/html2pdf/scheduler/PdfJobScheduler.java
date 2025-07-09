package com.easymed.html2pdf.scheduler;

import com.easymed.html2pdf.entity.PdfJob;
import com.easymed.html2pdf.model.PdfJobStatus;
import com.easymed.html2pdf.repository.PdfJobRepository;
import com.easymed.html2pdf.service.AsyncPdfJobService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class PdfJobScheduler {
    private static final Logger log = LoggerFactory.getLogger(PdfJobScheduler.class);


    @Autowired
    private PdfJobRepository pdfJobRepository;

    @Autowired
    private AsyncPdfJobService asyncPdfJobService;

    @Scheduled(fixedDelay = 10000)
    public void checkPendingJobs() {
        pdfJobRepository.findFirstByJobStatus(PdfJobStatus.PENDING).ifPresent(asyncPdfJobService::processJob);
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