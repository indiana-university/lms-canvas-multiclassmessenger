package edu.iu.uits.lms.multiclassmessenger.model.announcement;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
/**
 * An object for creating an Announcement in Canvas
 */
public class CreateAnnouncement {

    /**
     * Must be set to true to create an Announcement. Announcements and DiscussionTopics share the same
     * object.
     */
    private boolean isAnnouncement = true;

    /**
     *  title
     */
    private String title;

    /**
     * HTML content of the message body
     */
    private String message;

    /**
     * Whether this discussion is published (true) or draft state (false)
     */
    private boolean published;

    /**
     * The datetime to publish the topic (if not right away).
     */
    private String delayedPostAt;


    /**
     * By default, discussions are sorted chronologically by creation date, you can pass the id
     * of another topic to have this one show up after the other when they are listed.
     */
    private String positionAfter;


    /**
     * The datetime to lock the topic (if ever)
     */
    private String lockAt;

    /**
     * If true then a user may not respond to other replies until that user has made an
     * initial reply. Defaults to false.
     */
    private String requireInitialPost;

    /**
     * true if users can rate/like entries related to this topic
     */
    private boolean allowRating;

    /**
     * True if grader permissions are required to rate/like entries
     */
    private boolean onlyGradersCanRate;

    /**
     * True if entries should be sorted by rating
     */
    private boolean sortByRating;

    /**
     * True if topic has associated podcast feed
     */
    private boolean podcastEnabled;


    /**
     * True if podcast will include posts from students, as well
     */
    private boolean podcastHasStudentPosts;


    /**
     * A comma-separated list of sections ids to which the discussion topic should be made specific to.
     * If it is not desired to make the discussion topic specific to sections, then this parameter may
     * be omitted or set to “all”. Can only be present only on announcements and only those that are
     * for a course (as opposed to a group).
     */
    private String specificSections;
}
