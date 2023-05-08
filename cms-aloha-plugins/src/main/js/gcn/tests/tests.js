/*global testOrder: true, module: true, asyncTest: true, ok: true, start: true, equal: true */
(function () {
	'use strict';

	var GCN;
	var Tags;
	var Links;
	var plugin;
	var PAGE_ID = 115;
	var CONSTRUCT_ID = 67;

	function login(next) {
		module('Setup');
		var proceed = function () {
			start();
			next();
		};
		asyncTest('Login + page load', function () {
			GCN.login('node', 'node', function (success) {
				ok(true, 'Login successful');
				plugin.page = GCN.page(PAGE_ID, function () {
					ok(true, 'Page loaded');
					proceed();
				}, proceed);
			}, proceed);
		});
	}

	var tagName;

	function createTag(next) {
		module('Tags');
		var proceed = function () {
			start();
			next();
		};
		asyncTest('Create tag', function () {
			plugin.createTag(CONSTRUCT_ID, false, function (html, tag, data, callback) {
				Tags.append('#blackhole', tag, data, callback);
				equal(
					tag.prop('active'),
					true,
					'All created tags should be automatically activated'
				);
				tagName = tag.prop('name');
				proceed();
			});
		});
	}

	function savedPage(next) {
		module('Tags');
		var proceed = function () {
			start();
			next();
		};
		asyncTest('Open tag', function () {
			plugin.page.tag(tagName).prop('active', false);
			plugin.savePage({onsuccess: function (a, b) {
				ok(true, 'Page saved');
				plugin.page._clearCache();
				plugin.page = GCN.page(PAGE_ID, function (page) {
					equal(
						page.tag(tagName).prop('active'),
						false,
						'Tag should have been deactivated'
					);
					proceed();
				}, proceed);
			}});
		});
	}

	Aloha.require([
		'/CNPortletapp/DEV/gcnjsapi/debug/gcnjsapi.js',
		'gcn/gcn-plugin',
		'gcn/gcn-tags',
		'gcn/gcn-links'
	], function (gcn, gcnPlugin, gcnTags, gcnLinks) {
		GCN = gcn;
		Tags = gcnTags;
		Links = gcnLinks;
		plugin = gcnPlugin;
		GCN.sub('error-encountered', function (error) {
			ok(false, error.toString());
		});
	});

	testOrder.push(
		login,
		createTag,
		savedPage
	);
}());
