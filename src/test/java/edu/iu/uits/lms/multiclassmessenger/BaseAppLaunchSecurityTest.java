package edu.iu.uits.lms.multiclassmessenger;

/*-
 * #%L
 * lms-canvas-multiclassmessenger
 * %%
 * Copyright (C) 2015 - 2021 Indiana University
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

import edu.iu.uits.lms.canvas.config.CanvasClientTestConfig;
import edu.iu.uits.lms.canvas.model.Course;
import edu.iu.uits.lms.canvas.services.CanvasService;
import edu.iu.uits.lms.canvas.services.CourseService;
import edu.iu.uits.lms.lti.LTIConstants;
import edu.iu.uits.lms.lti.config.LtiClientTestConfig;
import edu.iu.uits.lms.lti.config.TestUtils;
import edu.iu.uits.lms.multiclassmessenger.config.ToolConfig;
import edu.iu.uits.lms.multiclassmessenger.controller.AnnouncementController;
import edu.iu.uits.lms.multiclassmessenger.service.MultiClassMessengerToolService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import uk.ac.ox.ctl.lti13.security.oauth2.client.lti.authentication.OidcAuthenticationToken;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = AnnouncementController.class, properties = {"oauth.tokenprovider.url=http://foo"})
@Import({ToolConfig.class, CanvasClientTestConfig.class, LtiClientTestConfig.class})
//@ActiveProfiles("ltirest")
public abstract class BaseAppLaunchSecurityTest {

   @Autowired
   private MockMvc mvc;

   @MockBean
   private MessageSource messageSource = null;

   @MockBean
   private CourseService courseService = null;

   @MockBean
   private CanvasService canvasService = null;

   @MockBean
   private MultiClassMessengerToolService mcmToolService = null;

   @Test
   public void appNoAuthnLaunch() throws Exception {
      //This is a secured endpoint and should not not allow access without authn
      mvc.perform(get("/annc/loading")
            .header(HttpHeaders.USER_AGENT, TestUtils.defaultUseragent())
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isForbidden());
   }

   @Test
   public void appAuthnWrongContextLaunch() throws Exception {
      OidcAuthenticationToken token = TestUtils.buildToken("userId", "asdf", LTIConstants.INSTRUCTOR_AUTHORITY);

      SecurityContextHolder.getContext().setAuthentication(token);

      //This is a secured endpoint and should not allow access without authn
      mvc.perform(get("/annc/1234/createAnnouncement")
            .header(HttpHeaders.USER_AGENT, TestUtils.defaultUseragent())
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isInternalServerError())
            .andExpect(MockMvcResultMatchers.model().attributeExists("error"))
            .andExpect(MockMvcResultMatchers.view().name ("error"));
   }

   @Test
   public void appAuthnLaunchToLoading() throws Exception {
      OidcAuthenticationToken token = TestUtils.buildToken("userId", "1234", LTIConstants.INSTRUCTOR_AUTHORITY);

      SecurityContextHolder.getContext().setAuthentication(token);

      //This is a secured endpoint and should not not allow access without authn
      mvc.perform(get("/annc/loading")
            .header(HttpHeaders.USER_AGENT, TestUtils.defaultUseragent())
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk());
   }

   @Test
   public void appAuthnLaunch() throws Exception {
      OidcAuthenticationToken token = TestUtils.buildToken("userId", "1234", LTIConstants.INSTRUCTOR_AUTHORITY);

      SecurityContextHolder.getContext().setAuthentication(token);

      //Mock course and stuff
      Course course = new Course();
      course.setEnrollmentTermId("term1");
      when(courseService.getCourse(anyString())).thenReturn(course);

      //This is a secured endpoint and should not not allow access without authn
      mvc.perform(get("/annc/1234/createAnnouncement")
                  .header(HttpHeaders.USER_AGENT, TestUtils.defaultUseragent())
                  .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk());
   }

   @Test
   public void randomUrlNoAuth() throws Exception {
      SecurityContextHolder.getContext().setAuthentication(null);
      //This is a secured endpoint and should not not allow access without authn
      mvc.perform(get("/asdf/foobar")
            .header(HttpHeaders.USER_AGENT, TestUtils.defaultUseragent())
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isForbidden());
   }

   @Test
   public void randomUrlWithAuth() throws Exception {
      OidcAuthenticationToken token = TestUtils.buildToken("userId", "foo", TestUtils.defaultRole());
      SecurityContextHolder.getContext().setAuthentication(token);

      //This is a secured endpoint and should not not allow access without authn
      mvc.perform(get("/asdf/foobar")
            .header(HttpHeaders.USER_AGENT, TestUtils.defaultUseragent())
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isNotFound());
   }
}
