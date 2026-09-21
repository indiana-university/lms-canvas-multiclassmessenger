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

function applyRecipientSelectAccessibility() {
    $(".select2-selection--multiple").attr({
        "id": "recipientsSelectSelection",
        "tabindex": "0",
        "aria-labelledby": "recipientsSelectLabel",
        "aria-label": "Recipients"
    });
}

function setRecipientValidationState(isValid) {
    const $searchField = $(".select2-search__field");
    const $hiddenSelect = $("#recipientsSelect");

    if (isValid) {
        $searchField.removeAttr("aria-invalid aria-describedby");
        $hiddenSelect.removeAttr("aria-invalid aria-describedby");
    } else {
        $searchField.attr({
            "aria-invalid": "true",
            "aria-describedby": "missingRecipMessage"
        });
        $hiddenSelect.attr({
            "aria-invalid": "true",
            "aria-describedby": "missingRecipMessage"
        });
    }
}

jQuery(document).ready(function($) {
    $(".recipient-select").select2({
      placeholder: "Select at least one recipient",
      closeOnSelect: true
    });

    applyRecipientSelectAccessibility();
    $(".select2-search__field").addClass("formInput");
});

$(".recipient-select").on('select2:select', function (e) {
    updateRecipientSRLabel();
    applyRecipientSelectAccessibility();
});

$(".recipient-select").on('select2:unselect', function (e) {
    updateRecipientSRLabel();
    applyRecipientSelectAccessibility();
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
    applyRecipientSelectAccessibility();
});

function validation() {

    var valid = true;
    if (!$("#msgSubject").val() || !$("#msgSubject").val().trim()) {

        displayValidation('#msgSubject', '#missingSubject', '#inlineSubjectError', true);

        $("#msgSubject").attr({
          "aria-describedby": "missingSubjectMessage",
          "aria-invalid": "true"
        });

        valid = false;
    }

    if (!$("#msgText").val() || !$("#msgText").val().trim()) {

        displayValidation('#msgText', '#missingText', '#inlineTextError', true);

        $("#msgText").attr({
          "aria-describedby": "missingTextMessage",
          "aria-invalid": "true"
        });

        valid = false;
    }

    // make sure at least one course is checked (other than the current course)
    var recipients = $('#recipientsSelect').val();
    if (!recipients || recipients.length < 1) {

        displayValidation('#recipientContainer', '#missingRecipients', '#inlineRecipError', true);
        setRecipientValidationState(false);

        valid=false;
    } else {
        setRecipientValidationState(true);
    }

    if (!valid) {
        $("#msgErrors").removeClass("rvt-display-none").focus();
    }

    return valid;
}

function displayValidation(inputId, errorMsgId, inlineErrorId, useLocalErrorClass) {
    $(errorMsgId).removeClass("rvt-display-none");
    $(inlineErrorId).removeClass("rvt-display-none");

    const inputErrorClass = useLocalErrorClass ? 'alert-danger-inline' : 'rvt-validation-danger';
    $(inputId).addClass(inputErrorClass);
}
