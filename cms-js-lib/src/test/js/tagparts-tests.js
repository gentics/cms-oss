/*global testOrder: true, module: true, asyncTest: true, ok: true, start: true, equal: true, window: true */
(function () {

	'use strict';

	// Custom error handler
	var handleError;

	var GCN = window.GCN;

	// Catch all errors
	GCN.sub('error-encountered', function (error) {
		if (handleError) {
			handleError(error);
		} else {
			ok(false, error.toString());
		}
	});

	function login(next) {
		module('Login');
		asyncTest('Login', function () {
			GCN.login('node', 'node', function (success) {
				ok(true, 'login successful');
				start();
				next();
			});
		});
	}

	function basic(next) {
		module('Basic tests for parts()');

		// Move on to next tests
		function proceed() {
			start();
			next();
		}

		asyncTest('part()', function () {
			GCN.page(GLOBAL_IDS.PAGE).tag('content', function (tag) {
				ok(tag.part('text') !== undefined,
					'part("text") should not return undefined');

				equal(tag.part('text', 'test'), 'test',
					'part("text", "test") should return string "test"');

				equal(tag.part('text'), 'test',
					'part("text") should now return string "test"');

				var page = tag.parent();
				page.save(function () {
					page._clearCache();
					GCN.page(page.id(), function (page) {
						equal(tag.part('text'), 'test',
							'part("text") should still return string "test"');
						proceed();
					}, proceed);
				}, proceed);
			}, proceed);
		});
	}

	// Test SELECT parts
	function select(next) {
		module('Select & Multiselect');

		// Move on to next tests
		function proceed() {
			start();
			next();
		}

		function fail(error) {
			ok(false, error.toString());
			proceed();
		}

		var PAGE = 138;
		var TAGNAME = 'selecttag1';
		var PARTNAME = 'select';

		asyncTest('select', function () {
			// Load tag
			GCN.page(PAGE).tag(TAGNAME, function (tag) {
				var part = tag.part(PARTNAME);

				if (!part) {
					return proceed();
				}

				// Because these remembered tag part's possible options and
				// values will be used as control values in assertions.
				var select_option_1 = JSON.stringify(part.options[0]);
				var select_option_2 = JSON.stringify(part.options[1]);
				var select_value_1 = part.options[0].value;
				var select_value_2 = part.options[1].value;
				var selected_options;

				// Because this relationship should be true:
				// tag.part(name) !== tag.part(name)
				ok(part !== tag.part(PARTNAME), 'part() should always return a (defensive) copy, and never the original');

				// Because this relationship should also be true:
				// tag.part(name).primitive === tag.part(name).primitive
				equal(tag.part(PARTNAME).datasourceId,
				      tag.part(PARTNAME).datasourceId,
				      'part("' + PARTNAME + '") should return different copies but with identical information');

				// Because every select part should have a few basic properties
				equal(jQuery.type(part), 'object', 'Part should be an object');
				equal(jQuery.type(part.options), 'array', 'Part should array property called "options"');
				equal(jQuery.type(part.selectedOptions), 'array', 'Part should array property called "selectedOptions"');
				equal(jQuery.type(part.datasourceId), 'number', 'Part should number property called "datasourceId"');

				// Select 1 option

				part = tag.part(PARTNAME, select_value_1);
				if (!part) {
					return proceed();
				}

				equal(jQuery.type(part), 'object', 'part() should always return an object');
				equal(jQuery.type(part.selectedOptions), 'array', 'Part should array property called "selectedOptions"');
				if (jQuery.type(part.selectedOptions) === 'array') {
					equal(part.selectedOptions.length, 1, 'selectedOptions of part should contain 1 item');
					if (part.selectedOptions.length) {
						equal(jQuery.type(part.selectedOptions[0]), 'object', 'selectedOptions should contain an object');
						equal(JSON.stringify(part.selectedOptions[0]),
							  select_option_1,
							  'selectedOptions should contain an object that matches one of the available options');
					}
				}

				// Select 0 options using []

				part = tag.part(PARTNAME, []);
				equal(part.selectedOptions.length, 0, 'selectedOptions of part should contain 0 items');

				part = tag.part(PARTNAME, [select_value_2]);
				equal(part.selectedOptions.length, 1, 'selectedOptions of part should contain 1 item');
				if (part.selectedOptions.length) {
					equal(part.selectedOptions[0].value, select_value_2,
					      'selectedOption should have "value" set to "' + select_value_2 + '"');
					equal(JSON.stringify(part.selectedOptions[0]), select_option_2,
					      'selectedOptions should contain an object that matches one of the available options');
				}

				// Select 0 options using null

				part = tag.part(PARTNAME, null);
				equal(part.selectedOptions.length, 0, 'selectedOptions of part should contain 0 items');

				handleError = function (error) { equal(error.code, 'VALUE_DOES_NOT_EXIST', 'Expecting error'); };
				tag.part(PARTNAME, 'non-existant-value-' + Math.random().toString(32).substr(2));
				handleError = null;

				// Select 2 options

				part = tag.part(PARTNAME, [select_value_1, select_value_2]);
				equal(part.selectedOptions.length, 2, 'selectedOptions should contain 2 items');
				if (2 === part.selectedOptions.length) {
					equal(part.selectedOptions[0].value, select_value_1,
					      '1st item in selectedOptions should contain "' + select_value_1 + '"');
					equal(part.selectedOptions[1].value, select_value_2,
					      '2st item in selectedOptions should contain "' + select_value_2 + '"');
				}

				part = tag.part(PARTNAME, part);
				if (2 === part.selectedOptions.length) {
					equal(part.selectedOptions[0].value, select_value_1,
					      'For backward compatibility, 1st item in selectedOptions should contain "' + select_value_1 + '"');
					equal(part.selectedOptions[1].value, select_value_2,
					      'For backward compatibility, 2st item in selectedOptions should contain "' + select_value_2 + '"');
				}

				// Save page with its modified select tag and then refetch it so
				// check that the parts have been saved correctly.

				handleError = function (error) { ok(false, error.toString()); proceed(); };
				tag.parent().save(function (page) {
					page._clearCache();
					GCN.page(page.id(), function (page) {
						handleError = null;
						part = page.tag(TAGNAME).part(PARTNAME);
						equal(part.selectedOptions.length, 2,
						      'selectedOptions of saved page should contain 2 objects');
						equal(part.selectedOptions[0].value, select_value_1,
						      'selectedOptions of saved page should have "value" set to "' + select_value_1 + '"');
						equal(part.selectedOptions[1].value, select_value_2,
						      'selectedOptions of saved page should have "value" set to "' + select_value_2 + '"');
						proceed();
					}, fail);
				}, fail);
			}, fail);
		});
	}

	testOrder.push(
		login,
		basic,
		select
	);

}());
