'use strict';

var $ = require('jquery');
var messages = require('./util/messages');

$(document).ready(function () {
    $('#refresh-messages').click(function (event) {
        event.preventDefault();
        $.ajax(window.contextPath + '/admin/refresh-messages', {
            method: 'POST'
        }).done(function (reply) {
            messages.publishMessage(reply, 'refresh-messages-message');
        }).fail(function (jqXHR) {
            messages.publishMessage('Failed to refresh messages: ' + jqXHR.responseText, 'refresh-messages.png-message', {error: true});
        });
    });
});