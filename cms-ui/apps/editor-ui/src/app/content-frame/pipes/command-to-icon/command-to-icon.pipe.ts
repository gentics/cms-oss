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
} from '../../../common/models/aloha-integration';

@Pipe({
    name: 'gtxCommandToPipe',
})
export class CommandToIconPipe implements PipeTransform {

    transform(value: string): string {
        switch (value) {
            case COMMAND_STYLE_BOLD:
                return 'format_bold';
            case COMMAND_STYLE_ITALIC:
                return 'format_italic';
            case COMMAND_STYLE_UNDERLINE:
                return 'format_underlined';
            case COMMAND_STYLE_STRIKE_THROUGH:
                return 'format_strikethrough';
            case COMMAND_STYLE_CODE:
                return 'code';
            case COMMAND_STYLE_QUOTE:
                return 'format_quote';
            case COMMAND_STYLE_CITATION:
                return 'comment_bank';
            case COMMAND_STYLE_ABBREVIATION:
                return 'book'
            case COMMAND_STYLE_SUBSCRIPT:
                return 'subscript';
            case COMMAND_STYLE_SUPERSCRIPT:
                return 'superscript';
            case COMMAND_LIST_UNORDERED:
                return 'format_list_bulleted';
            case COMMAND_LIST_ORDERED:
                return 'format_list_numbered';
            case COMMAND_TYPOGRAPHY_PARAGRAPH:
                return 'format_paragraph';
            case COMMAND_TYPOGRAPHY_HEADING1:
                return 'format_h1';
            case COMMAND_TYPOGRAPHY_HEADING2:
                return 'format_h2';
            case COMMAND_TYPOGRAPHY_HEADING3:
                return 'format_h3';
            case COMMAND_TYPOGRAPHY_HEADING4:
                return 'format_h4';
            case COMMAND_TYPOGRAPHY_HEADING5:
                return 'format_h5';
            case COMMAND_TYPOGRAPHY_HEADING6:
                return 'format_h6';
            case COMMAND_TYPOGRAPHY_PREFORMATTED:
                return 'segment';
            default:
                return value;
        }
    }
}
