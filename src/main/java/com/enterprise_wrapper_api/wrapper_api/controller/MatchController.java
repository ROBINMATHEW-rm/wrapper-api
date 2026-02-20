package com.enterprise_wrapper_api.wrapper_api.controller;

import com.enterprise_wrapper_api.wrapper_api.model.ResumeMatchRequest;
import com.enterprise_wrapper_api.wrapper_api.model.ResumeMatchResponse;
import com.enterprise_wrapper_api.wrapper_api.service.impl.ResumeMatchService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
public class MatchController {

    private final ResumeMatchService matchService;

    public MatchController(ResumeMatchService matchService) {
        this.matchService = matchService;
    }

    // ✅ JSON input (resume text + job description)
    @PostMapping("/match")
    public ResponseEntity<ResumeMatchResponse> match(
            @RequestBody ResumeMatchRequest request) {

        ResumeMatchResponse response = matchService.getMatch(request);
        return ResponseEntity.ok(response);
    }

    // ✅ File upload (PDF/DOCX) + job description
    @PostMapping("/upload")
    public ResponseEntity<ResumeMatchResponse> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam("jobDescription") String jobDescription) {

        ResumeMatchResponse response =
                matchService.matchUploadedResume(file, jobDescription);

        return ResponseEntity.ok(response);
    }
}
