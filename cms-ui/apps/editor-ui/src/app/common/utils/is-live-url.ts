/**
 * Returns true if the given value looks like a page liveUrl
 * Will check for protocol, given host names one slash "/" after the host name
 * As a shortcut, if there is a leading "http" or "https" we assume it is a liveUrl and return true
 *
 * Examples:
 * something -> false
 * host -> false
 * http://host -> false
 * https://host -> false
 * https://host/ -> false
 * host/test -> true
 * host/directory/test.html -> true
 * https://host/test -> true
 * https://host/test.html -> true
 * https://host/directory/test -> true
 * https://host/directory/test.html -> true
 */
export function isLiveUrl(value: string, hosts: string[]): boolean {
    // has protocol?
    if (new RegExp(/^http/, 'i').test(value)) {
        return true;
    }
    // matches one of existing hosts?
    else if (
        hosts.some((host) => new RegExp(`^(https?\/\/\:)?(${host})\/.+`, 'i').test(value))
    ) {
        return true;
    } else {
        return false;
    }
}
