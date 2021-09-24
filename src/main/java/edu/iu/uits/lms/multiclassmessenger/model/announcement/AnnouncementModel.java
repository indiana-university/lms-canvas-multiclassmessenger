package edu.iu.uits.lms.multiclassmessenger.model.announcement;

import lombok.Data;
import lombok.ToString;

import java.util.Collection;

@ToString
@Data
public class AnnouncementModel {
    private String announcementTitle;
    private String announcementMessage;
    private boolean podcastFeed;
    private boolean podcastReplies;
    private boolean allowLiking;
    private boolean graderLikes;
    private boolean sortLikes;

    private Collection<SelectableCourse> courseOptions;
    private SelectableCourse currentCourse;
    /**
     * Will be in format course id:course display name
     */
    private Collection<String> selectedCourses;

}
