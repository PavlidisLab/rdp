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
    "use strict";

    var $ = require('jquery');
    var activityDetector = require('activity-detector');

    // Constants
    var serverTimeCookieName = 'serverTime';
    var expireTimeCookieName = 'sessionExpiry';
    var timeoutWindow = 60 * 1000; // User has one minute to react when warned for expiring session.
    var timeoutWarningMessage = 'Your session is going to be end in less than one minute. Please click OK to continue, or Cancel to log out.';

    /**
     * Time difference between client and server times.
     *
     * Adding this to a "server" time converts it to a "client" time.
     *
     * Normally, this is very small (<10ms) and essentially due to the round-trip latency, but if the client or server
     * has misconfigured clock, the offset will correct for that.
     *
     * @type {Number}
     */
    var offset = 0;

    var ad = activityDetector.default();

    /**
     * Get one of the timeout cookies, as an integer.
     * @return {Number}
     */
    function getTimeoutCookie(name) {
        name = name + "=";
        var ca = document.cookie.split(';');
        for (var i = 0; i < ca.length; i++) {
            var c = ca[i].trim();
            if (c.indexOf(name) === 0) { // Cookie found
                return parseInt(c.substring(name.length, c.length));
            }
        }
        throw new Error("No cookie with name " + name + " is found.");
    }

    /**
     * Call backend to get a server time and expiry time cookie.
     */
    function refreshTimeoutCookies() {
        var clientTime = (new Date()).getTime();
        return $.ajax({
            type: "GET",
            url: window.contextPath + "/gettimeout",
            data: null,
            contentType: "application/json"
        }).done(function () {
            // update time offset
            var serverTime = getTimeoutCookie(serverTimeCookieName);
            offset = clientTime - serverTime;
            if (Math.abs(offset) > 1000) {
                window.console.warn("The difference between client and server times is high: " + offset + " ms. Your clock (or the server's?) might not be properly synchronized.");
            }
        }).fail(function () {
            window.console.error("Failed to update the session, logging out...");
            logout();
        });
    }

    function deleteTimeoutCookies() {
        // Force cookies to be expired.
        var expiredCookie = "=; expires=Thu, 01 Jan 1970 00:00:00 UTC; path=/; ";
        document.cookie = serverTimeCookieName + expiredCookie;
        document.cookie = expireTimeCookieName + expiredCookie;
    }

    /**
     * Perform a logout.
     */
    function logout() {
        ad.stop();
        deleteTimeoutCookies();
        window.location.href = window.contextPath + "/logout";
    }

    /**
     * Compute time left before logging out the user.
     *
     * The session time is based on the server cookies. The offset is the difference between the client time and the
     * server time and is computed once only.
     *
     * @return {Number}
     */
    function getTimeToLogout() {
        var expireTime = getTimeoutCookie(expireTimeCookieName);
        var currTime = (new Date()).getTime();
        return expireTime + offset - currTime;
    }

    /**
     * Compute time left before asking the user to refresh its session.
     *
     * @return {Number}
     */
    function getTimeToWarning() {
        return getTimeToLogout() - timeoutWindow;
    }

    ad.on('active', function () {
        refreshTimeoutCookies();
    });

    ad.on('idle', function () {
        var timeToWarning = getTimeToWarning();
        if (timeToWarning < 0) {
            // check if there is still time before and after asking the user
            // if the user takes too much time, we will have to log him/her out unfortunately
            if (getTimeToLogout() > 0 && window.confirm(timeoutWarningMessage) && getTimeToLogout() > 0) {
                // Reset the timeout
                refreshTimeoutCookies();
            } else {
                logout();
            }
        }
    });

    // Get the cookies and then start the activity monitor (which requires the cookie set!)
    refreshTimeoutCookies().then(function () {
        ad.init();
    });
})();