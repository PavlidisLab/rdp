   /**
    * @memberOf support
    */
(function( support, $, undefined ) {
   
//   support.submit = function() {
//      var formData = new FormData($('#contact-form')[0]);
//      $.ajax({
//          url: 'contactSupport.html',  //Server script to process data
//          type: 'POST',
//          xhr: function() {  // Custom XMLHttpRequest
//              var myXhr = $.ajaxSettings.xhr();
//              if(myXhr.upload){ // Check if upload property exists
//                  myXhr.upload.addEventListener('progress',progressHandlingFunction, false); // For handling the progress of the upload
//              }
//              return myXhr;
//          },
//          //Ajax events
//          beforeSend: beforeSendHandler,
//          success: completeHandler,
//          error: errorHandler,
//          // Form data
//          data: formData,
//          //Options to tell jQuery not to process data or worry about content-type.
//          cache: false,
//          contentType: false,
//          processData: false
//      });
//   }
//   
//   function progressHandlingFunction(e){
//      if(e.lengthComputable){
//          $('progress').attr({value:e.loaded,max:e.total});
//      }
//  }
    
   
   
}( window.support = window.support || {}, jQuery ));

//Initialize document
$( document ).ready( function() {
   $('a[href="#support-attach"]').click(function(){
      $('#contact-form > div.form-group.file-group > div').show();
    });
   //$('#contact-form > div.form-group .fileinput').fileinput();
   
   (function() {
      
      var status = $('#contact-form .alert div');
      var progressbar = $('#upload-progress .progress-bar');
         
      $('#contact-form').ajaxForm({
          iframe: true,
          beforeSerialize: function($form, options) {
             //console.log("form", $form)
             //console.log("options", options)
             //$form.find('input[type="hidden"]').remove();
             //$form.find('input[type="file"]').prop('name', 'attachFile');
          },
          beforeSend: function() {
              utility.hideMessage(status);
              var percentVal = '0%';
              progressbar.width(percentVal);
              progressbar.html(percentVal);
              progressbar.attr('aria-valuenow', percentVal)
          },
          uploadProgress: function(event, position, total, percentComplete) {
              var percentVal = percentComplete + '%';
              progressbar.width(percentVal);
              progressbar.html(percentVal);
              progressbar.attr('aria-valuenow', percentVal)
          },
          success: function(responseText, statusText, xhr, $form) {
              var percentVal = '100%';
              progressbar.width(percentVal);
              progressbar.html(percentVal);
              progressbar.attr('aria-valuenow', percentVal)
          },
         complete: function(xhr, textStatus) {
            console.log("xhr", xhr);
            if ( xhr.responseText.match(/exception/i) || xhr.responseText.match(/http/i) || !xhr.responseText.match(/sent/i) ) {
               // We need to use this as the error catcher because the method this 
               // form plugin uses to make ajax file upload work is iframes.  Because of this
               // any native ajax error catching cannot be used.
               
               utility.showMessage("<p>Oops, there was an error while sending your email. Please contact support using the e-mail below.</p>", status, "alert-danger");
               
            } else {
               utility.showMessage(xhr.responseText, status, "alert-success");
            }
            //status.html(xhr.responseText);
         },
         beforeSubmit: function(arr, $form, options) { 
            var fileDataIndex = -1;
            //console.log(arr)
            $.each(arr, function(index, value) {
                 if (value.name == "attachFile"){
                     if (value.value.length != 0){
                        progressbar.parent().show()
                     } else {
                        fileDataIndex = index;
                        progressbar.parent().hide()
                     }
                 }
               }); 
            
            if (fileDataIndex != -1 ) {
               arr[fileDataIndex].type="file"; 
            }
        },
        //contentType: "multipart/form-data"
      }); 

      })(); 
   
   $('#contact-form > div.form-group input[type=file]').on('change', function(e){
      $(this);
      if ( $(this).val() != "" ) {
         console.log($(this).closest('span'));
         $(this).siblings('a').show();
      } else {
         $(this).siblings('a').hide();
      }
   });
   
   $('#file-upload a.close').on('click', function(e){
      $("#attachFile").val("");
      $("#attachFile").replaceWith( $("#attachFile").clone( true ) );
      $(this).hide();
   });
   
   
} );