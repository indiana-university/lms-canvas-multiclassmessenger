package edu.iu.uits.lms.multiclassmessenger.controller;

import edu.iu.uits.lms.canvas.model.Course;
import edu.iu.uits.lms.canvas.services.CanvasService;
import edu.iu.uits.lms.canvas.services.CourseService;
import edu.iu.uits.lms.lti.LTIConstants;
import edu.iu.uits.lms.lti.security.LtiAuthenticationToken;
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

    @RequestMapping("/{context}/loading")
    public String loading(@PathVariable("context") String context, Model model) {
        model.addAttribute("context", context);
        model.addAttribute("hideFooter", true);
        model.addAttribute("toolPath", TOOL_PATH_ANNC + "/" + context + "/createAnnouncement");
        model.addAttribute("toolTitle", messageSource.getMessage(ANNC_TITLE_KEY, null, Locale.getDefault()));
        return "loading";
    }

    @RequestMapping("/{context}/createAnnouncement")
    @Secured(LTIConstants.INSTRUCTOR_AUTHORITY)
    public String createAnnouncement (@PathVariable ("context") String context, Model model) {
        LtiAuthenticationToken token = getValidatedToken(context);
        String currentUser = (String)token.getPrincipal();

        AnnouncementModel mcmModel = new AnnouncementModel();
        Course thisCourse = courseService.getCourse(context);
        String thisTerm = thisCourse.getEnrollmentTermId();

        // We want to include all of the courses that the current user is enrolled in as an instructor
        List<Course> instructorCourses = courseService.getCoursesTaughtBy(currentUser, true, false, false);
        SelectableCourse currentCourse = null;
        List<SelectableCourse> courseOptions = new ArrayList<>();

        if (instructorCourses != null) {
            // we need to filter out the current course, courses not in the curr course term, and remove duplicates
            courseOptions = instructorCourses.stream()
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

        LtiAuthenticationToken token = getValidatedToken(context);
        String currentUser = (String)token.getPrincipal();

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

        // We need to check if the course quotas will be exceeded by this attachment and warn the user
        List<String> quotaViolations = mcmToolService.getCourseQuotaViolations(announcementAttachment, courseIdToDisplayNameMap);
        if (!quotaViolations.isEmpty()) {
            error = true;
            String prettyQuotaViolators = quotaViolations.stream().sorted().collect(Collectors.joining(", "));
            model.addAttribute("quotaError", messageSource.getMessage("annc.error.quota", new String[]{prettyQuotaViolators}, Locale.getDefault()));
        }

        model.addAttribute("anncError", error);

        if (error) {
            return createAnnouncement(context, model);
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
            return createAnnouncement(context, model);
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
