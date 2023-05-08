/*global module: true */

/**
 * @overview: Surfaces API feature of the GCN JS API which are NodeJS specific.
 */
(function (GCN) {

	'use strict';

	if (!GCN.global.isNode) {
		GCN.FolderAPI.prototype.createFile = function (file, success, error) {
			GCN.handleError(GCN.createError('UNAVAILABLE_FEATURE',
				'createFile() functionality is only available when using the' +
				'GCN JS API in Node.js environment.'), error);
		};
		return;
	}

	var fs    = require('fs');
	var url   = require('url');
	var path  = require('path');
	var http  = require('http');
	var https = require('https');
	var mime  = require('mime');

	/**
	 * Performs HTTP requests, including POST and GET.
	 *
	 * @private
	 * @param {string} uri The URI to send the request to.
	 * @param {object} options A object specifying options to use in the HTTP
	 *                         request.
	 * @param {function(string)} callback A function that will receive the HTTP
	 */
	function httpRequest(uri, options, callback) {
		if (typeof options === 'function') {
			callback = options;
			options = {};
		}
		var defaultPort;
		var protocol;
		if (!/^https?\:\/\//.test(uri)) {
			uri = 'http://' + uri;
			defaultPort = 80;
			protocol = http;
		} else {
			defaultPort = 443;
			protocol = https;
		}
		var urlSegments = url.parse(uri);
		var data = (typeof options.data !== 'undefined') ? options.data : '';
		var settings = {
			host: urlSegments.host,
			port: urlSegments.port || defaultPort,
			path: urlSegments.path,
			method: options.method || 'GET',
			headers: {
				'Content-Length': data.length
			}
		};
		var header;
		for (header in options.headers) {
			if (options.headers.hasOwnProperty(header)) {
				settings.headers[header] = options.headers[header];
			}
		}
		var request = protocol.request(settings, function (response) {
			var responseText = [];
			response.setEncoding('utf8');
			response.on('data', function (chunk) {
				responseText.push(chunk);
			});
			response.on('end', function () {
				callback(responseText.join(''), response);
			});
		});
		if (options.data) {
			request.write(options.data);
		}
		request.end();
	}

	/**
	 * Uploads a file at the given location and creates correspoding file
	 * content object for it in the GCN backend.
	 *
	 * @public
	 * @function
	 * @name createFile
	 * @memberOf GCN.FolderAPI
	 * @param {string} file The relative path to the file to be uploaded.
	 * @param {function(GCN.File)} success A callback that will receive the
	 *                                     created file object as its only
	 *                                     argument.
	 * @param {function(GCNError):boolean=} error An optional custom error
	 *                                            handler.
	 * @return {undefined}
	 */
	GCN.FolderAPI.prototype.createFile = function (file, success, error) {
		var folder = this;
		this._read(function () {
			fs.readFile(file, function (err, data) {
				if (err) {
					GCN.handleError(GCN.createError('UPLOAD_FAILED',
						'Could not upload file: ' + file, err), error);
					return;
				}
				var url = GCN.settings.BACKEND_PATH
						+ '/rest/file/createSimple?sid=' + GCN.sid
						+ '&folderId=' + folder.id()
						+ '&qqfile=' + path.basename(file)
						+ GCN._getChannelParameter(folder, '&');

				httpRequest(url, {
					method: 'POST',
					headers: {
						'Content-Type': mime.lookup(file),
						'Accept': 'application/json'
					},
					data: data
				}, function (responseText, response) {
					folder.handleUploadResponse(JSON.parse(responseText),
						success, error || function () {});
				});
			});
		}, error);
	};

	module.exports = GCN;

}(GCN));
