/**
 * Parses two string urls and returns true if they are of the same origin according to the Same Origin Policy.
 */
export function isSameOrigin(urlA: string, urlB: string): boolean {
    let parsedA = parseUrl(urlA);
    let parsedB = parseUrl(urlB);
    return parsedA.protocol === parsedB.protocol &&
            parsedA.hostname === parsedB.hostname &&
            parsedA.port === parsedB.port;
}

/**
 * Parse a string URL into an object containing the parts pertinent to determining origin.
 */
function parseUrl(url: string): { protocol: string; hostname: string; port: string; } {
    if (url.indexOf('http') !== 0) {
       url = 'http://' + url;
    }
    let parser = document.createElement('a');
    parser.href = url;
    return {
        protocol: parser.protocol,
        hostname: parser.hostname,
        port: parser.port
    };
}
