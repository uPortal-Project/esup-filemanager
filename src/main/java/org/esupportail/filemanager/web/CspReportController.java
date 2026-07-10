package org.esupportail.filemanager.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CspReportController {

    private static final Logger log = LoggerFactory.getLogger(CspReportController.class);

    private static final int MAX_REPORT_LOG_LENGTH = 4000;

    @PostMapping(value = "/csp-report", consumes = MediaType.ALL_VALUE)
    public ResponseEntity<Void> collect(@RequestBody(required = false) String report) {
        if (report != null && !report.isBlank()) {
            String sanitizedReport = report.replace('\r', ' ').replace('\n', ' ');
            if (sanitizedReport.length() > MAX_REPORT_LOG_LENGTH) {
                sanitizedReport = sanitizedReport.substring(0, MAX_REPORT_LOG_LENGTH) + "...";
            }
            log.warn("CSP violation report: {}", sanitizedReport);
        }

        return ResponseEntity.noContent().build();
    }
}
