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
    // use the setCustomValidity function of the Validation API
    // to provide an user feedback if the value does not exist in the datalist
    if (optionFound) {
      this.setCustomValidity('');
    } else {
      this.setCustomValidity('Please select a valid value.');
    }
  });
}