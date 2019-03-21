
$(document).ready(function () {
    
    // Parameters
    var ONE_MINUTE = 60 * 1000;
    var SESSION_MINUTES = (ONE_MINUTE * 5);
    
    var serverTimeCookieName = 'serverTime';
    var expireTimeCookieName = 'sessionExpiry';
    var offset;

    var startTime = (new Date()).getTime();
    var timeoutWindow = (ONE_MINUTE * 1); // User has one minute to react.

    var timeoutWarningMessage = 'Your session is going to be end in ' + (timeoutWindow/ONE_MINUTE)  + '  min, Please click OK to continue, or Cancel to log out.';

    // Runtime
    var serverTime = null; //getCookie(currTimeCookieName);
    //serverTime = serverTime==null ? null : Math.abs(serverTime);
    var offset = null; //(new Date()).getTime() - serverTime;

    function getCookie(name)
    {
	var name = name + "=";
	var ca = document.cookie.split(';');
	for(var i=0; i<ca.length; i++)
	{
	    var c = ca[i].trim();
	    if (c.indexOf(name)==0) { // Cookie found
		return parseInt(c.substring(name.length, c.length));
	    }
	}
	return null; // Cookie is not set.
    }

    function getTimeToWarning() {
	expireTime = getCookie(expireTimeCookieName);
	
	if (offset == null) {	    
	    // Set for the first time only.
	    serverTime = getCookie(serverTimeCookieName);
	    if (serverTime == null){
		// Cookie is null. No timeout occurs.
		return null;
	    }

	    // Offset is the different between "server time" and "client time"
	    offset = startTime - serverTime ;
	    console.log("Computing offset as: now - serverTime == offset " +
			"|offset="+offset +
			"|startTime="+startTime +
			"|expireTime="+expireTime);

	}
	
	var currTime = (new Date()).getTime();
	var timeToWarning = parseInt(expireTime) + parseInt(offset) - parseInt(currTime) - parseInt(timeoutWindow);
	//var timeToWarning = parseInt(expireTime) + parseInt(offset) - parseInt(warningTime);

	
	console.log("Expire Time:" + expireTime +
		    "|offset:"+ offset +
		    "|currTime:" + currTime +
		    "|timeoutWindow" + timeoutWindow +
		    "|serverTime" + serverTime +
		    "| expireTime + offset - currTime - warningTime: " + timeToWarning);
	
	return timeToWarning;
    }

    function checkForTimeout() {
	//	var timeUntilWarning = getTimeToWarning();
	var checkInterval = 10000;

	setTimeout(function(){
	    timeToWarning = getTimeToWarning();
	    console.log("Warning expected in: " + timeToWarning + "ms | " + timeToWarning/1000 + "s");		    
	    if (timeToWarning < 0) {
		if(confirm(timeoutWarningMessage) && getTimeToWarning() > (-1 * timeoutWindow) ) {		    
		    $.ajax({
			type: "GET",
			url: "/gettimeout",
			data: null,
			contentType: "application/json",
			success: function(r) {
			    // Confirm session is renewed.
			    $('.success-row').show();
			    $('.error-row').hide();
			},
			error: function(r) {
			    // Logout because response failed.
			    window.location.href = "/logout";
			}
		    });

		    // Reset the timeout
		    checkForTimeout();
		} else {
		    // Logout requested
		    window.location.href = "/logout";
		}		
	    } else {
		checkForTimeout();
	    }
		    
	}, checkInterval); // Check if logout should occur in `checkInterval` time.
    }

    //Set the timeout.
    checkForTimeout();
});
