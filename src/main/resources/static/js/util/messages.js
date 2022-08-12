/**
 * Publish a message visible for the user.
 *
 * To produce an error message, set 'error' to true in the configuration.
 *
 * @param content the content of the message
 * @param id the ID of the message
 * @param {{error: Boolean}} configuration
 */
function publishMessage(content, id, configuration) {
    "use strict";
    if (configuration === undefined) {
        configuration = {error: false};
    }
    var messagesContainer = $('#messages');
    var existingMessage = document.getElementById('#' + id);
    if (existingMessage) {
        existingMessage.textContent = content;
    } else {
        var newMsg = $('<div class="alert">')
            .attr('id', '#' + id)
            .addClass('alert-' + (configuration.error === true ? 'error' : 'success'))
            .text(content);
        messagesContainer.append(newMsg);
    }
}

module.exports = {publishMessage: publishMessage};