import { Pipe, PipeTransform } from '@angular/core';

@Pipe({
    name: 'replaceEscapedCharacters',
    standalone: false
})
export class ReplaceEscapedCharactersPipe implements PipeTransform {
    transform(newString: string): string {
        if (newString) {
            newString = newString.replace(/&nbsp;/g, ' ')
                                 .replace(/&lt;/g, '<')
                                 .replace(/&gt;/g, '>')
                                 .replace(/&amp;/g, '&')
                                 .replace(/&quot;/g, '"');
        }
        return newString;
    }
}
