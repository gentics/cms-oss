/**
 * Attempts to sanitize a raw query string provided by the user which is then passed into the Elastic `query_string` query.
 * If not correctly sanitized, the query will fail with a 400 error.
 * See: https://www.elastic.co/guide/en/elasticsearch/reference/current/query-dsl-query-string-query.html#_reserved_characters
 *
 * Note: not all reserved characters will cause the query to fail, some are simply meaningless rather than syntax errors.
 * This method is not exhaustive but aims to handle the most likely cases.
 */
export function sanitizeElasticQuery(query: string): string {
    if (typeof query !== 'string') {
        throw new Error('Argument not of type string.');
    }
    const sanitizers = [
        stripInvalidPlus,
        stripInvalidColon,
        stripInvalidTilde,
        stripInvalidIdQuery,
        stripCarets,
        escapeForwardSlashes,
        escapeUnmatchedParentheses,
        escapeSquareBrackets,
        escapeCurlyBraces,
    ];

    return sanitizers.reduce((query, sanitizer) => sanitizer(query), query);
}

/**
 * The + char causes an error when used on its own.
 */
function stripInvalidPlus(query: string): string {
    return query.replace(/^(\s*)\+\s*$/, '$1');
}

/**
 * The : char causes an error when used on its own or without a leading word
 */
function stripInvalidColon(query: string): string {
    return query.replace(/^(\s*):/, '$1');
}

/**
 * Escapes the forward slash char but attempts to leave any regex unaffected. This is achieved by first replacing
 * all regex with a placeholder, the performing the escaping, and finally replacing the regex.
 */
function escapeForwardSlashes(query: string): string {
    return replaceExceptInsideRegex(query, '/', '\\/');
}

function replaceExceptInsideRegex(input: string, find: string | RegExp, replace: string): string {
    const re = /\/[^/)]+\//g;
    const placeholder = '___re___';
    const matches = input.match(re);
    const withPlaceholders = input.replace(re, placeholder);
    const foundAndReplaced = withPlaceholders.replace(find, replace);
    let i = 0;
    return foundAndReplaced.replace(new RegExp(placeholder, 'g'), () => {
        return matches[i++];
    });
}

/**
 * Escapes unmatched parentheses.
 */
function escapeUnmatchedParentheses(query: string): string {
    const re = /\([^)(]+\)/g;
    const placeholder = '___pair___';
    const matches = query.match(re);
    const withPlaceholders = query.replace(re, placeholder);
    const slashesEscaped = replaceExceptInsideRegex(withPlaceholders, /[()]/g, '\\$&');
    let i = 0;
    return slashesEscaped.replace(new RegExp(placeholder, 'g'), () => {
        return matches[i++];
    });
}

/**
 * Escapes unmatched square brackets.
 */
function escapeSquareBrackets(query: string): string {
    return replaceExceptInsideRegex(query, /[\[\]]/g, '\\$&');
}

/**
 * Escapes unmatched square brackets.
 */
function escapeCurlyBraces(query: string): string {
    return replaceExceptInsideRegex(query, /[{}]/g, '\\$&');
}
/**
 * Removes all non-regex caret chars (^).
 */
function stripCarets(query: string): string {
    return replaceExceptInsideRegex(query, '^', '');
}

/**
 * The tilde char is only valid after a word, to signify fuzzy search.
 */
function stripInvalidTilde(query: string): string {
    if (query.match(/^[~\s]+$/)) {
        return query.replace(/~/g, '');
    }
    return query.replace(/(\s|^|:)+~+(\s*)(?=[\w])/g, '$1$2');
}

/**
 * Strip invalid id queries of the type `id: foo`, which would cause an error if passed
 * through to Elastic. The id is indexed as an integer, so only numeric value are allowed.
 */
function stripInvalidIdQuery(query: string): string {
    return query.replace(/id:\s*[^\d\s]+\s*/, '');
}
