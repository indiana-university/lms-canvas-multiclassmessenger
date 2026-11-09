package edu.iu.uits.lms.multiclassmessenger.service;

/*-
 * #%L
 * lms-canvas-multiclassmessenger
 * %%
 * Copyright (C) 2015 - 2026 Indiana University
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

import edu.iu.uits.lms.canvas.model.Announcement;
import edu.iu.uits.lms.canvas.model.ConversationCreateWrapper;
import edu.iu.uits.lms.canvas.model.Course;
import edu.iu.uits.lms.canvas.model.QuotaInfo;
import edu.iu.uits.lms.canvas.services.AnnouncementService;
import edu.iu.uits.lms.canvas.services.ConversationService;
import edu.iu.uits.lms.canvas.services.CourseService;
import edu.iu.uits.lms.canvas.services.FileUploadService;
import edu.iu.uits.lms.canvas.services.UserService;
import edu.iu.uits.lms.multiclassmessenger.model.announcement.AnnouncementCreationResult;
import edu.iu.uits.lms.multiclassmessenger.model.announcement.CreateAnnouncement;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Every Canvas call in the tool goes through this service, and each must use the launching user's own Canvas
 * OAuth2 token (the {@code CanvasRestTemplateAsUser} bean) with no {@code as_user_id} masquerade.
 */
@ExtendWith(MockitoExtension.class)
class MultiClassMessengerToolServiceTest {

    @Mock
    private AnnouncementService announcementService;

    @Mock
    private CourseService courseService;

    @Mock
    private FileUploadService fileUploadService;

    @Mock
    private ConversationService conversationService;

    @Mock
    private UserService userService;

    @Mock
    private RestTemplate canvasRestTemplateAsUser;

    @InjectMocks
    private MultiClassMessengerToolService mcmToolService;

    @Test
    void getCourse_usesThePerUserToken() {
        Course course = new Course();
        when(courseService.getCourse("1234", canvasRestTemplateAsUser)).thenReturn(course);

        assertThat(mcmToolService.getCourse("1234")).isSameAs(course);
    }

    @Test
    void getCoursesTaughtBy_passesTheFlagsAndThePerUserToken() {
        List<Course> courses = List.of(new Course());
        when(courseService.getCoursesTaughtBy("jsmith", true, false, true, canvasRestTemplateAsUser)).thenReturn(courses);

        assertThat(mcmToolService.getCoursesTaughtBy("jsmith", true, false, true)).isSameAs(courses);
    }

    @Test
    void getUserQuotaInfo_asksForSelfWithThePerUserToken() {
        QuotaInfo quotaInfo = new QuotaInfo();
        when(userService.getUserQuotaInfo("self", canvasRestTemplateAsUser)).thenReturn(quotaInfo);

        assertThat(mcmToolService.getUserQuotaInfo()).isSameAs(quotaInfo);
    }

    @Test
    void getCourseQuotaViolations_checksEachCourseWithThePerUserToken() {
        // available space is quota minus quotaUsed; the attachment below is 5 bytes
        QuotaInfo roomy = new QuotaInfo();
        roomy.setQuota(1000L);
        QuotaInfo full = new QuotaInfo();
        full.setQuota(100L);
        full.setQuotaUsed(99L);
        when(courseService.getCourseQuotaInfo("1", canvasRestTemplateAsUser)).thenReturn(roomy);
        when(courseService.getCourseQuotaInfo("2", canvasRestTemplateAsUser)).thenReturn(full);

        List<String> violations = mcmToolService.getCourseQuotaViolations(
                new MockMultipartFile("mcmAttachment", "a.txt", "text/plain", "hello".getBytes()),
                Map.of("1", "Course One", "2", "Course Two"));

        assertThat(violations).containsExactly("Course Two");
    }

    @Test
    void createConversation_postsWithThePerUserTokenAndNoMasquerade() {
        mcmToolService.createConversation(Map.of("1", List.of("ALL")), "Subject", "Body", null);

        ArgumentCaptor<ConversationCreateWrapper> wrapper = ArgumentCaptor.forClass(ConversationCreateWrapper.class);
        verify(conversationService).postConversation(wrapper.capture(), isNull(), eq(true), eq(canvasRestTemplateAsUser));
        assertThat(wrapper.getValue().getRecipients()).containsExactly("course_1");
        // no attachment, so nothing to upload
        verifyNoInteractions(fileUploadService);
    }

    @Test
    void createConversation_uploadsTheAttachmentWithThePerUserToken() {
        mcmToolService.createConversation(Map.of("1", List.of("ALL")), "Subject", "Body",
                new MockMultipartFile("mcmAttachment", "a.txt", "text/plain", "hello".getBytes()));

        verify(fileUploadService).uploadConversationFile(eq("a.txt"), eq(5L), eq("text/plain"), any(), eq(false),
                eq(canvasRestTemplateAsUser));
    }

    @Test
    void createAnnouncementForCourses_postsWithThePerUserTokenAndNoMasquerade() {
        when(announcementService.createAnnouncement(eq("1"), any(Announcement.class), eq(false), isNull(), isNull(),
                isNull(), eq(canvasRestTemplateAsUser))).thenReturn(new Announcement());

        CreateAnnouncement announcement = new CreateAnnouncement();
        announcement.setTitle("Title");
        announcement.setMessage("Message");

        AnnouncementCreationResult result = mcmToolService.createAnnouncementForCourses("1", announcement, List.of("1"), null);

        assertThat(result.getSuccessCourses()).containsExactly("1");
    }
}
