var console = window.console;

/**
 * Publish a message visible for the user.
 *
 * To produce an error message, set 'error' to true in the configuration.
 *
 * @param content the content of the message
 * @param id the ID of the message
 * @param {{[containerId]: String, [error]: Boolean}} [configuration] optional extra configuration for the published message
 */
function publishMessage(content, id, configuration) {
    "use strict";
    var defaultConfiguration = {containerId: 'messages', error: false};
    if (configuration === undefined) {
        configuration = defaultConfiguration;
    } else {
        configuration = Object.assign(defaultConfiguration, configuration);
    }
    var messagesContainer = document.getElementById(configuration.containerId);
    var existingMessage = document.getElementById(id);
    if (existingMessage) {
        existingMessage.textContent = content;
    } else if (messagesContainer) {
        var newMsg = document.createElement('div');
        newMsg.id = id;
        newMsg.classList.add('alert');
        if (configuration.error === true) {
            newMsg.classList.add('alert-danger');
            newMsg.setAttribute('role', 'alert');
        } else {
            newMsg.classList.add('alert-success');
        }
        newMsg.textContent = content;
        messagesContainer.append(newMsg);
    } else {
        console.warn("No with container with ID '" + configuration.containerId + "', will have to resort to using window.alert().");
        window.alert(content);
    }
}

module.exports = {publishMessage: publishMessage};