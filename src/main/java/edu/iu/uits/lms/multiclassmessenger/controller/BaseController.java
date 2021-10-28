package edu.iu.uits.lms.multiclassmessenger.controller;

import edu.iu.uits.lms.lti.controller.LtiAuthenticationTokenAwareController;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@Slf4j
public class BaseController extends LtiAuthenticationTokenAwareController {


    protected static final String TOOL_PATH_MSG = "msg";
    protected static final String TOOL_PATH_ANNC = "annc";


    @RequestMapping(value = "/accessDenied")
    public String accessDenied() {
        return "accessDenied";
    }

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

}
