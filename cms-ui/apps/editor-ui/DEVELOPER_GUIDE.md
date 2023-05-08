# GCMS Editor UI Developer Guide

## Purpose
This guide is intended for developers who are configuring and/or implementing extensions for the Gentics CMS UI.

## Customer Config

All customer-specific configuration and code should reside in the `customer-config` directory which should be a sibling of the
`ui` folder. The structure of this folder should be as follows:

```
<webserver root>/
|
|- ui/  # the GCMS UI
|
|- customer-config/
    |
    |- config/
    |
    |- scripts/
        |
        |-index.js
```

### Config Directory

This directory contains any config files required by the UI. Currently the UI supports the following config files:

* Keycloak: `customer-config/config/keycloak.json`
* [UI Overrides](./UI_OVERRIDES.md): `customer-config/config/ui-overrides.json`
* [Form Editor](./FORM_EDITOR.md): `customer-config/config/form-editor.json`
  * [Support for additional Form Types](./FORM_EDITOR.md#Additional-Form-Types): `customer-config/config/form-[FORM TYPE]-editor.json`

### Scripts Directory

This directory contains JavaScript which can be executed within the context of the Gentics CMS IFrame when previewing or
editing pages, or editing the object properties of pages, files, folders or images.

The UI will look for the file `customer-config/scripts/index.js` which should export a function like so:

```JavaScript
module.exports = function someCustomScript(GCMSUI) {
    // custom code goes here
};
```

This function will be passed an object, `GCMSUI`, which contains data and methods which can be useful in developing
custom functionality. 

#### GCMSUI Object

```TypeScript

export interface GCMSUI {
    /** An object containing useful information about the current state of the UI */
    appState: ExposedPartialState;
    /** Registers a callback which is invoked whenever the contents of the appState change */
    onStateChange: (handler: (state: ExposedPartialState) => any) => void;
    /** Paths to various endpoints in use by the UI */
    paths: {
        apiBaseUrl: string;
        alohapageUrl: string;
        contentnodeUrl: string;
        imagestoreUrl: string;
    };
    /**
     * Makes a GET request to an endpoint of the GCMS REST API and returns the parsed JSON object.
     * The endpoint should not include the base URL of the REST API, but just the endpoint as per
     * the documentation, e.g. `/folder/create`.
     */
    restRequestGET: (endpoint: string, params?: object) => Promise<object>;
    /**
     * Makes a POST request to an endpoint of the GCMS REST API and returns the parsed JSON object.
     * The endpoint should not include the base URL of the REST API, but just the endpoint as per
     * the documentation, e.g. `/folder/create`.
     */
    restRequestPOST: (endpoint: string, data: object, params?: object) => Promise<object>;
    /**
     * Tells the editor whether the page content has been modified. When set to `true`, the
     * "save" button will be enabled.
     */
    setContentModified: (modified: boolean) => void;
}

/** The app state as exposed by the GCMSUI.appState property */
interface ExposedPartialState {
    currentItem: Folder | Page | File | Image | Node;
    editMode: 'preview' | 'edit' | 'editProperties' | 'previewVersion' | 'compareVersionContents' | 'compareVersionSources';
    pageLanguage?: {
        /** The numeric ID of the language */
        id: number;
        /** A 2-letter language code like "en" / "de" / "it". */
        code: string;
        /** A human-readable name of the language like "Italiano (Italian)" */
        name: string;
    };
    sid: number;
    uiLanguage: 'en' | 'de';
    uiVersion: string;
    userId: number;
}
```

#### Example Customer Script

```JavaScript

module.exports = function customerScript(GCMSUI) {
    
    const launchAssetPickerButton = document.querySelector('#pick-asset');
    let uiLanguage = state.uiLanguage;
    
    if (launchAssetPickerButton) {
        launchAssetPickerButton.addEventListener('click', e => {
            e.preventDefault();
            openAssetPicker();
        });
    }
    
    GCMSUI.onStateChange(state => {
        // Update the value of uiLanguage whenever the user changes it in the UI
        uiLanguage = state.uiLanguage;
    });
    
    function openAssetPicker() {
        const state = GCMSUI.appState;
        
        someThirdPartyAssetPickerScript
            .open({ language: uiLanguage }) // some imaginary API to a 3rd party script
            .then(result => {
                // make a POST request to the GCMS REST API to create a new page
                // based on the returned data
                GCMSUI.restRequestPOST('/page/create', {
                  folderId: state.currentItem.folderId,
                  templateId: state.currentItem.templateId,
                  language: state.pageLanguage.id,
                  nodeId: state.inheritedFromId,
                  pageName: result.asset.title,
                  description: result.asset.description
                });
            });
    }
}

```
