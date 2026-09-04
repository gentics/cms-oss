/**
 * Matches a mime type against a list of allowed/disallowed types.
 * @example
 *   matchesMimeType('text/plain', 'text/*')              // true
 *   matchesMimeType('image/jpeg', 'text/*')              // false
 *   matchesMimeType('text/plain', 'text/*, image/*')     // true
 *   matchesMimeType('image/gif', 'image/*, !image/gif')  // false
 */
export function matchesMimeType(type: string, allowedTypes: string): boolean {
    if (typeof type !== 'string' || typeof allowedTypes !== 'string' || !allowedTypes) {
        return false;
    }

    if (allowedTypes === '*') {
        return true;
    }

    const regex = compilePatternsToRegex(allowedTypes);
    return regex.test(type);
}

const patternCache: { [k: string]: RegExp } = {};
function compilePatternsToRegex(pattern: string): RegExp {
    if (patternCache[pattern]) {
        return patternCache[pattern];
    }

    const globs = pattern.split(/,\s*/).filter((pattern) => pattern !== '');
    const parts = globs.map((pattern) => pattern.replace(/([\/\.\\\?\(\)\[\]\{\}])/g, '\\$1').replace(/\*/g, '.+'));
    let inclusions: string = parts.filter((part) => part[0] !== '!').map((s) => s === '.+' ? '.*' : s).join('|');
    const exclusions: string = parts.filter((part) => part[0] === '!').map((s) => s.substr(1)).join('|');
    if (exclusions && (!inclusions || inclusions === '.+')) {
        inclusions = '.*';
    }
    const regexStr = exclusions ? `^(?!(?:${exclusions}$))(?:${inclusions || '.+'})$` : `^(?:${inclusions})$`;
    return patternCache[pattern] = new RegExp(regexStr);
}
