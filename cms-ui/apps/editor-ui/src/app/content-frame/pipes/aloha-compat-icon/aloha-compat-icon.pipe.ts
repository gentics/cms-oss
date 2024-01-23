import { Pipe, PipeTransform } from '@angular/core';

const MAPPING: Record<string, string> = {
    'aloha-icon-bold': 'format_bold',
    'aloha-icon-italic': 'format_italic',
    'aloha-icon-underline': 'format_underlined',
    'aloha-icon-strikethrough': 'format_strikethrough',
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
        return MAPPING[iconPart] || MAPPING[value] || FALLBACK_ICON;
    }
}
