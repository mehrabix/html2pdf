package com.easymed.html2pdf.repository;

import com.easymed.html2pdf.model.PdfJob;
import org.springframework.stereotype.Repository;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class PdfJobRepository {
    private final Map<UUID, PdfJob> jobs = new ConcurrentHashMap<>();
    public void save(PdfJob job) { jobs.put(job.getId(), job); }
    public PdfJob findById(UUID id) { return jobs.get(id); }
} 