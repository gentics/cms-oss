/*global testOrder: true, module: true, asyncTest: true, ok: true, start: true, equal: true, window: true */
(function () {
	'use strict';

	var GLOBAL_IDS = window.GLOBAL_IDS;

	GCN.sub('error-encountered', function (error) {
		ok(false, error.toString());
	});

	module('Chaining');

	function login(next) {
		asyncTest('login', function () {
			GCN.login('node', 'node', function (success) {
				ok(true, 'login successful');
				start();
				next();
			});
		});
	}

	/**
	 * Test chaining asynchronous methods
	 */
	function chaining(next) {
		var actual = [];
		var expect = ['tag.edit', 'tag.save1', 'page.save', 'tag.save2'];
		var count = 0;
		var proceed = function (action) {
			actual.push(action);
			if (++count === expect.length) {
				equal(actual.join('|'), expect.join('|'));
				start();
				next();
			}
		};
		asyncTest('chained calls', function () {
			GCN.page(GLOBAL_IDS.PAGE).clear();
			var tag = GCN.page(GLOBAL_IDS.PAGE).tag('content').edit('#tmp', function () {
				proceed('tag.edit');
			}, proceed);
			tag.save(function (tag) {
				proceed('tag.save1');
				tag.parent().save(function () {
					proceed('page.save');
				}, proceed).save(function () {
					proceed('tag.save2');
				}, proceed);
			}, proceed);
		});
	}

	/**
	 * Test recursive callbacks
	 */
	function callbacks(next) {
		var errorConstructor = GCN.createError().constructor;
		function proceed(arg) {
			if (arg instanceof errorConstructor) {
				ok(false, arg.toString());
			}
			start();
			next();
		}
		asyncTest('callbacks', function () {
			GCN.page(GLOBAL_IDS.PAGE).clear();
			var date = ' ' + new Date;
			var page = GCN.page(GLOBAL_IDS.PAGE);
			var tags = ['teasers', 'content'];
			(function next(pos) {
				if (pos) {
					var index = pos - 1;
					page.tag(tags[index], function (tag) {
						tag.part('text', tag.prop('name') + date);
						tag.save(function () {
							next(index);
						}, proceed);
					}, proceed);
				} else {
					GCN.page(GLOBAL_IDS.PAGE).clear();
					page = GCN.page(GLOBAL_IDS.PAGE);
					page.tags(function () {
						for (var i = 0; i < tags.length; i++) {
							var tag = page.tag(tags[i]);
							equal(tag.part('text'), tag.prop('name') + date);
							proceed();
						}
					}, proceed);
				}
			}(tags.length));
		});
	}

	testOrder.push(login, chaining, callbacks);
}());
