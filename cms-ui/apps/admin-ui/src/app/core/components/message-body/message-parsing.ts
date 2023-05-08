export interface MessageLink {
    type: 'page';
    id: number;
    textBefore: string;
    name: string;
    nodeName: string;
    fullPath: string;
}


// tslint:disable
/**
 * Some messages contain a link to a page or pages and need to be parsed.
 * Sorry.
 *
 * Source:
 *   https://git.gentics.com/psc/contentnode/blob/dev/contentnode-lib/src/main/resources/contentnode_de_DE.properties
 *   https://git.gentics.com/psc/contentnode/blob/dev/contentnode-lib/src/main/resources/contentnode_en_EN.properties
 */
const knownMessagePatterns = [
    'The translation master {{page}} of page {{page}} has changed.',
    'The page {{page}} has been taken into revision.',
    'The page {{page}} has been taken into revision.\n\n--\n\n{{message}}',
    'Publishing of page {{page}} at {{date}} has been approved.',
    '{{user}} wants to publish the page \'{{page}}\'.',
    'The page {{page}} in folder {{folder}} in node {{node}} has not been modified for a while. Please check if the content is still up to date.',
    '{{user}} assigned the page {{page}} to you.',
    'The page {{page}} has been published.',
    ' wants to publish the page \'{{page}}\'.',
    ' assigned the page {{page}} to you.',
    'The page {{page}} in folder "{{folder}}" in node "{{node}}" has at least one invalid link, because the target page {{page}} has been {{status}}',

    'Die Übersetzungsvorlage {{page}} der Seite {{page}} hat sich geändert.',
    'Die Seite {{page}} wurde in Arbeit zurück gestellt.',
    'Die Seite {{page}} wurde in Arbeit zurück gestellt.\n\n--\n\n{{message}}',
    'Die Veröffentlichung der Seite {{page}} am {{date}} wurde freigegeben.',
    '{{user}} möchte die Seite {{page}} veröffentlichen.',
    'Die Seite {{page}} in   Ordner {{folder}} in Node {{node}} ist seit längerer Zeit unverändert. Bitte prüfen Sie den Inhalt auf Aktualität.',
    '{{user}} hat Ihnen die Seite {{page}} zugewiesen.',
    'Die Seite {{page}} wurde veröffentlicht.',
    ' möchte die Seite {{page}} veröffentlichen.',
    ' hat Ihnen die Seite {{page}} zugewiesen.',
    'Der Link-Tag "{{tag}}" in der Seite {{page}} ist nicht mehr gültig, da die Zielseite {{page}} {{status}} wurde.'
].map(placeholderStringToRegExp);
// tslint:enable

const placeholderPatterns: {[k: string]: RegExp} = {
    date: /[0-9.:\-AMP,\/]+( [0-9.:\-AMP,\/]+)*/,
    folder: /\([^\n]+\)|[^\n]+/,
    node: /\([^\n]+\)|[^\n]+/,
    message: /[\s\S]+/,
    page: /(([^/\n]+)[^\n]*?\/([^/\n]+?)) \((\d+)\)|\((\d+)\) (([^/\n]+)[^\n]*?\/([^/\n]+?))/,
    status: /[\w ]+/,
    tag: /[a-zA-Z0-9_\-]+/,
    user: /[^\n]+ [^\n]+/
};


/**
 * Extracts the information from a message received by the backend and transforms it into links.
 */
export function parseMessage(message: string, nodes?: { id: number, name: string }[]): { links: MessageLink[], textAfterLinks: string } {
    message = message.toString();
    // Iterate over all known patterns and try to find one that might match (fast)
    // and then check if its parts also match (slow but exact).
    for (const pattern of knownMessagePatterns) {
        const matched = message.match(pattern.regExp);

        if (matched) {
            const matchedGroups: string[] = matched.slice(1);
            const placeholderGroups: string[][] = [];
            const allPlaceholdersMatch = pattern.placeholders.every((groupName, groupIndex) => {
                const placeholder = matchPlaceholder(groupName, matchedGroups[2 * groupIndex + 1]);
                if (placeholder.isMatch) {
                    placeholderGroups.push(placeholder.matches);
                }
                return placeholder.isMatch;
            });

            if (allPlaceholdersMatch) {
                return extractLinkInfo(matchedGroups, placeholderGroups, pattern.placeholders);
            }
        }
    }

    // No pattern matches, search for links to pages
    if (nodes) {
        return findPageLinks(message, nodes);
    }

    // No known pattern matches, just a normal message.
    return { links: [], textAfterLinks: message };
}

/**
 * Creates links for pages, ignores other placeholders
 * @internal
 */
function extractLinkInfo(matchedGroups: string[], placeholderGroups: string[][], placeholderNames: string[]):
        { links: MessageLink[], textAfterLinks: string } {

    let textBefore = '';
    const links: MessageLink[] = [];

    placeholderNames.forEach((placeholderName, index) => {
        textBefore += matchedGroups[index * 2];
        const groupText = matchedGroups[index * 2 + 1];

        if (placeholderName === 'page') {
            const groups = placeholderGroups[index];
            const fullPath = groups[0] || groups[5];
            const nodeName = groups[1] || groups[6] || '';
            const name = groups[2] || groups[7] || '';
            const id = Number(groups[3] != null ? groups[3] : groups[4]);

            links.push({ type: 'page', id, textBefore, name, nodeName, fullPath });
            textBefore = '';
        } else {
            textBefore += groupText;
        }
    });

    const textAfterLinks = textBefore + matchedGroups[matchedGroups.length - 1];
    return { links, textAfterLinks };
}

/** @internal */
export function matchPlaceholder(name: string, text: string): { isMatch: boolean, matches?: string[] } {
    const pattern: RegExp = placeholderPatterns[name];
    if (pattern) {
        const rx = new RegExp('^(?:' + pattern.source + ')$');
        const matches = text.toString().match(rx);
        if (matches) {
            return {
                isMatch: true,
                matches: matches.slice(1)
            };
        }
    }
    return { isMatch: false };
}

/**
 * Parses "Hello {{name}}!" to { pattern: /^(Hello )(.+?)(!)/, placeholders: ["name"] }
 * @internal
 */
export function placeholderStringToRegExp(str: string): { regExp: RegExp, placeholders: string[] } {
    const placeholders: string[] = [];
    const regexParts = ['^'];
    const findGroupsRegEx = /([\s\S]*?)\{\{\s*([^\}\s]+)\s*\}\}/g;
    let startOfRemainingText = 0;
    let match: RegExpExecArray;

    // Matches Text before a group name and the group name
    while (match = findGroupsRegEx.exec(str)) {
        regexParts.push('(', replaceRegExpSpecialChars(match[1]), ')');
        placeholders.push(match[2]);
        regexParts.push('([\\s\\S]*?)');
        startOfRemainingText = findGroupsRegEx.lastIndex;
    }

    regexParts.push('(', replaceRegExpSpecialChars(str.substr(startOfRemainingText)), ')');
    regexParts.push('$');

    const regExp = new RegExp(regexParts.join(''));
    return { regExp, placeholders };
}

/**
 * Replaces special characters for a string to be used in `new RegExp(userInput)`
 */
export function replaceRegExpSpecialChars(input: string): string {
    return input.replace(/[\^\$\[\]\(\)\{\}\.\+\*\?\|]/g, '\\$&').replace(/\n/g, '\\n');
}

/**
 * Finds links by a pattern like "Node name/Folder1/Folder2/Page name" (15)
 *   or (15) "Node name/Folder1/Folder2/Page name"
 */
export function findPageLinks(input: string, nodes: { id: number, name: string }[]): { links: MessageLink[], textAfterLinks: string } {

    // Matches any node name
    const nodeNamePattern = nodes.map(node => replaceRegExpSpecialChars(node.name)).join('|');

    // Matches "/folder/", "/some folder/", "/folder a/folder b/"
    const pathPattern = `/(?:[^/\\n]+?/)*?`;

    // Matches "page", "a page", "some page", ...
    const pageNamePattern = `[^/\\n]+?`;

    // Matches "(15)", "(155124)"
    const idPattern = `\\((\\d+)\\)`;

    // Matches "nodeName/folderName/pageName"
    const fullPathPattern = `((${nodeNamePattern})${pathPattern}(${pageNamePattern}))`;

    // Matches '"nodeName/folderName/pageName" (id)' or '(id) "nodeName/folderName/pageName"'
    const pageLinkPattern = `"${fullPathPattern}" ${idPattern}|${idPattern} "${fullPathPattern}"`;

    const linkRx = new RegExp(pageLinkPattern, 'g');

    const links: MessageLink[] = [];
    let lastMatchEnd = 0;
    let matched: RegExpExecArray;

    while (matched = linkRx.exec(input)) {
        const textBefore = input.substring(lastMatchEnd, matched.index);
        const fullPath = matched[1] || matched[6];
        const nodeName = matched[2] || matched[7] || '';
        const name = matched[3] || matched[8] || '';
        const id = Number(matched[4] != null ? matched[4] : matched[5]);

        links.push({ type: 'page', id, textBefore, name, nodeName, fullPath });
        lastMatchEnd = linkRx.lastIndex;
    }

    const textAfterLinks = input.substr(lastMatchEnd);

    return { links, textAfterLinks };
}
