import { Pipe, PipeTransform } from '@angular/core';
import {
    COMMAND_STYLE_BOLD,
    COMMAND_STYLE_ITALIC,
    COMMAND_STYLE_UNDERLINE,
    COMMAND_STYLE_CODE,
    COMMAND_STYLE_QUOTE,
    COMMAND_STYLE_SUBSCRIPT,
    COMMAND_STYLE_SUPERSCRIPT,
    COMMAND_LIST_UNORDERED,
    COMMAND_LIST_ORDERED,
    COMMAND_TYPOGRAPHY_PARAGRAPH,
    COMMAND_TYPOGRAPHY_HEADING1,
    COMMAND_TYPOGRAPHY_HEADING2,
    COMMAND_TYPOGRAPHY_HEADING3,
    COMMAND_TYPOGRAPHY_HEADING4,
    COMMAND_TYPOGRAPHY_HEADING5,
    COMMAND_TYPOGRAPHY_HEADING6,
    COMMAND_STYLE_STRIKE_THROUGH,
    COMMAND_STYLE_CITATION,
    COMMAND_STYLE_ABBREVIATION,
    COMMAND_TYPOGRAPHY_PREFORMATTED,
    COMMAND_SPECIAL_STYLE_REMOVE_FORMAT,
    COMMAND_LIST_DESCRIPTION,
    COMMAND_LINK,
} from '../../../common/models/aloha-integration';

const COMMAND_ICON_MAP: Record<string, string> = {
    [COMMAND_STYLE_BOLD]:                   'format_bold',
    [COMMAND_STYLE_ITALIC]:                 'format_italic',
    [COMMAND_STYLE_UNDERLINE]:              'format_underlined',
    [COMMAND_STYLE_STRIKE_THROUGH]:         'format_strikethrough',
    [COMMAND_STYLE_CODE]:                   'code',
    [COMMAND_STYLE_QUOTE]:                  'format_quote',
    [COMMAND_STYLE_CITATION]:               'comment_bank',
    [COMMAND_STYLE_ABBREVIATION]:           'book',
    [COMMAND_STYLE_SUBSCRIPT]:              'subscript',
    [COMMAND_STYLE_SUPERSCRIPT]:            'superscript',
    [COMMAND_SPECIAL_STYLE_REMOVE_FORMAT]:  'format_clear',
    [COMMAND_LIST_UNORDERED]:               'format_list_bulleted',
    [COMMAND_LIST_ORDERED]:                 'format_list_numbered',
    [COMMAND_LIST_DESCRIPTION]:             'label',
    [COMMAND_TYPOGRAPHY_PARAGRAPH]:         'format_paragraph',
    [COMMAND_TYPOGRAPHY_HEADING1]:          'format_h1',
    [COMMAND_TYPOGRAPHY_HEADING2]:          'format_h2',
    [COMMAND_TYPOGRAPHY_HEADING3]:          'format_h3',
    [COMMAND_TYPOGRAPHY_HEADING4]:          'format_h4',
    [COMMAND_TYPOGRAPHY_HEADING5]:          'format_h5',
    [COMMAND_TYPOGRAPHY_HEADING6]:          'format_h6',
    [COMMAND_TYPOGRAPHY_PREFORMATTED]:      'segment',
    [COMMAND_LINK]:                         'link',
};

const OFF_COMMAND_ICON_MAP: Record<string, string> = {
    [COMMAND_LINK]:                         'link_off',
};

@Pipe({
    name: 'gtxCommandToPipe',
})
export class CommandToIconPipe implements PipeTransform {

    transform(value: string, type: 'on' | 'off' = 'on', returnEmpty: boolean = false): string {
        switch (type) {
            case 'on':
                if (COMMAND_ICON_MAP[value]) {
                    return COMMAND_ICON_MAP[value]
                }
                break;

            case 'off':
                if (OFF_COMMAND_ICON_MAP[value]) {
                    return OFF_COMMAND_ICON_MAP[value] || value;
                }
                break;
        }

        return returnEmpty ? '' : value;
    }
}
