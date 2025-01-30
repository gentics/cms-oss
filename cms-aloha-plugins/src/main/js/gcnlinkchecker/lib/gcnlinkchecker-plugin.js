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
 * @property {string} url URL which was checked - Out of spec, passed down by the UI
 */

/**
 * @typedef {CMSResponse & LinkCheckerCheckResponseProperties} LinkCheckerCheckResponse
 */

define([
    'jquery',
    'aloha/core',
    'PubSub',
    'aloha/plugin',
    'aloha/ephemera',
    'block/blockmanager',
    'link/link-plugin',
    'gcn/gcn-plugin',
    'gcn/gcn-util',
    'css!gcnlinkchecker/css/gcnlinkchecker.css'
], function (
    /** @type {jQuery} */
    $,
    Aloha,
    PubSub,
    Plugin,
    Ephemera,
    BlockManager,
    LinkPlugin,
    GCNPlugin,
    GCNUtil
) {
    "use strict";

    const CLASS_LINK_CHECKER_ITEM = 'aloha-gcnlinkchecker-item';
    const CLASS_CHECKED = 'aloha-gcnlinkchecker-checked';
    const CLASS_UNCHECKED = 'aloha-gcnlinkchecker-unchecked';
    const CLASS_VALID_URL = 'aloha-gcnlinkchecker-valid-url'
    const CLASS_INVALID_URL = 'aloha-gcnlinkchecker-invalid-url';
    const CLASS_BLOCK_INDICATOR = 'aloha-gcnlinkchecker-block-indicator';
    const CLASS_BLOCK_HANDLE = 'aloha-block-handle';
    const CLASS_BLOCK_HANDLE_ICON = 'aloha-block-button-icon';
    const CLASS_LINK = 'aloha-link-text';

    const ATTR_TAG_ID = 'data-gcn-tagid';
    const ATTR_QUEUED_LINK_CHECK = 'gcmsui-queued-link-check';
    const ATTR_LINK_TARGET = 'data-gcnlinkchecker-href';

    const STATUS_UNCHECKED = 'unchecked';
    const STATUS_VALID = 'valid';
    const STATUS_INVALID = 'invalid';

    const TAGTYPE_ALL = '*';

    /**
     * 
     * @param {array.<T>} arr The array which should be checked for
     * @param {T} elem The element to remove
     * @param {function} fn The optional function to use in find-index
     */
    function removeFromArray(arr, elem, fn) {
        if (!Array.isArray(arr)) {
            return;
        }

        let idx;
        if (typeof fn === 'function') {
            idx = arr.findIndex(fn);
        } else {
            idx = arr.findIndex(arrElem => arrElem === elem);
        }

        if (idx > -1) {
            arr.splice(idx, 1);
        }
    }

    /**
     * Create the plugin
     */
    let linkCheckerPlugin = {

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
         * The default settings for this plugin
         */
        defaults: {
            /** If it should check newly added/edited links in the editmode */
            livecheck: true,
            /** How long it should wait after editing a link before checking */
            delay: 500,
            /** Settings which can be defined per editable. */
            config: {
                /**
                 * A list of construct-names, which should be checked/have a indicator.
                 * Similar to how it works in the gcn-plugin, except when empty, it'll never display anything.
                 * If the array contains `"*"` however, it'll count it as a wildcard and allow all constructs.
                 */
                tagtypeWhitelist: [],
            },
        },

        /**
         * Initialize the plugin
         */
        init: function () {
            Ephemera.classes(CLASS_LINK_CHECKER_ITEM, CLASS_CHECKED, CLASS_UNCHECKED, CLASS_VALID_URL, CLASS_INVALID_URL);
            Ephemera.attributes(ATTR_QUEUED_LINK_CHECK, ATTR_LINK_TARGET);

            PubSub.sub('gcn.tag-editor.resolve', function(result) {
                if (!result?.tag?.id) {
                    return;
                }

                const elem = document.querySelector(`[${ATTR_TAG_ID}="${result.tag.id}"]`);

                if (!elem) {
                    return;
                }

                // Remove the initial-check data of the tag if has been edited, otherwise the indicator will not update
                // correctly.
                const idx = linkCheckerPlugin._initialCheckLinks.findIndex(/** @param {ExternalLink} linkObj*/function(linkObj) {
                    return linkObj.contenttagId === result.tag.id;
                });
                if (idx > -1) {
                    linkCheckerPlugin._initialCheckLinks.splice(idx, 1);
                }

                // When a tag is edited, and the handles are already present, then they are
                // stale/not updated until the event below is triggered.
                // Therefore it'ld always have the wrong icon/status displayed.
                // This will mark the link as unchecked and update all other stuff accordingly
                linkCheckerPlugin.updateLinkStatus(elem);
            });

            PubSub.sub('gcn.block.handles-available', function (data) {
                let idx = 0;
                let found = false;

                for (; idx < linkCheckerPlugin._initialCheckLinks.length; idx++) {
                    const linkObj = linkCheckerPlugin._initialCheckLinks[idx];
                    const elem = data?.$el?.[0];
                    if (elem == null) {
                        continue;
                    }

                    const id = linkObj.contenttagId;
                    if (`${id}` !== data.$el.attr(ATTR_TAG_ID)) {
                        continue;
                    }

                    linkCheckerPlugin.updateLinkStatus(elem, linkObj);
                    found = true;
                    break;
                }

                if (found) {
                    return;
                }

                // If we haven't found any initial data for this tag - either already processed at some point,
                // the tag has been edited, or a newly inserted tag, then we simply mark it as unchecked.
                // This would also be the place to check for the status of the tag if we add it.
                const elem = data?.$el?.[0];
                if (elem != null) {
                    linkCheckerPlugin.updateLinkStatus(elem);
                }
            });
        },

        /**
         * Updates the `brokenLinks` object/map and approiately marks all the links in the DOM.
         * @param {array.<ExternalLink>} linkObjects The link objects from the API which need to be updated.
         */
        initializeBrokenLinks: function (linkObjects) {
            linkCheckerPlugin._initialCheckLinks = (linkObjects || []);
            linkCheckerPlugin.brokenLinks = [];
            linkCheckerPlugin.validLinks = [];
            linkCheckerPlugin.uncheckedLinks = [];

            linkCheckerPlugin._initialCheckLinks.forEach(/** @param {ExternalLink} linkObj */function (linkObj) {
                const id = linkObj.contenttagId;
                const elem = document.querySelector(`[${ATTR_TAG_ID}="${id}"]`);
                if (elem == null) {
                    return;
                }

                linkCheckerPlugin.updateLinkStatus(elem, linkObj);
            });

            return linkCheckerPlugin.brokenLinks;
        },

        refreshLinksFromDom: function () {
            linkCheckerPlugin.brokenLinks = Array.from(document.querySelectorAll('.' + CLASS_LINK_CHECKER_ITEM + '.' + CLASS_INVALID_URL));
            linkCheckerPlugin.validLinks = Array.from(document.querySelectorAll('.' + CLASS_LINK_CHECKER_ITEM + '.' + CLASS_VALID_URL));
            linkCheckerPlugin.uncheckedLinks = Array.from(document.querySelectorAll('.' + CLASS_LINK_CHECKER_ITEM + '.' + CLASS_UNCHECKED));
        },

        /**
         * @param {HTMLElement} element
         * @param {(ExternalLink|LinkCheckerCheckResponse)=} res
         */
        updateLinkStatus: function (element, res) {
            if (element == null) {
                return false;
            }

            const status = res?.lastStatus ?? (
                res?.valid === true
                    ? STATUS_VALID
                    : res?.valid === false
                        ? STATUS_INVALID
                        : STATUS_UNCHECKED
            );

            function handler() {
                switch (status) {
                    case STATUS_VALID:
                        linkCheckerPlugin._addValidLink(element);
                        break;
                        
                    case STATUS_INVALID:
                        linkCheckerPlugin._addBrokenLink(element, res);
                        break;
    
                    case STATUS_UNCHECKED:
                        linkCheckerPlugin._addUncheckedLink(element);
                        break;
                }

                // Update the handles
                linkCheckerPlugin._updateBlockHandles(element, status);
            }

            // If it's a inline-link, we don't need extra checks
            if (linkCheckerPlugin._isInlineLink(element)) {
                handler();
                return;
            }

            // For tags, we first need to check the whitelist if they are allowed to be displayed.
            GCNUtil.getConstructFromId(element.getAttribute('data-gcn-constructid')).then(function(construct) {
                const $element = $(element);
                const config = linkCheckerPlugin.getEditableConfig($element);
                let whitelist = config?.tagtypeWhitelist;

                if (!Array.isArray(whitelist)) {
                    whitelist = [];
                }

                if (!whitelist.includes(construct.keyword) && !whitelist.includes(TAGTYPE_ALL)) {
                    linkCheckerPlugin.removeLink(element);
                } else {
                    handler();
                }
            });
        },

        /**
         * Removes the link element from the link-checker tracking
         * @param {HTMLElement} element The element to remove
         * @return {boolean} If the element has been removed
         */
        removeLink: function (element) {
            if (element == null) {
                return false;
            }

            element.classList.remove(CLASS_LINK_CHECKER_ITEM, CLASS_UNCHECKED, CLASS_CHECKED, CLASS_VALID_URL, CLASS_INVALID_URL);

            removeFromArray(linkCheckerPlugin.validLinks, element);
            removeFromArray(linkCheckerPlugin.brokenLinks, element);
            removeFromArray(linkCheckerPlugin.uncheckedLinks, element);

            linkCheckerPlugin._removeBlockHandles(element);

            return true;
        },

        /**
         * Helper method to select the link-element (technically works on any element), and activates the
         * block that it is currently in.
         * @param {HTMLElement} element The element to select
         */
        selectLinkElement: function (element) {
            if (!linkCheckerPlugin._isInlineLink(element)) {
                const block = BlockManager.getBlock(element);
                // Check if we can get a block of it and activate that one
                if (block != null) {
                    block.activate();
                }

                // Always scroll to the element
                window.scrollTo({
                    behavior: 'smooth',
                    top: element.getBoundingClientRect().top,
                });

                return;
            }

            Aloha.getSelection().removeAllRanges();
            const range = Aloha.createRange();
            range.setStart(element, 0);
            range.setEndAfter(element);
            Aloha.getSelection().addRange(range);

            const editable = Aloha.getEditableHost($(element));
            if (editable != null) {
                editable.activate();
            }
        },

        /**
         * Opens the appropiate editing method for the link element.
         * @param {HTMLElement} element The block-element (magiclink block, or regular block) which has the broken link
         * @returns {Promise.<void>}
         */
        editLink: function (element) {
            // If it's a magic-link element
            if (linkCheckerPlugin._isInlineLink(element)) {
                linkCheckerPlugin.selectLinkElement(element);
                return LinkPlugin.showLinkModal(element);
            }

            // Regular tag handling
            
            // Remove the link from the checker first, as editing a tag will
            // replace the element, and cause a dead object to be laying around otherwise.
            linkCheckerPlugin.removeLink(element);
            // Set it to loading straight away
            linkCheckerPlugin._updateBlockHandles(element, STATUS_UNCHECKED);

            const tagId = element.getAttribute(ATTR_TAG_ID);
            const pageId = element.getAttribute('data-gcn-pageid');

            return GCNPlugin.openTagFill(tagId, pageId);
        },

        /**
         * Deletes the link/block/tag, and also removes it from the link-checker
         * @param {HTMLElement} element The block-element (magiclink block, or regular block) which has the broken link
         * @returns {Promise.<void>}
         */
        deleteLink: function (element) {
            // If it's a regular block/tag, we have to use the default delete function of it
            if (!linkCheckerPlugin._isInlineLink(element)) {
                const block = BlockManager.getBlock(element);
                if (!block) {
                    return Promise.resolve();
                }

                return new Promise(function(resolve, reject) {
                    const name = element.getAttribute('data-gcn-tagname');
                    block.confirmedDestroy(function() {
                        linkCheckerPlugin.removeLink(element);
                        block.deleteInstance();
                        resolve();
                    }, name);
                });
            }

            linkCheckerPlugin.removeLink(element);
            return linkCheckerPlugin._deleteTag(element).then(function () {
                linkCheckerPlugin.selectLinkElement(element);
                LinkPlugin.removeLink(true, element);
            });
        },

        /**
         * 
         * @param {HTMLElement} element 
         * @returns {Promise.<boolean>}
         */
        _deleteTag: function (element) {
            return new Promise(function (resolve, reject) {
                const $elem = $(element);
                const pageId = GCNPlugin.page.id();
                const tagName = $elem.attr("data-keyword");

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

        _addLinkClickHandler: function ($elem) {
            if ($elem == null || !$elem.length) {
                return;
            }

            // Just to be sure, to not add the same click handler twice
            $elem.off('click.gcn-link-checker');

            $elem.on('dblclick.gcn-link-checker', function ($event) {
                $event.preventDefault();
                LinkPlugin.showLinkModal($elem[0]);
                return true;
            });
        },

        _removeLinkClickHandler: function ($elem) {
            $elem.off('click.gcn-link-checker');
        },

        _removeBlockHandles: function(element) {
            const handle = element.querySelector(`.${CLASS_BLOCK_HANDLE}`);
            if (handle == null) {
                return;
            }
            const indicator = element.querySelector(`.${CLASS_BLOCK_INDICATOR}`);
            if (indicator != null) {
                indicator.remove();
            }
        },

        /**
         * Updates the block-handle to display the current status of the link.
         * @param {HTMLElement} element The block element to handle
         * @param {'unchecked' | 'valid' | 'invalid'} state The state the link is in
         */
        _updateBlockHandles: function (element, state) {
            // Inline links/aloha-links do not have any handles, therefore skip
            if (linkCheckerPlugin._isInlineLink(element)) {
                return;
            }

            const handle = element.querySelector(`.${CLASS_BLOCK_HANDLE}`);
            if (handle == null) {
                return;
            }

            let indicator = element.querySelector(`.${CLASS_BLOCK_INDICATOR}`);
            if (indicator == null) {
                indicator = document.createElement('div');
                indicator.classList.add('gcn-block-button', CLASS_BLOCK_INDICATOR);
                handle.append(indicator);
            }

            let icon = indicator.querySelector(`.${CLASS_BLOCK_HANDLE_ICON}`);
            if (icon == null) {
                icon = document.createElement('i');
                icon.classList.add('material-symbols-outlined', CLASS_BLOCK_HANDLE_ICON);
                indicator.append(icon);
            }

            switch (state) {
                case STATUS_INVALID:
                    icon.textContent = 'warning';
                    handle.classList.remove(CLASS_UNCHECKED, CLASS_VALID_URL);
                    handle.classList.add(CLASS_INVALID_URL);
                    break;

                case STATUS_VALID:
                    icon.textContent = 'check';
                    handle.classList.remove(CLASS_UNCHECKED, CLASS_INVALID_URL);
                    handle.classList.add(CLASS_VALID_URL);
                    break;

                case STATUS_UNCHECKED:
                default:
                    icon.textContent = 'question_mark';
                    handle.classList.remove(CLASS_INVALID_URL, CLASS_VALID_URL);
                    handle.classList.add(CLASS_UNCHECKED);
                    break;
            }
        },

        /**
         * Adds the element to the unchecked Links and sets the appropiate classes
         * @param {HTMLElement} element The element to add
         * @param {string=} url The url which hasn't been checked yet
         */
        _addUncheckedLink: function (element, url) {
            if (element == null) {
                return false;
            }

            removeFromArray(linkCheckerPlugin.validLinks, element);
            removeFromArray(linkCheckerPlugin.brokenLinks, element);

            element.classList.remove(CLASS_CHECKED, CLASS_VALID_URL, CLASS_INVALID_URL);
            element.classList.add(CLASS_LINK_CHECKER_ITEM, CLASS_UNCHECKED);

            if (url) {
                element.setAttribute(ATTR_LINK_TARGET, url);
            } else {
                element.removeAttribute(ATTR_LINK_TARGET)
            }

            if (linkCheckerPlugin.uncheckedLinks.includes(element)) {
                return false;
            }

            linkCheckerPlugin.uncheckedLinks.push(element);
            PubSub.pub('gcmsui.update-linkchecker', {});
            return true;
        },

        _addValidLink: function (element) {
            if (element == null) {
                return;
            }
            
            removeFromArray(linkCheckerPlugin.uncheckedLinks, element);
            removeFromArray(linkCheckerPlugin.brokenLinks, element);

            element.classList.remove(CLASS_UNCHECKED, CLASS_INVALID_URL);
            element.classList.add(CLASS_LINK_CHECKER_ITEM, CLASS_CHECKED, CLASS_VALID_URL);
            
            if (linkCheckerPlugin.validLinks.includes(element)) {
                return false;
            }
            
            // Notify that a has occurred
            PubSub.pub('gcmsui.update-linkchecker', {});
            linkCheckerPlugin.validLinks.push(element);
            return true;
        },

        /**
         * 
         * @param {HTMLElement} element The link element
         * @param {(ExternalLink | LinkCheckerCheckResponse)=} linkObj The data to enrich the element with
         * @returns If the element has been added to the brokenLinks array
         */
        _addBrokenLink: function (element, linkObj) {
            if (element == null) {
                return false;
            }

            removeFromArray(linkCheckerPlugin.validLinks, element);
            removeFromArray(linkCheckerPlugin.uncheckedLinks, element);

            element.classList.remove(CLASS_UNCHECKED, CLASS_VALID_URL);
            element.classList.add(CLASS_LINK_CHECKER_ITEM, CLASS_CHECKED, CLASS_INVALID_URL);

            if (linkObj?.url != null) {
                element.setAttribute(ATTR_LINK_TARGET, linkObj.url);
            } else {
                element.removeAttribute(ATTR_LINK_TARGET);
            }

            if (linkCheckerPlugin.brokenLinks.includes(element)) {
                return false;
            }

            linkCheckerPlugin.brokenLinks.push(element);
            linkCheckerPlugin._addLinkClickHandler($(element));
            PubSub.pub('gcmsui.update-linkchecker', {});
            return true;
        },

        /**
         * @param {HTMLElement} element The element to check
         * @return {boolean} If the element is a inline-link; Otherwise it's to assume it is a regular tag/block.
         */
        _isInlineLink: function(element) {
            return element.tagName === 'A';
        }
    };

    linkCheckerPlugin = Plugin.create('gcnlinkchecker', linkCheckerPlugin);

    return linkCheckerPlugin;
});
