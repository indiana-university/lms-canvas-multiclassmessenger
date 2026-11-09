package edu.iu.uits.lms.multiclassmessenger;

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

import edu.iu.uits.lms.canvas.model.CanvasTerm;
import edu.iu.uits.lms.canvas.model.Course;
import edu.iu.uits.lms.canvas.services.CanvasService;
import edu.iu.uits.lms.canvasoauth2.security.CanvasOAuth2AuthorizedClientRepository;
import edu.iu.uits.lms.common.server.ServerInfo;
import edu.iu.uits.lms.lti.LTIConstants;
import edu.iu.uits.lms.lti.config.TestUtils;
import edu.iu.uits.lms.lti.service.LmsDefaultGrantedAuthoritiesMapper;
import edu.iu.uits.lms.multiclassmessenger.config.SecurityConfig;
import edu.iu.uits.lms.multiclassmessenger.controller.MessageController;
import edu.iu.uits.lms.multiclassmessenger.model.message.MessageCreationResult;
import edu.iu.uits.lms.multiclassmessenger.service.MultiClassMessengerToolService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.oauth2.client.endpoint.OAuth2AccessTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import uk.ac.ox.ctl.lti13.security.oauth2.client.lti.authentication.OidcAuthenticationToken;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static edu.iu.uits.lms.multiclassmessenger.CanvasOAuth2TestSupport.REGISTRATION_ID;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(value = MessageController.class, properties = {"oauth.tokenprovider.url=http://foo"})
@ContextConfiguration(classes = {MessageController.class, SecurityConfig.class,
      edu.iu.uits.lms.canvasoauth2.controller.OAuth2ConsentControllerAdvice.class,
      edu.iu.uits.lms.canvasoauth2.controller.CanvasOAuth2ConsentText.class,
      CanvasOAuth2TestSupport.TestConfig.class})
public class MessageControllerTest {

   private static final String LOGIN_ID = "jsmith";

   @Autowired
   private MockMvc mvc;

   @MockitoBean
   private MessageSource messageSource = null;

   @MockitoBean
   private CanvasService canvasService = null;

   @MockitoBean
   private MultiClassMessengerToolService mcmToolService = null;

   @MockitoBean
   private LmsDefaultGrantedAuthoritiesMapper lmsDefaultGrantedAuthoritiesMapper;

   @MockitoBean
   private ClientRegistrationRepository clientRegistrationRepository;

   @MockitoBean(name = ServerInfo.BEAN_NAME)
   private ServerInfo serverInfo;

   // SecurityConfig injects this from CanvasOAuth2ClientConfig, which this slice doesn't load
   @MockitoBean
   private OAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest> canvasOAuth2AccessTokenResponseClient;

   @Autowired
   private CanvasOAuth2AuthorizedClientRepository canvasOAuth2AuthorizedClientRepository;

   @BeforeEach
   void resetCanvasOAuth2AuthorizedClientRepositoryMock() {
      reset(canvasOAuth2AuthorizedClientRepository);
      when(canvasOAuth2AuthorizedClientRepository.hasResolvableCanvasUserId(any())).thenReturn(true);
   }

   private static OidcAuthenticationToken instructorToken() {
      Map<String, Object> customMap = new HashMap<>();
      customMap.put(LTIConstants.CUSTOM_CANVAS_USER_LOGIN_ID_KEY, LOGIN_ID);
      return TestUtils.buildToken("userId", LTIConstants.INSTRUCTOR_AUTHORITY, new HashMap<>(), customMap);
   }

   private void stubConsented() {
      when(canvasOAuth2AuthorizedClientRepository.loadAuthorizedClient(eq(REGISTRATION_ID), any(), any()))
            .thenReturn(CanvasOAuth2TestSupport.authorizedClient());
   }

   private void stubNotConsented() {
      when(canvasOAuth2AuthorizedClientRepository.loadAuthorizedClient(eq(REGISTRATION_ID), any(), any())).thenReturn(null);
   }

   private static Course publishedCourse(String id, String name) {
      Course course = new Course();
      course.setId(id);
      course.setName(name);
      course.setWorkflowState("available");
      // the real call always includes the term, which the locked-course filter reads
      course.setTerm(new CanvasTerm());
      return course;
   }

   @Test
   public void createMessageNoAuthn() throws Exception {
      mvc.perform(get("/msg/createMessage")
                  .header(HttpHeaders.USER_AGENT, TestUtils.defaultUseragent()))
            .andExpect(status().isForbidden());
   }

   @Test
   public void createMessageRequiresCanvasOAuth2ConsentWhenNoAuthorizedClient() throws Exception {
      stubNotConsented();

      mvc.perform(get("/msg/createMessage")
                  .with(authentication(instructorToken()))
                  .header(HttpHeaders.USER_AGENT, TestUtils.defaultUseragent()))
            .andExpect(status().isOk())
            .andExpect(view().name("connectCanvas"))
            .andExpect(model().attribute("authorizationUri", "/oauth2/authorization/" + REGISTRATION_ID));

      // No Canvas call may happen before Canvas OAuth2 consent has been established
      verifyNoInteractions(mcmToolService);
   }

   @Test
   public void createMessageListsCoursesTaught() throws Exception {
      stubConsented();
      when(mcmToolService.getCoursesTaughtBy(LOGIN_ID, true, true, true))
            .thenReturn(List.of(publishedCourse("1", "Course One"), publishedCourse("2", "Course Two")));

      mvc.perform(get("/msg/createMessage")
                  .with(authentication(instructorToken()))
                  .header(HttpHeaders.USER_AGENT, TestUtils.defaultUseragent()))
            .andExpect(status().isOk())
            .andExpect(view().name("createMessage"));

      verify(mcmToolService).getCoursesTaughtBy(LOGIN_ID, true, true, true);
   }

   @Test
   public void createMessageShowsErrorWhenUserTeachesNoActiveCourses() throws Exception {
      stubConsented();
      when(mcmToolService.getCoursesTaughtBy(LOGIN_ID, true, true, true))
            .thenReturn(List.of());

      mvc.perform(get("/msg/createMessage")
                  .with(authentication(instructorToken()))
                  .header(HttpHeaders.USER_AGENT, TestUtils.defaultUseragent()))
            .andExpect(status().isOk())
            .andExpect(view().name("toolerror"));
   }

   @Test
   public void sendRequiresCanvasOAuth2ConsentWhenNoAuthorizedClient() throws Exception {
      stubNotConsented();

      mvc.perform(multipart("/msg/send")
                  .file(new MockMultipartFile("mcmAttachment", new byte[0]))
                  .param("messageSubject", "Subject")
                  .param("messageText", "Body")
                  .param("selectedRecipients", "1:ALL")
                  .with(csrf())
                  .with(authentication(instructorToken()))
                  .header(HttpHeaders.USER_AGENT, TestUtils.defaultUseragent()))
            .andExpect(status().isOk())
            .andExpect(view().name("connectCanvas"));

      verifyNoInteractions(mcmToolService);
   }

   @Test
   public void sendCreatesConversationAndRedirectsToCanvasInbox() throws Exception {
      stubConsented();
      when(canvasService.getBaseUrl()).thenReturn("https://canvas.test");
      when(mcmToolService.createConversation(anyMap(), eq("Subject"), eq("Body"), any()))
            .thenReturn(new MessageCreationResult(List.of(), List.of("1"), List.of(), false));

      mvc.perform(multipart("/msg/send")
                  .file(new MockMultipartFile("mcmAttachment", new byte[0]))
                  .param("messageSubject", "Subject")
                  .param("messageText", "Body")
                  .param("selectedRecipients", "1:ALL", "2:STUDENTS")
                  .with(csrf())
                  .with(authentication(instructorToken()))
                  .header(HttpHeaders.USER_AGENT, TestUtils.defaultUseragent()))
            .andExpect(status().isOk())
            .andExpect(view().name("redirectToCanvas"))
            .andExpect(model().attribute("redirectUrl", "https://canvas.test/conversations"));

      verify(mcmToolService).createConversation(
            eq(Map.of("1", List.of("ALL"), "2", List.of("STUDENTS"))), eq("Subject"), eq("Body"), any());
   }

   @Test
   public void sendAsGetAfterConsentRedirectsToANewForm() throws Exception {
      // OAuth2ConsentControllerAdvice returns the user to the request that needed consent, as a GET
      mvc.perform(get("/msg/send")
                  .with(authentication(instructorToken()))
                  .header(HttpHeaders.USER_AGENT, TestUtils.defaultUseragent()))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/msg/createMessage"));

      verifyNoInteractions(mcmToolService);
   }

   @Test
   public void sendWithMissingFieldsReturnsToTheFormWithoutSending() throws Exception {
      stubConsented();
      when(mcmToolService.getCoursesTaughtBy(LOGIN_ID, true, true, true))
            .thenReturn(List.of(publishedCourse("1", "Course One")));

      mvc.perform(multipart("/msg/send")
                  .file(new MockMultipartFile("mcmAttachment", new byte[0]))
                  .with(csrf())
                  .with(authentication(instructorToken()))
                  .header(HttpHeaders.USER_AGENT, TestUtils.defaultUseragent()))
            .andExpect(status().isOk())
            .andExpect(view().name("createMessage"))
            .andExpect(model().attribute("msgErrors", true))
            .andExpect(model().attribute("msgSubjectError", true))
            .andExpect(model().attribute("msgTextError", true))
            .andExpect(model().attribute("msgRecipError", true));

      verify(mcmToolService, org.mockito.Mockito.never()).createConversation(anyMap(), any(), any(), any());
   }
}
