'use strict';

var URLSearchParams = window.URLSearchParams;

/**
 * Serialize the given form's input.
 *
 * This is the jQuery equivalent of $(form).serialize().
 *
 * @param {HTMLFormElement} form the form to be serialized
 * @returns {string} an URL-encoded serialized representation of the form
 */
var serialize = function (form) {
    return new URLSearchParams(new FormData(form)).toString();
};

module.exports = {
    serialize: serialize
};