import { Pipe, PipeTransform } from '@angular/core';

@Pipe({
    name: 'gtxRange',
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
            if (typeof maxOrInclusive === 'number') {
                min = minOrMax;
                max = maxOrInclusive;
            } else {
                min = minOrMax;
                inclusive = !!maxOrInclusive;
            }
        } else {
            max = minOrMax;
        }

        const out = [];
        out.length = (max - (inclusive ? 0 : 1)) - min;
        return Array.from(out).map((_, idx) => idx + min);
    }
}
