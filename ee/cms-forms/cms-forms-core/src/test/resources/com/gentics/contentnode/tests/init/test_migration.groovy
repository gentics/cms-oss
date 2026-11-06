// Migrate the columns setting to numberOfColumns
if (element && element.columns_i18n && element.columns_i18n.de) {
    uiProperty.formGridOptions.numberOfColumns = element.columns_i18n.de as Integer
}

// Migrate the tooltip to the description
if (element && element.tooltip_i18n) {
    uiProperty.description = element.tooltip_i18n
}

// Migrate the placeholder to the label
if (element && element.placeholder_i18n) {
    uiProperty.label = element.placeholder_i18n
}