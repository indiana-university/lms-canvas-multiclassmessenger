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

import edu.iu.uits.lms.common.server.ServerInfo;
import edu.iu.uits.lms.lti.config.TestUtils;
import edu.iu.uits.lms.lti.service.LmsDefaultGrantedAuthoritiesMapper;
import edu.iu.uits.lms.lti.service.Lti13Service;
import edu.iu.uits.lms.multiclassmessenger.config.SecurityConfig;
import edu.iu.uits.lms.multiclassmessenger.controller.Config13Controller;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.security.oauth2.client.endpoint.OAuth2AccessTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Covers which paths {@link SecurityConfig} leaves open without an LTI launch.
 */
@WebMvcTest(value = Config13Controller.class, properties = {"oauth.tokenprovider.url=http://foo"})
@ContextConfiguration(classes = {Config13Controller.class, SecurityConfig.class})
public class SecurityConfigTest {

   @Autowired
   private MockMvc mvc;

   @MockitoBean
   private Lti13Service lti13Service;

   @MockitoBean
   private LmsDefaultGrantedAuthoritiesMapper lmsDefaultGrantedAuthoritiesMapper;

   @MockitoBean
   private ClientRegistrationRepository clientRegistrationRepository;

   @MockitoBean(name = ServerInfo.BEAN_NAME)
   private ServerInfo serverInfo;

   // SecurityConfig injects this from CanvasOAuth2ClientConfig, which this slice doesn't load
   @MockitoBean
   private OAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest> canvasOAuth2AccessTokenResponseClient;

   @Test
   public void messagesConfigJsonIsPublic() throws Exception {
      mvc.perform(get("/msg/config.json")
                  .header(HttpHeaders.USER_AGENT, TestUtils.defaultUseragent()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.title").value("Multiclass Messages"))
            .andExpect(jsonPath("$.custom_fields.canvas_user_id").value("$Canvas.user.id"));
   }

   @Test
   public void announcementsConfigJsonIsPublic() throws Exception {
      mvc.perform(get("/annc/config.json")
                  .header(HttpHeaders.USER_AGENT, TestUtils.defaultUseragent()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.title").value("Multiclass Announcements"));
   }

   @Test
   public void staticAssetsArePublic() throws Exception {
      // Nothing is mapped here in this slice, so an unsecured path gets 404 rather than 403
      mvc.perform(get("/jsrivet/rivet.min.css")
                  .header(HttpHeaders.USER_AGENT, TestUtils.defaultUseragent()))
            .andExpect(status().isNotFound());
   }

   @Test
   public void toolPagesRequireAuthn() throws Exception {
      mvc.perform(get("/msg/createMessage")
                  .header(HttpHeaders.USER_AGENT, TestUtils.defaultUseragent()))
            .andExpect(status().isForbidden());
      mvc.perform(get("/annc/1234/createAnnouncement")
                  .header(HttpHeaders.USER_AGENT, TestUtils.defaultUseragent()))
            .andExpect(status().isForbidden());
   }
}
