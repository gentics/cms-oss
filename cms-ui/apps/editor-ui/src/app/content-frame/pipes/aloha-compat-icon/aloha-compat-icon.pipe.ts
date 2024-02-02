import { Pipe, PipeTransform } from '@angular/core';

const MAPPING: Record<string, string> = {
    'aloha-icon-bold': 'format_bold',
    'aloha-icon-italic': 'format_italic',
    'aloha-icon-underline': 'format_underlined',
    'aloha-icon-strikethrough': 'format_strikethrough',
    'aloha-icon-subscript': 'subscript',
    'aloha-icon-superscript': 'superscript',
    'aloha-icon-abbr': 'book',
    'aloha-icon-emphasis': 'format_italic',
    'aloha-icon-strong': 'format_bold',
    'aloha-icon-code': 'code',
    'aloha-icon-clear': 'format_clear',

    'aloha-icon-typography': 'format_size',
    'aloha-icon-paragraph': 'format_paragraph',
    'aloha-icon-h1': 'format_h1',
    'aloha-icon-h2': 'format_h2',
    'aloha-icon-h3': 'format_h3',
    'aloha-icon-h4': 'format_h4',
    'aloha-icon-h5': 'format_h5',
    'aloha-icon-h6': 'format_h6',
    'aloha-icon-pre': 'segment',

    'aloha-icon-link': 'link',
    'aloha-icon-unlink': 'link_off',
    'aloha-icon-anchor': 'anchor',

    'aloha-icon-createTable': 'table',
    'aloha-icon-mergecells': 'cell_merge',
    'aloha-icon-splitcells': 'arrows_outward',
    'aloha-icon-deletetable': 'delete',
    'aloha-icon-table-caption': 'title',
    'aloha-icon-addcolumnleft': 'splitscreen_left',
    'aloha-icon-addcolumnright': 'splitscreen_right',
    'aloha-icon-deletecolumns': 'delete',
    'aloha-icon-columnheader': 'leaderboard',
    'aloha-icon-addrowbefore': 'splitscreen_top',
    'aloha-icon-addrowafter': 'splitscreen_bottom',
    'aloha-icon-deleterows': 'delete',
    'aloha-icon-rowheader': 'leaderboard',

    'aloha-icon-toggledragdrop': 'drag_pan',
    'aloha-icon-tree': 'account_tree',

    'aloha-icon-indent': 'format_indent_increase',
    'aloha-icon-outdent': 'format_indent_decrease',

    'aloha-icon-characterpicker': 'special_character',
    'aloha-icon-textcolor-color': 'format_color_text',
    'aloha-icon-textcolor-background-color': 'format_color_fill',
};
const FALLBACK_ICON = 'question_mark';

@Pipe({
    name: 'gtxAlohaCompatIcon',
})
export class AlohaCompatIconPipe implements PipeTransform {
    public transform(value: string): string {
        if (typeof value !== 'string') {
            return '';
        }
        const iconPart = value.split(' ').find(part => part.startsWith('aloha-icon-'));
        return MAPPING[iconPart] || MAPPING[value] || value || FALLBACK_ICON;
    }
}
