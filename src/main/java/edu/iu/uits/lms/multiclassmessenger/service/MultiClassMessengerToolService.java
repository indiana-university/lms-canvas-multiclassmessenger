package edu.iu.uits.lms.multiclassmessenger.service;

/*-
 * #%L
 * lms-canvas-multiclassmessenger
 * %%
 * Copyright (C) 2015 - 2025 Indiana University
 * %%
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * 3. Neither the name of the Indiana University nor the names of its contributors
 *    may be used to endorse or promote products derived from this software without
 *    specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */

import edu.iu.uits.lms.canvas.helpers.CanvasConstants;
import edu.iu.uits.lms.canvas.model.Announcement;
import edu.iu.uits.lms.canvas.model.CanvasFile;
import edu.iu.uits.lms.canvas.model.Conversation;
import edu.iu.uits.lms.canvas.model.ConversationCreateWrapper;
import edu.iu.uits.lms.canvas.model.QuotaInfo;
import edu.iu.uits.lms.canvas.services.AnnouncementService;
import edu.iu.uits.lms.canvas.services.ConversationService;
import edu.iu.uits.lms.canvas.services.CourseService;
import edu.iu.uits.lms.canvas.services.FileUploadService;
import edu.iu.uits.lms.canvas.services.UserService;
import edu.iu.uits.lms.multiclassmessenger.model.announcement.AnnouncementCreationResult;
import edu.iu.uits.lms.multiclassmessenger.model.announcement.CreateAnnouncement;
import edu.iu.uits.lms.multiclassmessenger.model.message.MessageCreationResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class MultiClassMessengerToolService {

    @Autowired
    private AnnouncementService announcementService = null;

    @Autowired
    private CourseService courseService = null;

    @Autowired
    private FileUploadService fileUploadService = null;

    @Autowired
    private ConversationService conversationService = null;

    @Autowired
    private UserService userService = null;

    public enum ROLE_OPTION {
        ALL,
        TEACHERS,
        TAS,
        DESIGNERS,
        STUDENTS,
        OBSERVERS
    }

    public AnnouncementCreationResult createAnnouncementForCourses(String context, CreateAnnouncement announcement, List<String> courseIds, MultipartFile multipartFileAttachment, String asUser) {
        AnnouncementCreationResult result = new AnnouncementCreationResult();
        List<Announcement> savedAnnouncements = new ArrayList<>();
        List<String> successfulCourses = new ArrayList<>();
        List<String> failedCourses = new ArrayList<>();

        File attachment = null;
        String attachmentFileName = null;
        if (multipartFileAttachment != null) {
            attachment = convertToTempFile(context, multipartFileAttachment);
            attachmentFileName = multipartFileAttachment.getOriginalFilename();
        }

        if (courseIds != null) {
            for (String courseId : courseIds) {
                try {
                    log.info("trying to make an announcement");
                    // convert most of CreateAnnouncement to Announcement
                    Announcement realAnnouncement = new Announcement();
                    realAnnouncement.setTitle(announcement.getTitle());
                    realAnnouncement.setMessage(announcement.getMessage());
                    realAnnouncement.setPodcastHasStudentPosts(announcement.isPodcastHasStudentPosts());
                    realAnnouncement.setAllowRating(announcement.isAllowRating());
                    realAnnouncement.setOnlyGradersCanRate(announcement.isOnlyGradersCanRate());
                    realAnnouncement.setSortByRating(announcement.isSortByRating());
                    realAnnouncement.setPublished(announcement.isPublished());

                    // TODO might add a isPodcastEnabled attribute to the Announcement/DiscussionTopic model object to
                    // simplify this call a bit. It might also eliminate the new for the CreateAnnouncement object
                    Announcement savedAnnouncement = announcementService.createAnnouncement(courseId, realAnnouncement,
                            announcement.isPodcastEnabled(),
                            CanvasConstants.API_FIELD_SIS_LOGIN_ID + ":" + asUser,
                            attachmentFileName, attachment);
                    log.info("made the announcement");

                    if (savedAnnouncement != null) {
                        savedAnnouncements.add(savedAnnouncement);
                        successfulCourses.add(courseId);
                    } else {
                        log.debug("Announcement creation failure for course: " + courseId);
                        failedCourses.add(courseId);
                    }
                } catch (RuntimeException re) {
                    log.debug("Announcement creation failure for course: " + courseId);
                    log.info("Exception ", re);
                    failedCourses.add(courseId);
                }
            }
        }

        // delete the file
        if (attachment != null) {
            attachment.delete();
        }

        result.setCreatedAnnouncements(savedAnnouncements);
        result.setSuccessCourses(successfulCourses);
        result.setFailedCourses(failedCourses);

        return result;
    }

    public List<String> getCourseQuotaViolations(MultipartFile attachment, Map<String, String> courseIdToDisplayNameMap) {
        List<String> quotaViolations = new ArrayList<>();
        if (attachment != null && !attachment.isEmpty()) {
            for (String courseId : courseIdToDisplayNameMap.keySet()) {
                // check to see if there is quota available for this attachment
                QuotaInfo quotaInfo = courseService.getCourseQuotaInfo(courseId);
                if (quotaInfo != null && attachment.getSize() > quotaInfo.getQuotaAvailable()) {
                    log.debug("Attachment is too large for site " + courseId);
                    quotaViolations.add(courseIdToDisplayNameMap.get(courseId));
                }
            }
        }

        return quotaViolations;
    }

    public QuotaInfo getUserQuotaInfo(String username) {
        return userService.getUserQuotaInfo(CanvasConstants.API_FIELD_SIS_LOGIN_ID + ":" + username);
    }

    public MessageCreationResult createConversation(String username, Map<String, List<String>> courseIdToRoleListMap, String subject, String body, MultipartFile multipartAttachment) {

        List<String> successCourses = new ArrayList<>();
        List<String> failedCourses = new ArrayList<>();
        List<Conversation> newConvos = new ArrayList<>();

        // First, we need to upload the attachment to Canvas
        String[] attachmentIds = null;
        if (multipartAttachment != null && !multipartAttachment.isEmpty()) {
            CanvasFile uploadedAttachment  = uploadConversationFileToCanvas(username, multipartAttachment);
            if (uploadedAttachment == null) {
                failedCourses = new ArrayList<>(courseIdToRoleListMap.keySet());
                return new MessageCreationResult(newConvos, successCourses, failedCourses, true);
            } else {
                attachmentIds = new String[]{uploadedAttachment.getId()};
            }
        }

        for (String courseId : courseIdToRoleListMap.keySet()) {
            List<String> roles = courseIdToRoleListMap.get(courseId);
            String recipPrefix = "course_" + courseId;
            String[] recipients;
            if (roles.contains(ROLE_OPTION.ALL.name())) {
                // we are sending to the entire course
                recipients = new String[]{recipPrefix};

            } else {
                // otherwise, we have to send to each selected role
                recipients = new String[roles.size()];
                for (int i = 0; i < roles.size(); i++) {
                    recipients[i] = recipPrefix + "_" + roles.get(i).toLowerCase();
                }
            }

// TODO - see if this non constructor massive sets works
//            ConversationCreateWrapper convoWrapper = new ConversationCreateWrapper(recipients, subject, body, null, false, attachmentIds, null);

            ConversationCreateWrapper convoWrapper = new ConversationCreateWrapper();
            convoWrapper.setRecipients(recipients);
            convoWrapper.setSubject(subject);
            convoWrapper.setBody(body);
            convoWrapper.setGroupConversation(false);
            if (attachmentIds != null) {
                convoWrapper.setAttachmentIds(attachmentIds);
            }

            // not sure if we'd ever want a non bulk mode
            final boolean isBulk = true;

            Conversation savedConvo = conversationService.postConversation(convoWrapper, CanvasConstants.API_FIELD_SIS_LOGIN_ID + ":" + username, isBulk);

            // in isBulk mode we never get a savedConvo back from the service.  But leaving this if-else in the code
            // in case Canvas ever allows unlimited/large sends using normal sync send
            if (! isBulk && savedConvo == null) {
                failedCourses.add(courseId);
            } else {
                successCourses.add(courseId);
                newConvos.add(savedConvo);
            }
        }

        return new MessageCreationResult(newConvos, successCourses, failedCourses, false);
    }

    private CanvasFile uploadConversationFileToCanvas(String username, MultipartFile multipartFile) {
        File tempFile = convertToTempFile(username, multipartFile);
        CanvasFile canvasFile = null;

        try {
            canvasFile = fileUploadService.uploadConversationFile(CanvasConstants.API_FIELD_SIS_LOGIN_ID + ":" + username,
                    multipartFile.getOriginalFilename(), multipartFile.getSize(), multipartFile.getContentType(), tempFile, false);

        } catch(Exception cfue) {
            log.error("An error occurred uploading the conversation file to Canvas for user " + username, cfue);
        } finally {
            if (tempFile != null) {
                tempFile.delete();
            }
        }

        return canvasFile;
    }

    private static File convertToTempFile(String context, MultipartFile file) {

        File newFile = null;
        if (file != null && !file.isEmpty()) {
            try {
                newFile = File.createTempFile(context + "-", ".tmp");
                FileOutputStream fos = new FileOutputStream(newFile);
                fos.write(file.getBytes());
                fos.close();
            } catch (IOException ioe) {
                log.error("Error converting announcement attachment to File. Null attachment returned.", ioe);
                newFile = null;
            }
        }

        return newFile;
    }
}
