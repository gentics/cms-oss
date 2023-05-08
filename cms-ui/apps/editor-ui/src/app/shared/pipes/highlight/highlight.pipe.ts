import {Pipe, PipeTransform} from '@angular/core';
import {DomSanitizer, SafeHtml} from '@angular/platform-browser';
import * as DOMPurify from 'dompurify';

/**
 * Adds highlighting markup to a string based on matching against a provided term, and
 * outputs SafeHtml which can then be bound directly into a template element's [innerHTML] property.
 */
@Pipe({ name: 'highlight' })
export class HighlightPipe implements PipeTransform {

    constructor(private sanitizer: DomSanitizer) {}

    transform(value: string, term: string = ''): SafeHtml {
        // abort if value is not set
        if (!value) {
            return;
        }
        if (typeof term !== 'string' || term === '') {
            value = DOMPurify.sanitize(value);
            return this.sanitizer.bypassSecurityTrustHtml(value);
        }
        let escapedTerm = term.replace(/[-\/\\^$*+?.()|[\]{}]/g, '\\$&');
        let re = new RegExp(`(${escapedTerm})`, 'gi');
        let rawHtml = value.replace(re, `<span class="hl-pipe">$1</span>`);
        rawHtml = DOMPurify.sanitize(rawHtml);
        return this.sanitizer.bypassSecurityTrustHtml(rawHtml);
    }
}
