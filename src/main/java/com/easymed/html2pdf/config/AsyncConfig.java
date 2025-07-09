package com.easymed.html2pdf.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
public class AsyncConfig {

    @Bean("pdfExecutor")
    public Executor pdfExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2); // Number of threads to keep in the pool
        executor.setMaxPoolSize(4); // Maximum number of threads to allow
        executor.setQueueCapacity(50); // Number of jobs to queue before rejecting new tasks
        executor.setThreadNamePrefix("PdfGenerator-");
        executor.initialize();
        return executor;
    }
} 