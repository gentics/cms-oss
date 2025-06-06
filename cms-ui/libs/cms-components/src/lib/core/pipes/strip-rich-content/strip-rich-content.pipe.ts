import { Pipe, PipeTransform } from '@angular/core';
import { extractRichContent, getDisplayTextFromContent } from '../../../common/utils/rich-content';

/**
 * Pipe which will take in any potential rich-content text, parse it's rich-content,
 * and return a stripped version of the text with only the display text.
 * This is for places where we have to print out the content, but the rich-content
 * can't be inserted.
 */
@Pipe({
    name: 'gtxStripRichContent',
    standalone: false
})
export class StripRichContentPipe implements PipeTransform {

    public transform(value: string): string {
        return extractRichContent(value || '').map(part => {
            return typeof part === 'string' ? part : getDisplayTextFromContent(part);
        }).join('');
    }
}
