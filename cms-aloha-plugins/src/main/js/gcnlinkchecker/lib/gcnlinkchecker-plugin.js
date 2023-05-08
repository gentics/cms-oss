/*global define: true, window: true */
/*!
* Aloha Editor
* Author & Copyright (c) 2011 Gentics Software GmbH
* aloha-sales@gentics.com
* Licensed under the terms of http://www.aloha-editor.com/license.html
*/

define(
    [
        'jquery',
        'PubSub',
        'aloha/core',
        'aloha/plugin',
        'i18n!gcnlinkchecker/nls/i18n',
        'gcn/gcn-plugin',
        'gcn/gcn-links',
        'util/dom',
        'block/blockmanager',
        'css!gcnlinkchecker/css/gcnlinkchecker.css'
    ],
    function (
        jQuery,
        PubSub,
        Aloha,
        Plugin,
        i18n,
        GCNPlugin,
        GCNLinks,
        Dom,
        BlockManager
    ) {
        "use strict";

        var $ = jQuery,
            panelId = 'aloha-gcnlinkchecker-panel',
            panelContentId = 'aloha-gcnlinkchecker-panel-content',
            sidebar = Aloha.Sidebar.right,
            $panelContainer,
            checks = [],
            defaultSettings = {
                defaultProtocol: 'http',
                livecheck: true,
                delay: 500
            },
            plugin;

        /**
         * Load the linkChecker results for the given page and pass the response to the success function
         * 
         * @param {string} id page ID
         * @param {function} success success function
         */
        function loadResults(id, success) {
            GCNPlugin.performRESTRequest({
                type: 'GET',
                url: GCNPlugin.settings.stag_prefix + GCNPlugin.restUrl + '/linkChecker/pages/' + id,
                success: function (data) {
                    success(data.items);
                },
                error: function (jqXHR, textStatus, errorThrown) {
                    Aloha.Log.error('Could not load external links for page with id {'
                        + id + '}', textStatus, errorThrown);
                }
            });
        }

        /**
         * Check the href for the element with a delay. If called again for the same element within the delay time,
         * the first call will not be done.
         * 
         * @param {string} href URL to be checked
         * @param {jQuery|Dom} element link element
         * @param {jQuery|Dom} input input field
         */
        function checkWithDelay(href, element, input) {
            if (checks[element]) {
                window.clearTimeout(checks[element]);
            }
            checks[element] = window.setTimeout(function () {
                checkUrl(href, element, input);
            }, plugin.settings.delay);
        }

        /**
         * Fix the url, before it is checked, by prepending default protocol or base URL
         * 
         * @param {string} url URL to be fixed
         * @returns fixed URL
         */
        function fixUrl(url) {
            if (url.indexOf('//') === 0) {
                return plugin.settings.defaultProtocol + ":" + url;
            }
            if (url.indexOf("/") === 0) {
                if (typeof plugin.settings.absoluteBase === 'string') {
                    return plugin.settings.absoluteBase + url;
                } else {
                    return url;
                }
            }
            if (url.indexOf('http') !== 0 && url.indexOf(':') < 0) {
                if (typeof plugin.settings.relativeBase === 'string') {
                    return plugin.settings.relativeBase + url;
                } else {
                    return url;
                }
            }

            return url;
        }

        /**
         * Check the given URL asynchronously and mark link and input field according to the result.
         * 
         * @param {sting} href URL to be checked
         * @param {jQuery|Dom} element link element
         * @param {jQuery|Dom} input input field
         */
        function checkUrl(href, element, input) {
            href = fixUrl(href);
            GCNPlugin.performRESTRequest({
                url: GCNPlugin.settings.stag_prefix + GCNPlugin.restUrl + '/linkChecker/check',
                type: 'POST',
                body: {
                    url: href
                },
                success: function (data) {
                    var $input = $(input);
                    if (data.valid === true) {
                        $(element).removeClass("aloha-gcnlinkchecker-invalid-url");
                        $input.parent()
                            .removeClass('gcn-link-uri-warning')
                            .addClass('gcn-link-uri-success');
                        GCNLinks.getIcon($(input), i18n.t('success.valid-uri'))
                            .html('\u2713');
                    } else if (data.valid === false) {
                        GCNLinks.getIcon($input, i18n.t('error.invalid-external.link'))
                            .html('!');
                        $(element).addClass("aloha-gcnlinkchecker-invalid-url");
                        $input.parent()
                            .addClass('gcn-link-uri-warning')
                            .removeClass('gcn-link-uri-success');
                    } else {
                        GCNLinks.getIcon($input, i18n.t('error.invalid-external.link'))
                            .html('');
                        $(element).removeClass("aloha-gcnlinkchecker-invalid-url");
                        $input.parent()
                            .removeClass('gcn-link-uri-warning')
                            .removeClass('gcn-link-uri-success');
                    }
                }
            });
        }

        function openTagfill(event) {
            event.preventDefault();
            var $elem = $(this);
            var pageId = GCNPlugin.page.id();
            GCN.page(pageId, function (page) {
                page.tag($elem.attr("data-keyword"), function (tag) {
                    // first we have to try to close any open tag fill
                    try {
                        GCNPlugin.closeLightbox();
                    } catch (err) {
                        // tag fill could not be closed because it was not open
                    }
                    // now we can open the desired object property as tag fill
                    console.log('opening tag fill for', pageId, tag.prop('id'));
                    GCNPlugin.openTagFill(tag.prop('id'), pageId);
                });
            });

            return false;
        }

        function deleteTag(event) {
            event.preventDefault();
            console.log('delete' + $(this).attr("data-keyword"));
            var $elem = $(this);
            var pageId = GCNPlugin.page.id();
            GCN.page(pageId, function (page) {
                page.tag($elem.attr("data-keyword"), function (tag) {
                    // remove item from list
                    event.target.parentElement.remove();
                    // remove tag from taglist (this will only be persisted on page save)
                    tag.remove();
                });
            });
            return false;
        }

        /**
         * Load the linkChecker results for the current page and populate the sidebar panel with the results.
         */
        function populatePanel() {
            loadResults(GCNPlugin.page.id(), function (items) {
                var invalidLinksCount = 0,
                    invalidLinksNotInDomCount = 0,
                    panel,
                    $invalidLinksNotInDom = $('<ul></ul>'),
                    $lastStatus = false;
                $panelContainer.empty();
                
                // mark all broken link items not found in the dom - these will get "delete" buttons later
                $.each(items, function(index, item) {
                    items[index].notFound = !document.querySelector("[data-gcn-tagid='" + items[index].contenttagId + "']");
                })
                // sort items by position in viewport (left to right and top to bottom)
                if (typeof Array.prototype.sort === 'function') {
                    function compareByVisibleLocation(itemA, itemB) {
                        var a = document.querySelector("[data-gcn-tagid='" + itemA.contenttagId + "']");
                        var b = document.querySelector("[data-gcn-tagid='" + itemB.contenttagId + "']");
                        // element will be sorted to the end of the list when not found in dom
                        if (!a) {
                            return -1;
                        }
                        if (!b) {
                            return 1;
                        }

                        if (a.offsetTop > b.offsetTop) {
                            return 1;
                        } else if (a.offsetTop < b.offsetTop) {
                            return -1;
                        }

                        if (a.offsetLeft > b.offsetLeft) {
                            return 1;
                        } else if (a.offsetLeft < b.offsetLeft) {
                            return -1;
                        } else {
                            return 0;
                        }
                    }

                    items.sort(compareByVisibleLocation);
                }


                $.each(items, function (index, item) {
                    if (item.lastStatus === 'invalid') {
                        // setup last status once from the first invalid link item
                        // all items are expected to have the same timestamp
                        if (!$lastStatus) {
                            $lastStatus = $('<p></p>')
                                .addClass('aloha-gcnlinkchecker-statustimestamp')
                                // convert timestamp form db to milliseconds
                                .text(i18n.t('lastStatusLabel') + new Date(item.lastCheckTimestamp * 1000).toLocaleString());
                        }
                        var $contentTag = $("[data-gcn-tagid='" + item.contenttagId + "']");
                        var $entry = $('<li></li>');
                        var $link = $('<h1></h1>');
                        var $openTagfill = $('<button class="aloha-gcnlinkchecker-opentagfill" data-keyword="' + item.contenttagName + '" title="edit ' + item.contenttagName + '">' + i18n.t('openTagfill') + '</button>');
                        var $deleteTag = $('<button class="aloha-gcnlinkchecker-deleteTag"  data-keyword="' + item.contenttagName + '" title="delete ' + item.contenttagName + '">' + i18n.t('deleteTag') + '</button>');

                        $contentTag.addClass('aloha-gcnlinkchecker-invalid-url');
                        $entry.attr('id', 'aloha-gcnlinkchecker-entry-' + item.id);

                        // setup click handler to send user to contentTag
                        $entry.click(function () {
                            var $contentTag = $("[data-gcn-tagid='" + item.contenttagId + "']");
                            if ($contentTag.length) {
                                var $host = $(Dom.getEditingHostOf($contentTag.get(0)));
                                var editable = Aloha.getEditableById($host.attr('id'));
                                var blockId = $contentTag.eq(0).attr('id');
                                if (editable) {
                                    editable.activate();
                                    Dom.setCursorInto($contentTag.get(0));
                                    Aloha.scrollToSelection();
                                    Dom.selectDomNode($contentTag.get(0));
                                    Aloha.Selection.updateSelection();
                                } else {
                                    if (blockId) {
                                        var block = BlockManager.getBlock(blockId);
                                        if (block) {
                                            block.activate(block.$element);
                                        }
                                    }
                                    $(window.document).scrollTop($contentTag.offset().top);
                                }
                            }
                        });
                        $entry.append($link);
                        if (item.text) {
                            $link.text('Inline Link');
                            $entry.append('<h2>URL:</h2>' + item.url)
                                .append('<h2>' + i18n.t('entry.linktext.title') + '</h2>')
                                .append(item.text);
                        } else {
                            $link.text(i18n.t('entry.linktag.title') + ' ' + item.contenttagName);
                            $entry
                                .append('<h2>URL:</h2>' + item.url)
                                .append("<h2>" + i18n.t('entry.linkpart.title') + "</h2>")
                                .append(item.partName)
                                .append('<br>')
                                .append($openTagfill);
                        }


                        // check if the element was found in DOM
                        if (item.notFound) {
                            $entry.append('<br>').append($deleteTag);
                            $invalidLinksNotInDom.append($entry);
                            invalidLinksNotInDomCount++;
                        } else {
                            $panelContainer.append($entry);
                        }
                        invalidLinksCount++;
                    }
                });

                panel = sidebar.getPanelById(panelId);
                if (panel) {
                    panel.setTitle(i18n.t('title') + ' (' + invalidLinksCount + ')');
                    if ($lastStatus) {
                        $panelContainer.parent().prepend($lastStatus);
                    }
                    if (invalidLinksNotInDomCount > 0) {
                        $panelContainer
                            .parent()
                            .append('<h2>' + i18n.t('notInDom') + '</h2>')
                            .append($invalidLinksNotInDom);
                    }
                    if (invalidLinksCount > 0) {
                        sidebar.open();
                    }
                }
            });
        }

        /**
         * Create the sidebar panel and populate with the linkChecker results for the current page
         */
        function createPanel() {
            sidebar.addPanel({
                id: panelId,
                title: i18n.t('title'),
                expanded: true,
                activeOn: true,
                content: '<ul id="' + panelContentId + '"></ul>',
                onInit: function () {
                    var that = this;
                    $panelContainer = $(that.content).find('#' + panelContentId);
                    populatePanel();
                    // register click handler
                    $('body')
                        .on('click', '.aloha-gcnlinkchecker-opentagfill', openTagfill)
                        .on('click', '.aloha-gcnlinkchecker-deleteTag', deleteTag);
                }
            });
        }

        // Add an event handler for changed links.
        PubSub.sub('aloha.link.changed', function (msg) {
            // if livecheck is activated and the link is external and not empty and not only an anchor, the check is done after a delay
            if (plugin.settings.livecheck && plugin.settings.livecheck !== 'false' && msg.href !== '' && msg.href.indexOf('#') !== 0 && !GCNLinks.isInternal(msg.element)) {
                checkWithDelay($.trim(msg.href), msg.element, msg.input);
            }
        });

        /**
         * Create the plugin
         */
        plugin = Plugin.create('gcnlinkchecker', {

            /**
             * Configure the available languages (i18n) for this plugin
             */
            languages: ['en', 'de'],

            /**
             * Initialize the plugin
             */
            init: function () {
                // in old UI - where GCMSUI is not defined, just run the plugin
                // in the new UI - do nothing in preview mode
                if (typeof GCMSUI === 'undefined' || (typeof GCMSUI.appState !== 'undefined' && GCMSUI.appState.editMode !== 'preview')) {
                    // load config
                    plugin.settings = $.extend(true, {}, defaultSettings, plugin.settings);
                    // setup sidebar pannel
                    createPanel();
                }
            },

            refresh: function () {
                populatePanel();
            }
        });

        return plugin;
    }
);
