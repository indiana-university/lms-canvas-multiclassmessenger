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
import edu.iu.uits.lms.canvas.services.CanvasService;
import edu.iu.uits.lms.canvas.services.CourseService;
import edu.iu.uits.lms.lti.LTIConstants;
import edu.iu.uits.lms.lti.service.OidcTokenUtils;
import edu.iu.uits.lms.multiclassmessenger.model.announcement.AnnouncementCreationResult;
import edu.iu.uits.lms.multiclassmessenger.model.announcement.AnnouncementModel;
import edu.iu.uits.lms.multiclassmessenger.model.announcement.CreateAnnouncement;
import edu.iu.uits.lms.multiclassmessenger.model.announcement.SelectableCourse;
import edu.iu.uits.lms.multiclassmessenger.service.MultiClassMessengerToolService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import uk.ac.ox.ctl.lti13.security.oauth2.client.lti.authentication.OidcAuthenticationToken;

import javax.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Controller
@RequestMapping("/annc")
public class AnnouncementController extends BaseController {

    @Autowired
    private MessageSource messageSource = null;

    @Autowired
    private CourseService courseService = null;

    @Autowired
    private CanvasService canvasService = null;

    @Autowired
    private MultiClassMessengerToolService mcmToolService = null;

    private static final String ANNC_TITLE_KEY = "annc.tool.title";

    private static final String DELIM = "::";

    @RequestMapping("/loading")
    public String loading(Model model) {
        OidcAuthenticationToken token = getTokenWithoutContext();
        OidcTokenUtils oidcTokenUtils = new OidcTokenUtils(token);
        String courseId = oidcTokenUtils.getCourseId();

        model.addAttribute("context", courseId);
        model.addAttribute("hideFooter", true);
        model.addAttribute("toolPath", TOOL_PATH_ANNC + "/" + courseId + "/createAnnouncement");
        model.addAttribute("toolTitle", messageSource.getMessage(ANNC_TITLE_KEY, null, Locale.getDefault()));
        return "loading";
    }

    @RequestMapping("/{context}/createAnnouncement")
    @Secured(LTIConstants.INSTRUCTOR_AUTHORITY)
    public String createAnnouncement (@PathVariable ("context") String context, Model model, AnnouncementModel mcmModel) {
        OidcAuthenticationToken token = getValidatedToken(context);
        OidcTokenUtils oidcTokenUtils = new OidcTokenUtils(token);
        String currentUser = oidcTokenUtils.getUserLoginId();

        if (mcmModel == null) {
            mcmModel = new AnnouncementModel();
        }

        Course thisCourse = courseService.getCourse(context);
        String thisTerm = thisCourse.getEnrollmentTermId();

        // We want to include all of the courses that the current user is enrolled in as an instructor
        List<Course> instructorCourses = courseService.getCoursesTaughtBy(currentUser, true, false, true);
        SelectableCourse currentCourse = null;
        List<SelectableCourse> courseOptions = new ArrayList<>();

        if (instructorCourses != null) {
            // we want only active courses, then need to filter out the current course, courses not in the curr course term, and remove duplicates
            courseOptions = instructorCourses.stream()
                    .filter(CourseHelper::isCourseActive)
                    .map(p -> new SelectableCourse(p.getId(), p.getName(), p.getEnrollmentTermId()))
                    .filter(course -> (!context.equals(course.getCourseId()) && thisTerm.equals(course.getTermId())))
                    .distinct()
                    .collect(Collectors.toList());

            for (Course course : instructorCourses) {
                if (course.getId().equals(context)) {
                    currentCourse = new SelectableCourse(course.getId(), course.getCourseCode(), course.getEnrollmentTermId());
                    break;
                }
            }
        }

        if (currentCourse == null) {
            // User should not get to this point if they do not have teaching privileges for the current course
            log.error("Unauthorized role using multiclass messenger");
            model.addAttribute("errorText", messageSource.getMessage("annc.error.instructor", null, Locale.getDefault()));
            model.addAttribute("toolTitle", messageSource.getMessage(ANNC_TITLE_KEY, null, Locale.getDefault()));
            return errorPage();
        }

        if (courseOptions.isEmpty()) {
            // Tool should not be available if not allowed to send to multiple courses
            log.error("Tool should not be available if only one authorized course");
            model.addAttribute("errorText", messageSource.getMessage("annc.error.instrAtLeastTwo", null, Locale.getDefault()));
            model.addAttribute("toolTitle", messageSource.getMessage(ANNC_TITLE_KEY, null, Locale.getDefault()));
            return errorPage();
        }

        courseOptions.sort(Comparator.comparing(SelectableCourse::getCourseDisplayName));
        mcmModel.setCurrentCourse(currentCourse);
        mcmModel.setCourseOptions(courseOptions);

        model.addAttribute("mcmModel", mcmModel);

        // We need to redirect to the main announcements tool in Canvas when a user clicks cancel
        model.addAttribute("cancelRedirectUrl", getCanvasAnnouncementsToolUrl(context));

        return "createAnnouncement";
    }

    @RequestMapping(value = "/{context}/publish", method = RequestMethod.POST)
    @Secured(LTIConstants.INSTRUCTOR_AUTHORITY)
    public String publish(@PathVariable("context") String context,
                          @ModelAttribute AnnouncementModel mcmModel,
                          @RequestParam(value = "mcmAttachment", required = false) MultipartFile announcementAttachment,
                          Model model,
                          HttpSession session) {

        OidcAuthenticationToken token = getValidatedToken(context);
        OidcTokenUtils oidcTokenUtils = new OidcTokenUtils(token);
        String currentUser = oidcTokenUtils.getUserLoginId();

        // Put the returned selected courses (which are in the format course id::course display name) in
        // a map for easier handling
        Map<String, String> courseIdToDisplayNameMap =
                mcmModel.getSelectedCourses()
                        .stream()
                        .map(elem -> elem.split(DELIM, 2))
                        .filter(elem -> elem.length==2)
                        .collect(Collectors.toMap(e -> e[0], e -> e[1]));

        // add the current course to our courses to send announcement to
        Course currentCourse = courseService.getCourse(context);
        courseIdToDisplayNameMap.put(context, currentCourse.getName());

        // Check for errors before we attempt to add the announcement to Canvas
        boolean error = false;
        List<String> anncErrors = secondaryValidation(mcmModel);
        if (!anncErrors.isEmpty()) {
            for (String errorAttribute : anncErrors) {
                model.addAttribute(errorAttribute, true);
                error = true;
            }
        }

        // Check for the file being too big
        if (!validateFileSize(announcementAttachment)) {
            error = true;
            model.addAttribute("fileUploadError", true);
        }

        // We need to check if the course quotas will be exceeded by this attachment and warn the user
        List<String> quotaViolations = mcmToolService.getCourseQuotaViolations(announcementAttachment, courseIdToDisplayNameMap);
        if (!quotaViolations.isEmpty()) {
            error = true;
            String prettyQuotaViolators = quotaViolations.stream().sorted().collect(Collectors.joining(", "));
            model.addAttribute("quotaError", messageSource.getMessage("annc.error.quota", new String[]{prettyQuotaViolators}, Locale.getDefault()));
        }

        model.addAttribute("anncError", error);

        if (error) {
            return createAnnouncement(context, model, mcmModel);
        }

        // Now we are ready to add this announcement to Canvas
        List<String> anncCourses = new ArrayList<>();
        anncCourses.addAll(courseIdToDisplayNameMap.keySet());

        CreateAnnouncement newAnnc = new CreateAnnouncement();
        newAnnc.setAllowRating(mcmModel.isAllowLiking());
        newAnnc.setOnlyGradersCanRate(mcmModel.isGraderLikes());
        newAnnc.setSortByRating(mcmModel.isSortLikes());

        newAnnc.setPodcastEnabled(mcmModel.isPodcastFeed());
        newAnnc.setPodcastHasStudentPosts(mcmModel.isPodcastReplies());

        newAnnc.setTitle(mcmModel.getAnnouncementTitle());
        newAnnc.setMessage(mcmModel.getAnnouncementMessage());

        newAnnc.setPublished(true);

        AnnouncementCreationResult result = mcmToolService.createAnnouncementForCourses(context, newAnnc, anncCourses, announcementAttachment, currentUser);

        if (!result.getSuccessCourses().isEmpty()) {
            String prettySuccessList = getPrettyResultList(result.getSuccessCourses(), courseIdToDisplayNameMap);
            model.addAttribute("successMsg", messageSource.getMessage("annc.success", new String[]{prettySuccessList}, Locale.getDefault()));
        }

        if (!result.getFailedCourses().isEmpty()) {
            String prettyFailureList = getPrettyResultList(result.getFailedCourses(), courseIdToDisplayNameMap);
            model.addAttribute("anncCreateError", messageSource.getMessage("annc.error.fail", new String[]{prettyFailureList}, Locale.getDefault()));
            model.addAttribute("anncError", true);
            return createAnnouncement(context, model, mcmModel);
        } else {
            model.addAttribute("redirectUrl", getCanvasAnnouncementsToolUrl(context));
            // redirect to the Canvas Announcements tool
            return redirectToCanvas();
        }

    }



    /**
     * We do validation of the form via javascript in the UI, but this is backup validation in case
     * the javascript fails
     * @return a list of the model attributes associated with these errors for display. If no errors,
     * returns empty list
     */
    private List<String> secondaryValidation(AnnouncementModel mcmModel) {
        List<String> modelErrors = new ArrayList<>();
        if (mcmModel.getAnnouncementTitle() == null || mcmModel.getAnnouncementTitle().isEmpty()) {
            log.debug("Missing announcement title");
            modelErrors.add("announcementTitleError");
        }

        if (mcmModel.getAnnouncementMessage() == null || mcmModel.getAnnouncementMessage().isEmpty()) {
            log.debug("Missing announcement message");
            modelErrors.add("announcementMessageError");
        }

        if (mcmModel.getSelectedCourses() == null || mcmModel.getSelectedCourses().isEmpty()) {
            // error handling to select at least one course
            log.debug("Must select at least one course");
            modelErrors.add("selectCourseError");
        }

        return modelErrors;
    }

    /**
     *
     * @param courseId
     * @return the URL of this course's Announcements tool in Canvas
     */
    private String getCanvasAnnouncementsToolUrl(String courseId) {
       return canvasService.getBaseUrl() + "/courses/" + courseId + "/announcements";
    }
}
