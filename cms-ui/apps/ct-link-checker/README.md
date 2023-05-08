# Link Checker Custom Tool

This custom tool is used as a UI for the Link Checker functionality in the CMS to display links in all pages and specifically display the broken links.

## Integration into Gentics CMS

To integrate this custom tool into Gentics CMS for development purposes, run the development server and add the following configuration to
one of the .conf files in the conf.d folder of your Gentics CMS installation:

```PHP
<?php

$CUSTOM_TOOLS[] = array(
    "id" => 1,
    "key" => "linkchecker",
    "toolUrl" => 'http://localhost:4200/?sid=${SID}',
    "iconUrl" => "link",
    "name" => array(
        "de" => "Link Checker",
        "en" => "Link Checker"
    ),
    "newtab" => false
);
```
