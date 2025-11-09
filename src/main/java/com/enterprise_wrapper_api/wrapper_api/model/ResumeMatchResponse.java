package com.enterprise_wrapper_api.wrapper_api.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ResumeMatchResponse {
    private double matchScore;
    private List<String> missingSkills;
    private String summary;
}
