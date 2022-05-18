package edu.iu.uits.lms.multiclassmessenger.controller;

/*-
 * #%L
 * lms-canvas-multiclassmessenger
 * %%
 * Copyright (C) 2015 - 2022 Indiana University
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

import edu.iu.uits.lms.lti.controller.ConfigurationController;
import edu.iu.uits.lms.lti.model.Canvas13Extension;
import edu.iu.uits.lms.lti.model.Canvas13Placement;
import edu.iu.uits.lms.lti.model.Canvas13Settings;
import edu.iu.uits.lms.lti.model.Lti13Config;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static edu.iu.uits.lms.lti.LTIConstants.CUSTOM_CANVAS_COURSE_ID_KEY;
import static edu.iu.uits.lms.lti.LTIConstants.CUSTOM_CANVAS_COURSE_ID_VAL;
import static edu.iu.uits.lms.lti.LTIConstants.CUSTOM_CANVAS_USER_LOGIN_ID_KEY;
import static edu.iu.uits.lms.lti.LTIConstants.CUSTOM_CANVAS_USER_LOGIN_ID_VAL;

@RestController
public class Config13Controller extends ConfigurationController {

    @GetMapping("/{type}/config.json")
    public Lti13Config getConfig(@PathVariable("type") String type, HttpServletRequest request) {
        String urlPrefix = ServletUriComponentsBuilder.fromContextPath(request).toUriString();
        Canvas13Placement coursePlacement = Canvas13Placement.builder()
              .placement(Canvas13Placement.Placement.COURSE_NAVIGATION)
              .enabled(true)
              .messageType(Canvas13Placement.MessageType.LtiResourceLinkRequest)
//              .targetLinkUri(urlPrefix + "/launch")
              .build();
//        Canvas13Placement accountPlacement = Canvas13Placement.builder()
//              .placement(Canvas13Placement.Placement.ACCOUNT_NAVIGATION)
//              .enabled(true)
//              .messageType(Canvas13Placement.MessageType.LtiResourceLinkRequest)
//              .build();
        List<Canvas13Placement> placements = Arrays.asList(coursePlacement);
        Canvas13Settings canvas13Settings = Canvas13Settings.builder()
              .placements(placements)
              .build();
        Collection<Canvas13Extension> extensions  = Collections.singleton(Canvas13Extension.builder()
              .platform(Canvas13Extension.INSTRUCTURE)
              .domain(request.getServerName())
              .privacyLevel(Lti13Config.PrivacyLevel.PUBLIC)
              .settings(canvas13Settings)
              .build());
        Map<String, String> customFields = new HashMap<>();
        customFields.put(CUSTOM_CANVAS_COURSE_ID_KEY, CUSTOM_CANVAS_COURSE_ID_VAL);
        customFields.put(CUSTOM_CANVAS_USER_LOGIN_ID_KEY, CUSTOM_CANVAS_USER_LOGIN_ID_VAL);

        String title = "Multiclass Messages";
        String description = "Multiclass Messages";
        String login = "lms_lti_multiclassmessenger_msg";
        String launch = "/msg/loading";

        if ("annc".equals(type)) {
            title = "Multiclass Announcements";
            description = "Multiclass Announcements";
            login = "lms_lti_multiclassmessenger_annc";
            launch = "/annc/loading";
        }

        return Lti13Config.builder()
              .title(title)
              .description(description)
              .oidcInitiationUrl(urlPrefix + "/lti/login_initiation/" + login)
              .targetLinkUri(urlPrefix + launch)
              .extensions(extensions)
              .publicJwk(getJKS().toPublicJWK().toJSONObject())
              .customFields(customFields)
//              .privacyLevel(Lti13Config.PrivacyLevel.PUBLIC)
//              .scopes(Collections.singleton(Lti13Config.Scopes.SCOPE_MEMBERSHIP_READONLY.getValue()))
              .build();
    }

}
