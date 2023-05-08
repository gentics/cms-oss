let regexpCache = new Map<string, RegExp>();
const MAX_CACHE = 1000;

/**
 * Matches a string by a fuzzy pattern (e.g. "replasye" matches "Reports of last year").
 * Only continous characters and beginnings of words are matched, for example:
 * - "prod" matches "project designs", but not "projektende"
 * - "recar" matches "red sports cars", but not "red ocarina"
 */
export function fuzzyMatch(pattern: string, text: string): boolean {
    if (!pattern) {
        return false;
    }

    const regex = createRegexFromFuzzyPattern(pattern);
    return regex.test(text);
}

/**
 * Match by a fuzzy pattern and format it as html highlighting the matches
 * @example
 *   fuzzyHighlight('ab', 'alpha beta', 'em'); // -> '<em>a</em>lpha <em>b</em>eta'
 */
export function fuzzyHighlight(pattern: string, text: string, tag: string = 'em', cssClass?: string): string {
    const openingTag = '<' + tag + (cssClass ? ' class="' + cssClass + '"' : '') + '>';
    const closingTag = '</' + tag + '>';

    const matches = fuzzyMatches(pattern, text);
    const htmlEscaped = matches.map(match => {
        const html = escapeHtml(match.text);
        return match.highlight ? (openingTag + html + closingTag) : html;
    });

    return htmlEscaped.join('');
}

/**
 * Match by a fuzzy pattern and return the matches as array
 * @example
 *   fuzzyHighlight('ab', 'alpha beta', 'em'); // -> '<em>a</em>lpha <em>b</em>eta'
 */
export function fuzzyMatches(pattern: string, text: string): Array<{ text: string, highlight: boolean }> {
    if (pattern) {
        const regex = createRegexFromFuzzyPattern(pattern);

        const matches = String(text).match(regex);
        if (matches) {
            const combined = combineConsecutiveMatches(matches);
            let parts = combined.map((text, index) => ({
                text,
                highlight: index % 2 === 1
            }));

            if (!parts[0].text) {
                parts = parts.slice(1);
            }

            if (!parts[parts.length - 1].text) {
                parts = parts.slice(0, parts.length - 1);
            }

            return parts;
        }
    }

    return [{ text, highlight: false }];
}

/**
 * Combine consecutive matches
 * @example
 *   ['before', 'a', '', 'b', 'after'] => ['before', 'ab', 'after']
 */
function combineConsecutiveMatches(matches: RegExpMatchArray): string[] {
    const combinedMatches = [matches[1]];
    for (let index = 2; index < matches.length; index += 2) {
        const currentChars = [matches[index]];
        while (!matches[index + 1] && index + 2 < matches.length) {
            index += 2;
            currentChars.push(matches[index]);
        }
        combinedMatches.push(currentChars.join(''), matches[index + 1]);
    }

    return combinedMatches;
}


const RESERVED_REGEX_CHARS = '\\[](){}.?*+^$|';

/**
 * Create a RegExp object from a fuzzy search pattern.
 *
 * The generated RegExp captures matched text parts with .match (e.g. for highlighting)
 * The resulting groups take the form:
 *   $1 = Text before match
 *   $N + 0 = matching character N
 *   $N + 1 = surrounding non-matching text
 *   $Last = Text after match
 * For example when searching "abc" in "for all big carrots":
 *   $1 = 'for '   (before)
 *   $2 = 'a'      (match)
 *   $3 = 'll '    (padding)
 *   $4 = 'b'      (match)
 *   $5 = 'ig '    (padding)
 *   $6 = 'c'      (match)
 *   $7 = 'arrots' (after)
 * That means for "abc", the pattern would be:
 *   /^(|.+?\b)(a)(|.+?\b)(b)(|.+?\b)(c)(.*)$/i
 */
function createRegexFromFuzzyPattern(searchPattern: string): RegExp {
    let regex = regexpCache.get(searchPattern);
    if (regex) { return regex; }

    if (!searchPattern) {
        regex = /(?:)/;
    } else {
        // Escape characters that have a special purpose in regular expressions
        const chars = searchPattern.split('')
            .map(char => (RESERVED_REGEX_CHARS.indexOf(char) >= 0 ? '\\' : '') + char);

        const charGroups = chars.map(char => `(|.+?\\b)(${char})`);
        const pattern = '^' + charGroups.join('') + '(.*)$';
        regex = new RegExp(pattern, 'i');
    }

    regexpCache.set(searchPattern, regex);
    // Trim cache when the application runs for a long time
    if (regexpCache.size > MAX_CACHE) {
        regexpCache = new Map(Array.from(regexpCache.entries()).slice(MAX_CACHE / 2));
    }

    return regex;
}


function escapeHtml(text: string): string {
    return text.replace(/[&<>]/g, escapeHtmlChar);
}

const htmlEscapes: { [char: string]: string } = {
    '&': '&amp;',
    '<': '&lt;',
    '>': '&gt;'
};

function escapeHtmlChar(char: string): string {
    return htmlEscapes[char];
}
