/*global test: true, equal: true, asyncTest: true, start: true, ok: true, next: true, module: true, testOrder: true*/
(function () {

	'use strict';

	var GLOBAL_IDS = window.GLOBAL_IDS;

	/**
	 * this will be switched to false for exposeAPI
	 */
	var FAIL_ON_UNKNOWN_ARGUMENT = true;
	var FAIL_COUNT_UNKNOWN_ARGUMENT = 0;

	//bind an error handler (that will let the test fail)
	GCN.sub('error-encountered', function (error) {
		if (error.code === 'UNKNOWN_ARGUMENT' && !FAIL_ON_UNKNOWN_ARGUMENT) {
			// part of testExposeAPI
			ok(true, 'failed correctly because of unknown arguments');
			FAIL_COUNT_UNKNOWN_ARGUMENT++;
			return;
		}

		ok(false, error.toString());
	});

	function countObjectsInCache(clazz) {
		var cache = clazz.__gcncache__;
		var count = 0;
		var i;

		for (i in cache) {
			if (cache.hasOwnProperty(i)) {
				++count;
			}
		}

		return count;
	}

	/**
	 * This is actually not a real test, but will just authenticate the GCN JS
	 * Lib so we don't need to re-authenticate for each and every test
	 */
	function testLogin(next) {
		module('Login');

		asyncTest('Login', function () {
			GCN.login('node', 'node', function (success) {
				ok(true, 'login successful');
				start();
				next();
			});
		});
	}

	// chainback api tests
	function testAPI(next) {
		module('API');

		test('API', function () {
			var page = GCN.page(1);
			equal(page._data.id, 1, 'Page ID stored');

			// equal(page._ids[0], 1, 'Page ID stored');

			equal(page.__chainbacktype__, 'PageAPI', 'Correct API returned');
			equal(jQuery.type(page.tag), 'function', 'Correct API method available');

			var tag = page.tag('content');
			equal(tag.__chainbacktype__, 'TagAPI', 'Correct API returned');
			equal(jQuery.type(tag.render), 'function', 'Correct API method available');

			// step by step chainback tests
			var node5 = GCN.node(5);
			equal(node5._data.id, 5, 'id stored');
			equal(node5.__chainbacktype__, 'NodeAPI', 'NodeAPI returned');

			var node5Folder = node5.folder();

			if (!node5Folder) {
				ok(false, 'Could not get folder of node5 via ' +
					'`node5.folder()\'');

				start();
				next();

				return;
			}

			equal(node5Folder.__chainbacktype__, 'FolderAPI', 'FolderAPI returned');

			var page13 = node5Folder.page(13);
			equal(page13._data.id, 13, 'id stored');
			equal(page13.__chainbacktype__, 'PageAPI', 'PageAPI returned');

			var tagHeader = page13.tag("header");
			equal(tagHeader._data.id, "header", 'id stored');
			equal(tagHeader.__chainbacktype__, 'TagAPI', 'TagAPI returned');

			var tagHeaderParent = tagHeader.parent();
			equal(tagHeaderParent._data.id, 13, 'correct page id restored');
			equal(tagHeaderParent.__chainbacktype__, 'PageAPI', 'PageAPI returned');

			var template144 = GCN.template(144);
			equal(template144._data.id, 144, 'id stored');
			equal(template144.__chainbacktype__, 'TemplateAPI', 'TemplateAPI returned');

			// chained chainback tests :)
			var folder55 = GCN.node(34).folder().folder(55);
			equal(folder55._data.id, 55, 'id stored');
			equal(folder55.__chainbacktype__, 'FolderAPI', 'FolderAPI returned');

			var page89 = GCN.node(34).folder().folder(55).page(89);
			equal(page89._data.id, 89, 'page id stored');
			equal(page89.__chainbacktype__, 'PageAPI', 'PageAPI returned');

			var tagContentParent = GCN.node(34).folder().folder(55).page(89)
			                          .tag('content').parent();
			equal(tagContentParent._data.id, 89, 'page id stored');
			equal(tagContentParent.__chainbacktype__, 'PageAPI', 'PageAPI returned');

			// test NodeAPI for correct methods
			jQuery(["folder"]).each(function (i, m) {
				equal(jQuery.type(node5[m]), 'function', 'node must have a ' +
						m + '() method');
			});

			jQuery(["tag", "tags", "page", "preview", "online", "offline"])
				.each(function (i, m) {
					equal(jQuery.type(node5[m]), 'undefined',
						'node must NOT have a ' + m + '() method');
				});

			// test FolderAPI for correct methods
			jQuery(["parent", "save", "remove", "page", "pages", "createPage",
			         /* "template", "templates", "createTemplate" */
					 "folder", "folders", "createFolder"]).each(function (i, m) {
				equal(jQuery.type(node5Folder[m]), 'function',
					'folder must have a ' + m + '() method');
			});

			jQuery(["preview", "online", "takeOffline"]).each(function (i, m) {
				equal(jQuery.type(node5Folder[m]), 'undefined',
					'folder must NOT have a ' + m + '() method');
			});

			// test PageAPI for correct methods
			jQuery(["remove", "unlock", "takeOffline", "preview", "publish",
			        "save", "folder", "template"]).each(function (i, m) {
				equal(jQuery.type(page13[m]), 'function',
					'page must have a ' + m + '() method');
			});

			jQuery(["page", "createPage"]).each(function (i, m) {
				equal(jQuery.type(page13[m]), 'undefined',
					'page must NOT have a ' + m + '() method');
			});

			// test TagAPI for correct methods
			jQuery(["remove", "parent", "part", "render", "edit", "save"])
				.each(function (i, m) {
					equal(jQuery.type(tagHeader[m]), 'function',
						'tag must have a ' + m + '() method');
				});

			jQuery(["createPage", "node", "folder", "template"]).each(function (i, m) {
				equal(jQuery.type(tagHeader[m]), 'undefined',
					'tag must NOT have a ' + m + '() method');
			});

			// test TemplateAPI for correct methods
			jQuery([ "remove", "save", "folder" ]).each(function (i, m) {
				equal(jQuery.type(template144[m]), 'function',
					'template must have a ' + m + '() method');
			});

			jQuery([ "createPage", "publish", "template" ]).each(function (i, m) {
				equal(jQuery.type(template144[m]), 'undefined',
					'template must NOT have a ' + m + '() method');
			});
		});

		start();
		next();
	}

	/**
	 * test expose API function
	 */
	function testExposeAPI(next) {
		test('exposeAPI', function () {
			// for now tests should not fail on unknown arguments
			FAIL_ON_UNKNOWN_ARGUMENT = false;

			var p;
			// id
			p = GCN.page(1);
			equal(p._data.id, 1, 'Page id was set correctly');

			// id, settings, settings - should fail!
			GCN.page(2, {}, {});

			// id, success, error and another function - should fail!
			GCN.page(3, function () {}, function () {
				return false;
			}, function () {});

			// id, id - should fail!
			GCN.page(4, 5);

			// id, success, error, settings - should work!
			p = GCN.page(6, function () {}, function () {
				return false;
			}, {});
			equal(p._data.id, 6, 'Page id was set correctly');

			// id, success, settings, error - should work!
			p = GCN.page(7, function () {}, {}, function () {
				return false;
			});
			equal(p._data.id, 7, 'Page id was set correctly');

			// setting both id and array of ids - should fail!
			GCN.page(8, [ 1, 2, 3 ]);

			equal(FAIL_COUNT_UNKNOWN_ARGUMENT, 4, 'there should have been 3 ' +
				'errors for unknown arguments');

			// from now on test should continue to fail on unknown arguments
			FAIL_ON_UNKNOWN_ARGUMENT = true;

			next();
		});
	}

	// test cache clearing
	function testClearCache(next) {
		module('clear page cache');

		asyncTest('clear page cache', function () {
			GCN.page(GLOBAL_IDS.PAGE, function (page) {
				equal(jQuery.type(page._data.id), 'number',
					'Cache for page was built correctly');

				equal(page._fetched, true, 'fetched flag is set');

				// now clear the cache
				page.clear();

				var pageId = page.id();

				// data should be gone now
				equal(typeof page._data, 'object',
					'page._data should still be there after clear()');

				equal(page._data.id, pageId,
					'the page\'s id should have been maintained even after clear()');

				equal(page._fetched, false,
					'page._fetched should have been set to false');

				equal(JSON.stringify(page._data), '{"id":' + pageId + '}',
					'page._data should only contain the id of the content object');

				start();
				next();
			}, function (error) {
				start();
				next();
			});
		});
	}

	// chainback ajax test
	function testAsync(next) {
		module('async test');

		asyncTest('ASYNC', function () {
			GCN.page(GLOBAL_IDS.PAGE, function (page) {
				ok(true, 'Callback was invoked from PageAPI.init');

				equal(page.__chainbacktype__, 'PageAPI', 'A PageAPI object was ' +
					'given in the callback');

				equal(jQuery.type(page._data.id), 'number', 'Page data is correct');

				start();
				next();
			});

			GCN.page(GLOBAL_IDS.PAGE).tag('content', function (tag) {

			});
		});
	}

	// Chainback cache tests
	function testCache(next) {
		module('Cache test');

		test('Cache', function () {
			var j;
			var i;
			var name;
			var cached;
			var count;

			var all = ['TagAPI', 'PageAPI', 'TemplateAPI', 'FolderAPI',
				'NodeAPI'];

			j = all.length;
			while (j) {
				GCN[all[--j]].__gcncache__ = {};
			}

			GCN.page(GLOBAL_IDS.PAGE).tag('0');
			GCN.page(GLOBAL_IDS.PAGE).tag('1');
			GCN.page(GLOBAL_IDS.PAGE).tag('2');

			// `TagAPI.__gcncache__' should contain 3 objects

			count = countObjectsInCache(GCN.TagAPI);

			equal(count, 3, 'We should have 3 objects in the ' +
				'\'GCN.TagAPI.__gcncache__\'');

			// `TagAPI.__gcncache__' should contain 6 objects

			GCN.folder(GLOBAL_IDS.FOLDER).tag('0');
			GCN.folder(GLOBAL_IDS.FOLDER).tag('1');
			GCN.folder(GLOBAL_IDS.FOLDER).tag('2');

			count = countObjectsInCache(GCN.TagAPI);

			equal(count, 6, 'We should have 6 objects in the ' +
				'\'GCN.TagAPI.__gcncache__\'');

			var objs = ['node', 'folder', 'page', 'template'];
			for (j = 0; j < objs.length; j++) {
				for (i = 0; i < 3; i++) {
					GCN[objs[j]](i);
				}
			}

			// Iterate over each api's cache

			var apis = ['NodeAPI', 'FolderAPI', 'PageAPI', 'TemplateAPI'];

			for (j = 0; j < apis.length; j++) {
				name = apis[j];
				for (i = 0; i < 3; i++) {
					cached = GCN[name].__gcncache__[name + ':0/' + i];
					equal(
						jQuery.type(cached),
						'object', 'Key "' + name + ':0/' + i + '" should be in `' + name + '.__gcncache__\'.'
					);
					equal(
						cached.__gcnhash__, name + ':0/' + i,
						'`__gcnhash__\' should match key in `__gcncache__\''
					);
					cached.clear();
					equal(
						jQuery.type(GCN[name].__gcncache__[name + ':0/' + i]),
						'undefined',
						'Check that `' + name + ':' + i + '\''
					);
				}
			}

			next();
		});
	}

	testOrder.push(
		testLogin,
		testExposeAPI,
		testAPI,
		testClearCache,
		testAsync,
		testCache
	);

}());
