package com.easymed.html2pdf.scheduler;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class PdfJobScheduler {
    // For future batch or periodic job processing
    @Scheduled(fixedDelay = 60000)
    public void checkPendingJobs() {
        // Implement batch or retry logic if needed
    }
} 