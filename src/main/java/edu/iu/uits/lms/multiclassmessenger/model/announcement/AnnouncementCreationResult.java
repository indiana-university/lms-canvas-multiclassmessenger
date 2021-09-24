package edu.iu.uits.lms.multiclassmessenger.model.announcement;

import edu.iu.uits.lms.canvas.model.Announcement;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
/**
 * An object to represent bulk announcement creation success and failures
 */
public class AnnouncementCreationResult implements Serializable {

    private List<Announcement> createdAnnouncements;
    private List<String> successCourses;
    private List<String> failedCourses;

}
