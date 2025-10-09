"use strict";

(() => {
    // Only load, when Aloha is actually defined/loaded.
    if (
        Aloha == null
        || typeof Aloha !== "object"
        || typeof Aloha.settings !== "object"
        || typeof Aloha.settings.plugins !== "object"
    ) {
        return;
    }

    // default formatting options
    // see https://www.alohaeditor.org/guides/plugin_format.html

    if (typeof Aloha.settings.plugins.format !== "object") {
        Aloha.settings.plugins.format = {};
    }
    Aloha.settings.plugins.format.config = ['b', 'i', 'u', 's', 'sub', 'sup', 'code', 'q', 'abbr', 'cite', 'removeFormat', 'foobar'];

    // table plugin
    Aloha.settings.plugins.table = {
        tableConfig: [
            { name: 'table-style-1', label: 'Table style 1', icon: 'table' },
            { name: 'table-style-2', label: 'Table style 2', icon: 'table' },
        ],
        columnConfig: [
            { name: 'column-style-1', label: 'Column style 1', icon: 'view_column' },
            { name: 'column-style-2', label: 'Column style 2', icon: 'view_column' },
        ],
        rowConfig: [
            { name: 'row-style-1', label: 'Row style 1', icon: 'table_rows_narrow' },
            { name: 'row-style-2', label: 'Row style 2', icon: 'table_rows_narrow' },
        ],
        cellConfig: [
            { name: 'cell-style-1', label: 'Cell style 1', icon: 'background_dot_small' },
            { name: 'cell-style-2', label: 'Cell style 2', icon: 'background_dot_small' },
        ],

        // add a class to all tables
        defaultClass: 'table',
        // add a class to all rows
        defaultRowClass: '',
        // add a class to all header rows
        defaultHeaderRowClass: '',
        // add a class to all cells
        defaultCellClass: '',
        // add a class to all header cells
        defaultHeaderCellClass: '',
        // wrapping div class
        wrapClass: 'table-responsive',
    };

})();
