package edu.iu.uits.lms.multiclassmessenger.model.message;

import edu.iu.uits.lms.canvas.model.Conversation;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
/**
 * An object to represent bulk message creation success and failures
 */
public class MessageCreationResult implements Serializable {

    private List<Conversation> createdMessages;
    private List<String> successCourses;
    private List<String> failedCourses;
    private boolean attachmentUploadFailure;

}
