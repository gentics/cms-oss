import { Pipe, PipeTransform } from '@angular/core';

const ELLIPSES = '+';

/**
 * Truncates a long number (big = longer than maxDigits).
 * The number will be digits count 9's and +.
 *
 * 15664 with maxLength 2
 * ->
 * 99+
 */
@Pipe({
    name: 'truncateNumber',
    standalone: false
})
export class TruncateNumberPipe implements PipeTransform {
    transform(value: string | number, maxLength: number = 2): any {
        if (!(typeof value === 'string' || typeof value === 'number') || String(value).length <= maxLength) {
            return value;
        }

        return '9'.repeat(maxLength) + ELLIPSES;
    }
}
