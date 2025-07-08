package com.easymed.html2pdf.scheduler;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class PdfJobScheduler {
    @Scheduled(fixedDelay = 60000)
    public void checkPendingJobs() {
    }
} 