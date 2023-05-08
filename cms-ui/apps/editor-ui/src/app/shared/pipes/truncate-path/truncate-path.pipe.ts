import { Pipe, PipeTransform } from '@angular/core';

const ELLIPSES = '...';

/**
 * Truncates a long path (long = longer than maxLength) but keeps the
 * first and last segments. The resulting path length will be
 * equal or less than maxLength.
 *
 * my-project/some/very/long/path/to/a/file.ext
 * ->
 * my-project/.../file.ext
 */
@Pipe({ name: 'truncatePath' })
export class TruncatePathPipe implements PipeTransform {
    transform(value: string, maxLength: number = 50, delimiter: string = '/'): any {
        if (typeof value !== 'string' || value.length <= maxLength) {
            return value;
        }
        // flag which gets set to true if one of the segments itself must be truncated,
        // as would be the case with very low maxLength value or a very long
        // first or last segment.
        let truncatingSegments = false;

        let segments = removeTrailingDelimiter(value, delimiter).split(delimiter);
        while (maxLength < getPathLength(segments, delimiter, !truncatingSegments)) {
            if (segments.length === 2) {
                truncatingSegments = true;
                segments = truncateFirstSegment(segments, delimiter, maxLength, (s, e) => s.substring(0, s.length - e));
            } else if (segments.length === 1) {
                truncatingSegments = true;
                segments = truncateFirstSegment(segments, delimiter, maxLength, (s, e) => s.substring(e));
            } else {
                segments.splice(1, 1);
            }
        }

        if (segments.length === 2 && truncatingSegments) {
            segments[0] += ELLIPSES;
        } else if (segments.length === 1) {
            segments[0] = ELLIPSES + segments[0];
        } else {
            segments.splice(1, 0, ELLIPSES);
        }

        return restoreTrailingDelimiter(segments.join(delimiter), delimiter);
    }
}

/**
 * Returns the calculated length of the path segments once they have been joined and
 * had an ellipses added.
 */
function getPathLength(segments: string[], delimiter: string, assumeExtraItem: boolean = true): number {
    let extraItemDelimiterLength = assumeExtraItem ? delimiter.length : 0;
    return segments.join(delimiter).length + ELLIPSES.length + extraItemDelimiterLength;
}

/**
 * Replaces a trailing delimiter with a placeholder so that the last segment does not get split.
 */
function removeTrailingDelimiter(value: string, delimiter: string): string {
    let trailingDelimiterRe = new RegExp(`${delimiter}\\s*$`);
    return value.replace(trailingDelimiterRe, '_'.repeat(delimiter.length));
}

/**
 * Restores the trailing delimiter if it had been removed.
 */
const restoreTrailingDelimiter = (value: string, delimiter: string): string => value.replace(/_+$/, delimiter);

/**
 * When there are only 2 or fewer segments remaining, yet the length is still greater than
 * maxLength, we need to start truncating the first segment string itself.
 */
function truncateFirstSegment(segments: string[],
    delimiter: string,
    maxLength: number,
    truncateFn: (segment: string, excess: number) => string): string[] {
    let excess = getPathLength(segments, delimiter, false) - maxLength;
    let firstSegment = segments[0];
    if (excess < firstSegment.length) {
        segments[0] = truncateFn(firstSegment, excess);
    } else {
        segments.splice(0, 1);
    }
    return segments;
}
