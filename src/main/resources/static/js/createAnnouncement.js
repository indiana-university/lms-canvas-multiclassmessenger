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





