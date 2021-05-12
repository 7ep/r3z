/*
    the load event is fired after everything in the page is
    absolutely fully loaded.  See https://developer.mozilla.org/en-US/docs/Web/API/Window/load_event
*/
window.addEventListener("load", function(){

    preventInvalidInputOnDatalistInput();
    totalHoursValidation();
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

/*
Prevents input of time that would cause there to be more
than 24 hours entered in a day.  pretty easy for adding
entries, but a bit trickier for editing entries - you have
to remove the old before adding the new to get an accurate
sum.
*/
function totalHoursValidation() {
    // Find all inputs on the DOM which are bound to a datalist via their list attribute.
    let timeinput = document.getElementById('create-time') || document.getElementById('edit-time');
    let dateInput = document.getElementById('create-date') || document.getElementById('edit-date');
    let twentyFourHrs = 24 * 60;

    let checkValidation = function() {
          if (typeof previoustime === 'undefined')
             previoustime = 0;

          let hoursGreaterThanPossible = ((timeinput.value * 60) - previoustime + timeentries[dateInput.value]) > twentyFourHrs;

          if (hoursGreaterThanPossible) {
              this.setCustomValidity('You are entering more hours than are possible in a day');
          } else {
              this.setCustomValidity('');
          }
      }

    // When the value of the input changes
    timeinput.addEventListener('change', checkValidation);
    timeinput.addEventListener('keyup', checkValidation);
}

// scroll the row being edited into view
function centerTheEditedRow() {
    let timeBeingEdited = document.getElementsByClassName("being_edited")[0];
    if (typeof timeBeingEdited !== 'undefined') {
        timeBeingEdited.scrollIntoView({behavior: "smooth", block: "center", inline: "nearest"});
    }
}