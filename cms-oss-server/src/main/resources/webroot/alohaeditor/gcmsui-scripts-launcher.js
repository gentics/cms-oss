/**
 * This performs the child IFrame initialization logic for IFrames loaded inside the GCMS UI.
 *
 * We use this to call methods on the global GCMSUI object, which are provided by the GCMS UI.
 */
(function() {

    var initChildIFrame = function() {
        var gcmsUi = window.parent.GCMSUI_childIFrameInit(window, document);
        gcmsUi.runPreLoadScript();
    }

    var runPostLoadScript = function() {
        GCMSUI.runPostLoadScript();
    }

    // Run the Pre-Load Script on DOMContentLoaded
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', initChildIFrame);
    } else {
        // `DOMContentLoaded` has already fired
        initChildIFrame()
    }


    // Run the Post-Load Script when the load event is fired.
    if (document.readyState === 'complete') {
        // `load` has already fired
        runPostLoadScript();
    } else {
        window.addEventListener('load', runPostLoadScript);
    }

})();
