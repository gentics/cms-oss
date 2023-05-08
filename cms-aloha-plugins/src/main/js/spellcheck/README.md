Alhoa-Editor Spell check plugin
===============================

This plugin uses the [LanguageTool proofreading service][1] to check spelling
in the current editable.

Setup
-----

The plugin assumes that an instance of the LanguageTool is available under
`/spelling`.

Configuration
-------------

In the configuration a mapping can be defined from the two character language
codes the CMS uses to language variant codes LanguageTool uses, as well as a
default language when there is no suitable mapping for the language of the
edited page.

Furthermore a list of rules which should be disabled during spell checking can
be provided

### Example

This example will map german and english to austrian german and US english
respectively as well as setting US english as the default language. Also the
rules warning about unpaired brackets and consecutive sentences starting with
the same word will be disabled.

```javascript
Aloha.settings.plugins.spellchecker = {
	languageCodes: {
		de: 'de-AT',
		en: 'en-US'
	},
	defaultLanguage: 'en-US',
	disabledRules: 'EN_UNPAIRED_BRACKETS,ENGLISH_WORD_REPEAT_BEGINNING_RULE'
}
```

[1]: https://languagetool.org/
