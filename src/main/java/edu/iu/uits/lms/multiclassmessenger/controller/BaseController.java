package edu.iu.uits.lms.multiclassmessenger.controller;

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
import edu.iu.uits.lms.lti.controller.OidcTokenAwareController;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.ClientAuthorizationRequiredException;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@Slf4j
public class BaseController extends OidcTokenAwareController {


    protected static final String TOOL_PATH_MSG = "/msg";
    protected static final String TOOL_PATH_ANNC = "/annc";

    @Autowired
    private OAuth2AuthorizedClientRepository canvasOAuth2AuthorizedClientRepository = null;

    @Autowired
    private CanvasOAuth2Registration canvasOAuth2Registration = null;

    @RequestMapping(value = "/error2")
    public String errorPage() {
        return "toolerror";
    }

    @RequestMapping(value = "/redirectToCanvas")
    public String redirectToCanvas() {
        return "redirectToCanvas";
    }

    /**
     *
     * @param courseIdList List of course IDs
     * @param courseIdToDisplayNameMap Map of courseIDs to Course display names
     * @return a comma-delimited, sorted string of the course display names
     */
    public String getPrettyResultList(List<String> courseIdList, Map<String, String> courseIdToDisplayNameMap) {
        List<String> courseNameList = courseIdList.stream()
                .filter(course -> courseIdToDisplayNameMap.containsKey(course))
                .map(p -> courseIdToDisplayNameMap.get(p))
                .collect(Collectors.toList());

        return courseNameList.stream().sorted().collect(Collectors.joining(", "));
    }

    /**
     * Makes sure the current user has an authorized Canvas OAuth2 client on file before a handler makes
     * per-user Canvas API calls. If not, throws the same exception the {@code @RegisteredOAuth2AuthorizedClient}
     * resolver would have; {@code OAuth2ConsentControllerAdvice} catches it and renders the
     * "connect your Canvas account" consent breakout page instead of an error.
     * @param request the current request
     */
    protected void ensureCanvasOAuth2Consent(HttpServletRequest request) {
        OAuth2AuthorizedClient authorizedClient = canvasOAuth2AuthorizedClientRepository.loadAuthorizedClient(
                canvasOAuth2Registration.getRegistrationId(), SecurityContextHolder.getContext().getAuthentication(), request);
        if (authorizedClient == null) {
            throw new ClientAuthorizationRequiredException(canvasOAuth2Registration.getRegistrationId());
        }
    }

    /**
     * Check if a file is less than 10 MB
     * @param uploadedFile
     * @return
     */
    protected boolean validateFileSize(MultipartFile uploadedFile) {
        long TEN_MB = 1024 * 1024 * 10;
        return uploadedFile.getSize() <= TEN_MB;
    }

}
