//
// NOTE: Keep this file in sync with the other gcn-tooltips.js file which 
//       resides within the aloha editor contentnode webapp minibrowser.
//
// Regressive enhancement for parts of Gentics Content.Node that do not have
// AMD support.
(window.define || function (deps, callback) {
	'use strict';
	window.GCN_TOOLTIPS = callback(window.jQuery, window.GCNREST);
})([
	'jquery',
	'gcn/gcn-plugin',
	'./vendor/tipsy'
], function (
	$,
	GCNREST
) {
	'use strict';

	/**
	 * The delay (in milliseconds) with which to wait before closing all
	 * tooltips via initiateTooltipClosing().
	 *
	 * @type {number}
	 * @const
	 */
	var CLOSE_DELAY = 100;

	/**
	 * The width of the thumbnail image.
	 *
	 * @type {number}
	 * @const
	 */
	var THUMBNAIL_WIDTH = 22;

	/**
	 * The height of the thumbnail image. Determined by the "Golden Quotient"
	 * 1.62.
	 *
	 * @type {number}
	 * @const
	 */
	var THUMBNAIL_HEIGHT = Math.round(THUMBNAIL_WIDTH / 1.62);

	/**
	 * The width of the preview image.
	 *
	 * @type {number}
	 * @const
	 */
	var PREVIEW_SIZE = 250;

	/**
	 * A flag denoting whether jQuery.Tipsy is available.
	 *
	 * This flag is used to determine whether or not any attempt should be made
	 * initialize tooltips.
	 *
	 * @type {boolean}
	 * @const
	 */
	var TIPSY = !!$.fn.tipsy;

	/**
	 * Tipsy configuration for preview.
	 * (http://onehackoranother.com/projects/jquery/tipsy/).
	 *
	 * @type {object<string, number|string|function>}
	 * @const
	 */
	var TIPSY_PREVIEW_CONFIG = {
		trigger: 'manual',
		html: true,
		delayIn: 0,
		delayOut: 0,
		opacity: 1,
		gravity: function () {
			// Display the preview to the left of the thumbnail, if there is enough space
			// and to the right otherwise. This way the preview will not overlap with
			// filenames nearby. If it is displayed to the left, it will just cover the
			// tree, if it is displayed to the right, pointTooltip() should place the
			// preview near the right border of the browser.
			var $row = $(this);
			var $img = $($row.attr('original-title')).find('.gtx-repobrowser-preview-image');
			var match = $img.attr('src').match(/GenticsImageStore\/(\d+)\//);
			var width = match ? parseInt(match[1], 10) : PREVIEW_SIZE;

			return $row.offset().left > width ? 'e' : 'w';
		},
		fade: false
	};

	/**
	 * Tipsy configuration for ellipsises.
	 * (http://onehackoranother.com/projects/jquery/tipsy/).
	 *
	 * @type {object<string, number|string|function>}
	 * @const
	 */
	var TIPSY_ELLIPSIS_CONFIG = {
		trigger: 'manual',
		html: true,
		delayIn: 0,
		delayOut: 0,
		opacity: 1,
		gravity: 'n',
		fade: false
	};

	/**
	 * Gentics Content Node settings object.
	 *
	 * @type {object<string, *>}
	 */
	var gcnSettings;

	/**
	 * Gets the GCNREST settings that have been provided in environment in which
	 * this library is used.
	 *
	 * @return {object<string, *>}
	 */
	function getGCNRESTSettings() {
		if (gcnSettings) {
			return gcnSettings;
		}
		if (typeof Aloha !== 'undefined') {
			gcnSettings = Aloha.require('gcn/gcn-plugin').settings;
		} else {
			gcnSettings = GCNREST.settings;
		}
		return gcnSettings;
	}

	/**
	 * Gets the stag_prefix wherever it is provided in the environment.
	 *
	 * @return {string} The configured stag_prefix.
	 */
	function getSTagPrefix() {
		return getGCNRESTSettings().stag_prefix;
	}
	
	/**
	 * Gets the proxy_prefix from the GCNREST settings
	 *
	 * @return {string} The configured proxy_prefix.
	 */
	function getProxyPrefix() {
		return GCNREST.settings.proxy_prefix || "";
	}

	/**
	 * Gets the portletapp_prefix from the GCNREST settings
	 * @return {string} the configured portletapp_prefix
	 */
	function getPortletappPrefix() {
		return GCNREST.settings.portletapp_prefix || "/";
	}

	/**
	 * Gets the session id, wherever it is provided.
	 *
	 * @return {string} The session id.
	 */
	function getSID() {
		return getGCNRESTSettings().sid || window.GCNREST.sid;
	}

	/**
	 * Positions the tipsy container such that its arrow is aligned to at the
	 * center and bottom of the the given target element.
	 *
	 * @param {jQuery.<HTMLElement>} $anchor jQuery unit set of the DOM element
	 *                                       on which to position the tooltip
	 *                                       arrow.
	 * @return {jQuery.<HTMLElement>} jQuery tipsy DOM element.
	 */
	function pointTooltip($anchor) {
		var $tipsy = $('.tipsy');
		if ($anchor && $anchor.length) {
			if ($tipsy.is('.tipsy-w')) {
				// The tooltip is shown to the right of the thumbnail, move it to the
				// right side of the browser, so it does not cover other filenames.
				var $row = $anchor.closest('tr');

				$tipsy.css('left', $row.width() * .95 + $row.offset().left - $tipsy.width())
			} else {
				var $arrow = $tipsy.find('.tipsy-arrow');
				var position = $anchor.offset().left + ($anchor[0].offsetWidth / 2);
				var delta = position - $arrow.offset().left;
				var left = parseInt($tipsy.css('left'), 10);

				$tipsy.css('left', left + delta);
			}
		}
		return $tipsy;
	}

	/**
	 * Formats a bytes value into a human readable format (e.g., 1 bytes, 2K,
	 * 34M).
	 *
	 * Does binary conversion, hence the divisor 1024.
	 *
	 * Output value is rounded to 1 decimal space.
	 *
	 * @param {number} size File size in bytes.
	 * @return {string} A human readable representation of the given bytes.
	 */
	function format(size) {
		if (!size) {
			return '0 bytes';
		}
		var sizes = ['bytes', 'KB', 'MB', 'GB', 'TB'];
		var index = 1;
		while (size > 1024 && index < sizes.length) {
			size /= 1024;
			index++;
		}
		var seperator = (1 === index) ? ' ' : '';
		return Math.round(size) + seperator + sizes[index - 1];
	}

	/**
	 * Constrains the width and height values to be no greater than a given
	 * constraint while maintaining aspect ratio.
	 *
	 * @param {number} w Width.
	 * @param {number} h Height.
	 * @param {number} constraint The number within which w and h should be
	 *                            constrained.
	 * @return {object} An object containing width, and height properties that
	 *                  are constrained.
	 */
	function constrain(w, h, constraint) {
		if (!w || !constraint) {
			return {
				width: 0,
				height: 0
			};
		}
		h = h || w;
		var scaleFactor = (w < constraint) ? 1 : constraint / w;
		var height = h * scaleFactor;
		if (height > constraint) {
			scaleFactor = constraint / h;
			height = h * scaleFactor;
		}
		return {
			width: Math.round(w * scaleFactor),
			height: Math.round(height)
		};
	}

	/**
	 * Creates a GenticsImageStore url for the given image id.
	 *
	 * @param {number|string} id Id of the image.
	 * @param {number} width The width of the image to generate, in pixels.
	 * @param {number} height The height of the image to generate, in pixels.
	 * @param {string} mode How to handle image proportion the image
	 *                      (smart|prop).
	 * @return {string} GenticsImageStore URL
	 */
	function createGenticsImageStoreUrl(id, width, height, mode) {
		return (getPortletappPrefix() + 'GenticsImageStore/' + width + '/'
				+ (height || 'auto') + '/' + (mode || 'prop')
				+ getSTagPrefix() + '?do=16000' + '&id=' + id + '&sid='
				+ getSID());
	}

	/**
	 * Creates HTML markup for a thumbnail.
	 *
	 * @param {number} id The id of the thumbnail's image.
	 * @param {boolean} inherited Whether this image is an inherited object via
	 *                            multi-channelling.
	 * @param {boolean} whether the image is resizable by the Gentics Image Store.
	 * @return {string} HTML markup.
	 */
	function createThumbnail(id, inherited, gisResizable) {
		var url = null;

		if (gisResizable) {
			url = createGenticsImageStoreUrl(id, THUMBNAIL_WIDTH, THUMBNAIL_HEIGHT,
					'smart')
		} else {
			url = getSTagPrefix() + '?do=11&module=content&img=image.gif';
		}

		return ('<div class="gtx-repobrowser-thumbnail'
			+ (inherited ? ' gtx-repobrowser-thumbnail-inherited">' : '">')
			+ '<span style="background-image:url(' + url + ');"></span>'
			+ (inherited ? '<b></b>' : '')
			+ '</div>');
	}

	/**
	 * Hides the current preview placeholder and shows the actual preview image
	 * when is `onload' event is trigger.
	 *
	 * @param {jQuery.Event} $event jQuery's normalized event object.
	 */
	function onPreviewLoaded($event) {
		$($event.target)
			.unbind('load', onPreviewLoaded)
			.show()
			.closest('div')
			.find('.gtx-repobrowser-preview-placeholder').hide();
	}

	/**
	 * A timer used to delay the closing of tooltips.
	 *
	 * @type {number}
	 */
	var closeTooltipsTimer;

	/**
	 * Aborts any delayed closing of tooltips that has been initiated via
	 * initiateTooltipClosing()
	 */
	function abortTooltipClosing() {
		if (closeTooltipsTimer) {
			clearTimeout(closeTooltipsTimer);
			closeTooltipsTimer = null;
		}
	}

	/**
	 * Hides and removes all tooltips from the document DOM.
	 */
	function closeTooltips() {
		$('.tipsy').hide().remove();
		abortTooltipClosing();
	}

	/**
	 * Closes all tooltips after `CLOSE_DELAY' milliseconds.
	 */
	function initiateTooltipClosing() {
		abortTooltipClosing();
		closeTooltipsTimer = setTimeout(closeTooltips, CLOSE_DELAY);
	}

	/**
	 * Finds the thumbnail container in the given DOM element.
	 *
	 * @param {jQuery.<HTMLElement>} $element jQuery unit set containing a DOM
	 *                                        element.
	 * @return {jQuery.<HTMLElement>} jQuery unit set of the thumbnail
	 *                                container in the given element.
	 */
	function findThumbnail($element) {
		return $element.find('.gtx-repobrowser-thumbnail:first');
	}

	/**
	 * Displays the preview that is bound to the given element.
	 *
	 * @param {jQuery.<HTMLElement>} $element jQuery unit set containing a DOM
	 *                                        element.
	 * @return {jQuery.<HTMLElement>} jQuery element on which the tooltip is
	 *                                bound to.
	 */
	function showPreview($element) {
		abortTooltipClosing();
		var $preview = $element.tipsy('show');
		var $tipsy = pointTooltip(findThumbnail($element));
		$tipsy.find('.tipsy-inner').css('max-width', 'none');
		$tipsy.find('.gtx-repobrowser-preview-image').load(onPreviewLoaded);
		return $preview;
	}

	/**
	 * Displays the ellipses that is bound to the given element.
	 *
	 * @param {jQuery.<HTMLElement>} $element jQuery unit set containing a DOM
	 *                                        element.
	 * @param {function(jQuery.Event)} onClick Callback function that will be
	 *                                         invoked when the ellipsis
	 *                                         tooltip element is clicked.
	 */
	function showEllipsis($item, onClick) {
		closeTooltips();
		$item.tipsy('show');
		var $tipsy = $('.tipsy');
		$tipsy.mouseenter(abortTooltipClosing)
		      .mouseleave(closeTooltips);
		if (onClick) {
			$tipsy.find('.gtx-repobrowser-ellipsis').click(onClick);
		}
	}

	/**
	 * Provide the document with necessary styles for repository browser
	 * thumbnails.
	 */
	$('head:first').append('<style>\
		.tipsy { padding: 5px; font-size: 10px; position: absolute; z-index: 999999; }\
		 .tipsy-inner { padding: 5px 8px 4px 8px; background-color: black; color: white; max-width: 250px;\
			-moz-box-shadow: 0 0 5px #333;\
			-webkit-box-shadow: 0 0 5px #333;\
			box-shadow: 0 0 5px #333;\
		 }\
		 .tipsy-inner { border-radius: 3px; -moz-border-radius:3px; -webkit-border-radius:3px; }\
		 .tipsy-arrow { position: absolute; background: url("/.Node/?do=11&module=system&img=tipsy.gif") no-repeat top left; width: 9px; height: 5px; }\
		 .tipsy-n .tipsy-arrow { top: 0; left: 50%; margin-left: -4px; }\
		   .tipsy-nw .tipsy-arrow, .tipsy-nww .tipsy-arrow, .tipsy-nwe .tipsy-arrow { top: 0; left: 10px; }\
		   .tipsy-ne .tipsy-arrow, .tipsy-nee .tipsy-arrow, .tipsy-new .tipsy-arrow { top: 0; right: 10px; }\
		   .tipsy-s .tipsy-arrow { bottom: 0; left: 50%; margin-left: -4px; background-position: bottom left; }\
		   .tipsy-sw .tipsy-arrow, .tipsy-sww .tipsy-arrow, .tipsy-swe .tipsy-arrow { bottom: 0; left: 10px; background-position: bottom left; }\
		   .tipsy-se .tipsy-arrow, .tipsy-sew .tipsy-arrow, .tipsy-see .tipsy-arrow { bottom: 0; right: 10px; background-position: bottom left; }\
		 .tipsy-e .tipsy-arrow { top: 50%; margin-top: -4px; right: 0; width: 5px; height: 9px; background-position: top right; }\
		 .tipsy-w .tipsy-arrow { top: 50%; margin-top: -4px; left: 0; width: 5px; height: 9px; }\
		 .tipsy h6 {margin:0; padding:0 0 3px 0; font-size:14px;}\
		 \
		.gtx-repobrowser-preview {\
			font: normal 10px Arial;\
			line-height: 10px;\
			text-align: center;\
		}\
		.gtx-repobrowser-preview-image {\
			display: none;\
		}\
		.gtx-repobrowser-preview img {\
			margin-bottom: 5px;\
		}\
		.gtx-repobrowser-thumbnail {\
			position: relative;\
			border: 1px solid whiteSmoke;\
		}\
		.gtx-repobrowser-thumbnail span {\
			display: block;\
			background-repeat: no-repeat;\
			width: ' + THUMBNAIL_WIDTH + 'px;\
			height: ' + THUMBNAIL_HEIGHT + 'px;\
		}\
		.gtx-repobrowser-thumbnail-inherited span {\
			opacity: 0.6;\
			filter: alpha(opacity=60);\
		}\
		.gtx-repobrowser-thumbnail b {\
			display: block;\
			position: absolute;\
			top: 0px;\
			right: 0px;\
			width: 8px;\
			height: 8px;\
			background: url("/.Node/?do=11&module=content&img=channel-dart.gif") no-repeat;\
		}\
		.gtx-repobrowser-ellipsis {\
			width: 100px;\
		}\
		.gtx-repobrowser-ellipsis img {\
			margin: 3px;\
			cursor: pointer;\
		}\
	</style>');

	return {
		constrain                  : constrain,
		closeTooltips              : closeTooltips,
		createGenticsImageStoreUrl : createGenticsImageStoreUrl,
		createThumbnail            : createThumbnail,
		format                     : format,
		initiateTooltipClosing     : initiateTooltipClosing,
		showPreview                : showPreview,
		showEllipsis               : showEllipsis,
		PREVIEW_SIZE               : PREVIEW_SIZE,
		THUMBNAIL_WIDTH            : THUMBNAIL_WIDTH,
		THUMBNAIL_HEIGHT           : THUMBNAIL_HEIGHT,
		TIPSY                      : TIPSY,
		TIPSY_PREVIEW_CONFIG       : TIPSY_PREVIEW_CONFIG,
		TIPSY_ELLIPSIS_CONFIG      : TIPSY_ELLIPSIS_CONFIG
	};
});
