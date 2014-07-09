// Make sure that the error state element is hidden.
$( ".ui-state-error" ).hide();

var createAccount = $( "#createAccountFrom" ).dialog( {

   
   // Set the dialog to be a modal meaning all other elements on the page will be disabled.
   modal : true,

   // Set autoOpen to False as we don't want the dialog opening on page load.
   autoOpen : false,

   // Define the dialogs buttons
   buttons : {

      // Button for adding a to-do item
      "Register" : function() {

         alert( "createAccount" );

         // create a JSON object to pass the data from the form to the template.
         /*
          * var newItem = [ { task : $( "#task" ).val(), description : $( "#description" ).val(), duedate : $(
          * "#duedate" ).val() } ],
          */

         // select and cache the jQuery Object for the currently visible accordion.
         // $accordion = $tabs.find(".ui-tabs-panel:visible .accordion");
         // Select the template, render it with the JSON data above and append it to
         // the visible accordion.
         // $("#ToDoItemTemplate")
         // .tmpl(newItem)
         // .appendTo($accordion);
         // Call the refreshAccordion to add the new item to the accordion
         // $accordion.refreshAccordion();
         // Close the dialog
         $( this ).dialog( "close" );

         // Clear the fields in the dialog
         $( "#username, #email, #confirmEmail", "#password", "#confirmPassword", "#captcha" ).val( "" );
      },

      // Button for cancelling adding a new to-do item
      "Cancel" : function() {

         // close the dialog
         $( this ).dialog( "close" );

         // Clear the field in the dialog
         $( "#username, #email, #confirmEmail", "#password", "#confirmPassword", "#captcha" ).val( "" );
      }
   }
} );

// Define a live click event that will open the dialog to add a new todo list.
// We use live instead of just binding the click event because we will be
// dynamically adding new Add To Do buttons with each new tab.
$( ".btnCreateAccount" ).button()

.bind( "click", function() {

   // call our variable that defines our Add To-Do Dialog and
   createAccount.dialog( 'open' );
   // Call button widget to format the Add a Project button.

   return false;
} );

// Call the button widget method on the login button to format it.
$( "#btnLogin" ).button()

// Now bind a click event to handle the login of the form.
.bind( "click", function() {

   // Remember me http://www.baeldung.com/spring-security-remember-me
   // _spring_security_remember_me: rememberMe

   var userName = $( "#username" ).val();
   var password = $( "#password" ).val();
   // var rememberMe = form.rememberMe.value;
   $.ajax( {

      cache : false,
      type : 'POST',
      url : "j_spring_security_check",
      // crossDomain: true,
      async : false,
      data : {
         j_username : userName,
         j_password : password,
         ajaxLoginTrue : true
      },
      beforeSend : function(xhr) {
         // xhr.setRequestHeader("x-ajax-call", "true");
         xhr.setRequestHeader( 'Content-Type', 'application/x-www-form-urlencoded' );
      },
      success : function(response, xhr) {
         if ( response.indexOf( "success:true" ) > -1 ) {
            // window.location.href = "success.jsp";
            document.location = "register.html";
            // location.href = "http://www.example.com/ThankYou.html";
            // window.location.replace("http://stackoverflowssss.com");
         } else {
            // If the login credentials are not correct,
            // show our error state element.
            $( ".ui-state-error" ).show();
            // Add an jQuery UI effect that shakes the whole login form
            // like as if it's shaking its head no.
            $( "#login section" ).effect( "shake", 150 );
         }

      },
      error : function(xhr) {
         alert( "Error with request. Status is: " + xhr.status );
         console.log( "Error with request. Status is: " + xhr.status );
         console.log( xhr.responseText );
         // If the login credentials are not correct,
         // show our error state element.
         $( ".ui-state-error" ).show();
         // Add an jQuery UI effect that shakes the whole login form
         // like as if it's shaking its head no.
         $( "#login section" ).effect( "shake", 150 );
      },
      complete : function(result) {
      }
   } );

   // return false to cancel normal form submit event methods.
   return false;
} );