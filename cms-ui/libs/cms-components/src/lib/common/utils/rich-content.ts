/* eslint-disable @typescript-eslint/no-use-before-define */
import {
    ATTR_CONTENT_TYPE,
    ATTR_ITEM_ID,
    ATTR_LANG_CODE,
    ATTR_LINK_TYPE,
    ATTR_NODE_ID,
    ATTR_TARGET,
    ATTR_URL,
    CLASS_ITEM_LINK,
    CLASS_RICH_ELEMENT,
    RichContent,
    RichContentLink,
    RichContentType,
} from '../models';

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

export function elementToRichContentString(element: HTMLElement): string {
    const content = extractRichContentFromElement(element);
    return toRichContentTemplate(content);
}

export function encodeRichContentText(text: string): string {
    let current = text;
    Object.entries(ESCAPE_MAPPING).forEach(([search, replace]) => {
        current = current.replaceAll(search === '&' ? /(&(?!\w+;))/g : search, `&${replace};`);
    });
    return current;
}

export function decodeRichContentText(text: string): string {
    let current = text;
    Object.entries(REVERSE_MAPPING).forEach(([search, replace]) => {
        current = current.replaceAll(`&${search};`, replace);
    });
    return current;
}

export function findParentRichElement(start: Node, boundary: HTMLElement): HTMLElement | null {
    let elem = start;

    while (elem != null) {
        if (elem.nodeType !== Node.ELEMENT_NODE) {
            elem = elem.parentElement;
            continue;
        }

        // Max reached
        if (elem === boundary) {
            elem = null;
            break;
        }

        if ((elem as HTMLElement).classList.contains(CLASS_RICH_ELEMENT)) {
            break;
        }

        elem = elem.parentElement;
    }

    return elem as HTMLElement;
}

/* ABSTRACT/TYPE AGNOSTRIC FUNCTIONS */

export function extractRichContent(text: string): (string | RichContent)[] {
    return extractRichContentLinks(text);
}

export function toRichContentTemplate(content: RichContent): string {
    switch (content.type) {
        case RichContentType.LINK:
            return toRichContentLinkTemplate(content);
        default:
            return '';
    }
}

export function richContentToHtml(content: RichContent): string {
    switch (content.type) {
        case RichContentType.LINK:
            return richContentLinkToHtml(content);
        default:
            return '';
    }
}

export function extractRichContentFromElement(element: HTMLElement): RichContent | null {
    const type = element.getAttribute(ATTR_CONTENT_TYPE) as RichContentType;
    switch (type) {
        case RichContentType.LINK:
            return extractLinkDataFromElement(element);
        default:
            return null;
    }
}

export function getDisplayTextFromContent(content: RichContent): string {
    switch (content.type) {
        case RichContentType.LINK:
            return content.displayText;
        default:
            return '';
    }
}

export function updateElementWithContent(element: HTMLElement, content: RichContent): void {
    switch (content.type) {
        case RichContentType.LINK:
            updateElementWithLinkContent(element, content);
            return;
    }
}

/* RICH CONTENT LINK UTIL FUNCTIONS */

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

        const link: RichContentLink = {
            type: RichContentType.LINK,
            ...res.groups,
        } as any;

        // Try to parse numbers
        if (link.nodeId) {
            const nodeId = Number(link.nodeId);
            if (!isNaN(nodeId) && isFinite(nodeId)) {
                link.nodeId = nodeId;
            }
        } else {
            link.nodeId = null;
        }
        if (link.itemId) {
            const itemId = Number(link.itemId);
            if (!isNaN(itemId) && isFinite(itemId)) {
                link.itemId = itemId;
            }
        } else {
            link.itemId = null;
        }

        // If optional values aren't present (i.E. ''), then set them explicitly to `null`.
        if (!link.langCode) {
            link.langCode = null;
        }
        if (!link.target) {
            link.target = null;
        }
        if (!link.url) {
            link.url = null;
        }

        link.displayText = decodeRichContentText(link.displayText);

        out.push(link);
        lastStart = EXTRACT_PATTERN.lastIndex;
    }

    const after = text.substring(EXTRACT_PATTERN.lastIndex || lastStart);
    if (after !== '') {
        out.push(after);
    }

    return out;
}

export function toRichContentLinkTemplate(link: RichContentLink): string {
    let buffer = `{{LINK|${link.linkType}:${link.nodeId}:${link.itemId}`;
    if (link.langCode) {
        buffer += `:${link.langCode}`;
    }
    buffer += `|${encodeRichContentText(link.displayText)}`;
    if (link.target) {
        buffer += `|${link.target}`;
    }
    buffer += '}}';

    return buffer;
}

export function richContentLinkToHtml(link: RichContentLink): string {
    return `<span
        class="${CLASS_RICH_ELEMENT} ${CLASS_ITEM_LINK}"
        ${ATTR_CONTENT_TYPE}="${RichContentType.LINK}"
        ${ATTR_LINK_TYPE}="${link.linkType}"
        ${ATTR_NODE_ID}="${link.nodeId}"
        ${ATTR_ITEM_ID}="${link.itemId}"
        ${ATTR_LANG_CODE}="${link.langCode || ''}"
        ${ATTR_URL}="${link.url}"
        ${ATTR_TARGET}="${link.target || ''}"
    >${link.displayText}</span>`;
}

export function extractLinkDataFromElement(element: HTMLElement): RichContentLink {
    return {
        type: RichContentType.LINK,
        linkType: element.getAttribute(ATTR_LINK_TYPE) as any,
        nodeId: element.getAttribute(ATTR_NODE_ID),
        itemId: element.getAttribute(ATTR_ITEM_ID),
        langCode: element.getAttribute(ATTR_LANG_CODE),
        url: element.getAttribute(ATTR_URL),
        target: element.getAttribute(ATTR_TARGET),
        displayText: element.textContent,
    };
}

export function updateElementWithLinkContent(element: HTMLElement, link: RichContentLink): void {
    // Make sure all classes are properly set
    element.classList.add(CLASS_RICH_ELEMENT, CLASS_ITEM_LINK);

    element.setAttribute(ATTR_CONTENT_TYPE, RichContentType.LINK);
    element.setAttribute(ATTR_LINK_TYPE, link.linkType);
    element.setAttribute(ATTR_NODE_ID, `${link.nodeId || ''}`);
    element.setAttribute(ATTR_ITEM_ID, `${link.itemId || ''}`);
    element.setAttribute(ATTR_LANG_CODE, link.langCode);
    element.setAttribute(ATTR_URL, link.url);
    element.setAttribute(ATTR_TARGET, link.target);
}
