import {Pipe} from '@angular/core';

/**
 * Pipe for making the first letter in each word uppercase.
 * Based on https://github.com/AngularClass/angular2-examples/blob/master/large-app/pipes/CapitalizePipe.ts
 */
@Pipe({
    name: 'capitalize',
    standalone: false
})
export class CapitalizePipe {
    regexp: RegExp = /([^\W_]+[^\s-]*) */g;

    transform(value: string, allWords: boolean = false): any {
        if (!value) {
            return '';
        } else if (allWords) {
            return value.replace(this.regexp, this.capitalizeWord);
        } else {
            return this.capitalizeWord(value);
        }
    }

    capitalizeWord(txt: string): string {
        return txt.charAt(0).toUpperCase() + txt.substr(1).toLowerCase();
    }
}
