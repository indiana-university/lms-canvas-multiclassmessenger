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
import edu.iu.uits.lms.lti.config.LtiClientTestConfig;
import edu.iu.uits.lms.lti.model.LmsLtiAuthz;
import edu.iu.uits.lms.lti.service.LtiAuthorizationServiceImpl;
import edu.iu.uits.lms.multiclassmessenger.config.ToolConfig;
import edu.iu.uits.lms.multiclassmessenger.controller.MultiClassMessengerLtiController;
import net.oauth.OAuthAccessor;
import net.oauth.OAuthConsumer;
import net.oauth.OAuthException;
import net.oauth.OAuthMessage;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.tsugi.basiclti.BasicLTIConstants;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

//@RunWith(SpringRunner.class)
@WebMvcTest(MultiClassMessengerLtiController.class)
@Import({ToolConfig.class, CanvasClientTestConfig.class, LtiClientTestConfig.class})
@ActiveProfiles("none")
public class LtiLaunchSecurityTest {

   @Autowired
   private MockMvc mvc;

   @MockBean
   private LtiAuthorizationServiceImpl ltiAuthorizationService;

   @Test
   public void ltiLaunch() throws Exception {

      String key = "asdf";
      String secret = "secret";

      LmsLtiAuthz ltiAuthz = new LmsLtiAuthz();
      ltiAuthz.setActive(true);
      ltiAuthz.setConsumerKey(key);
      ltiAuthz.setSecret(secret);


      doReturn(ltiAuthz).when(ltiAuthorizationService).findByKeyContextActive(any(), any());

      Map<String, String> params = new HashMap<>();
      params.put(BasicLTIConstants.LTI_MESSAGE_TYPE, BasicLTIConstants.LTI_MESSAGE_TYPE_BASICLTILAUNCHREQUEST);
      params.put(BasicLTIConstants.LTI_VERSION, BasicLTIConstants.LTI_VERSION_1);
      params.put("oauth_consumer_key", key);
      params.put(BasicLTIConstants.USER_ID, "user");
      params.put(BasicLTIConstants.ROLES, "Instructor");

      Map<String, String> signedParams = signParameters(params, key, secret, "http://localhost/lti", "POST");


      List<NameValuePair> nvpList = signedParams.entrySet().stream()
            .map(entry -> new BasicNameValuePair(entry.getKey(), entry.getValue()))
            .collect(Collectors.toCollection(() -> new ArrayList<>(signedParams.size())));


      //This is an open endpoint and should not be blocked by security
      mvc.perform(post("/lti")
            .header(HttpHeaders.USER_AGENT, TestUtils.defaultUseragent())
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .content(EntityUtils.toString(new UrlEncodedFormEntity(nvpList))))

            .andExpect(status().isOk());
   }

   private Map<String, String> signParameters(Map<String, String> parameters, String key, String secret, String url, String method) throws Exception {
      OAuthMessage oam = new OAuthMessage(method, url, parameters.entrySet());
      OAuthConsumer cons = new OAuthConsumer(null, key, secret, null);
      OAuthAccessor acc = new OAuthAccessor(cons);
      try {
         oam.addRequiredParameters(acc);

         Map<String, String> signedParameters = new HashMap<>();
         for(Map.Entry<String, String> param : oam.getParameters()){
            signedParameters.put(param.getKey(), param.getValue());
         }
         return signedParameters;
      } catch (OAuthException | IOException | URISyntaxException e) {
         throw new Exception("Error signing LTI request.", e);
      }
   }

}
