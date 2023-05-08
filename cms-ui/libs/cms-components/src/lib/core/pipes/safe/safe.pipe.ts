import { Pipe, PipeTransform } from '@angular/core';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';

/**
 * Use this Pipe with great CAUTION only for sources you can verify to be secure.
 * @example
 * ```
 * <iframe width="100%" height="300" [src]="url | safe"></iframe>
 * ```
 * @see https://angular.io/guide/security#xss
 */
@Pipe({ name: 'safe' })
export class SafePipe implements PipeTransform {

    constructor(
        private sanitizer: DomSanitizer,
    ) {}

    transform(url: string): SafeResourceUrl {
        return this.sanitizer.bypassSecurityTrustResourceUrl(url);
    }
}
