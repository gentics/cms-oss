define([
    'ui/surface',
    'i18n!ui/nls/i18n'
], function (
    Surface,
    i18n
) {
    'use strict';

    function internalOpenDialog(config, resolveFn) {
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
                // TODO: Handle error
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

        show: function () {
            // No op
        },
        hide: function () {
            // No op
        }
    });

    return GCMSUISurface;
});
