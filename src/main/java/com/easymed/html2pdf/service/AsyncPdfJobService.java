package com.easymed.html2pdf.service;

import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.easymed.html2pdf.entity.PdfJob;
import com.easymed.html2pdf.model.PdfJobResult;
import com.easymed.html2pdf.model.PdfJobStatus;
import com.easymed.html2pdf.model.PdfRequest;
import com.easymed.html2pdf.repository.PdfJobRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class AsyncPdfJobService {

    @Autowired
    private PdfJobRepository pdfJobRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private Html2PdfService html2PdfService;

    public UUID submitJob(PdfRequest request) {
        PdfJob pdfJob = new PdfJob();
        pdfJob.setId(UUID.randomUUID());
        pdfJob.setJobStatus(PdfJobStatus.PENDING);
        try {
            pdfJob.setPdfRequestJson(objectMapper.writeValueAsString(request));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Could not serialize PDF request", e);
        }
        pdfJobRepository.save(pdfJob);
        return pdfJob.getId();
    }

    @Async("pdfExecutor")
    @Transactional("transactionManager")
    public void processJob(PdfJob job) {
        job.setJobStatus(PdfJobStatus.RUNNING);
        pdfJobRepository.save(job);
        try {
            PdfRequest request = objectMapper.readValue(job.getPdfRequestJson(), PdfRequest.class);
            byte[] pdf = html2PdfService.generatePdf(request);
            job.setPdfData(pdf);
            job.setJobStatus(PdfJobStatus.SUCCESS);
        } catch (Exception e) {
            job.setErrorMessage(e.getMessage());
            job.setJobStatus(PdfJobStatus.FAILED);
        }
        pdfJobRepository.save(job);
    }

    public PdfJobStatus getJobStatus(UUID jobId) {
        return pdfJobRepository.findById(jobId).map(PdfJob::getJobStatus).orElse(null);
    }

    public PdfJobResult getJobResult(UUID jobId) {
        return pdfJobRepository.findById(jobId)
                .filter(job -> job.getJobStatus() == PdfJobStatus.SUCCESS)
                .map(job -> new PdfJobResult(job.getPdfData(), job.getErrorMessage()))
                .orElse(null);
    }

   @Transactional("transactionManager")
    public PdfJobResult getJobResultAndClear(UUID jobId) {
        Optional<PdfJob> jobOptional = pdfJobRepository.findById(jobId)
                .filter(job -> job.getJobStatus() == PdfJobStatus.SUCCESS);

        if (jobOptional.isPresent()) {
            PdfJob job = jobOptional.get();
            PdfJobResult result = new PdfJobResult(job.getPdfData(), job.getErrorMessage());
            pdfJobRepository.delete(job);
            return result;
        }
        return null;
    }
} 