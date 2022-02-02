package edu.iu.uits.lms.multiclassmessenger.controller;

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

import edu.iu.uits.lms.canvas.helpers.CourseHelper;
import edu.iu.uits.lms.canvas.model.Course;
import edu.iu.uits.lms.canvas.model.QuotaInfo;
import edu.iu.uits.lms.canvas.services.CanvasService;
import edu.iu.uits.lms.canvas.services.CourseService;
import edu.iu.uits.lms.lti.security.LtiAuthenticationToken;
import edu.iu.uits.lms.multiclassmessenger.model.message.MessageCreationResult;
import edu.iu.uits.lms.multiclassmessenger.model.message.MessageModel;
import edu.iu.uits.lms.multiclassmessenger.model.message.SelectableRecipient;
import edu.iu.uits.lms.multiclassmessenger.service.MultiClassMessengerToolService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Controller
@RequestMapping("/msg")
public class MessageController extends BaseController {

    @Autowired
    private MessageSource messageSource = null;

    @Autowired
    private CourseService courseService = null;

    @Autowired
    private CanvasService canvasService = null;

    @Autowired
    private MultiClassMessengerToolService mcmToolService = null;

    private static final String DELIM = "::";

    @RequestMapping("/loading")
    public String loading( Model model) {
        model.addAttribute("hideFooter", true);
        model.addAttribute("toolPath", TOOL_PATH_MSG + "/createMessage");
        model.addAttribute("toolTitle", messageSource.getMessage("msg.tool.title", null, Locale.getDefault()));
        return "loading";
    }

    @RequestMapping("/createMessage")
    public String createMessage (Model model, MessageModel msgModel) {
        LtiAuthenticationToken token = getTokenWithoutContext();
        String currentUser = (String) token.getPrincipal();

        // Determine the sites for which the user has instructor privileges
        List<Course> courseList = courseService.getCoursesTaughtBy(currentUser, true, true, true);

        if (courseList != null && !courseList.isEmpty()) {
            // Now we need to filter these courses for published, active courses
            courseList = courseList.stream()
                    .filter(course -> CourseHelper.isPublished(course) && ! CourseHelper.isLocked(course, false))
                    .distinct()
                    .collect(Collectors.toList());
        }

        if (courseList == null || courseList.isEmpty()) {
            // User must be an instructor in at least one course in an active term
            log.error("User not an instructor in at least one active course");
            model.addAttribute("errorText", messageSource.getMessage("msg.error.instructor", null, Locale.getDefault()));
            model.addAttribute("toolTitle", messageSource.getMessage("msg.tool.title", null, Locale.getDefault()));
            return errorPage();
        }

        if (msgModel == null) {
            msgModel = new MessageModel();
        }

        Map<String, String> roleMap = new LinkedHashMap<>();
        for (MultiClassMessengerToolService.ROLE_OPTION role : MultiClassMessengerToolService.ROLE_OPTION.values()) {
            roleMap.put(role.name(), messageSource.getMessage("msg.role." + role.toString(), null, Locale.getDefault()));
        }

        List<SelectableRecipient> recipientOptions = new ArrayList<>();
        List<String> courseIdCourseTitle = new ArrayList<>();
        for (Course course : courseList) {
            SelectableRecipient recip = new SelectableRecipient();
            recip.setCourseId(course.getId());
            recip.setCourseDisplayName(course.getName());
            recip.setRoleIdToNameMap(roleMap);

            recipientOptions.add(recip);

            courseIdCourseTitle.add(course.getId() + DELIM + course.getName());
        }

        msgModel.setRecipientOptions(recipientOptions);
        msgModel.setCourseIdCourseTitle(courseIdCourseTitle);

        model.addAttribute("mcmModel", msgModel);

        // We need to redirect to the main Messages tool in Canvas when a user clicks cancel
        model.addAttribute("cancelRedirectUrl", getMessagesToolUrl());

        return "createMessage";
    }

    @RequestMapping(value = "/send", method = RequestMethod.POST)
    public String sendMessage(@ModelAttribute MessageModel messageModel,
                          @RequestParam(value = "mcmAttachment", required = false) MultipartFile messageAttachment,
                          Model model,
                          HttpSession session) {

        LtiAuthenticationToken token = getTokenWithoutContext();
        String currentUser = (String)token.getPrincipal();

        // Check for errors before we attempt to send the message
        boolean error = false;

        // Check for the file being too big
        if (!validateFileSize(messageAttachment)) {
            error = true;
            model.addAttribute("fileUploadError", true);
        }

        List<String> msgErrors = secondaryValidation(messageModel, messageAttachment, currentUser);
        if (!msgErrors.isEmpty()) {
            for (String errorAttribute : msgErrors) {
                model.addAttribute(errorAttribute, true);
                error = true;
            }
        }

        model.addAttribute("msgErrors", error);

        if (error) {
            return createMessage(model, messageModel);
        }

        // Put the returned selected courses (which are in the format course id:role) in
        // a map for easier handling. Course Id -> List of selected roles
        Map<String, List<String>> courseIdToRoleList = new HashMap<>();

        for (String recipient : messageModel.getSelectedRecipients()) {
            String [] courseIdRole = recipient.split(":", 2);
            String courseId = courseIdRole[0];
            String role = courseIdRole[1];

            List<String> roleList = new ArrayList<>();
            if (courseIdToRoleList.containsKey(courseId)) {
                roleList = courseIdToRoleList.get(courseId);
            }

            roleList.add(role);
            courseIdToRoleList.put(courseId, roleList);
        }

        MessageCreationResult result = mcmToolService.createConversation(currentUser, courseIdToRoleList, messageModel.getMessageSubject(), messageModel.getMessageText(), messageAttachment);

        if (result != null && (result.getFailedCourses() == null || result.getFailedCourses().isEmpty())) {
            model.addAttribute("redirectUrl", getMessagesToolUrl());
            // redirect to the User's inbox
            return redirectToCanvas();
        } else {

            if (result.isAttachmentUploadFailure()) {
                model.addAttribute("msgSendFailure", messageSource.getMessage("msg.error.attachment", null, Locale.getDefault()));
                model.addAttribute("msgErrors", true);
                return createMessage(model, messageModel);
            }

            // we need to build a message indicating failures
            // We stored a list of courseId:courseTitle to save us having to look up the course title again. Let's
            // convert it to a map for easier access.

            Map<String, String> courseIdToDisplayNameMap =
                    messageModel.getCourseIdCourseTitle()
                            .stream()
                            .map(elem -> elem.split(DELIM, 2))
                            .filter(elem -> elem.length == 2)
                            .collect(Collectors.toMap(e -> e[0], e -> e[1]));

            if (!result.getSuccessCourses().isEmpty()) {
                String prettySuccessList = getPrettyResultList(result.getSuccessCourses(), courseIdToDisplayNameMap);
                model.addAttribute("successMsg", messageSource.getMessage("msg.success", new String[]{prettySuccessList}, Locale.getDefault()));
            }

            if (!result.getFailedCourses().isEmpty()) {
                String prettyFailureList = getPrettyResultList(result.getFailedCourses(), courseIdToDisplayNameMap);
                model.addAttribute("msgSendFailure", messageSource.getMessage("msg.error.sendFailure", new String[]{prettyFailureList}, Locale.getDefault()));
                model.addAttribute("msgErrors", true);
            }

            return createMessage(model, messageModel);
        }

    }

    /**
     * We do validation of the form via javascript in the UI, but this is backup validation in case
     * the javascript fails. Also checks for attachment exceeding user's file quota
     * @return a list of the model attributes associated with these errors for display. If no errors,
     * returns empty list
     */
    private List<String> secondaryValidation(MessageModel msgModel, MultipartFile messageAttachment, String currentUser) {
        List<String> modelErrors = new ArrayList<>();
        if (msgModel.getMessageSubject() == null || msgModel.getMessageSubject().trim().isEmpty()) {
            log.debug("Missing message subject");
            modelErrors.add("msgSubjectError");
        }

        if (msgModel.getMessageText() == null || msgModel.getMessageText().trim().isEmpty()) {
            log.debug("Missing message text");
            modelErrors.add("msgTextError");
        }

        if (msgModel.getSelectedRecipients() == null || msgModel.getSelectedRecipients().isEmpty()) {
            // error handling to select at least one recipient
            log.debug("Must select at least one recipient");
            modelErrors.add("msgRecipError");
        }

        // Check to see if we will exceed the user's file quota
        if (messageAttachment != null && !messageAttachment.isEmpty()) {
            QuotaInfo quotaInfo = mcmToolService.getUserQuotaInfo(currentUser);
            if (quotaInfo != null && messageAttachment.getSize() > quotaInfo.getQuotaAvailable()) {
                log.debug("Quota exceeded for attachment for user: " + currentUser);
                modelErrors.add("attachQuotaError");
            }
        }

        return modelErrors;
    }

    /**
     *
     * @return the URL of this user's Messaging tool in Canvas
     */
    private String getMessagesToolUrl() {
        return canvasService.getBaseUrl() + "/conversations";
    }
}
