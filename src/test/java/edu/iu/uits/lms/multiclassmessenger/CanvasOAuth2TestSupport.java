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

import edu.iu.uits.lms.canvasoauth2.CanvasOAuth2Registration;
import edu.iu.uits.lms.canvasoauth2.security.CanvasOAuth2AuthorizedClientRepository;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.OAuth2AccessToken;

import java.time.Instant;

import static org.mockito.Mockito.mock;

/**
 * Shared Canvas OAuth2 setup for controller tests that load a narrow {@code @WebMvcTest} slice
 * (without {@code CanvasOAuth2ClientConfig}).
 */
public final class CanvasOAuth2TestSupport {

   public static final String REGISTRATION_ID = "lms_canvas_oauth2_multiclassmessenger";

   private CanvasOAuth2TestSupport() {
   }

   /**
    * Plain {@code @Bean}s rather than {@code @MockitoBean}s: {@code CanvasOAuth2Registration} only comes from
    * {@code CanvasOAuth2ClientConfig}, which these slices deliberately don't load, and a
    * {@code @MockitoBean} of {@code CanvasOAuth2AuthorizedClientRepository} would sit alongside the
    * auto-configured {@code OAuth2AuthorizedClientRepository} and make that interface ambiguous.
    * Because the repository isn't a {@code @MockitoBean}, tests must {@code reset} it themselves.
    */
   @TestConfiguration
   public static class TestConfig {
      @Bean
      public CanvasOAuth2Registration canvasOAuth2Registration() {
         return new CanvasOAuth2Registration("multiclassmessenger", "/jsrivet");
      }

      @Bean
      public CanvasOAuth2AuthorizedClientRepository canvasOAuth2AuthorizedClientRepository() {
         return mock(CanvasOAuth2AuthorizedClientRepository.class);
      }
   }

   /**
    * @return an authorized client, as the repository returns once the user has consented
    */
   public static OAuth2AuthorizedClient authorizedClient() {
      ClientRegistration clientRegistration = ClientRegistration.withRegistrationId(REGISTRATION_ID)
            .clientId("test-client")
            .clientSecret("test-secret")
            .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
            .authorizationUri("https://canvas.test/login/oauth2/auth")
            .tokenUri("https://canvas.test/login/oauth2/token")
            .build();
      OAuth2AccessToken accessToken = new OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER,
            "test-access-token", Instant.now(), Instant.now().plusSeconds(3600));
      return new OAuth2AuthorizedClient(clientRegistration, "userId", accessToken);
   }
}
