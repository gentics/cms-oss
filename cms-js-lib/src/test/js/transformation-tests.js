/*global testOrder: true, module: true, asyncTest: true, ok: true, start: true, equal: true, console: true, setTimeout: true */
(function () {

	'use strict';

	var nodeNotationRegExp = /<node ([a-z0-9_\-\.]+?)>/gim;

	GCN.sub('error-encountered', function (error) {
		console.warn(error.toString());
		ok(false, error.toString());
	});

	function login(next) {
		GCN.login('node', 'node', function (success) {
			if (success) {
				next();
			} else {
				ok(false, 'Should be able to login as "node"');
			}
		});
	}

	/**
	 * Will find all tags in a page object, build a string containing "<node
	 * tagname>" notation, and decode this string.  This resulting output
	 * should have all <node> notations replaces with standard HTML.
	 */
	function decode(next) {
		asyncTest('decoding', function () {
			GCN.page(window.GLOBAL_IDS.PAGE, function (page) {
				var strBuilder = [];
				var tags = page._data.tags;
				var tagsNum = 0;
				var name;
				// Make a string of all page tags in <node *> notation form
				for (name in tags) {
					if (tags.hasOwnProperty(name)) {
						++tagsNum;
						strBuilder.push('<node ', name, '>');
					}
				}
				var str = strBuilder.join('');
				var old_matches = str.match(nodeNotationRegExp);
				equal(
					old_matches.length,
					tagsNum,
					'We should have ' + tagsNum + ' matches for "' + nodeNotationRegExp + '"'
				);
				// assert (old_matches > 0)
				page.decode(str, function (html) {
					var new_matches = html.match(nodeNotationRegExp);
					ok(
						!new_matches,
						'All occurances of "<node *>" should have be ' + 'replaced'
					);
					start();
					next();
				});
			}, function (error) {
				console.warn(error.toString(), error);
				start();
				next();
				return false;
			});
		});
	}

	// Test encoding of tags
	function encode(next) {
		asyncTest('encoding', function () {
			GCN.page(window.GLOBAL_IDS.PAGE, function (page) {
				var original = page.tag('content').part('text');
				page.decode(original, function (decoded) {
					var encoded = page.encode(jQuery('<div>' + decoded + '</div>'));
					equal(
						original,
						encoded,
						'Encoded value should be identical to the original value before decoding'
					);
					start();
					next();
				});
			});
		});
	}

	function editing(next) {
		asyncTest('editing', function () {
			jQuery('<div id="test-content"></div><div id="test-content-2"></div>')
				.appendTo('body');

			GCN.page(window.GLOBAL_IDS.PAGE2).tag('content').edit('#test-content');

			GCN.page(window.GLOBAL_IDS.PAGE2).tag('content')
				.edit(jQuery('#test-content-2'), function (html) {
					setTimeout(function () {
						var tag = GCN.page(window.GLOBAL_IDS.PAGE2).tag('content');
						var parent = tag && tag.parent();
						var id;
						var hasEditable = false;

						for (id in parent._editables) {
							if (parent._editables.hasOwnProperty(id)) {
								hasEditable = true;
							}
						}

						ok(hasEditable, 'The `_editables\' hash set of the ' +
							'received tag\'s parent should contain at least one ' +
							'editable');

						start();
						next();
					}, 2000);
				});
		});
	}

	// Check that the edit() function will place the correct content into the dom
	function editingCheck(next) {
		asyncTest('editingCheck', function () {
			GCN.page(window.GLOBAL_IDS.PAGE2).tag('content').edit(function (html, tag) {
				var renderedHTML = jQuery(html).html();

				equal(jQuery('#test-content').html(), renderedHTML, 'Passing a ' +
					'string to the `edit()\' method should have placed the ' +
					'object in the DOM');

				equal(jQuery('#test-content-2').html(), renderedHTML, 'Passing a ' +
					'jQuery object to the `edit()\' method should have placed ' +
					'the object in the DOM');

				equal(jQuery.type(html), 'string', 'First argument should be a ' +
					 'string');

				equal(tag && tag.__chainbacktype__, 'TagAPI', 'Second argument ' +
					'should be a TagAPI');

				// Clean up
				jQuery('#test-content, #test-content-2').remove();

				start();
				next();
			});
		});
	}

	// Test subscribing to the "content-rendered" event
	// We will add some text to the content and later trace that it is there.
	function onRenderHandler(next) {
		GCN.sub('content-rendered', function (html, contentObject, callback) {
			equal(
				jQuery.type(html),
				'string',
				'`onRender\' handler should receive a string as the first argument'
			);
			equal(
				jQuery.type(callback),
				'function', '`onRender\' handler should receive a function as the second argument'
			);
			if (html) {
				html = 'TOUCHED_BY_ONRENDER_HANDLER' + html;
			}
			if (callback) {
				callback(html);
			}
		});
		next();
	}

	function onRenderTag(next) {
		asyncTest('onRender tag', function () {
			GCN.page(window.GLOBAL_IDS.PAGE).tag('content').render(function (html) {
				ok(/^TOUCHED_BY_ONRENDER_HANDLER/.test(html), 'Received html ' +
					'string should contain the string ' +
					'"TOUCHED_BY_ONRENDER_HANDLER" at the front.');

				start();
				next();
			});
		});
	}

	function onRenderPreview(next) {
		asyncTest('onRender preview', function () {
			GCN.page(window.GLOBAL_IDS.PAGE).preview(function (html) {
				ok(/^TOUCHED_BY_ONRENDER_HANDLER/.test(html), 'Received html ' +
					'string should contain the string ' +
					'"TOUCHED_BY_ONRENDER_HANDLER" at the front.');

				start();
				next();
			});
		});
	}

	testOrder.push(
		login,
		decode,
		encode,
		editing,
		editingCheck,
		onRenderHandler,
		onRenderTag,
		onRenderPreview
	);

}());
