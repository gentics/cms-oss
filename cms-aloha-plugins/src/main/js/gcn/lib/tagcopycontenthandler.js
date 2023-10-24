/*global define:true*/
/*!
 * Aloha Editor
 * Author & Copyright (c) 2012 Gentics Software GmbH
 * aloha-sales@gentics.com
 * Licensed unter the terms of http://www.aloha-editor.com/license.html
 */
define([
	'aloha',
	'jquery',
	'aloha/contenthandlermanager',
	'PubSub',
	'gcn/gcn-util'
], function (
	Aloha,
	$,
	ContentHandlerManager,
	PubSub,
	Util
) {
	'use strict';

	Aloha.bind('gcn-block-handled', function (event, data) {
		Util.finishedCopyingBlock(Aloha.getEditableHost($(data)));
	});

	/**
	 * Register the tag copy contenthandler.
	 */
	var TagCopyContentHandler = ContentHandlerManager.createHandler({

		/**
		 * Handle the pasting.  Remove all unwanted stuff.
		 *
		 * @param {string} content
		 * @param {object} options
		 */
		handleContent: function (content, options) {
			var copyBlocks = false;
			var $content;

			if (typeof content === 'string') {
				$content = $('<div>' + content + '</div>');
			} else if (content instanceof $) {
				$content = $('<div>').append(content);
			}

			options = options || {};

			if ('insertHtml' === options.command) {
				$content.find('[data-gcn-tagname]').each(function () {
					var $elem = $(this);
					Util.setPlaceholder($elem,
					                    $elem.attr('data-gcn-pageid'),
					                    $elem.attr('data-gcn-tagname'));
					copyBlocks = true;
				});

				// If no tag to copy was found, trigger the "finished" event
				// immediately.
				if (!copyBlocks) {
					PubSub.pub('gcn.tagcopy.finished', {
						editable: Aloha.activeEditable
					});
				}
			}

			return $content.html();
		}
	});

	return TagCopyContentHandler;
});
