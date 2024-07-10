# Link Checker Custom Tool

This custom tool is used as a UI for the Link Checker functionality in the CMS to display links in all pages and specifically display the broken links.

The Link-Checker itself is a Commercial Feature and is *not* included in the Open-Source version.
This project will not work without it.

## Integration into Gentics CMS

To integrate this custom tool into Gentics CMS, you have to add it to the configuration:

```yml
custom_tools:
  - id: 1 # or whatever ID you want this tool to have
    key: "linkchecker" # this must be the key for this Custom Tool!
    toolUrl: "/tools/link-checker/?sid=${SID}"
    iconUrl: "link" # Material Icon name or a URL
    newtab: false
    name:
      de: "Link Checker"
      en: "Link Checker"
```
