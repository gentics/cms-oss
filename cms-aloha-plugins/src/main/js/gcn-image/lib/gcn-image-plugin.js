/*global define: true, PROXY_PREFIX: true */
/*
 * Aloha Editor
 * Author & Copyright (c) 2018 Gentics Software GmbH
 * aloha-sales@gentics.com
 * Licensed under the terms of http://www.aloha-editor.com/license.html
 */

define(
[ 
	'aloha',
	'jquery',
	'aloha/plugin', 
	'image/image-plugin',
	'aloha/pluginmanager',
	'util/json2',
	'i18n!gcn-image/nls/i18n',
	'i18n!aloha/nls/i18n'
],
function (Aloha, jQuery, Plugin, ImagePlugin, PluginManager, json, i18n, i18nCore) {
	'use strict';
	var $ = jQuery;

	/**
	 * Gentics Content.Node Image Plugin
	 */
	return Plugin.create('gcn-image', {

		dependencies: ['image'],

		// This property will hold all image properties
		imageProperties: {},

		/**
		 * Flag that indicates whether the image was modified (cropped/resized) 
		 */
		imageIsModified: false,

		/**
		 * Configure the available languages
		 */
		languages : [ 'en', 'de' ],

		init: function () {

			Aloha.log('gcn-image-init');
			var that = this;
			
			var urlParams = this.getUrlVars(); 
			if (typeof urlParams.id === 'undefined') {
				Aloha.Log.error('Could not get initial image id from url params. Aborting init.');
				return false;
			}

			this.updateImageProperties(urlParams.id);

			$('body').bind('aloha-image-cropped', function (e, image, props) {
				that.onCropped($(image), props);
			});

			$('body').bind('aloha-image-reset', function (e, image) {
				that.onReset($(image));
			});

			$('body').bind('aloha-image-resized', function (e, image) {
				that.onResized($(image));
			});

			$('body').bind('aloha-image-focalpoint', function(e, point) {
				that.onFocalPoint(point);
			});
		},

		/**
		 * Load the image from GCN and update the imageProperties
		 */
		updateImageProperties: function (id, optionalOnSuccessCallback) {
			var that = this;

			$.ajax({
				//TODO handle custom urls properly.. stag prefix..
				url: window.PORTLETAPP_PREFIX + 'rest/image/load/'+ id + '?sid=' + window.SID,
				dataType: 'json',
				success: function (data) {
					var extendedProperties = jQuery.extend(true, that.imageProperties, data.image);
					that.imageProperties = extendedProperties;
					$('body').trigger('aloha-gcn-image-updated', [extendedProperties]);

					// Invoke the optional callback if possible
					if (typeof optionalOnSuccessCallback === 'function') {
						optionalOnSuccessCallback(extendedProperties);
					}

				},
				error: function(jqXHR, textStatus, errorThrown) {
					Aloha.Log.error('Could not load image properties for image with id{'
							+ id + '}', textStatus, errorThrown);
				}
			});
			
		},
		
		
		/**
		 * Rest helper method that invokes a post uppon the rest api
		 */
		_storeData: function (restUrl, content, onSuccess, onError) {
			//TODO handle custom urls properly.. stag prefix..
			$.ajax({
				url: window.PORTLETAPP_PREFIX + 'rest/' + restUrl,
				type: 'POST',
				dataType: 'json',
				contentType: 'application/json; charset=utf8',
				data: json.stringify(content),
				success: onSuccess,
				error: onError
			});
		},

		/**
		 * This method will invoke the image save properties rest call
		 */
		invokeImageSavePropertiesRestCall: function (imageProperties, onSuccess, onError) {
			var restUrl = 'image/save/' + imageProperties.id +
					'?sid=' + window.SID;
			
			var imageRestPayload = {
					'image': {
						'id': imageProperties.id,
						'name': imageProperties.name,
						'fpX': imageProperties.fpX,
						'fpY': imageProperties.fpY,
						'description': imageProperties.description
					}
				};

			function onSavePropertiesError(jqXHR, textStatus, errorThrown) {
				if (typeof onError === 'function') {
					onError(jqXHR, textStatus, errorThrown);
				}
			}

			function onSavePropertiesSuccess(data) {
				if (typeof onSuccess === 'function') {
					onSuccess(data);
				}
			}

			this._storeData(restUrl, imageRestPayload, onSavePropertiesSuccess, onSavePropertiesError);
		},
		
		
		/**
		 * This method invokes the resize rest call using the given imageProperties 
		 * 
		 * @param {Object}             imageProperties  Property object
		 * @param {?function(Object)}  onSuccess        Custom success callback function
		 * @param {?function(Object, string, string)}  onError          Custom error callback function
		 * @param {boolean}            copyFile         Whether to copy the image during the resize
		 */
		invokeImageResizeRestCall: function (imageProperties, onSuccess, onError, copyFile) {
			
				var that         = this;
				var targetFormat = 'png';

				if (imageProperties.fileType === 'image/jpeg') {
					// The GenticsImageStore also supports PNG,
					// otherwise default to JPG.
					targetFormat = 'jpg';
				}

				var imageResizeData = {
						'image': {
							'id': imageProperties.id
						},
						'cropHeight': imageProperties.info.ch,
						'cropWidth': imageProperties.info.cw,
						'cropStartX': imageProperties.info.x,
						'cropStartY': imageProperties.info.y,
						'width': imageProperties.info.w,
						'height': imageProperties.info.h,
						'mode': 'cropandresize',
						'resizeMode': 'force',
						'targetFormat': targetFormat
				};

				if (copyFile) {
					imageResizeData.copyFile = true;
				}

				/**
				 * Handle errors that might happen during resizing of the image
				 */
				function onImageResizeError (jqXHR, textStatus, errorThrown) {
					
					if (typeof onError === 'function') {
						onError(jqXHR, textStatus, errorThrown);
					}

				}

				function onImageResizeSuccess (data) {
					
					// Invalidate the old restore props
					PluginManager.plugins.image.restoreProps = [];
					
					//TODO remove gis from url
					//TODO change timestamp of url

					if (copyFile) {
						that.onImageCopied(onSuccess, data);
					} else {
						if (typeof onSuccess === 'function') {
							onSuccess(data);
						}
					}
				}

				var restUrl = 'image/resize/?sid=' + window.SID;
				this._storeData(restUrl, imageResizeData, onImageResizeSuccess, onImageResizeError);
			
		},

		/**
		 * Handle the user interaction for copy file
		 * @param {function(Object)}  customSuccessHandler  Custom success callback function
		 * @param {function()}        customErrorHandler    Custom error callback function
		 */
		handleCopyFileAction: function (customSuccessHandler, customErrorHandler) {
			this.copyFile(this.imageProperties.id, null, customSuccessHandler, customErrorHandler);
		},

		/**
		 * Helper function that is used to parse the request uri
		 */
		getUrlVars: function () {
			var vars = [], hash;
			var hashes = window.location.href.slice(window.location.href.indexOf('?') + 1).split('&');
			for(var i = 0; i < hashes.length; i++) {
				hash = hashes[i].split('=');	
				vars.push(hash[0]);
				vars[hash[0]] = hash[1];
			}
			return vars;
		},

		/**
		 * This method will invoke a rest call that creates a copy of the given file within gcn
		 * @param {number}            fileIDtoBeCopied      ID of the file to be copied
		 * @param {?string}           newFilename           You can specify a new filename or null
		 * @param {function(Object)}  customSuccessHandler  Custom success callback function
		 * @param {function()}        customErrorHandler    Custom error callback function
		 */
		copyFile: function (fileIDtoBeCopied, newFilename, customSuccessHandler, customErrorHandler) {
			var that = this;
			if (typeof fileIDtoBeCopied === 'undefined') {
				Aloha.Log.error('Could not copy file since the information about'
						+ ' the sourcefile has not been entered', this);
				return;
			}

			// Prepare the filecopyrequest data
			var content = {
					'newFilename': newFilename,
					'file': {
						'id': fileIDtoBeCopied
					}
			};

			$.ajax({
				//TODO handle custom gcn urls.. stag prefix..
				url: window.PORTLETAPP_PREFIX + 'rest/file/copy/?sid=' + window.SID,
				type: 'POST',
				dataType: 'json',
				contentType: 'application/json; charset=utf8',
				data: json.stringify(content),
				success: jQuery.proxy(that.onImageCopied, that, customSuccessHandler),
				error: function(jqXHR, textStatus, errorThrown) {
					Aloha.Log.error('Could not copy file with id {' + fileIDtoBeCopied + '}',
							  textStatus, errorThrown);

					if (typeof customErrorHandler === 'function') {
						customErrorHandler();
					}
				}
			});
			
		},

		/**
		 * This method gets called when a image has been copied
		 * @param {function()}  customSuccessHandler  Custom success callback function
		 * @param {Object}      data                  Rest API json response object
		 */
		onImageCopied: function (customSuccessHandler, data) {
			var that = this;

			// We created a new file. That means we need to update our properties 
			that.updateImageProperties(data.file.id, function(imageProperties) {
				// When the image was cropped before being copied, the cropping
				// rectangle will no longer be "inside" the image, so we have
				// to reset the cropping start point to the origin of the image.
				var info = imageProperties.info;

				if (info) {
					info.x = info.y = 0;
					info.cw = info.w;
					info.ch = info.h;
				}

				// Once the image properties are updated we'll invoke a reset of the image
				that.removeImageStoreFromImage(function() {
					// Image has been replaced, properties updates. Now throw the event
					$('body').trigger('aloha-gcn-image-copied', [imageProperties]);
				});

				if (typeof customSuccessHandler === 'function') {
					customSuccessHandler();
				}
			});
			
		},

		/**
		 * This method will remove the imagestore parts from the url and 
		 * change the timestamp. This will force a reload.
		 */
		removeImageStoreFromImage: function (onImageLoad) {

			var that = this;
			var $image = ImagePlugin.imageObj;
			// We also need to replace the displayed image
			var info = that.analyzeImage($image);

			var src = $image.attr('src');
			// Analyzing will fail if the image has not been resized yet due to missing genticsimagestore 
			// part within the image url but thats ok since we just copied the image. 
			// If the image is using the genticsimagestore we'll use the genticsimagestore src instead. 
			// This is ok since we want to get rid of the genticsimagestore parts anyway.
			
			if (info) {
				src = info.src; 
			}
			// Replace the original fileId with the new fileId
			src = src.replace(/([&|?])id=[0-9]*(&*)/,'$1id=' + that.imageProperties.id + '$2');
			// Modify the timestamp to avoid caching. 
			var ts = Math.round((new Date()).getTime() / 1000);
			src = src.replace(/([&|?])time=[0-9]*(&*)/,'$1time=' + ts + '$2');

			// The original image will be created again to determine it's original size
			var originalImage = new Image();
			originalImage.src = src;

			originalImage.onload = function () {
				// Calling endresize will disable the wrapped div hide the shadow which 
				// otherwise would stay at different width/height than the image. 
				ImagePlugin.endResize();
				$image.width(this.width);
				$image.height(this.height);
				$image.attr('src', this.src);
				ImagePlugin.restoreProps = [];
				that.imageIsModified = false;
				if (typeof onImageLoad === 'function') {
					onImageLoad();
				}
			};
		},

		/**
		 * Generate a GenticsImageStore  URL URLs have 
		 * to be generated this way to prevent nesting 
		 * of GenticsImageStore calls in URLs
		 * 
		 * A GenticsImageStore URL looks like:
		 * http://SERVER/GenticsImageStore /300 - width
		 * /200 - height /cropandresize - mode /force -
		 * resizemode /20 - crop start left /20 - crop
		 * start top /100 - crop width /100 - crop
		 * height
		 * /.Node/?sid=hXjD3HoJN31Bnam&time=1293543192&do=16000&id=214&keepsid=1
		 * 
		 * @param props
		 *            object with crop and resize
		 *            properties, eg: { w : 400, //
		 *            resize width h : 300, // resize
		 *            height x : 10, // x-pos of crop y :
		 *            10, // y-pos of crop cw : 200, //
		 *            crop width ch : 150, // crop
		 *            height src : 'http://...' //
		 *            original image source }
		 */
		getURLFromInfo: function(p) {
			var imageUrlToResize = p.src;
			// There is a customer who uses a web proxy with URL security enabled that doesn't allow passing
			// full URL's as URL parameters.
			// This pattern will extract the actual path from the URL like this:
			//		- http://host/path  -> path
			//		- https://host/path -> path
			//		- /path             -> path
			//		- //path            -> path
			var pattern = /^(?:(?:https?:\/\/[^/]*)|\/?)\/?(.*)$/;
			var match = pattern.exec(p.src);

			if (match) {
				imageUrlToResize = match[1];
			}

			return '/GenticsImageStore/' + p.w + '/'
					+ p.h + '/'
					+ 'cropandresize/force/' + p.x
					+ '/' + p.y + '/' + p.cw + '/'
					+ p.ch + '/' + imageUrlToResize;
		},

		/**
		 * Reset callback integration 
		 * 
		 * @param image reference to the image that has
		 * been resized
		 */
		onReset: function ($image){
			// after resetting, focus is again set to the image
			// in order to update field values
			this.removeImageStoreFromImage(function() {
				$image.click();
			});
		},

		/**
		 * Update the focal point in the image properties.
		 * 
		 * @param point Point which contains the focal point data
		 */
		onFocalPoint: function(point) {
			this.imageProperties.fpX = point.fpx;
			this.imageProperties.fpY= point.fpy;
		},

		/**
		 * Resize callback integration
		 * 
		 * @param image reference to the image that has been resized
		 */
		onResized: function ($image) {
			var info = this.analyzeImage($image);

			// the original image will be created again to determine it's original size
			var oImg = new Image(); 

			// the image was previously resized or cropped, so keep the parameters from the info
			if (info) {
				oImg.src = info.src;
			} else {
				// generate new image info
				oImg.src = $image.attr('src');
				info = {
					x : 0, // x-pos of crop
					y : 0, // y-pos of crop
					cw : oImg.width, // crop width 
					ch : oImg.height, // crop height 
					src : oImg.src	// image source
				};
			}

			// now set the resize options
			info.w = $image.width();
			info.h = $image.height();

			var newurl = PORTLETAPP_PREFIX.replace(/\/+$/, "") + this.getURLFromInfo(info);
			$image.attr('src', newurl);
			this.imageProperties.info = info;
			this.imageIsModified = true;
		},

		/**
		 * @param image reference to the image that has been resized
		 * @param props cropping properties
		 */
		onCropped: function (image, props) {
			var info = this.analyzeImage(image);
			// the original image will be created again to determine it's original size 
			var oImg = new Image(); 

			// the image was previously resized or cropped, so keep the parameters from the info
			if (info) {
				oImg.src = info.src;

				// determine scaling factor if the image has been scaled BEFORE
				// cropping a x- and y-scaling factor needs to be determined to
				// recalculate x- and y- positions located on an unscaled image
				var sfX = 1; // scaling factor for the x-axis
				var sfY = 1; // an for y
				if (info.w != info.cw) {
					sfX = info.cw / info.w;
				}
				if (info.h != info.ch) {
					sfY = info.ch / info.h;
				}

				// as the image has already been resized or cropped before,
				// things are getting interesting here first we have to correct
				// the x- and y-pos of the crop
				info.x = info.x	+ parseInt(props.x * sfX);
				info.y = info.y	+ parseInt(props.y * sfY);

				// then we have to scale the current
				// crop area up to the
				// whole image size and then re-apply
				// the scaling
				info.cw = parseInt(props.w * sfX);
				info.ch = parseInt(props.h * sfY);
			} else {
				oImg.src = image.attr('src');
				info = {
					x : props.x, // x-pos of crop
					y : props.y, // y-pos of crop
					cw : props.w, // crop width
					ch : props.h, // crop height
					src : oImg.src
				// image source
				};
			}

			// now set the resize options
			info.w = props.w; // image.width();
			info.h = props.h; // image.height();

			var newurl = PORTLETAPP_PREFIX.replace(/\/+$/, "") + this.getURLFromInfo(info);
			image.attr('src', newurl)
			     .width(props.w)
			     .height(props.h);
			this.imageProperties.info = info;

		},

		/**
		 * Analyze image for previous GenticsImageStore
		 * crop or resize actions
		 * 
		 * @param image
		 *            jQuery image object to be analyzed
		 * @return false if no GenticsImageStore
		 *         cropandresize URL was detected or an
		 *         object containing all of the detected
		 *         properties: { prefix :
		 *         '/GenticsImageStore', // the prefix
		 *         of the GenticsImageStore URL w : 400, //
		 *         resize width h : 300, // resize
		 *         height x : 10, // x-pos of crop y :
		 *         10, // y-pos of crop cw : 200, //
		 *         crop width ch : 150, // crop height
		 *         src : 'http://...', // original image
		 *         source id : 15, // GCN image id
		 *         repository :
		 *         'com.gentics.aloha.GCN.Image' //
		 *         repository the image was taken from }
		 */
		analyzeImage : function ($image) {

			var src = $image.attr('src');
			
			if (!src) {
				return false;
			}

			var matchRegExp = new RegExp('(.+GenticsImageStore)\\/(\\d+)\\/(\\d+)\\/'
					+ 'cropandresize\\/force\\/(\\d+)\\/(\\d+)\\/(\\d+)\\/(\\d+)\\/(.+)');
			var m = src.match(matchRegExp);
			if (m) {
				return {
					prefix : m[1],
					w : parseInt(m[2]), // resize width
					h : parseInt(m[3]), // resize height
					x : parseInt(m[4]), // x-pos of crop
					y : parseInt(m[5]), // y-pos of crop
					cw : parseInt(m[6]), // crop width
					ch : parseInt(m[7]), // crop height
					// image source
					src : '/' + m[8], 
					// gcn5's id for this image
					id : parseInt($image.attr('data-gentics-aloha-object-id')), 
					repository : $image.attr('data-gentics-aloha-repository')
				// the corresponding repository
				};
			} else {
				return false;
			}
		}

	});
});
