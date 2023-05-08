import {Injectable} from '@angular/core';

const IFRAME_STYLES = require('./iframe-styles/iframe-styles.precompile-scss');

/**
 * This registers a Blob URL which points to styles for IFrames with
 * a custom Tag or TagProperty editor.
 */
@Injectable()
export class IFrameStylesService {

    /** The URL of the styles for the IFrames */
    get stylesUrl(): string {
        return this.gcmsStylesBlobUrl;
    }

    private gcmsStylesForIFrame: Blob;
    private gcmsStylesBlobUrl: string;

    constructor() {
        const iFrameStylesStr = IFRAME_STYLES && IFRAME_STYLES.default ? IFRAME_STYLES.default : IFRAME_STYLES;
        this.gcmsStylesForIFrame = new Blob([ iFrameStylesStr ], { type : 'text/css' });
        this.gcmsStylesBlobUrl = window.URL.createObjectURL(this.gcmsStylesForIFrame);
    }

}
