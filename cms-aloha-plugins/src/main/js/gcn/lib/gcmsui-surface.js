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
    });

    return GCMSUISurface;
});
