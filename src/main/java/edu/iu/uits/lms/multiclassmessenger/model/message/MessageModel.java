package edu.iu.uits.lms.multiclassmessenger.model.message;

import lombok.Data;
import lombok.ToString;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@ToString
@Data
public class MessageModel {


    private String messageSubject;
    private String messageText;

    /**
     * Will be in format course id:role id
     */
    private Collection<String> selectedRecipients;

    private List<SelectableRecipient> recipientOptions;

    private List<String> courseIdCourseTitle = new ArrayList<>();

}
