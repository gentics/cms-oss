"use strict";

(() => {
    // Only load, when Aloha is actually defined/loaded.
    if (
        Aloha == null
        || typeof Aloha !== "object"
        || typeof Aloha.settings !== "object"
        || typeof Aloha.settings.plugins !== "object"
    ) {
        return;
    }

    // default formatting options
    // see https://www.alohaeditor.org/guides/plugin_format.html

    if (typeof Aloha.settings.plugins.format !== "object") {
        Aloha.settings.plugins.format = {};
    }
    Aloha.settings.plugins.format.config = ['b', 'i', 'u', 's', 'sub', 'sup', 'code', 'q', 'abbr', 'cite', 'removeFormat'];

})();
