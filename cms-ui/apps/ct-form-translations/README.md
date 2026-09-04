# Form Translations Custom Tool

Custom Tool to manage translations for all forms in Gentics CMS.

## Integration into Gentics CMS

The custom tool is already bundled into the CMS (Enterprise Edition),
and only needs to be added to the `custom_tools` configuration.
See https://www.gentics.com/Content.Node/cmp8/guides/admin_custom_tools.html for more information.

Note that the tool requires the `forms` feature to be properly licensed and the feature has to be enabled,
or otherwise the tool is unusable, as the backend doesn't exist for it.

```yml
custom_tools:
  - # These settings have to be set exactly like this, otherwise it won't work correctly.
    id: form-translations
    key: form_translations
    toolUrl: /tools/form-translations/?sid=${SID}
    newtab: false
    # These can be changed if needed, but should stay the same for consistency
    iconUrl: language
    name:
      de: Formular-Übersetzungen
      en: Form Translations
```

## Local development

Starting the development server works just like for any other app, by running `npm start ct-form-translations`.
Make sure the `proxy.conf.json` in the root is properly configured before starting.

Since this custom tool expects to be run in the editor-ui with already handles the authentication, you need to manually set the `GCMSUI_sid` into the localStorage.
The easiest way is to open the editor- or admin-ui, login, get the `GCMSUI_sid` from there, open the local instance http://localhost:4200, and set the sid into the localStorage, then reload the page.
