/**
 * @file activity-monitor.js
 * @author Manuel Belmadani <manuel.belmadani@msl.ubc.ca>
 * @date 20/03/2019
 *
 * Monitors time since last activity, and logs out user if inactive or too long.
 *
 * The extra line between the end of the @file docblock
 * and the file-closure is important.
 */

(function () {

    // Constants
    var ONE_MINUTE = 60 * 1000;
    var serverTimeCookieName = 'serverTime';
    var expireTimeCookieName = 'sessionExpiry';
    var timeoutWindow = ONE_MINUTE; // User has one minute to react when warned for expiring session.
    var timeoutWarningMessage = 'Your session is going to be end in ' +
        (timeoutWindow / ONE_MINUTE) + '  minutes. Please click OK to continue, or Cancel to log out.';

    // Runtime
    var startTime = (new Date()).getTime();  // Get initial client timestamp.
    var serverTime = null;
    var offset = null;
    var activityDetected = false;

    function getCookie(name) {
        /*
          Get cookie by name; parse out value.
         */
        name = name + "=";
        var ca = document.cookie.split(';');
        for (var i = 0; i < ca.length; i++) {
            var c = ca[i].trim();
            if (c.indexOf(name) === 0) { // Cookie found
                return parseInt(c.substring(name.length, c.length));
            }
        }
        return null; // Cookie is not set.
    }

    function deleteCookies() {
        /*
          Force cookies to be expired.
         */
        var expiredCookie = "=; expires=Thu, 01 Jan 1970 00:00:00 UTC; path=/; ";
        document.cookie = serverTimeCookieName + expiredCookie;
        document.cookie = expireTimeCookieName + expiredCookie;
    }


    function getTimeToWarning() {
        /*
          Compute time left before issuing a warning. The session time is  based on the server cookies.
          offset is the difference between the client time and the server time and is computed once only.
         */

        var expireTime = getCookie(expireTimeCookieName);

        if (offset == null) {
            // Set only once.
            serverTime = getCookie(serverTimeCookieName);

            if (serverTime == null) {
                // Cookie is null. No timeout occurs.
                return null;
            }

            // Offset is the different between "server time" and "client time"
            offset = startTime - serverTime;

            /*
            console.log("Computing offset as: now - serverTime == offset " +
                "|offset="+offset +
                "|startTime="+startTime +
                "|expireTime="+expireTime);
            */

        }

        /*
        console.log("Expire Time:" + expireTime +
                "|offset:"+ offset +
                "|currTime:" + currTime +
                "|timeoutWindow" + timeoutWindow +
                "|serverTime" + serverTime +
                "| expireTime + offset - currTime - warningTime: " + timeToWarning);
        */

        var currTime = (new Date()).getTime();

        return parseInt(expireTime) + offset - currTime - timeoutWindow;
    }

    function updateTimeout() {
        /*
          Call backend to get a server time and expiry time cookie.
         */
        $.ajax({
            type: "GET",
            url: "/gettimeout",
            data: null,
            contentType: "application/json",
            success: function () {
                // Confirm session is renewed.
                // $('.success-row').show();
                // $('.error-row').hide();
            },
            error: function () {
                // Logout because response failed.
                $(document).off("mousemove", monitorMovements); // Unbind monitor on mouse movements.
                deleteCookies();
                window.location.href = "/logout";
            }
        });
    }

    function checkForTimeout() {
        /*
          Check for activity every 10 seconds; give a warning when timeoutWindow time is left.
         */

        var checkInterval = 10000; // Check activity monitor every 10s.
        setTimeout(function () {

            if (activityDetected) {
                // Check if there was any activity since the last command.
                updateTimeout();
                // console.log("Activity was detected so timeout was updated.");
                activityDetected = false;
            }

            var timeToWarning = getTimeToWarning();
            //console.log("Warning expected in: " + timeToWarning + "ms | " + timeToWarning/1000 + "s");
            if (timeToWarning < 0) {
                if (confirm(timeoutWarningMessage) && getTimeToWarning() > (-1 * timeoutWindow)) {
                    // Reset the timeout
                    updateTimeout();
                    checkForTimeout();
                } else {
                    // Logout requested
                    $(document).off("mousemove", monitorMovements); // Unbind monitor on mouse movements.
                    deleteCookies();
                    window.location.href = "/logout";
                }
            } else {
                checkForTimeout(); // Continue to monitor.
            }
        }, checkInterval); // Check if logout should occur in `checkInterval` time.
    }

    function monitorMovements() {
        activityDetected = true; // Update activity on mouse movement.
    }

    // console.log("Inactivity monitor loaded.");
    updateTimeout(); // Get the cookies        
    $(document).on("mousemove", monitorMovements); // Bind monitor on mouse movements.
    checkForTimeout(); // Set the timeout.
})();