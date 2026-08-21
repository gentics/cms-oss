define([
    'ui/surface',
    'i18n!ui/nls/i18n'
], function (
    Surface,
    i18n
) {
    'use strict';

    /**
     * @typedef {object} ConfirmDialogOptions
     * @property {string} title The title of the dialog
     * @property {string=} text The text value of the dialog body. May only be present if `html` is not provided.
     * @property {string=} html The HTML value of the dialog body. May only be present if `text` is not provided.
     * @property {()=>any=} yes Function to call when the dialog was confirmed.
     * @property {()=>any=} no Function to call when the dialog was denied.
     * @property {(boolean)=>any=} answer Function to call when the dialog was ansered. The value is a boolean, which indicates if it was confirmed.
     * @property {()=>any=} close Function to call when the dialog was closed.
     * @property {string=} cls CSS Class which is applied to the dialog.
     * @property {object.<string, (boolean?)=>any>=} buttons Object which has the button-label as key and a function to call as value when the button was pressed.
     */

    /**
     * Opens the dialog via the GCMSUI-Bridge
     * @param {object} config Configuration for the `GCMSUI.openDialog` function.
     * @param {(boolean)=>void} resolveFn Function which is called when the dialog was closed by one of the buttons.
     * @param {()=>} closeFn Function which is called when the dialog is closed by any means.
     * @returns 
     */
    function internalOpenDialog(config, resolveFn, closeFn) {
        var control;

        window.GCMSUI.openDialog(config)
            .then(function (ctl) {
                control = ctl;
                return ctl.value;
            })
            .then(function (value) {
                if (typeof resolveFn === 'function') {
                    resolveFn(value);
                }
            })
            .catch(function (err) {
                closeFn();
            });

        return function () {
            if (control != null) {
                control.close();
            }
        };
    }

    var GCMSUISurface = Surface.extend({
        _context: null,
        _settings: null,

        _constructor: function (context, settings) {
            this._super(context);
            this._context = context
        },

        adoptInto: function (slot, component) {
            if (!window.GCMSUI) {
                return;
            }
            window.GCMSUI.registerComponent(slot, component);
        },

        unadopt: function (slot) {
            if (!window.GCMSUI) {
                return;
            }
            window.GCMSUI.unregisterComponent(slot);
        },

        openDynamicDropdown: function (componentName, config) {
            if (!window.GCMSUI) {
                return Promise.reject(new Error('GCMSUI is not defined!'));
            }
            return window.GCMSUI.openDynamicDropdown(config, componentName);
        },
        openDynamicModal: function (config) {
            if (!window.GCMSUI) {
                return Promise.reject(new Error('GCMSUI is not defined!'));
            }
            return window.GCMSUI.openDynamicModal(config);
        },
        /**
         * @param {ConfirmDialogOptions} config configuration for the dialog
         */
        openConfirmDialog: function (config) {
            if (!window.GCMSUI) {
                return Promise.reject(new Error('GCMSUI is not defined!'));
            }

            return internalOpenDialog({
                title: config.title,
                body: config.text,
                buttons: [
                    {
                        label: (config.buttons || {}).No || i18n.t('button.no.label'),
                        returnValue: false,
                        type: 'secondary',
                        flat: true,
                    },
                    {
                        label: (config.buttons || {}).Yes || i18n.t('button.yes.label'),
                        returnValue: true,
                        type: 'default',
                    },
                ],
            }, function (didConfirm) {
                if (didConfirm && typeof config.yes === 'function') {
                    config.yes();
                } else if (typeof config.no === 'function') {
                    config.no();
                }
                if (typeof config.answer === 'function') {
                    config.answer(didConfirm);
                }
                if (typeof config.close === 'function') {
                    config.close();
                }
            }, function() {
                if (typeof config.close === 'function') {
                    config.close();
                }
            });
        },
        openAlertDialog: function (config) {
            if (!window.GCMSUI) {
                return Promise.reject(new Error('GCMSUI is not defined!'));
            }

            return internalOpenDialog({
                title: config.title,
                body: config.text,
                buttons: [
                    {
                        label: i18n.t('button.dismiss.label'),
                        returnValue: null,
                        type: 'secondary',
                        flat: true,
                    },
                ],
            });
        },
        focusTab: function(tabId) {
            if (!window.GCMSUI) {
                return;
            }
            window.GCMSUI.focusEditorTab(tabId);
        },

        show: function () {
            // No op
        },
        hide: function () {
            // No op
        }
    });

    return GCMSUISurface;
});
