/*global testOrder: true, module: true, asyncTest: true, ok: true, start: true, equal: true, setTimeout: true */
(function () {

	'use strict';

	var GLOBAL_IDS = window.GLOBAL_IDS;

	GCN.sub('error-encountered', function (error) {
		if (error.code === 'NOTFOUND') {
			ok(false, error.toString() + ' Consider changing the ' +
				'`GLOBAL_IDS.PAGE\' variable in tag-test.js');
		} else if (error.code === 'TAG_NOT_FOUND') {
			ok(error.data._name === 'i-do-not-exist',
				'Expected TAG_NOT_FOUND error. Received ' + error.data._name);
		} else if (error.code === 'UNFETCHED_OBJECT_ACCESS') {
			ok(error.data._name === 'i-do-not-exist',
				'Expected UNFETCHED_OBJECT_ACCESS error for ' + error.data._name);
		} else if (error.code === 'PART_NOT_FOUND') {
			ok(!!/i-do-not-exist/.test(error.message),
				'Expected PART_NOT_FOUND error');
		} else {
			ok(false, error.toString());
			throw error;
		}
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

	function getConstructs(next) {
		asyncTest('Getting constructs', function () {
			GCN.page(GLOBAL_IDS.PAGE)
				.node()
				.constructs(function (constructs) {
					equal(jQuery.type(constructs), 'object',
						'A object containing key-value pairs of tags should' +
						'be passed to `node().constructs()\'');
					equal(jQuery.type(constructs.gtxalohapagelink), 'object',
						'Check if "Aloha Link" exists in the constructs map.');
					start();
					next();
				});
		});
	}

	function getConstructCategories(next) {
		asyncTest('Getting constructs categories', function () {
			GCN.page(GLOBAL_IDS.PAGE)
				.node()
				.constructCategories(function (constructCategories) {

					equal(jQuery.type(constructCategories.categories), 'object',
						'A object containing key-value pairs of construct categories should' +
						'be passed to `node().constructs()\'');

					equal(jQuery.type(constructCategories.categorySortorder), 'array',
						'The constructCategories field should contain an sorted array with category names');

					start();
					next();
				});
		});
	}

	/**
	 * This test will test whether a tag can be created on the client.
	 *
	 * A link will be created and stored in the cache and retrieved locally
	 * without ever storing it in the server.
	 */
	function createTagOnClient(next) {
		asyncTest('Create local link', function () {
			GCN.page(GLOBAL_IDS.PAGE, function (page) {
				var tag = page.createTag({
					keyword: GCN.settings.MAGIC_LINK,
					magicValue: 'client-link'
				});
				deepEqual(
					tag._chain,
					page,
					'New tag should belong to page from which it was created'
				);
				start();
				next();
			});
		});
	}

	// Test creating link tags
	function createTag(next) {
		asyncTest('Create tag', function () {
			GCN.page(GLOBAL_IDS.PAGE)
				.createTag({
					keyword: GCN.settings.MAGIC_LINK,
					magicValue: 'link-1'
				}, function (tag) {
					equal(
						jQuery.type(tag),
						'object',
						'The received object should be an object'
					);
					if (!tag) {
						return;
					}
					equal(
						jQuery.type(tag._data),
						'object',
						'The received object should be an object'
					);
					if (!tag._data) {
						return;
					}
					equal(
						tag._data.type,
						'CONTENTTAG',
						'Expecting type to be "CONTENTTAG"'
					);
					ok(tag._fetched, '`_fetched\' value should be `true\'');
					equal(
						tag.part('text'),
						'link-1',
						'Magic value should have set to "link-1"'
					);
					start();
					next();
				});
		});
	}

	function createAndRender(next) {
		jQuery('<div><div id="test1"></div><div id="test2"></div></div>')
			.appendTo('body');

		asyncTest('Create & render', function () {
			GCN.page(GLOBAL_IDS.PAGE)
				.createTag({
					keyword: GCN.settings.MAGIC_LINK,
					magicValue: 'link-2'
				})
				.render('#test1')
				.render('#test2')
				.render(function (html) {
					setTimeout(function () {
						equal(jQuery.type(html), 'string',
							'Check that passed is HTML string');

						equal(jQuery('#test1 a').length, 1, 'There should be' +
							' one link rendered inside div#test1');

						equal(jQuery('#test2 a').length, 1, 'There should be' +
							'one link rendered inside div#test2');

						jQuery('#test1,#test2').remove();

						start();
						next();
					}, 500);
				});
		});
	}

	/**
	 * Test for the SELECT part type which is special.
	 */
	function partTypes(next) {
		asyncTest('SELECT Parttype', function () {
			GCN.page(GLOBAL_IDS.PAGE, function (page) {
				var sel = page.tag('select').part('sel');
				if (!sel) {
					ok(false, 'Could not find part "sel" in tag "select"');
				} else {
					equal(jQuery.type(sel), 'object', 'Check SELECT parttype');
					equal(jQuery.type(sel.datasourceId), 'number',
						'Check properties of SELECT parttype');

					var oldId = sel.datasourceId;
					++sel.datasourceId;

					page.tag('select').part('sel', sel);

					equal(page.tag('select').part('sel').datasourceId, ++oldId,
						'Check that SELECT parttype can be modified correctly');
				}

				start();
				next();
			}, function (error) {
				start();
				next();
			});
		});
	}

	/**
	 * Checks following part types:
	 * STRING (tested)
	 * RICHTEXT (tested)
	 * BOOLEAN
	 * FILE
	 * IMAGE (tested)
	 * FOLDER
	 * PAGE (tested)
	 */
	function tagParts(next) {
		asyncTest('Tag parts', function () {
			var page = GCN.page(GLOBAL_IDS.PAGE);

			page.tags(function () {
				// Check STRING
				var alt = page.tag('image1').part('alt');
				equal(jQuery.type(alt), 'string',
					'Check that `part("alt")\' returns a string');

				// Check IMAGE
				var src = page.tag('image1').part('src');
				var type = jQuery.type(src);
				ok(type === 'string' || type === 'number', 'Check that ' +
					'`part("src") returns number of string. Returned: "' +
					type + '"');

				// Check RICHTEXT
				var text = page.tag('content').part('text');
				equal(jQuery.type(text), 'string',
					'Check that `part("text")\' returns a string');

				// Check PAGE
				var url = page.tag('gtxalohapagelink1').part('url');
				equal(jQuery.type(url), 'string',
					'Check that `part("url")\' returns a string');

				page.tag('content').part('text', 'TOUCHED');
				equal(page.tag('content').part('text'), 'TOUCHED',
					'Expecting modified string value for `content\' tag');

				// TAG_NOT_FOUND and UNFETCHED_OBJECT_ACCESS
				var fail = page.tag('i-do-not-exist').part('whatever');

				// PART_NOT_FOUND
				var fail2 = page.tag('content').part('i-do-not-exist');

				start();
				next();
			});
		});
	}

	/**
	 * Tests saving
	 */
	function tagSave(next) {
		asyncTest('Save tag', function () {
			GCN.page(GLOBAL_IDS.PAGE, function (page) {
				page.createTag({
					keyword: GCN.settings.MAGIC_LINK,
					magicValue: 'client-link'
				}).save(function (tag) {
					equal(typeof tag, 'object', 'Callback received object');
					equal(tag.__chainbacktype__, 'TagAPI', 'Callback should receive Tag API object');
					start();
					next();
				});
			});
		});
	}

	/**
	 * Tests that the save() method of content objects properly receives an optional settings
	 * object for a first parameter.
	 * Will pass an unlock setting during page save to see if the page really does get locked and
	 * unlocked in the backend. If it does then the save() method is behaving as expected.
	 */
	function testSaveWithSettings(next) {
		asyncTest('Save tag (settings)', function () {
			GCN.page(GLOBAL_IDS.PAGE, function (page) {
				page.createTag({
					keyword: GCN.settings.MAGIC_LINK,
					magicValue: 'client-link'
				}).save(function (tag) {
					page.clear();
					GCN.page(GLOBAL_IDS.PAGE, function (page) {
						equal(page.prop('locked'), true, 'Page should be locked');
						page.createTag({
							keyword: GCN.settings.MAGIC_LINK,
							magicValue: 'client-link'
						}).save({unlock: true}, function (tag) {
							page.clear();
							GCN.page(GLOBAL_IDS.PAGE, function (page) {
								equal(page.prop('locked'), false, 'Page should be unlocked');
								start();
								next();
							});
						});
					});
				});
			});
		});
	}

	testOrder.push(
		login,
		createTagOnClient,
		getConstructs,
		getConstructCategories,
		createTag,
		createAndRender,
		partTypes,
		tagParts,
		tagSave
	);

}());
