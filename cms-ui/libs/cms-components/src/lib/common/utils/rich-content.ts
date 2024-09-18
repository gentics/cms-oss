/* eslint-disable @typescript-eslint/no-use-before-define */
import { RichContentLink } from '../models';

/* eslint-disable quote-props */
const ESCAPE_MAPPING: Record<string, string> = {
    '&':    'amp',
    '|':    'vert',
    '"':    'quot',
    '\'':   'apos',
    '$':    'dollar',
    '(':    'lpar',
    ')':    'rpar',
    ':':    'col',
    '<':    'lt',
    '>':    'gt',
    '{':    'lcub',
    '}':    'rcub',
};
/* eslint-enable quote-props */
const REVERSE_MAPPING: Record<string, string> = Object.entries(ESCAPE_MAPPING).reverse().reduce((acc, entry) => {
    acc[entry[1]] = entry[0];
    return acc;
}, {});

export function extractRichContentLinks(text: string): (string | RichContentLink)[] {
    const EXTRACT_PATTERN = /{{[\s]*(LINK)[\s]*[|][\s]*(?<linkType>(PAGE|FILE|URL))[:](?:(?<=URL:)(?<url>[^|]*)|(?<!URL:)(?<nodeId>[a-zA-Z0-9-.]+)[:](?<itemId>[a-zA-Z0-9-.]+)(?:[:](?<langCode>[a-zA-Z]{2}))?)[\s]*[|][\s]*(?<displayText>[^|]*)[\s]*(?:[|][\s]*(?<target>(_blank|_self|_top|_unfencedTop|_parent)))?[\s]*}}/g;

    const out: (string | RichContentLink)[] = [];

    let res: RegExpExecArray | null = null;
    let lastStart = 0;

    while ((res = EXTRACT_PATTERN.exec(text)) != null) {
        const before = text.substring(lastStart, res.index);
        if (before !== '') {
            out.push(before);
        }

        const link: RichContentLink = res.groups as any;

        // Try to parse numbers
        const nodeId = Number(link.nodeId);
        if (!isNaN(nodeId) && isFinite(nodeId)) {
            link.nodeId = nodeId;
        }
        const itemId = Number(link.itemId);
        if (!isNaN(itemId) && isFinite(itemId)) {
            link.itemId = itemId;
        }

        // If optional values aren't present (i.E. ''), then set them explicitly to `null`.
        if (!link.langCode) {
            link.langCode = null;
        }
        if (!link.target) {
            link.target = null;
        }

        link.displayText = decodeRichContentLinkText(link.displayText);

        out.push(link);
        lastStart = EXTRACT_PATTERN.lastIndex;
    }

    const after = text.substring(EXTRACT_PATTERN.lastIndex || lastStart);
    if (after !== '') {
        out.push(after);
    }

    return out;
}

export function encodeRichContentLinkText(text: string): string {
    let current = text;
    Object.entries(ESCAPE_MAPPING).forEach(([search, replace]) => {
        current = current.replaceAll(search === '&' ? /(&(?!\w+;))/g : search, `&${replace};`);
    });
    return current;
}

export function decodeRichContentLinkText(text: string): string {
    let current = text;
    Object.entries(REVERSE_MAPPING).forEach(([search, replace]) => {
        current = current.replaceAll(`&${search};`, replace);
    });
    return current;
}

export function toRichContentLinkTemplate(link: RichContentLink): string {
    let buffer = `{{LINK|${link.linkType}:${link.nodeId}:${link.itemId}`;
    if (link.langCode) {
        buffer += `:${link.langCode}`;
    }
    buffer += `|${encodeRichContentLinkText(link.displayText)}`;
    if (link.target) {
        buffer += `|${link.target}`;
    }
    buffer += '}}';

    return buffer;
}
