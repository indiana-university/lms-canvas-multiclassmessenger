package edu.iu.uits.lms.multiclassmessenger.model.message;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SelectableRecipient implements Serializable {

    private String courseId;
    private String courseDisplayName;
    Map<String, String> roleIdToNameMap;

}
