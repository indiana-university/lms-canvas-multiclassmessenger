
jQuery(document).ready(function($) {
    $(".recipient-select").select2({
      placeholder: "Select at least one recipient",
      closeOnSelect: true
    });

    $("input.select2-search__field").attr("aria-labelledby", "recipientsSelectLabel");
    $("input.select2-search__field").addClass("formInput");
    $("input.select2-search__field").attr("id", "recipientSearchField");
});

$(".recipient-select").on('select2:select', function (e) {
    updateRecipientSRLabel();
});

$(".recipient-select").on('select2:unselect', function (e) {
    updateRecipientSRLabel();
});

function updateRecipientSRLabel() {
    // Create a label that includes the selected recipients, if present
    var selectedRecip = '';
    var numRecip = 0;
    $('.select2-selection__choice').each(function () {
        selectedRecip += this.title + ' ';
        numRecip ++;
    });
    
    if (numRecip > 0) {
        var selectedRecipText = 'Currently ' + numRecip + ' selected: ' + selectedRecip;
        $('#recipients-sr').text(selectedRecipText);
    } else {
        $('#recipients-sr').text('Select at least one recipient.');
    }
}

$(".recipient-select").on('select2:open', function (e) {
    $('.select2-results__options[role="listbox"]').attr("aria-label", "Recipient options");
    $('.select2-selection[role="combobox"]').attr("aria-owns", "recipientSearchField select2-recipientsSelect-results");
});

function validation() {

    var valid = true;
    if (!$("#msgSubject").val() || !$("#msgSubject").val().trim()) {

        displayValidation('#msgSubject', '#missingSubject', '#inlineSubjectError', false);

        $("#msgSubject").attr({
          "aria-describedby": "msgSubjectError",
          "aria-invalid": "true"
        });

        valid = false;
    }

    if (!$("#msgText").val() || !$("#msgText").val().trim()) {

        displayValidation('#msgText', '#missingText', '#inlineTextError', false);

        $("#msgText").attr({
          "aria-describedby": "msgTextError",
          "aria-invalid": "true"
        });

        valid = false;
    }

    // make sure at least one course is checked (other than the current course)
    var recipients = $('#recipientsSelect').val();
    if (!recipients || recipients.length < 1) {

        displayValidation('#recipientContainer', '#missingRecipients', '#inlineRecipError', true);

        $("input.select2-search__field").attr({
            "aria-describedby": "msgRecipError",
            "aria-invalid": "true"
        });

        valid=false;
    }

    if (!valid) {
        $("#msgErrors").show().focus();
    }

    return valid;
}

function displayValidation(inputId, errorMsgId, inlineErrorId, useLocalErrorClass) {
    $(errorMsgId).show();
    $(inlineErrorId).removeClass("hideMe");

    const inputErrorClass = useLocalErrorClass ? 'alert-danger-inline' : 'rvt-validation-danger';
    $(inputId).addClass(inputErrorClass);
}