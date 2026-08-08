
package com.studynotion.dto.request;

import lombok.Data;
import java.util.List;

@Data
public class CreateCourseRequest {

    private String courseName;
    private String courseDescription;
    private String whatYouWillLearn;
    private Double price;
    private List<String> tag;

    // String type to handle both MongoDB ObjectId and PostgreSQL Long
    private String categoryId;

    private List<String> instructions;
    private String status;
    private String thumbnail;
}