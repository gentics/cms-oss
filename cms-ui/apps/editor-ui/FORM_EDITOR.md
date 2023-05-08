# GCMS Editor Form Editor Configuration

## Purpose
This configuration file allows for customers to have different sets of input elements available in the form editor.
This guide is intended for developers who are configuring and/or extending the functionality of the form editor for the Gentics CMS UI.

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
    |   |- form-editor.json
    |
    |- scripts/
        |
        |- index.js
```

## The form-editor.json file

The `form-editor.json` file contains an object that configures the available types of input elements and defines their properties.
If the `form-editor.json` file does not exist, a default set of input elements will be available.
Otherwise, the input elements configured in the `form-editor.json` file will be available exclusively.

### Additional Form Types

The Editor UI supports multiple form types. There are `"generic"` and `"poll"`. A form of type `"generic"` uses the default configuration, residing in `form-editor.json`. For all other form types (e.g. `"poll"`) the Editor UI tries to fetch the file `form-[FORM TYPE]-editor.json` (e.g. `form-poll-editor.json`) from the same directory. If no corresponding file is found, a default set of input elements will be available.

