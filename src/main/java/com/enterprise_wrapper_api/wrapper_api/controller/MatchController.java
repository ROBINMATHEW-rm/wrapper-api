package com.enterprise_wrapper_api.wrapper_api.controller;
import com.enterprise_wrapper_api.wrapper_api.model.ResumeMatchRequest;
import com.enterprise_wrapper_api.wrapper_api.model.ResumeMatchResponse;
import com.enterprise_wrapper_api.wrapper_api.service.impl.ResumeMatchService;
import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/resume")
public class MatchController {

    @Autowired
    private ResumeMatchService matchService;

    private final Tika tika = new Tika();

    @PostMapping("/match")
    public ResponseEntity<ResumeMatchResponse> match(@RequestBody ResumeMatchRequest request) {
        ResumeMatchResponse response = matchService.getMatch(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/upload")
    public ResponseEntity<ResumeMatchResponse> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam("jobDescription") String jobDescription) throws IOException, TikaException {

        String resumeText = tika.parseToString(file.getInputStream());
        ResumeMatchRequest request = new ResumeMatchRequest();
        request.setResumeText(resumeText);
        request.setJobDescription(jobDescription);

        ResumeMatchResponse response = matchService.getMatch(request);
        return ResponseEntity.ok(response);
    }
}
