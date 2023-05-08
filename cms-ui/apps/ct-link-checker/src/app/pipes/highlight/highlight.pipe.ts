import { Pipe, PipeTransform, SecurityContext } from '@angular/core';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';

/**
 * Adds highlighting markup to a string based on matching against a provided term, and
 * outputs SafeHtml which can then be bound directly into a template element's [innerHTML] property.
 */
@Pipe({ name: 'highlight' })
export class HighlightPipe implements PipeTransform {

    constructor(private sanitizer: DomSanitizer) {}

    transform(value: string, term: string = ''): SafeHtml {
        if (typeof term !== 'string' || term === '') {
            return this.sanitizer.sanitize(SecurityContext.HTML, value);
        }
        const escapedTerm = term.replace(/[-\/\\^$*+?.()|[\]{}]/g, '\\$&');
        const re = new RegExp(`(${escapedTerm})`, 'gi');
        const rawHtml = value.replace(re, `<span class="hl-pipe">$1</span>`);
        return this.sanitizer.sanitize(SecurityContext.HTML, rawHtml);
    }
}
