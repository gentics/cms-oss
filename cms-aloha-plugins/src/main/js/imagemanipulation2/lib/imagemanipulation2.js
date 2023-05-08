/*global define: true */
/*
 * This file will handle all ui specific code for the image manipulation2.
 */
define([
	'aloha', 
	'jquery', 
	'gcn-image/gcn-image-plugin',
	'image/image-plugin',
	'i18n!imagemanipulation2/nls/i18n', 
	'i18n!aloha/nls/i18n'
],
function (Aloha, aQuery, gcnImagePlugin, ImagePlugin, i18n, i18nCore) {
	'use strict';

	var $ = aQuery;
	var	jQuery = aQuery;

	Aloha.bind('aloha-ready', function () {

		var ImageManipulation = {

			urlParams: null,

			// This flag will be set to true when the only action was
			// the automatic resizing on image load.
			ommitImageResizeRestCallOnSave: false,

			propertyFillingEnabled: true,

			init: function () {
				var imageManipulation = this;
				
				$('#message-container').click(function () {
					$(this).slideUp();
				});

				// Trigger click on the image to enable resize
				$('img').filter('.global').trigger('click');

				//Workaround since event handling of sidebar is not working properly at the moment
				this.populateSidebar();

				// Use loaded properties and populate the image property fields
				var imageProperties = gcnImagePlugin.imageProperties;

				this.populateImagePropertyFields(imageProperties);

				var saveAction   = jQuery.proxy(this.handleSaveAction,   this);
				var copyAction   = jQuery.proxy(this.handleCopyAction,   this);
				var cancelAction = jQuery.proxy(this.handleCancelAction, this);

				// Handle save action for the current image	
				$('#save-action').click(function (event) {
					saveAction(false, true);
					event.preventDefault();
				});

				$('#copy-action').click(function (event) {
					copyAction();

					$(this).prop('disabled',true);
					var i18nLoadingButton = '?do=15&disabled=1&text=' + i18n.t('button.loading');
					$(this).find('img').attr('src', i18nLoadingButton);

					event.preventDefault();
				});
				
				$('#cancel-action').click(function (event) {
					cancelAction();
					event.preventDefault();
				});

				// Show save button if attribute is changed
				$('#description').bind('keydown', function () {
					if (!$('#image-actions').is(':visible')) {
						$('#image-actions').slideDown('slow');
					}					
				});
				
				// Show save button if attribute is changed
				$('#filename').bind('keydown', function () {
				
					if (!$('#image-actions').is(':visible')) {
						$('#image-actions').slideDown('slow');
					}					
				});
				
				$('body').bind('aloha-image-crop-start', function (e, image) {
					if ($('#image-actions').is(':visible')) {
						$('#image-actions').slideUp('slow');
					}
				});
				
				$('body').bind('aloha-image-crop-stop', function (e, image) {
					if (!$('#image-actions').is(':visible')) {
						$('#image-actions').slideDown('slow');
					}
				});
				
				$('body').bind('aloha-image-cropped', function (e, image, props) {
					imageManipulation.ommitImageResizeRestCallOnSave = false;

					if (!$('#image-actions').is(':visible')) {
						$('#image-actions').slideDown('slow');
					}					
				});
				
				// Show message when a size was entered that exceeded the given bounds
				$('body').bind('aloha-image-resize-outofbounds', function (e, type, limit, eventProps) {
					// Create the dynamic i18n message by replacing parts
					var i18nMsg = i18n.t('resize.image.outofbounds.max');
					
					if (limit === 'min') {
						i18nMsg = i18n.t('resize.image.outofbounds.min');
					}
					
					i18nMsg = i18nMsg.replace(/\[ORG\]/g, Math.floor(eventProps.org));
					// Array-syntax is used for "new" because it is a reserved keyword
					i18nMsg = i18nMsg.replace(/\[NEW\]/g, Math.floor(eventProps['new']));
					type = i18n.t(type);
					i18nMsg = i18nMsg.replace(/\[TYPE\]/g, type);
					imageManipulation.showMessageBox(i18nMsg, 5000);
				});
				
				// Handle the resize event and update some information about the image
				$('body').bind('aloha-image-resize', function (e, image) {
					imageManipulation.ommitImageResizeRestCallOnSave = false;
					// Show the actions menu
					if (!$('#image-actions').is(':visible')) {
						$('#image-actions').slideDown('slow');
					}
				});

				$('body').bind('aloha-image-focalpoint-start', function(e) {
					// Use the stored focal point information and update the FP
					ImagePlugin.setFocalPoint(imageProperties.fpX, imageProperties.fpY);
				});

				$('body').bind('aloha-gcn-image-copied', function (e, imageProperties) {
					$('#copy-action').prop('disabled',false);
					var i18nDuplicateButton = '?do=15&text=' + i18n.t('button.duplicate');
					$('#copy-action').find('img').attr('src',i18nDuplicateButton);
					imageManipulation.showMessageBox(i18n.t('copy.image.success'), 4000);

					if (imageManipulation.propertyFillingEnabled) {
						imageManipulation.populateImagePropertyFields(imageProperties);
					}
				});
				
				// Handle the image updated event and populate all field with the data
				$('body').bind('aloha-gcn-image-updated', function (e, imageProperties) {
					if (imageManipulation.propertyFillingEnabled) {
						imageManipulation.populateImagePropertyFields(imageProperties);
					}
				});

				this.ommitImageResizeRestCallOnSave = ImagePlugin.autoResize(); 
				if (imageManipulation.ommitImageResizeRestCallOnSave) {
					this.showMessageBox(i18n.t('autoresize.image'), 7000);
				} else {
					$('#message-container').slideUp('slow');
				}
			},

			/**
			 * This method will handle the image save action
			 * @param {boolean} copyFile   Copy the image file during resize?
			 * @param {boolean} closeView  Close the image manipulation afterwards?
			 */
			handleSaveAction: function (copyFile, closeView) {
				this.correctFilenameExtension();

				// Save the properties
				this.saveCurrentPropertiesWithRestCall(true,
					function () {
						// Success
						// Resize and maybe copy
						this.resizeAndCopyWithRestCall(false, function() {
							// Success
							// Should the image manipulation be closed?
							if (closeView) {
								this.handleCancelAction();
							}
					});
				});
			},

			/**
			 * Handle the copy action
			 */
			handleCopyAction: function () {
				// We have to disable property population, because we want
				// to save the properties afterwards, and copying image would reload them.
				this.propertyFillingEnabled = false;

				// Depending on whether the image has been resized/cropped or not,
				// we call different methods to copy.
				if (typeof gcnImagePlugin.imageProperties.info === 'undefined' || this.ommitImageResizeRestCallOnSave) {
					gcnImagePlugin.handleCopyFileAction(
							jQuery.proxy(this.handleCopyActionOnSuccess, this),
							jQuery.proxy(this.handleCopyActionOnError,   this));
				} else {
					// Resize and maybe copy
					this.resizeAndCopyWithRestCall(true,
							jQuery.proxy(this.handleCopyActionOnSuccess, this),
							jQuery.proxy(this.handleCopyActionOnError,   this));
				}
			},

			/**
			 * Saves the current properties (filename, description)
			 */
			handleCopyActionOnSuccess: function() {
				var that = this;

				this.propertyFillingEnabled = true;
				this.saveCurrentPropertiesWithRestCall(
					false,
					function () {
						that.ommitImageResizeRestCallOnSave = ImagePlugin.autoResize();

						if (that.ommitImageResizeRestCallOnSave) {
							that.showMessageBox(i18n.t('autoresize.image'), 7000);
						} else {
							$('#message-container').slideUp('slow');
						}
					});
			},

			/**
			 * Enables property Filling on error (clean up)
			 */
			handleCopyActionOnError: function() {
				this.propertyFillingEnabled = true;
			},

			/**
			 * Handle the cancel action
			 */
			handleCancelAction: function () {
				this.closeView();
			},

			/*
			 * If the file is not JPG or PNG we have to
			 * change the filetype to PNG because other target
			 * file formats are not supported by the GIS.
			 */
			correctFilenameExtension: function () {
				// PNG and JPG are supported, so the response
				// will always be one of those two.
				if (gcnImagePlugin.imageProperties.fileType === 'image/jpeg') {
					$('#extension').text('jpg');
				} else {
					$('#extension').text('png');
				}
			},

			/*
			 * Calls the rest API with a resize request.
			 * Optionally, the file can also  be copied during resize.
			 * 
			 * @param {boolean}            copyFile
			 *                                   Whether to copy the file also or not
			 * @param {?function(Object)}  customSuccessCallback
			 *                                   An optional callback function that will be called on success
			 */
			resizeAndCopyWithRestCall: function (copyFile, customSuccessCallback) {
				var imageManipulation = this;
				var imageProperties   = gcnImagePlugin.imageProperties;
				var callbackProxy = typeof customSuccessCallback == 'function'
					? jQuery.proxy(customSuccessCallback, imageManipulation, null)
					: undefined;

				// image resize info might be unpopulated when a user has not yet resized the image.
				// Omitting resizing in that case.
				if (typeof imageProperties.info !== 'undefined') {
					if (this.ommitImageResizeRestCallOnSave) {
						this.showMessageBox(i18n.t('resize.image.ommited'), 5000, 'info', callbackProxy);
					} else {
						//TODO Check if resize is needed or if the user has just changed the filename.
						gcnImagePlugin.invokeImageResizeRestCall(
								imageProperties,
								function (data) {
									imageManipulation.showMessageBox(i18n.t('resize.image.success'), 400);
									imageManipulation.correctFilenameExtension();

									if (typeof customSuccessCallback === 'function') {
										jQuery.proxy(customSuccessCallback, imageManipulation, data)();
									}
								},
								function (jqXHR, textStatus, errorThrown) {
									imageManipulation.showMessageBox(i18n.t('resize.image.error'),
											-1, 'warning');
								},
								copyFile);
					}
				} else {
					if (typeof customSuccessCallback === 'function') {
						jQuery.proxy(customSuccessCallback, imageManipulation, null)();
					}
				}
			},

			/*
			 * Save the image properties like the filename and description,
			 * using a rest call to the Rest API.
			 * @param {boolean}           showMessage            If to show the success message
			 * @param {function(Object)}  customSuccessCallback  Custom success callback function
			 * @param {?function()}       customErrorCallback    Custom error callback function
			 */
			saveCurrentPropertiesWithRestCall: function(showMessage,
					customSuccessCallback, customErrorCallback) {
				var imageManipulation = this;

				if ($('#filename').val() === '') {
					imageManipulation.showMessageBox(i18n.t('save.empty.filename'), -1, 'warning');
					return;
				}

				var imageProperties = gcnImagePlugin.imageProperties;
				imageProperties.name = $('#filename').val() + '.' + $('#extension').text();
				imageProperties.description = $('#description').val();
				gcnImagePlugin.invokeImageSavePropertiesRestCall(
						imageProperties,
						function (data) {
							imageManipulation.onPropertiesSaved(data, showMessage, customSuccessCallback);
						},
						function (jqXHR, textStatus, errorThrown) {
							imageManipulation.onPropertiesSaveError(jqXHR, textStatus, errorThrown,
									customErrorCallback);
						});
			},

			/**
			 * This is called when the saving of the current properties succeeds
			 * @param {Object}             data                   Rest call ajax data response object
			 * @param {boolean}            showMessage            If to show the success message
			 * @param {?Function(Object)}  customSuccessCallback  Custom callback function that will be called
			 */
			onPropertiesSaved: function (data, showMessage, customSuccessCallback) {
				var imageManipulation = this;
				var imageProperties = gcnImagePlugin.imageProperties;

				if (showMessage) {
					this.showMessageBox(i18n.t('save.properties.success'), 3000);
				}
				
				// We need to invoke this method since the filename might has been
				// changed due to filename conflict checks. Furthermore the rest
				// response currently does not contain a file/image object.
				gcnImagePlugin.updateImageProperties(imageProperties.id, function (loadedImageProperties) {
					imageManipulation.populateImagePropertyFields(loadedImageProperties);
					
					// Update the properties within the tagfill frame
					if (openedFromTagFill()) {
						window.opener.updateFileField(loadedImageProperties.name, loadedImageProperties.id);
					}
				});

				// Call the custom callback which will probably
				// do more rest calls etc...
				if (typeof customSuccessCallback === 'function') {
					jQuery.proxy(customSuccessCallback, imageManipulation, data)();
				}
			},

			/**
			 * Function that is called when an error occurs when saving
			 * the image properties with the Rest API
			 * @param {Object}               jqXHR
			 *                                             jQuery XHR object
			 * @param {string}               textStatus
			 *                                             Text status
			 * @param {string}  errorThrown  errorThrown
			 *                                             Error message
			 * @param {Function(Object)}     customSuccessCallback
			 *                                             Custom callback function that will be called
			 */
			onPropertiesSaveError: function (jqXHR, textStatus, errorThrown, customErrorCallback) {
				this.showMessageBox(i18n.t('save.properties.error'), -1, 'warning');

				if (typeof customErrorCallback === 'function') {
					customErrorCallback();
				}
			},

			/**
			 * Closes the image manipulation
			 */
			closeView: function () {
				if (openedFromTagFill()) {
					window.close();
				} else {
					// Close the window
					window.location.href = window.BACKURL;
				}
			},

			/**
			 * Populates the fields with image properties
			 * @param {Object}  imageProperties  Image property object
			 */
			populateImagePropertyFields: function (imageProperties) {
				$('#description').val(imageProperties.description);

				if (typeof imageProperties !== 'undefined' && typeof imageProperties.name !== 'undefined') {
					// Split extension from filename
					var expression = /(.*)\.([^.]+)$/;
					var matches = expression.exec(imageProperties.name);

					$('#filename').val(matches[1]);
					$('#extension').text(matches[2]);
				} 
				
			},
			
			/**
			 * Adds panels to the sidebar
			 */
			populateSidebar: function () {
				var imageManipulation = this;
				var right = Aloha.Sidebar.right;

				var propPanel = right.addPanel({
					title	 : i18n.t('Properties'),
					content  : $('#panelLeftPropertiesContainer').remove().html(),
					expanded : true,
					activeOn : '*',
					onInit	 : imageManipulation.loadImageProperties
				});

				right.show();
				right.open();

				propPanel.activate();
		
			},

			/**
			 * Shows a small message box at the top of the window.
			 * The box will stay for the given amount of time. 
			 * A given duration of -1 will ommit the slideup action.
			 * When callback is given, it will be called after the slideup.
			 * @param {string}  text
			 * @param {number}  duration
			 * @param {string}  type
			 * @param {function} callback
			 */
			showMessageBox: function (text, duration, type, callback) {
				// If no duration is specified we'll use a default amount	
				if (typeof duration === 'undefined') {
					duration = 3000;
				}

				function hideMessageBox() {
					$('#message-container').slideUp('slow', function () {
						if (typeof callback == 'function') {
							callback();
						}
					});
				}

				$('#message-container').removeClass();
				$('#message-image').removeClass();
				
				if (typeof type !== 'undefined') {
					$('#message-container').addClass(type);
					$('#message-image').addClass(type);
				}
				$('#message-container span').text(text);
				$('#message-container').slideDown('slow', function () {
					if (duration !== -1) {
						window.setTimeout(hideMessageBox, duration);
					}
				});
			}

		};

		// wait for the image to be loaded, before the image manipulation is initialized
		$( window ).on( "load", function() {
			ImageManipulation.init();
		});
	});

	/**
	 * Determines whether the imagemanipulation window was opened from the tag fill dialog.
	 *
	 * @return
	 *        Boolean true if the imagemanipulation window was opened from the tag fill dialog.
	 */
	function openedFromTagFill() {
		// The updateFileField is set to a function in the tagfill
		// dialog window (part_listels.func.php). Curiously, on IE the
		// type of this function is 'object', on other browsers it is
		// 'function'. I don't know why.
		return (window.opener && (typeof window.opener.updateFileField === 'function'
			|| typeof window.opener.updateFileField === 'object'));
	}
});
