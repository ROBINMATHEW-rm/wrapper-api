package com.enterprise_wrapper_api.wrapper_api.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ResumeMatchRequest {
    private String resumeText;
    private String jobDescription;
}
