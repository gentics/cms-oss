import { Pipe, PipeTransform } from '@angular/core';
import { DomSanitizer, SafeHtml, SafeResourceUrl } from '@angular/platform-browser';

/**
 * Use this Pipe with great CAUTION only for sources you can verify to be secure.
 * @example
 * ```
 * <iframe width="100%" height="300" [src]="url | safe"></iframe>
 * ```
 * @see https://angular.io/guide/security#xss
 */
@Pipe({
    name: 'safe',
    standalone: false,
})
export class SafePipe implements PipeTransform {

    constructor(
        private sanitizer: DomSanitizer,
    ) {}

    transform(content: string, type: 'url' | 'html' = 'url'): SafeResourceUrl | SafeHtml {
        switch (type) {
            case 'html':
                return this.sanitizer.bypassSecurityTrustHtml(content);
            default:
            case 'url':
                return this.sanitizer.bypassSecurityTrustResourceUrl(content);
        }
    }
}
