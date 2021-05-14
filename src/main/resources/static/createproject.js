/*
    the load event is fired after everything in the page is
    absolutely fully loaded.  See https://developer.mozilla.org/en-US/docs/Web/API/Window/load_event
*/
window.addEventListener("load", function(){

    validate("project_name", "tr > td:nth-child(1)");

});

function validate(inputSelector, existingValuesSelector) {
    let validatingInput = document.getElementById(inputSelector);
    let existingValues = [...document.querySelectorAll(existingValuesSelector)].map(x => x.innerText);

    let validationEvent = function() {
        let isDuplicate = existingValues.includes(validatingInput.value.trim());
        let isBlank = /^\s*$/.test(validatingInput.value.trim());

        /*
        use the setCustomValidity function of the Validation API
        to provide an user feedback if the value does not exist in the datalist
        */
        if (isDuplicate) {
          validatingInput.setCustomValidity('This is a duplicate entry');
        } else if (isBlank) {
           validatingInput.setCustomValidity('Enter one or more non-whitespace characters');
        } else {
          validatingInput.setCustomValidity('');
        }
    }

    validatingInput.addEventListener('keyup', validationEvent);
}
