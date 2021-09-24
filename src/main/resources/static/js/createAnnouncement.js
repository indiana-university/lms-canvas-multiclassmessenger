
jQuery(document).ready(function($) {
    //   LMSA-5011 these settings will be enabled at a later date
    //   setUpPodcastOptions();
    //   setUpLikingOptions();

    //   $("#podcastFeed").change(function() {
    //      setUpPodcastOptions();
    //   });
    //
    //   $("#allowLiking").change(function() {
    //      setUpLikingOptions();
    //   });


    function setUpPodcastOptions() {
        var podcastEnabled = $("#podcastFeed").is(":checked");

        if (!podcastEnabled) {
            // uncheck and hide the podcast reply option
            $("#podcastReplies").prop("checked", false);
            $("#podcastRepliesSection").hide();
        } else {
            $("#podcastRepliesSection").show();
            resizeTool();
        }
    }

    function setUpLikingOptions() {
        var likingEnabled = $("#allowLiking").is(":checked");

        if (!likingEnabled) {
            // uncheck and hide the "liking" options
            $("#graderLikes").prop("checked", false);
            $("#sortLikes").prop("checked", false);
            $("#allowLikingSection").hide();
        } else {
            $("#allowLikingSection").show();
            resizeTool();
        }
    }

});

function validation() {
    var valid = true;
    if (!$("#announcementTitle").val() || !$("#announcementTitle").val().trim()) {
        displayValidation('#announcementTitle', '#missingTitle', '#inlineTitleError', false);

        $("#announcementTitle").attr({
          "aria-describedby": "anncTitleError",
          "aria-invalid": "true"
        });

        valid = false;
    }

    var message = tinymce.get('announcementMessage').getContent({format: 'text'});

    if (!message || !message.trim().length) {

        displayValidation('#anncMsgContainer', '#missingMessage', '#inlineMessageError', true)

        $("#announcementMessage").attr({
          "aria-describedby": "anncMsgError",
          "aria-invalid": "true"
        });

        valid = false;
    }

    // make sure at least one course is checked (other than the current course)
    if ($("[name=selectedCourses]:checked").length == 0) {
        displayValidation(null, '#noCourseSelected', '#inlineCourseError', false);

        $("input[name='selectedCourses']").attr("aria-describedby", "selectTwoError");
        valid = false;
    }

    // show the error section
    if (!valid) {
        $("#announcementErrors").show().focus();
    }

    return valid;
}

function displayValidation(inputId, errorMsgId, inlineErrorId, useLocalErrorClass) {
    $(errorMsgId).show();
    $(inlineErrorId).removeClass("hideMe");

    if (inputId) {
        const inputErrorClass = useLocalErrorClass ? 'alert-danger-inline' : 'rvt-validation-danger';
        $(inputId).addClass(inputErrorClass);
    }
}





