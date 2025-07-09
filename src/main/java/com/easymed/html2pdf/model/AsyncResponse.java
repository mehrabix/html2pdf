package com.easymed.html2pdf.model;

import java.util.UUID;

import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class AsyncResponse {
    private final UUID jobId;
} 