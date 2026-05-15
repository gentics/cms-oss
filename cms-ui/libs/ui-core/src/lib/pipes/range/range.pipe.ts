import { Pipe, PipeTransform } from '@angular/core';

@Pipe({
    name: 'gtxRange',
    standalone: false,
})
export class RangePipe implements PipeTransform {

    transform(min: number, max?: number, inclusive?: boolean): number[];
    transform(max: number, inclusive?: boolean): number[];
    transform(minOrMax: number, maxOrInclusive?: number | boolean, inclusive: boolean = true): number[] {
        if (!Number.isInteger(minOrMax)) {
            return [];
        }
        let min = 0;
        let max = 0;

        if (maxOrInclusive != null) {
            if (typeof maxOrInclusive === 'boolean') {
                max = minOrMax;
                inclusive = maxOrInclusive;
            } else if (typeof maxOrInclusive === 'number') {
                min = minOrMax;
                max = maxOrInclusive;
            } else {
                max = minOrMax;
            }
        } else {
            max = minOrMax;
        }

        const out = [];
        out.length = (max + (inclusive ? 1 : 0)) - min;
        return Array.from(out).map((_, idx) => idx + min);
    }
}
