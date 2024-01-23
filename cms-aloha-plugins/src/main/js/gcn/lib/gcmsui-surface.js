define([
    'ui/surface'
], function(
    Surface
) {
    'use strict';

    var GCMSUISurface = Surface.extend({
        _context: null,
        _settings: null,

        _constructor: function(context, settings) {
            this._super(context);
            this._context = context
        },

        adoptInto: function(slot, component) {
            if (!window.GCMSUI) {
                return;
            }
            window.GCMSUI.registerComponent(slot, component);
        },

        unadopt: function(slot) {
            if (!window.GCMSUI) {
                return;
            }
            window.GCMSUI.unregisterComponent(slot);
        },

        openDynamicDropdown: function(componentName, config) {
			if (!window.GCMSUI) {
                return Promise.reject(new Error('GCMSUI is not defined!'));
            }
            return window.GCMSUI.openDynamicDropdown(config, componentName);
		},
		openDynamicModal: function(config) {
            if (!window.GCMSUI) {
                return Promise.reject(new Error('GCMSUI is not defined!'));
            }
			return window.GCMSUI.openDynamicModal(config);
		},

        show: function() {
            // No op
        },
        hide: function() {
            // No op
        }
    });

    return GCMSUISurface;
});
