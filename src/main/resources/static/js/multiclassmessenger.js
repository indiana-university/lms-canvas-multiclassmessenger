
jQuery(document).ready(function($) {

  $("#publishButton").click(function(event) {
    return publishToCanvas(event, false);
  });

  $("#previewPublish").click(function(event) {
    return publishToCanvas(event, true);
  });

  $("#cancelButton").click(function(event) {
       window.top.location.href = $("#cancelRedirectUrl").val();
  });

  $("#previewButton").click(function() {
          // we are using javascript to retrieve the user input and displaying the values in the popup
          // This preview may be used for announcements or messages, so we need to see which tool we are working with

          // The recipients section for a Message
          if ($("#recipientsSelect").length) {
                      var recipientsText = "None";
                      var selected = $("#recipientsSelect option:selected");
                      if (selected.length) {
                          recipientsText = $( selected ).map(function() {
                                               return $(this).text();
                                             }).get().join(", ");
                      }

                      $("#previewRecipients").text(recipientsText);
                    }

          // The announcement title/message subject
          var previewTitle;
          if ($("#announcementTitle").length) {
              previewTitle = $("#announcementTitle").val();
          } else {
              previewTitle = $("#msgSubject").val();
          }
          if (!previewTitle) {
              previewTitle = "None";
          }

          $("#previewTitle").text(previewTitle);

          // The text of the announcement/message. We use TinyMCE for the announcement text but plain text for messages
          var previewText;
          if ($("#announcementMessage").length) {
              previewText = tinymce.get('announcementMessage').getContent();
          } else {
              previewText = $("#msgText").val();

              previewText = previewText.replace(/\r?\n/g, '<br />');
          }
          if (!previewText) {
              previewText = "None";
          }
          $("#previewText").html(previewText);

          var attachment = $("#mcmAttachment").val();
          if (!attachment) {
              $("#previewAttachment").hide();
          } else {
               $("#previewAttachment").show();
              // for security, the returned filename will be prepended with this fakepath string. Let's remove it for display
              attachment = attachment.replace("C:\\fakepath\\", "");
              $("#previewAttachmentFile").text(attachment);
          }

          scrollToTopOfTool();
     });

     // this will prevent forms from submitting twice
     $('form').preventDoubleSubmission();

});

function closeModal() {
    // Find the modal you want to close in the DOM
    const modalToClose = document.querySelector('#modal-example-basic');

    // Close the modal
    Modal.close(modalToClose);
}

function publishToCanvas(event, isPreview) {
   $("#successMessage").hide();
   $(".loading-inline").show();

   clearErrors();
   if (!validation()) {
        scrollToTopOfTool();
        resizeTool();
        event.preventDefault();
        $(".loading-inline").hide();

        if (isPreview) {
            closeModal();
        }
        return false;
   }
}


function clearErrors() {
   $(".rvt-alert-list__item").hide();
   $(".errorSection").hide();

   // we have to add/remove this display class because using hide/show affects the alignment of the inline error msgs for some reason
   $(".rvt-inline-alert").addClass("hideMe");

   // Be careful not to remove non-error aria-describedby when you add this class to an element.
   // If there are existing aria-describedby you will have to handle it separately
   $(".formInput").removeAttr("aria-invalid aria-describedby");

   // remove the inline validation on the inputs
   $(".rvt-validation-danger").removeClass("rvt-validation-danger");
   $(".alert-danger-inline").removeClass("alert-danger-inline");
}


function resizeTool() {
    // Canvas has a message listener to resize the iframe
    parent.postMessage(JSON.stringify({subject: 'lti.frameResize', height: $(document).height()}), '*');

}

function scrollToTopOfTool() {
    // Canvas has a message listener to scroll to the top of the iframe
    parent.postMessage(JSON.stringify({subject: 'lti.scrollToTop'}), '*');

}

// jQuery plugin to prevent double submission of forms
jQuery.fn.preventDoubleSubmission = function() {
    $(this).on('submit',function(e){
        var $form = $(this);

        if ($form.data('submitted') === true) {
            // Previously submitted - don't submit again
            e.preventDefault();
        } else {
            // Mark it so that the next submit can be ignored
            $form.data('submitted', true);
            var buttons = $(':button');
            $(buttons).each(function() {
                $(this).addClass('disableSubmit');
                $(this).prop('disabled', true);
            });
        }
    });

    // Keep chainability
    return this;
};

$(window).on('load', function(){
  resizeTool();
});

// This will scroll back to the top after a successful submission
$(window).on('unload', function(){
  scrollToTopOfTool();
});