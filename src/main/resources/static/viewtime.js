/*
    the load event is fired after everything in the page is
    absolutely fully loaded.  See https://developer.mozilla.org/en-US/docs/Web/API/Window/load_event
*/
window.addEventListener("load", function(){

    preventInvalidInputOnDatalistInput();
    centerTheEditedRow();

});

/*
    On inputs that have a datalist associated with them, the browser
    will provide a suggestion of the datalist options, but will
    actually allow the user to type anything they want (including items
    not in the list).  This code will prevent entry of anything not
    in the datalist.
*/
function preventInvalidInputOnDatalistInput() {
     // Find all inputs on the DOM which are bound to a datalist via their list attribute.
    var inputs = document.querySelectorAll('input[list]');
    for (var i = 0; i < inputs.length; i++) {
      // When the value of the input changes
      inputs[i].addEventListener('change', function() {
        var optionFound = false,
          datalist = this.list;
        // Determine whether an option exists with the current value of the input.
        for (var j = 0; j < datalist.options.length; j++) {
            if (this.value == datalist.options[j].value) {
                optionFound = true;
                break;
            }
        }
        /*
        use the setCustomValidity function of the Validation API
        to provide an user feedback if the value does not exist in the datalist
        */
        if (optionFound) {
          this.setCustomValidity('');
        } else {
          this.setCustomValidity('Please select a valid value.');
        }
      });
    }
}

// scroll the row being edited into view
function centerTheEditedRow() {
    let timeBeingEdited = document.getElementsByClassName("being_edited")[0];
    if (typeof timeBeingEdited !== 'undefined') {
        timeBeingEdited.scrollIntoView({behavior: "smooth", block: "center", inline: "nearest"});
    }
}