/*global define: true */
/*!
* Aloha Editor
* Author & Copyright (c) 2011 Gentics Software GmbH
* aloha-sales@gentics.com
* Licensed under the terms of http://www.aloha-editor.com/license.html
*/

define(
[
	'jquery',
	'aloha/plugin',
	'ui/ui',
	'ui/button',
	'i18n!gcnfileupload/nls/i18n',
	'i18n!aloha/nls/i18n',
	'link/link-plugin',
	'gcn/gcn-plugin',
	'css!gcnfileupload/css/gcnfileupload.css',
	'gcnfileupload/jquery.form'
	],
function (jQuery, Plugin, Ui, Button, i18n, i18nCore) {
	"use strict";

	var $ = jQuery,
		GENTICS = window.GENTICS,
		Aloha = window.Aloha;

	return Plugin.create('gcnfileupload', {

		/**
		 * Configure the available languages (i18n) for this plugin
		 */
		languages: ['en', 'de'],

		/**
		 * List of accepted image extensions. All other files will be treaded
		 * as regular files. The filetype (image/file) determins the selected
		 * upload folder.
		 */
		imageExtensions: ['jpeg', 'jpg', 'png', 'gif'],

		/**
		 * Initialize the plugin
		 */
		init: function () {
			this.nsClasses = {
				overlay	: this.nsClass('overlay'),
				modal	: this.nsClass('modal'),
				block	: this.nsClass('modal-block'),
				header	: this.nsClass('modal-header'),
				left	: this.nsClass('modal-left-block'),
				right	: this.nsClass('modal-right-block'),
				inner	: this.nsClass('modal-inner'),
				divider	: this.nsClass('modal-divider'),
				btns	: this.nsClass('modal-btns'),
				clear	: this.nsClass('clear'),
				alt     : this.nsClass('alt'),
				link    : this.nsClass('link'),
				copy	: this.nsClass('copy'),
				desc	: this.nsClass('desc'),
				holder	: this.nsClass('holder'),
				cancel	: this.nsClass('cancel'),
				insert	: this.nsClass('insert'),
				preview	: this.nsClass('preview'),
				half	: this.nsClass('modal-half'),
				file	: this.nsClass('file'),
				form	: this.nsClass('form'),
				filename: this.nsClass('filename'),
				fileDescription : this.nsClass('fileDescription')
			};
			this.createButtons();

		},

		// Pseudo-namespace prefix
		ns : 'aloha-gcnfileupload',
		uid  :  +new Date(),
		// namespaced classnames
		nsClasses : {},


		/**
		 * Initialize the buttons
		 */
		createButtons : function () {
			var that = this;
			var lp = Aloha.require('link/link-plugin');

			// Button for adding a language markup to the current selection
			this._gcnFileUploadButton = Ui.adopt('gcnFileUpload', Button, {
				tooltip: i18n.t('button.gcnfileupload.tooltip'),
				icon: 'aloha-button aloha-button-gcnfileupload',
				click: function () {
					that.openModal();
				}
			});

			this._gcnFileUploadButton.show(false);
			Aloha.on('aloha-link-selected', function (event, rangeObject) {
				that._gcnFileUploadButton.show();
			});
			Aloha.on('aloha-link-unselected', function (event, rangeObject) {
				that._gcnFileUploadButton.show(false);
			});
		},

		supplant : function (str, obj) {
			return str.replace(/\{([a-z0-9\-\_]+)\}/ig, function (str, p1, offset, s) {
				var replacement = obj[p1] || str;
				return (typeof replacement === 'function') ? replacement() : replacement;
			});
		},

		/**
		 * Wrapper to all the supplant method on a given string, taking the
		 * nsClasses object as the associative array containing the replacement
		 * pairs
		 *
		 * @param {String} str
		 * @return {String}
		 */
		renderTemplate : function (str) {
			return (typeof str === 'string') ? this.supplant(str, this.nsClasses) : str;
		},

		/**
		 * Generates a selector string with this component's namepsace prefixed the
		 * each classname
		 *
		 * Usage:
		 *		nsSel('header,', 'main,', 'foooter ul')
		 *		will return
		 *		".aloha-myplugin-header, .aloha-myplugin-main, .aloha-mypluzgin-footer ul"
		 *
		 * @return {String}
		 */
		nsSel : function () {
			var strBldr = [], prx = this.ns;
			$.each(arguments, function () {
				strBldr.push('.' + (this === '' ? prx : prx + '-' + this));
			});
			return $.trim(strBldr.join(' '));
		},

		/**
		 * Generates s string with this component's namepsace prefixed the each
		 * classname
		 *
		 * Usage:
		 *		nsClass('header', 'innerheaderdiv')
		 *		will return
		 *		"aloha-myplugin-header aloha-myplugin-innerheaderdiv"
		 *
		 * @return {String}
		 */
		nsClass : function () {
			var strBldr = [], prx = this.ns;
			$.each(arguments, function () {
				strBldr.push(this === '' ? prx : prx + '-' + this);
			});
			return $.trim(strBldr.join(' '));
		},

		/**
		 * Brings the image modal into view
		 */
		openModal : function () {
			var that = this;
			var linkplugin = Aloha.require('link/link-plugin');
			this.editable = Aloha.activeEditable;
			this._rO = Aloha.Selection.getRangeObject();
			this._tO = linkplugin.hrefField.getTargetObject();
			var modal = this.getModal();
			modal.find('form#' + that.nsClass('form'))[0].reset();
			modal.show().css('opacity', 0);
			var overlay = $(this.nsSel('overlay'));

			overlay.show();

			var	w = modal.width(),
				h = modal.height(),
				win	= $(window);

			modal.css({
				opacity : 1,
				left    : (win.width() - w) / 2,
				top     : (win.height() - h) / 3
			});


		},

		/**
		 * Guess what, yes, this closes the opened modal
		 */
		closeModal : function () {
			var that = this;
			var modal = this.getModal();
			modal.hide();
			$(this.nsSel('overlay')).hide();
			modal.find('#' + this.nsClass('file')).val('');
			modal.find('#' + this.nsClass('insert')).html(i18n.t("button.gcnfileupload.text")).attr('disabled', false);
			if (typeof this.editable !== 'undefined') {
				this.editable.activate();
			}
		},

		/**
		 * Returns the filename of the selected file. Undefined will be
		 * returned when no file was selected.
		 *
		 * @private
		 * @name getFileName
		 * @return {string} Filename of the currently selected file or undefined
		 *					if no file was selected.
		 */
		getFileName : function () {
			var that = this;
			var modal = that.getModal();
			var form = modal.find('#' + that.nsClass('form'));
			var fileName = form.find('#' + that.nsClass('file')).val();
			if (fileName === '') {
				return undefined;
			}
			// strip the path from the filename
			if (fileName.lastIndexOf("\\") > 0) {
				fileName = fileName.substring(fileName.lastIndexOf("\\") + 1);
			}

			return fileName;
		},

		/**
		 * Returns the modal, creating it if needed.
		 * The modal and overlay elements are only created if and when needed to minimize adding unneed objects to the DOM
		 * returns {DOMObject} modal
		 */
		getModal : function () {
			var that = this;
			var modal = $(this.nsSel('modal'));

			if (modal.length === 0) {
				var overlay = $(this.renderTemplate('<div class="{overlay}">')).hide();

				modal = $(this.renderTemplate(
					'<div class="{modal}">' +
						'<div class="{header}">' +
							i18n.t("heading.gcnfileupload.uploadtext") +
						'</div>' +
						'<form id="{form}">' +
							'<input type="hidden" name="filename" id="{filename}">' +
							'<div class="{block}">' +
								'<div class="{left}"><div class="{inner}"><label for="{file}">' + i18n.t("field.gcnfileupload.file") + '</label></div></div>' +
								'<div class="{right}"><div class="{inner}"><input type="file" name="fileBinaryData" id="{file}"></div></div>' +
										'<div class="{clear}"></div>' +
								'<div class="{left}"><div class="{inner}"><label for="{fileDescription}">' + i18n.t("field.gcnfileupload.description") + '</label></div></div><div class="{right}"><div class="{inner}"><textarea id="{fileDescription}" name="fileDescription" class="{filedescription}"></textarea></div>' +
								'</div>' +
								'<div class="{clear}"></div>' +
							'</div>' +
							'<div class="{divider}"></div>' +

							'<div class="{btns}">' +
								'<div class="{inner}">' +
									'<button id="{insert}">' + i18n.t("button.gcnfileupload.text") + '</button>' +
									'<button id="{cancel}">' + i18nCore.t("cancel") + '</button>' +
									'<div class="{clear}"></div>' +
								'</div>' +
							'</div>' +
						'</form>' +
					'</div>'
				));

				modal.find('#' + this.nsClass('insert')).click(function () {
					var modal = that.getModal();
					var form = modal.find('#' + that.nsClass('form'));
					var fileName = that.getFileName();
					if (typeof fileName === 'undefined') {
						window.alert(i18n.t("validation.gcnfileupload.choosefile"));
						return false;
					}

					// Set the filename into the hidden field. This is necessary for IE, because depending on some security setting
					// ID will post the full path (wrong encoded), and Jersey will strip the backslashes, so that the resulting file
					// name is wrong
					form.find('#' + that.nsClass('filename')).val(fileName);
					modal.find('#' + that.nsClass('insert')).html(i18n.t("button.gcnfileupload.uploading.text")).attr('disabled', true);
					that.uploadFile(form, function (data) {
						if (typeof data === 'object' && data.success === true) {
							that.processNewFile(data);
						} else {
							window.alert(data.messages[0].message);
							modal.find('#' + that.nsClass('insert')).html(i18n.t("button.gcnfileupload.text")).attr('disabled', false);
							return false;
						}


					});

					return false;
				});
				modal.find('#' + this.nsClass('cancel')).click(function () {
					that.closeModal();
					return false;
				});

				$('body').append(overlay, modal);
			}

			return modal;
		},

		/*
		 * Creates a link to the given file.
		 */
		processNewFile : function (data) {
			var linkplugin = Aloha.require('link/link-plugin');
			data.file.url = '<plink id="10008.' + data.file.id + '" />';
			var document = this.getDocument(data.file, '10008');
			this.editable.activate();
			this._rO.select();
			linkplugin.hrefField.setTargetObject(this._tO, 'href');
			linkplugin.hrefField.setItem(document);
			this.closeModal();
		},

		/**
		 * Sends the file to the server.
		 * Takes the Form (jquery object) as first argument, that contains the input type file element.
		 * Takes a callback as second argument, that will be executed once the file was uploaded.
		 */
		uploadFile : function (form, callback) {
			var that = this;
			var gcnplugin = Aloha.require('gcn/gcn-plugin');

			GCN.node(gcnplugin.settings.nodeId, function (node) {
				// 1. The default upload folder is the current parent folder of the page
				var uploadFolderId = gcnplugin.settings.folderId;
				var defaultFileFolderId = node.prop('defaultFileFolderId');
				var defaultImageFolderId = node.prop('defaultImageFolderId');
				var fileName = that.getFileName();
				var extension =  fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
				var isImageFile = jQuery.inArray(extension, that.imageExtensions) > 0;

				// 2. Change the upload folder to the default image upload folder
				// when this is an image and the image upload folder was
				// set for this node. Select the default file upload folder
				// when one was specified for this node.
				if (isImageFile && typeof defaultImageFolderId !== 'undefined') {
					uploadFolderId = defaultImageFolderId;
				} else if (!isImageFile && typeof defaultFileFolderId !== 'undefined') {
					uploadFolderId = defaultFileFolderId;
				}
				// Load the folder so that we can fetch the upload url
				GCN.folder(uploadFolderId, function (folder) {
					var filterContentType = encodeURIComponent('text/html;charset=utf8');
					var restUrl = folder.multipartUploadURL(true, filterContentType);
					form.attr('action', restUrl);

					// Add more parts to the form. Those parts will contain
					// information that will be used by the server side upload
					// handler.
					if (form.find('#folderId').length <= 0) {
						var folderIdField = jQuery('<input type="hidden" value="' + uploadFolderId + '" name="folderId" id="folderId">');
						form.prepend(folderIdField);
					}
					if (gcnplugin.settings.nodeId && form.find('#nodeId').length <= 0) {
						var nodeIdField = jQuery('<input type="hidden" value="' + gcnplugin.settings.nodeId + '" name="nodeId" id="nodeId">');
						form.prepend(nodeIdField);
					}
					form.ajaxForm({
						beforeSubmit : function (a, f, o) {
							o.dataType = 'json';
						},
						success : function (data) {
							callback(data);
						}
					});
					form.submit();
				});
			});
		},

		/**
		* Transform the given data (fetched from the backend) into a repository item
		* @param {Object} data data of a page fetched from the backend
		* @return {Aloha.Repository.Object} repository item
		*/
		getDocument: function (data, objecttype) {
				var gcn = Aloha.require('gcn/gcn-plugin');

				if (!data) {
					return null;
				}

				objecttype = objecttype || 10007;
				// set the id
				data.id = objecttype + "." + data.id;

				// make the path information shorter by replacing path parts in the middle with ...
				var path = data.path;
				var pathLimit = 55;

				if (path && (path.length > pathLimit)) {
					path = path.substr(0, pathLimit / 2) + '...' + path.substr(path.length - pathLimit / 2);
				}

				data.path = path;

				// TODO make this more efficient (don't make a single call for every url)
				if (data.url && gcn.settings.renderBlockContentURL) {
					data.url = gcn.renderBlockContent(data.url);
				}
				data.repositoryId = 'com.gentics.aloha.GCN.Page';
				return new Aloha.RepositoryDocument(data);
			}
	});
});
