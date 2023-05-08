(function (GCN) {

	'use strict';

	/**
	 * Only grows, never shrinks.
	 *
	 * @private
	 * @type {number}
	 */
	var uniqueIdCounter = 0;

	/**
	 * Generates a unique id with an optional prefix.
	 *
	 * The returned value is only unique among other returned values,
	 * not globally.
	 *
	 * @public
	 * @param {string}
	 *        Optional prefix for the id to be generated.
	 * @return {string}
	 *        Never the same string more than once.
	 */
	function uniqueId(prefix) {
		return (prefix || '') + (++uniqueIdCounter);
	}

	/**
	 * Escapes the given property name by prefixing dots with backslashes.
	 *
	 * @param {string} name The property name to escape.
	 * @return {string} Escaed string.
	 */
	function escapePropertyName(name) {
		return name.replace(/\./g, '\\.');
	}

	/**
	 * A regular expression to identify executable script tags.
	 *
	 * @type {RegExp}
	 */
	var rgxpScriptType = /\/(java|ecma)script/i;


	/**
	 * A string to be used in regular expressions matching script tags.
	 *
	 * TODO: The string does not work correctly for complex cases and
	 * should be improved.
	 *
	 * @type {string}
	 */
	var SCRIPT_TAG = '<script(\\s[^>]*?)?>';

	/**
	 * This regular expression is used to find opening script tags.
	 *
	 * It uses the global flag to find several matching tags, for
	 * example to replace them.
	 *
	 * @type {RegExp}
	 */
	var rgxpScriptTags = new RegExp(SCRIPT_TAG, 'ig');

	/**
	 * This regular expression is used to find opening script tags.
	 * It does not use the global flag, and can be used to test if
	 * a string contains scripts at all.
	 *
	 * For testing strings, this expression should be used instead
	 * of rgxpScriptTags, because the latter keeps an index of found
	 * occurrences (due to its global flag), which causes the
	 * regular expression to often fail (in Firefox).
	 *
	 * @type {RegExp}
	 */
	var rgxpScriptTag = new RegExp(SCRIPT_TAG, 'i');



	/**
	 * A regular expression to find script types.
	 *
	 * @type {RegExp}
	 */
	var rgxpType = new RegExp(
		' type\\s*=\\s*'
			+ '[\\"\\\']'
			+ '([^\\"\\\']*[^\\s][^\\"\\\']*)'
			+ '[\\"\\\']',
		'i'
	);

	var rand = Math.random().toString().replace('.', '');

	/**
	 * Places sentinal strings in front of every executable script tag.
	 *
	 * @param {string} html HTML markup
	 * @return {string} HTML string with marked <script> tags.
	 */
	function markScriptTagLocations(html) {
		var i = 0;
		rgxpScriptTags.lastIndex = 0;
		return html.replace(rgxpScriptTags, function (str, substr, offset) {
			var type = substr && substr.match(rgxpType);
			if (!type || rgxpScriptType.test(type)) {
				return rand + (i++) + str;
			}
			return str;
		});
	}

	/**
	 * Masks a <script> tag's `type' attribute by replacing it with a random
	 * string.
	 *
	 * Masking script tags is done to prevent them from being handled specially
	 * by jQuery, which removes javascript/ecmascript tags when appending DOM
	 * elements into the document, but executes them.
	 *
	 * unmakeScriptType() reverses this.
	 *
	 * @param {jQuery.<HTMLElement>} $script jQuery unit set containing the
	 *                                       <script> tag that is to have its
	 *                                       type attribute masked.
	 */
	function maskScriptType($script) {
		var type = $script.attr('type');
		$script.attr('type', rand).attr('data-origtype', type);
	}

	/**
	 * Restores a <script> tag's original type attribute value if it had been
	 * masked using maskScriptType().
	 *
	 * Essentially the reverse of maskScriptType().
	 *
	 * @param {jQuery.<HTMLElement>} $script jQuery unit set containing the
	 *                                       <script> tag that is to have its
	 *                                       type attribute unmasked.
	 */
	function unmaskScriptType($script) {
		var orig = $script.attr('data-origtype');
		if (typeof orig === 'string') {
			$script.attr('type', orig).removeAttr('data-origtype');
		} else {
			$script.removeAttr('type');
		}
	}

	/**
	 * Replaces the type attribute of <script> tags with a value that will
	 * protect them from being specially handled by jQuery.
	 *
	 * @param {string} html Markup
	 * @return {string} Markup with <script> tags protected from jQuery.
	 */
	function protectScriptTags(html, $scripts) {
		var i;
		var type;
		var $script;
		for (i = 0; i < $scripts.length; i++) {
			$script = $scripts.eq(i);
			type = $script.attr('type');
			if (!type || rgxpScriptType.test(type)) {
				maskScriptType($script);
				html = html.replace(rand + i, $script[0].outerHTML);
				unmaskScriptType($script);
			}
		}
		return html;
	}

	/**
	 * Restores the type attribute for <script> tags that have been processed
	 * via protectScriptTags().
	 *
	 * @param {jQuery.<HTMLElement>} $element Root HTMLElement in which to
	 *                                        restore <script> tags.
	 */
	function restoreScriptTagTypes($element) {
		var $scripts = $element.find('script[type="' + rand + '"]');
		var $script;
		var i;
		for (i = 0; i < $scripts.length; i++) {
			$script = $scripts.eq(i);
			$script.removeClass(rand + i);
			unmaskScriptType($script);
		}
	}

	/**
	 * Joins the innerHTML of multiple elements.
	 *
	 * @param {jQuery.<HTMLElement>} $elements
	 * @return {string} A concatenated string of the contents of the given set
	 *                  of elements.
	 */
	function joinContents($elements) {
		var contents = '';
		$elements.each(function () {
			contents += jQuery(this).html();
		});
		return contents;
	}

	/**
	 * Inserts the inner HTML of the given HTML markup while preserving
	 * <script> tags, and still allowing jQuery to execute them.
	 *
	 * @param {jQuery.<HTMLElement>} $element
	 * @param {string} html
	 */
	function insertInnerHTMLWithScriptTags($element, html) {
		if (!rgxpScriptTag.test(html)) {
			$element.html(jQuery(html).contents());
			return;
		}
		var marked = markScriptTagLocations(html);
		var $html = jQuery(marked);
		// beware, starting with jQuery 1.9.0, the handling of scripts changed. Before 1.9.0 when doing e.g.
		// jQuery('<div><script></script></div>') the script tag would be moved out of the div and appended to the end.
		// therefore using the filter() method would give us all scripts found in the markup
		// Starting with jQuery 1.9.0, the script tags would remain were they are and therefore using the filter() method
		// would return an empty jQuery() object, unless the markup consists only of the script tag.
		var $scripts = jQuery(html).filter('script');
		var contents;
		// now we do a detection of the script-tag handling
		if ($scripts.length === 0) {
			// we are using jQuery >= 1.9.0, so we use .find() to extract all the script-nodes
			$scripts = jQuery(html).find('script');
			// from the $html, we now remove the script-nodes
			$html.find('script').remove();
			contents = $html.html();
		} else {
			// we are using jQuery < 1.9.0, to $html is already separated (non-script nodes and script nodes)
			// so with .filter() we can get all non-script nodes and join them
			contents = joinContents($html.filter(':not(script)'));
		}
		// when we get here, contents will consists of the original html markup with all script tags replaced by placeholders
		// next we will re-insert the script tags at their original place, but with the type replaced by something else than text/javascript
		// the purpose is that jQuery will NOT evaluate the scripts when inserting into the DOM
		contents = protectScriptTags(contents, $scripts);
		// insert the markup into the element. masked script nodes will be generated but not executed
		$element.html(contents);

		// Trap script errors originating from rendered tags and log it on the
		// console.
		try {
			// jQuery will now execute but not really append the scripts
			$element.append($scripts);
		} catch (ex) {
			var _console = 'console'; // Because jslint is paranoid.
			if (typeof window[_console] === 'function') {
				window[_console].error(ex);
			}
		}

		// finally, we unmask the script nodes to have them appear like in the original markup
		restoreScriptTagTypes($element);
	}

	/**
	 * Merge class names from one element into another.
	 *
	 * The merge result will be a unqiue set of space-seperated class names.
	 *
	 * @param {jQuery.<HTMLElement>} $first jQuery unit set containing the DOM
	 *                                      whose class names are to be merged.
	 * @param {jQuery.<HTMLElement>} $second jQuery unit set containing the DOM
	 *                                       whose class names are to be merged.
	 * @return {string} The merge result of the merge: a unqiue set of
	 *                  space-seperated class names.
	 */
	function mergeClassNames($first, $second) {
		var first = ($first.attr('class') || '').split(' ');
		var second = ($second.attr('class') || '').split(' ');
		var names = first.concat(second).sort();
		var i;
		for (i = 1; i < names.length; i++) {
			if (names[i] === names[i - 1]) {
				names.splice(i--, 1);
			}
		}
		return names.join(' ');
	}

	/**
	 * Creates a map of attributes merged from their value in $from with their
	 * value in $to.
	 *
	 * Class names--unlike other attributes, which are simply copied from $from
	 * into $to--are treaded specially to produce a unique set of
	 * space-seperated class names.
	 *
	 * @param {jQuery.<HTMLElement>} $to jQuery unit set containing the DOM
	 *                                   element which should receive the
	 *                                   merged attributes.
	 * @param {jQuery.<HTMLElement>} $from jQuery unit set containing the DOM
	 *                                     element whose attributes will be
	 *                                     merged into the other.
	 * @param {object<string, string>} A associate array of attributes.
	 */
	function mergeAttributes($to, $from) {
		var from = $from[0].attributes;
		var to = $to[0].attributes;
		var i;
		var attr = {};
		for (i = 0; i < from.length; i++) {
			attr[from[i].name] = ('class' === from[i].name)
			                   ? mergeClassNames($to, $from)
			                   : $from.attr(from[i].name);
		}
		return attr;
	}

	/**
	 * Renders the given HTML string onto (not into) an DOM element.
	 * Does nearly the equivelent of $.replaceWith() or changing the element's
	 * outerHTML.
	 *
	 *             http://bugs.jquery.com/ticket/8142#comment:6
	 *             ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	 * 'All of jQuery's insertion methods use a domManip function internally  to
	 * clean/process elements before and after they are inserted into  the  DOM.
	 * One of the things the domManip function  does  is  pull  out  any  script
	 * elements about to  be  inserted  and  run  them  through  an  "evalScript
	 * routine" rather than inject them with the rest of the  DOM  fragment.  It
	 * inserts the scripts separately, evaluates them,  and  then  removes  them
	 * from the DOM.
	 *
	 * 'I believe that  one  of  the  reasons  jQuery  does  this  is  to  avoid
	 * "Permission Denied" errors that  can  occur  in  Internet  Explorer  when
	 * inserting scripts under certain circumstances. It also avoids  repeatedly
	 * inserting/evaluating the  same  script  (which  could  potentially  cause
	 * problems) if it is within a containing element that you are inserting and
	 * then moving around the DOM.'
	 *
	 * @param {jQuery.<HTMLElement>} $element jQuery unit set containing the
	 *                                        DOM element we wish to render the
	 *                                        given html content onto.
	 * @param {string} html HTML content which will become give to the element.
	 */
	function renderOnto($element, html) {
		insertInnerHTMLWithScriptTags($element, html);
		var attr = mergeAttributes($element, jQuery(html));
		var name;
		for (name in attr) {
			if (attr.hasOwnProperty(name)) {
				$element.attr(name, attr[name]);
			}
		}
	}

	/**
	 * Removes the given chainback instance from its cached location.
	 *
	 * @param {Chainback} chainback Instance to remove from cache.
	 */
	function decache(chainback) {
		if (chainback._constructor.__gcncache__[chainback.__gcnhash__]) {
			delete chainback._constructor.__gcncache__[chainback.__gcnhash__];
		}
	}

	/**
	 * Maps constructs, that were fetched via the Rest API, to a hashmap, using
	 * their keyword as the keys.
	 *
	 * @param {object<string, object>} constructs Consturcts mapped against
	 *                                            their id.
	 * @return {object<string, object>} Constructs mapped against their keys.
	 */
	function mapConstructs(constructs) {
		if (!constructs) {
			return {};
		}
		var map = {};
		var constructId;
		for (constructId in constructs) {
			if (constructs.hasOwnProperty(constructId)) {
				map[constructs[constructId].keyword] = constructs[constructId];
			}
		}
		return map;
	}

	GCN.uniqueId = uniqueId;
	GCN.escapePropertyName = escapePropertyName;
	GCN.renderOnto = renderOnto;
	GCN.decache = decache;
	GCN.mapConstructs = mapConstructs;

}(GCN));
