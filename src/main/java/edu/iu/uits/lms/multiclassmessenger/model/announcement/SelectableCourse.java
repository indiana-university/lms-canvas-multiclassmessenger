package edu.iu.uits.lms.multiclassmessenger.model.announcement;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;

@Data
@AllArgsConstructor
public class SelectableCourse implements Serializable {

    private String courseId;
    private String courseDisplayName;
    private String termId;

}
