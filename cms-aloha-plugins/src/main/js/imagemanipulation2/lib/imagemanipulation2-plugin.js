/*global documents: true define: true */
define(
[ 
	'aloha/plugin',
	'./imagemanipulation2'
],
function (Plugin) {
	"use strict";

	/**
	 * Gentics Content.Node Imagemanipulation2
	 */
	return Plugin.create('imagemanipulation2', {

		dependencies: ['gcn-image'],

		init: function () {
			Aloha.require(["css!imagemanipulation2/css/imagemanipulation2"]);
		}

	});
});