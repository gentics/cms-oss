/*global define: true, window: true */
/*!
* Aloha Editor
* Author & Copyright (c) 2011 Gentics Software GmbH
* aloha-sales@gentics.com
* Licensed under the terms of http://www.aloha-editor.com/license.html
*/

/**
 * @typedef {'valid' | 'invalid' | 'unchecked' } ExternalLinkStatus
 */

/**
 * @typedef {object} ExternalLinkCheckHistoryEntry
 * @property {number} timestamp
 * @property {ExternalLinkStatus} status
 * @property {string} reason
 */

/**
 * @typedef {object} ExternalLink
 * @property {number} id
 * @property {number} contentId
 * @property {number} contenttagId
 * @property {string} contenttagName
 * @property {number} valueId
 * @property {string} partName
 * @property {string} url
 * @property {string} text
 * @property {number} lastCheckTimestamp
 * @property {ExternalLinkStatus} lastStatus
 * @property {string} lastReason
 * @property {array.<ExternalLinkCheckHistoryEntry>} history
 */

/**
 * @typedef {'OK' | 'NOTFOUND' | 'INVALIDDATA' | 'FAILURE' | 'PERMISSION' | 'AUTHREQUIRED' | 'MAINTENANCEMODE' | 'NOTLICENSED' | 'LOCKED'} ResponseCode
 */

/**
 * @typedef {object} ResponseMessage
 * @property {number=} id
 * @property {number} timestamp
 * @property {string=} message
 * @property {'CRITICAL' | 'INFO' | 'SUCCESS'} type
 * @property {string=} fieldName
 * @property {string=} image
 */

/**
 * @typedef {object} ResponseInfo
 * @property {ResponseCode} responseCode
 * @property {string=} responseMessage
 */

/**
 * @typedef {object} CMSResponse
 * @property {ResponseInfo} responseInfo
 * @property {Array.<ResponseMessage>=} messages
 */

/**
 * @typedef {object} LinkCheckerCheckResponseProperties
 * @property {boolean} valid
 * @property {string} reason
 */

/**
 * @typedef {CMSResponse & LinkCheckerCheckResponseProperties} LinkCheckerCheckResponse
 */

define([
    'jquery',
    'aloha/core',
    'aloha/plugin',
    'aloha/ephemera',
    'link/link-plugin',
    'gcn/gcn-plugin',
    'css!gcnlinkchecker/css/gcnlinkchecker.css'
], function (
    $,
    Aloha,
    Plugin,
    Ephemera,
    LinkPlugin,
    GCNPlugin
) {
    "use strict";
    
    var CLASS_LINK_CHECKER_ITEM = 'aloha-gcnlinkchecker-item';
    var CLASS_CHECKED = 'aloha-gcnlinkchecker-checked';
    var CLASS_UNCHECKED = 'aloha-gcnlinkchecker-unchecked';
    var CLASS_VALID_URL = 'aloha-gcnlinkchecker-valid-url'
    var CLASS_INVALID_URL = 'aloha-gcnlinkchecker-invalid-url';
    var CLASS_LINK = 'aloha-link-text';
    var ATTR_TAG_ID = 'data-gcn-tagid';
    var ATTR_QUEUED_LINK_CHECK = 'gcmsui-queued-link-check';
    var DEFAULT_SETTINGS = {
        defaultProtocol: 'http',
        livecheck: true,
        delay: 500
    };
    var plugin;

    /**
     * Create the plugin
     */
    plugin = Plugin.create('gcnlinkchecker', {

        /**
         * Configure the available languages (i18n) for this plugin
         */
        languages: ['en', 'de'],

        /**
         * @type {array.<HTMLElement>} array of broken link elements.
         */
        brokenLinks: [],

        /**
         * @type {array.<HTMLElement>} array of valid link elements.
         */
        validLinks: [],

        /**
         * @type {array.<HTMLElement>} array of unchecked link elements.
         */
        uncheckedLinks: [],

        /**
         * @type {array.<ExternalLink>}
         */
        _initialCheckLinks: [],

        /**
         * Initialize the plugin
         */
        init: function () {
            Ephemera.classes(CLASS_LINK_CHECKER_ITEM, CLASS_CHECKED, CLASS_UNCHECKED, CLASS_VALID_URL, CLASS_INVALID_URL);
            Ephemera.attributes(ATTR_QUEUED_LINK_CHECK);

            // in old UI - where GCMSUI is not defined, just run the plugin
            // in the new UI - do nothing in preview mode
            if (typeof GCMSUI === 'undefined' || (typeof GCMSUI.appState !== 'undefined' && GCMSUI.appState.editMode !== 'preview')) {
                // load config
                plugin.settings = $.extend(true, {}, DEFAULT_SETTINGS, plugin.settings);
            }
        },

        /**
         * Clears all references in the DOM (warning icons and stuff) and then resets the `brokenLinks` map.
         */
        clearBrokenLinks: function () {
            // Remove all DOM markings first
            Object.keys(plugin.brokenLinks).forEach(function (elem) {
                plugin._removeLinkClickHandler($(elem));
            });

            plugin.brokenLinks = [];
        },

        /**
         * Updates the `brokenLinks` object/map and approiately marks all the links in the DOM.
         * @param {array.<ExternalLink>} linkObjects The link objects from the API which need to be updated.
         */
        initializeBrokenLinks: function (linkObjects) {
            plugin._initialCheckLinks = (linkObjects || []);
            plugin.brokenLinks = [];
            plugin.validLinks = [];
            plugin.uncheckedLinks = [];

            plugin._initialCheckLinks.forEach(function (linkObj) {
                var id = linkObj.contenttagId;
                var elem = document.querySelector('[' + ATTR_TAG_ID + '="' + id + '"]');
                if (elem == null) {
                    return;
                }

                switch (linkObj.lastStatus) {
                    case 'unchecked':
                        plugin.addUncheckedLink(elem);
                        break;

                    case 'valid':
                        plugin.addValidLink(elem);
                        break;

                    case 'invalid':
                        plugin.addBrokenLink(elem);
                        break;
                }
            });

            return plugin.brokenLinks;
        },

        _findAllLinkElements: function() {
            /** @type {Array.<HTMLElement>} */
            var arr = plugin._initialCheckLinks.map(function(linkObj) {
                return document.querySelector('[' + ATTR_TAG_ID + '="' + linkObj.contenttagId + '"]')
            }).filter(function(elem) {
                return elem != null;
            });

            arr.concat(Array.from(document.querySelectorAll('.' + CLASS_LINK)));
        },

        refreshLinksFromDom: function() {
            plugin.brokenLinks = Array.from(document.querySelectorAll('.' + CLASS_LINK_CHECKER_ITEM + '.' + CLASS_INVALID_URL));
            plugin.validLinks = Array.from(document.querySelectorAll('.' + CLASS_LINK_CHECKER_ITEM + '.' + CLASS_VALID_URL));
            plugin.uncheckedLinks = Array.from(document.querySelectorAll('.' + CLASS_LINK_CHECKER_ITEM + '.' + CLASS_UNCHECKED));
        },

        /**
         * 
         * @param {HTMLElement} element The element to add
         * @param {LinkCheckerCheckResponse} res The response/validity of the element
         */
        addLink: function(element, res) {
            if (!res) {
                plugin.addUncheckedLink(element);
                return;
            } else if (res.valid) {
                plugin.addValidLink(element);
            } else {
                plugin.addBrokenLink(element);
            }
        },

        /**
         * Adds the element to the unchecked Links and sets the appropiate classes
         * @param {HTMLElement} element The element to add
         */
        addUncheckedLink: function(element) {
            if (element == null) {
                return false;
            }

            element.classList.remove(CLASS_CHECKED, CLASS_VALID_URL, CLASS_INVALID_URL);
            element.classList.add(CLASS_LINK_CHECKER_ITEM, CLASS_UNCHECKED);
            if (plugin.uncheckedLinks.includes(element)) {
                return false;
            }

            plugin.uncheckedLinks.push(element);
            return true;
        },

        addValidLink: function(element) {
            if (element == null) {
                return;
            }

            element.classList.remove(CLASS_UNCHECKED, CLASS_INVALID_URL);
            element.classList.add(CLASS_LINK_CHECKER_ITEM, CLASS_CHECKED, CLASS_VALID_URL);
            if (plugin.validLinks.includes(element)) {
                return false;
            }

            plugin.validLinks.push(element);
            return true;
        },

        addBrokenLink: function(element) {
            if (element == null) {
                return false;
            }

            element.classList.remove(CLASS_UNCHECKED, CLASS_VALID_URL);
            element.classList.add(CLASS_LINK_CHECKER_ITEM, CLASS_CHECKED, CLASS_INVALID_URL);
            if (plugin.brokenLinks.includes(element)) {
                return false;
            }

            plugin.brokenLinks.push(element);
            plugin._addLinkClickHandler($(element));
            return true;
        },

        /**
         * @param {HTMLElement} element
         * @param {LinkCheckerCheckResponse} res
         */
        updateLinkStatus: function(element, res) {
            if (element == null || res == null) {
                return false;
            }

            var idx;

            idx = plugin.brokenLinks.indexOf(element);
            if (idx !== -1) {
                // Nothing to do
                if (!res.valid) {
                    element.classList.remove(CLASS_UNCHECKED, CLASS_VALID_URL);
                    element.classList.add(CLASS_LINK_CHECKER_ITEM, CLASS_CHECKED, CLASS_INVALID_URL);
                    return false;
                }

                plugin.brokenLinks.splice(idx, 1);
                plugin._removeLinkClickHandler($(element));
                element.classList.remove(CLASS_UNCHECKED, CLASS_INVALID_URL);
                element.classList.add(CLASS_LINK_CHECKER_ITEM, CLASS_CHECKED, CLASS_VALID_URL);
                plugin.validLinks.push(element);

                return true;
            }

            idx = plugin.validLinks.indexOf(element);
            if (idx !== -1) {
                // nothing to do
                if (res.valid) {
                    element.classList.remove(CLASS_UNCHECKED, CLASS_INVALID_URL);
                    element.classList.add(CLASS_LINK_CHECKER_ITEM, CLASS_CHECKED, CLASS_VALID_URL);
                    return false;
                }
                plugin.validLinks.splice(idx, 1);
                element.classList.remove(CLASS_UNCHECKED, CLASS_VALID_URL);
                element.classList.add(CLASS_LINK_CHECKER_ITEM, CLASS_CHECKED, CLASS_INVALID_URL);
                plugin.brokenLinks.push(element);
                plugin._addLinkClickHandler($(element));

                return true;
            }

            idx = plugin.uncheckedLinks.indexOf(element);
            if (idx !== -1) {
                plugin.uncheckedLinks.splice(idx, 1);
                element.classList.remove(CLASS_UNCHECKED);
            }

            if (res.valid) {
                return plugin.addValidLink(element);
            } else {
                return plugin.addBrokenLink(element);
            }
        },

        /**
         * 
         * @param {HTMLElement} element The element to remove
         * @returns {boolean} If it was successfully from any of the link lists
         */
        removeLink: function(element) {
            if (element == null) {
                return false;
            }

            var idx;

            idx = plugin.brokenLinks.indexOf(element);
            if (idx !== -1) {
                plugin.brokenLinks.splice(idx, 1);
                plugin._removeLinkClickHandler($(element));
                element.classList.remove(CLASS_LINK_CHECKER_ITEM, CLASS_UNCHECKED, CLASS_CHECKED, CLASS_VALID_URL, CLASS_INVALID_URL);
                return true;
            }

            idx = plugin.validLinks.indexOf(element);
            if (idx !== -1) {
                plugin.validLinks.splice(idx, 1);
                element.classList.remove(CLASS_LINK_CHECKER_ITEM, CLASS_UNCHECKED, CLASS_CHECKED, CLASS_VALID_URL, CLASS_INVALID_URL);
                return true;
            }

            idx = plugin.uncheckedLinks.indexOf(element);
            if (idx !== -1) {
                plugin.uncheckedLinks.splice(idx, 1);
                element.classList.remove(CLASS_LINK_CHECKER_ITEM, CLASS_UNCHECKED, CLASS_CHECKED, CLASS_VALID_URL, CLASS_INVALID_URL);
                return true;
            }

            return false;
        },

        /**
         * Helper method to select the link-element (technically works on any element), and activates the
         * block that it is currently in.
         * @param {HTMLElement} element The element to select
         */
        selectLinkElement: function(element) {
            Aloha.getSelection().removeAllRanges();
            var range = Aloha.createRange();
            range.setStart(element, 0);
            range.setEndAfter(element);
            Aloha.getSelection().addRange(range);

            var editable = Aloha.getEditableHost($(element));
            if (editable != null) {
                editable.activate();
            }
        },
        editLink: function(element) {
            plugin.selectLinkElement(element);
            return LinkPlugin.showLinkModal(element);
        },
        deleteLink: function(element) {
            plugin.removeLink(element);
            return plugin.deleteTag(element).then(function() {
                plugin.selectLinkElement(element);
                LinkPlugin.removeLink(true, element);
            });
        },

        _addLinkClickHandler: function ($elem) {
            if ($elem == null || !$elem.length) {
                return;
            }

            // Just to be sure, to not add the same click handler twice
            $elem.off('click.gcn-link-checker');

            $elem.on('dblclick.gcn-link-checker', function($event) {
                $event.preventDefault();
                LinkPlugin.showLinkModal($elem[0]);
                return true;
            });
        },
        _removeLinkClickHandler: function ($elem) {
            $elem.off('click.gcn-link-checker');
        },

        deleteTag: function (element) {
            return new Promise(function (resolve, reject) {
                var $elem = $(element);
                var pageId = GCNPlugin.page.id();
                var tagName = $elem.attr("data-keyword");

                if (!tagName) {
                    resolve(false);
                    return;
                }

                GCN.page(pageId, function (page) {
                    page.tag(tagName, function (tag) {
                        // remove tag from taglist (this will only be persisted on page save)
                        tag.remove();
                        resolve(true);
                    }, function () {
                        reject(new Error('Could not find tag in page "' + pageId + '" with the name "' + tagName + '"'));
                    });
                }, function () {
                    reject(new Error('Could not find page with ID "' + pageId + '"'));
                });
            });
        },
    });

    return plugin;
});
