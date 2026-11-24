define([
    'aloha/core',
	'aloha/plugin',
    'ui/ui-plugin',
	'ui/overlayElement',
    'gcn/gcn-util',
    'gcn/gcmsui-surface',
], function(
    Aloha,
    Plugin,
    UiPlugin,
    OverlayElement,
    Util,
    GCMSUISurface
) {
    'use strict';
    
    var plugin = {
        init: function(withLinks) {
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

            if (withLinks) {
                try {
                    Aloha.require(['link/link-plugin', 'gcn/gcn-links'], (LinkPlugin, GCNLinks) => {
                        GCNLinks.interjectLinkPlugin(LinkPlugin, function(context) {
                            context.controls.url.options.pickerTypes = ['page', 'file'];

                            delete context.controls.title;
                            delete context.controls.lang;

                            return context;
                        });
                    });
                } catch (err) {
                    console.error('Could not setup gcn-link integration in texteditor-plugin!', err);
                }
            }
        },
    };

    plugin = Plugin.create('texteditor', plugin);

    return plugin;

});
