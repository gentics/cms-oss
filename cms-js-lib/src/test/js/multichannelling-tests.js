/*global testOrder: true, module: true, asyncTest: true, ok: true, start: true, equal: true, console: true, GLOBAL_IDS: true, GCN: true */
(function () {

	'use strict';

	/**
	 * GCN5 Demo/Corporate Blog/ 
	 * @type {number}
	 * @const
	 */
	var INHERITED_FOLDER_ID = 7;

	/**
	 * Id of a page that is inherited
	 * @type {number}
	 * @const
	 */
	var INHERITED_PAGE_ID = 63;

	/**
	 * Node id 0.
	 *
	 * @type {number}
	 * @const
	 */
	var MASTER_NODE = parseInt(GLOBAL_IDS.NODE, 10);

	/**
	 * Node id 3.
	 *
	 * @type {number}
	 * @const
	 */
	var CHANNEL_NODE = parseInt(GLOBAL_IDS.NODE3, 10);

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
	 * Test for expected failure for attempts to localize object when no channel is set
	 */
	function nochannel(next) {
		GCN.channel(false);
		asyncTest('no channel set', function () {
			GCN.page(GLOBAL_IDS.PAGE).localize(function () {
				ok(
					false,
					'Should not be possible to localize without setting a channel id.'
				);
				start();
				next();
			}, function (error) {
				ok(
					error,
					'Should not be possible to localize without setting a channel id.'
				);
				equal(
					error.code,
					'NO_CHANNEL_ID_SET',
					'Correct error should be given.'
				);
				start();
				next();
				return false;
			});
		});
	}

	// Make sure attempts to localized objects that are not inherited will
	// throw an error
	function notinherited(next) {
		asyncTest('not inherited object', function () {
			GCN.channel(MASTER_NODE);
			GCN.page(GLOBAL_IDS.PAGE).localize(function () {
				ok(
					false,
					'Should not be possible to localize an object that is not inherited.'
				);
				start();
				next();
			}, function (error) {
				ok(
					error,
					'Should not be possible to localize an object that is not inherited.'
				);
				equal(
					error.code,
					'CANNOT_LOCALIZE',
					'Correct error should be given.'
				);
				start();
				next();
				return false;
			});
		});
	}

	// localize a page
	function localize(next) {
		var reportError = function (error) {
			ok(false, error.toString());
			start();
			next();
			return false;
		};
		asyncTest('localizing inherited object', function () {
			GCN.channel(CHANNEL_NODE);
			// Localize page 63 which is inherited in node 3
			GCN.page(INHERITED_PAGE_ID).localize(function (page) {
				ok(
					page,
					'Should be possible to localize an object that is inherited.'
				);
				ok(
					page.id() !== INHERITED_PAGE_ID,
					'Localized id should be different to inherited page'
				);
				// Check that page.folder() receives the localized
				page.folder(function (folder) {
					equal(
						CHANNEL_NODE,
						folder._channel,
						'_channel property should be propagated to the localized object\'s folder.'
					);
					// Check that a new chain should have a brand new _channel
					// propagated through its linked objects.
					GCN.channel(false);
					GCN.folder(folder.id(), function (innerFolder) {
						ok(
							!innerFolder._channel,
							'Folder which is not derived from the localized object should not have a _channel property set.'
						);
						start();
						next();
					}, reportError);
				}, reportError);
			}, reportError);
		});
	}

	var rand = Math.random().toString(16);
	var inheritedTagContent;

	// Localizes a page and changes its content tag.
	function changeTags(next) {
		var reportError = function (error) {
			ok(false, error.toString());
			start();
			next();
			return false;
		};
		asyncTest('Change local tags', function () {
			// First get the master page's tag
			GCN.channel(false);
			GCN.page(INHERITED_PAGE_ID).tag('content', function (inheritedTag) {
				// Then get a localized page's tag
				GCN.channel(CHANNEL_NODE);
				GCN.page(INHERITED_PAGE_ID) // Gets inherited page (which should be the same as the master page)
					.localize()
					.tag(
						'content',
						function (localizedTag) {
							inheritedTagContent = inheritedTag.part('text');
							// inherited tag and localized tag should contain the
							// same content at this point
							equal(
								inheritedTagContent,
								localizedTag.part('text'),
								'Localized tag should be copy of inherited tag'
							);
							// change the localized version, and save it
							localizedTag.part('text', rand);
							localizedTag.save(function () {
								start();
								next();
							}, reportError);
						},
						reportError
					);
			}, reportError);
		});
	}

	// Compare the values of an inherited tag to that of its localized
	// copy, they should differ correctly.
	function compareTags(next) {
		var reportError = function (error) {
			ok(false, error.toString());
			start();
			next();
			return false;
		};
		asyncTest('Change local and inherited tags', function () {
			var page;
			for (page in GCN.PageAPI.__gcncache__) {
				if (GCN.PageAPI.__gcncache__.hasOwnProperty(page)) {
					GCN.PageAPI.__gcncache__[page].clear();
				}
			}
			// Get the master version tag
			GCN.channel(false);
			GCN.page(INHERITED_PAGE_ID).tag('content', function (inheritedTag) {
				// Get the localized version
				GCN.channel(CHANNEL_NODE);
				GCN.page(INHERITED_PAGE_ID)
					.localize()
					.tag('content', function (localizedTag) {
						// Check that we did not overwrite the master version
						equal(
							inheritedTag.part('text'),
							inheritedTagContent,
							'Localized tag should be unchanged'
						);
						// Check that the localized version was updated however
						equal(
							localizedTag.part('text'),
							rand,
							'Localized tag should be changed to ' + rand
						);
						start();
						next();
					}, reportError);
			}, reportError);
		});
	}

	// Test unlocalize().
	// Make sure that we can reveal inherited content
	function unlocalize(next) {
		var reportError = function (error) {
			ok(false, error.toString());
			start();
			next();
			return false;
		};
		asyncTest('unlocalize()', function () {
			GCN.channel(CHANNEL_NODE);
			GCN.page(INHERITED_PAGE_ID).unlocalize(function () {
				GCN.page(INHERITED_PAGE_ID).tag('content', function (tag) {
					equal(
						tag.part('text'),
						inheritedTagContent,
						'tag contents should return to being the inherited content'
					);
					start();
					next();
				}, reportError);
			}, reportError);
		});
	}

	function localizeFolder(next) {
		asyncTest('Localize a folder', function () {
			GCN.channel(CHANNEL_NODE);
			GCN.folder(INHERITED_FOLDER_ID).localize(function (folder) {
				ok(
					folder,
					'Should be possible to localize the folder that is inherited.'
				);
				ok(
					folder.id() !== INHERITED_FOLDER_ID,
					'Localized id should be different to inherited folder'
				);
				start();
				next();
			}, function (error) {
				ok(false, error);
				start();
				next();
				return false;
			});
		});
	}

	// Unlocalizes INHERITED_FOLDER_ID folder in case it has been localized
	function resetFolder(next) {
		var cb = function () {
			GCN.channel(false);
			next();
			return false;
		};
		GCN.channel(CHANNEL_NODE);
		GCN.folder(INHERITED_FOLDER_ID).unlocalize(cb, cb);
	}

	// Unlocalizes INHERITED_PAGE_ID page in case it has been localized
	function resetPage(next) {
		var cb = function () {
			GCN.channel(false);
			next();
			return false;
		};
		GCN.channel(CHANNEL_NODE);
		GCN.page(INHERITED_PAGE_ID).unlocalize(cb, cb);
	}

	testOrder.push(
		login,
		resetPage,
		resetFolder,
		function (next) {
			module('localize() / unlocalize()');
			next();
		},
		nochannel,
		notinherited,
		localize,
		resetPage,
		changeTags,
		compareTags,
		unlocalize,
		localizeFolder,
		resetPage,
		resetFolder
	);

}());
