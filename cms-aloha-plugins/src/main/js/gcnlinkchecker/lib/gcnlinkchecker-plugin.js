/*global define: true, window: true */
/*!
* Aloha Editor
* Author & Copyright (c) 2011 Gentics Software GmbH
* aloha-sales@gentics.com
* Licensed under the terms of http://www.aloha-editor.com/license.html
*/

define([
    'jquery',
    'aloha/core',
    'aloha/plugin',
    'link/link-plugin',
    'gcn/gcn-plugin',
    'css!gcnlinkchecker/css/gcnlinkchecker.css'
], function (
    $,
    Aloha,
    Plugin,
    LinkPlugin,
    GCNPlugin
) {
    "use strict";
    
    var CLASS_INVALID_URL = 'aloha-gcnlinkchecker-invalid-url';
    var CLASS_LINK = 'aloha-link-text';
    var ATTR_TAG_ID = 'data-gcn-tagid';
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
         * @type {array.<Dom>} array of broken link elements.
         */
        brokenLinks: [],

        /**
         * Initialize the plugin
         */
        init: function () {
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
                plugin._unmarkLinkElement($(elem));
            });

            plugin.brokenLinks = [];
        },

        /**
         * Updates the `brokenLinks` object/map and approiately marks all the links in the DOM.
         * @param {array.<*>} linkObjects The link objects from the API which need to be updated.
         */
        initializeBrokenLinks: function (linkObjects) {
            this.brokenLinks = (linkObjects || []).map(function (linkObj) {
                // Skip links which are valid
                if (linkObj.lastStatus !== 'invalid') {
                    return null;
                }

                var id = linkObj.contenttagId;
                var elem = document.querySelector('[' + ATTR_TAG_ID + '="' + id + '"]');
                if (elem == null) {
                    return null;
                }

                plugin._markLinkElement($(elem));
                return elem;
            }).filter(function(elem) {
                return elem != null;
            });

            return this.brokenLinks;
        },
        refreshLinksFromDom: function() {
            var _this = this;

            this.brokenLinks = Array.from(document.querySelectorAll('.' + CLASS_INVALID_URL))
                .filter(function(element) {
                    if (element.classList.contains(CLASS_LINK)) {
                        _this._markLinkElement($(element));
                        return true;
                    }

                    // If the element is no longer a link element, then remove all added data from our plugin (if it isn't gone yet).
                    _this._unmarkLinkElement(element);
                    return false;
                });
        },

        addBrokenLink: function(element) {
            if (element == null) {
                return false;
            }

            plugin._markLinkElement($(element));
            if (!plugin.brokenLinks.includes(element)) {
                plugin.brokenLinks.push(element);
                return true;
            }
            return false;
        },

        removeBrokenLink: function(element) {
            if (element == null) {
                return false;
            }

            plugin._unmarkLinkElement($(element));
            var idx = plugin.brokenLinks.indexOf(element);
            if (idx > -1) {
                plugin.brokenLinks.splice(idx, 1);
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
            this.selectLinkElement(element);
            return LinkPlugin.showLinkModal(element);
        },
        removeLink: function(element) {
            var _this = this;
            this.removeBrokenLink(element);
            return this.deleteTag(element).then(function() {
                _this.selectLinkElement(element);
                LinkPlugin.removeLink(true, element);
            });
        },

        _markLinkElement: function ($elem) {
            if ($elem.hasClass(CLASS_INVALID_URL)) {
                return;
            }
            $elem.addClass(CLASS_INVALID_URL);
            $elem.on('dblclick.gcn-link-checker', function($event) {
                $event.preventDefault();
                LinkPlugin.showLinkModal($elem[0]);
                return true;
            });
        },
        _unmarkLinkElement: function ($elem) {
            $elem.removeClass(CLASS_INVALID_URL);
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
