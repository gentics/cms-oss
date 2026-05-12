define([
    'aloha/core',
	'jquery',
	'PubSub',
	'aloha/plugin',
    'ui/ui-plugin',
	'ui/dialog',
	'ui/overlayElement',
    'gcn/gcn-util',
    'gcn/gcmsui-surface',
], function(
    Aloha,
    jQuery,
    PubSub,
    Plugin,
    UiPlugin,
    Dialog,
    OverlayElement,
    Util,
    GCMSUISurface
) {
    'use strict';
    
    var plugin = {
        init: function() {
            Util.withinCMS(function () {
                // Create the GCMSUI Surface and set it as active.
                // This forces the UI to be rendered in the GCMS UI instead of the Aloha Page/context.
                var gcmsuiSurface = new GCMSUISurface(UiPlugin.getContext(), UiPlugin.getToolbarSettings());
                UiPlugin.setActiveSurface(gcmsuiSurface, true, true);

                // Apply the correct error class from the UI, so we can do correct checks in here as well.
				if (window.GCMSUI.closeErrorClass) {
					OverlayElement.OverlayCloseError = window.GCMSUI.closeErrorClass;
				}
            });
        },
    };

    plugin = Plugin.create('texteditor', plugin);

    return plugin;

});
