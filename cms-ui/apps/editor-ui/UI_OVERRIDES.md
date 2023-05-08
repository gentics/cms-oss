# GCMS Editor UI Overrides

## Purpose
Some customers insist on having different behavior, e.g. to upload files.
Instead of adding extra behavior to the application, we allow customers to configure the behavior of specific parts of the UI.
This guide is intended for developers who are configuring and/or implementing overrides for the Gentics CMS UI.

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
    |   |
    |   |- ui-overrides.json
    |
    |- scripts/
        |
        |- index.js
```

## The overrides.json file

A simple JSON file with one single object, every key resembles an assigned slot in the GCMSUI which it configures.
For example, to disable the "write new message" button, the configuration would look as follows:
```json
{
    "composeMessageButton": {
        "disable": true
    }
}
```
The relevant part in the GCMSUI code:
```html
<gtx-button
    overrideSlot="composeMessageButton"
    (click)="openMessageComposer()">
        <icon>envelope</icon> New message
</gtx-button>
```

## Name of overrides

For a list of UI elements that can be overridden, refer to [ui-overrides.model.ts](src/app/shared/providers/ui-overrides/ui-overrides.model.ts),
or (just to be sure) search for "overrideSlot" in the repository.

## Options for overriding

### 1. Hide an element

```json
{
    "elementSlot": { "hide": true }
}
```

### 2. Disable an element

```json
{
    "elementSlot": { "disable": true }
}
```

### 3. Open a [custom tool](https://www.gentics.com/Content.Node/guides/admin_custom_tools.html) instead of a buttons default action

```json
{
    "elementSlot": {
        "openTool": "tool-name",
        "toolPath": "path/in/the/url?with={{VARIABLE}}",
        "restartTool": false
    }
}
```

To consider:

1. Parameters interpolated in the tool path must be defined in the CMSUI

    ```html
    <gtx-button
        overrideSlot="elementSlot"
        [overrideParams]="{ VARIABLE: 1234 }">
    </gtx-button>
    ```

2. Some tools will redirect to third-party-servers which can not react to the navigation requests of the GCMS Tool API.
In this case, use `"restartTool": true` to destroy and re-open the tool when a button is clicked.

3. To use the `toolPath` inside a custom tool:

    ```typescript
    ToolApi.connect({
        navigate(urlInTool: string) {
            console.log('The UI wants to navigate to: ', urlInTool);
        }
    }).then(toolApi => {
        console.log('The UI started with path: ', toolApi.handshake.path);
    })
    ```


### Example of all options

```json
{
    "fileDragAndDrop": {
        "disable": true,
        "hide": true
    },
    "newFileButton": {
        "openTool": "demotool",
        "toolPath": "new-file?folder={{FOLDERID}}&node={{NODEID}}",
        "restartTool": true
    },
    "newImageButton": {
        "openTool": "demotool",
        "toolPath": "new-image?folder={{FOLDERID}}&nodeid={{NODEID}}",
        "restartTool": true
    },
    "replaceFileButton": {
        "disable": true
    },
    "replaceImageButton": {
        "hide": true
    }
}
```
