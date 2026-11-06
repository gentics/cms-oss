import type { CmsProperty, CmsTag } from '@/services/cmsApiService/cmsApiService';

/*
 * Direct editing in the preview: what an edit writes into the CMS, and where. Plain functions on page elements
 * and data, no React and no requests.
 *
 * Terms:
 * - A *part* is a tag marked in the preview (`data-gcn-tagid`, rendered by the CMS with `tagmarkers=true`).
 * - An *editable* is a part whose text the CMS lets be edited; it carries the id of its stored value
 *   (`GENTICS_EDITABLE_<id>`), which is `Property.id` of that part in `GET /rest/page/load`.
 * - The *stored value* is the HTML the CMS keeps for an editable, with every tag embedded in it written as
 *   `<node NAME>`.
 *
 * An edit is saved in two steps:
 * 1. **Plan**, on the page in the preview frame (called by `PreviewColumn`): `planTextEdit`, `planLinkEdit` or
 *    `planImageEdit` look at the selected element and return an `EditPlan`:
 *    - `value`: the new stored value of an editable — a copy of the editable is changed (`changedCopy`) and
 *      turned back into the stored form (`storedValueOf`);
 *    - `text`, `linkTag`, `image`: new values for the own parts of a tag that is no editable (a block, a link
 *      tag, an image), found later by the tag's name;
 *    - `refused`: why the edit cannot be saved directly.
 * 2. **Apply**, on the page as the CMS has it now (called by `useSaveCmsEdit`, after `loadCmsPageTags`):
 *    `applyPlan` finds the part (by id) or the tag (by name), compares with the stored value and returns the
 *    one tag to save (`saveCmsPageTag`), `unchanged`, or a refusal.
 */

// --- Recognising editables and tags in the preview ---

/** Selector of an editable in the preview (see the terms above). */
export const EDITABLE = '[id^="GENTICS_EDITABLE_"]';

/** Selector of a part, i.e. any tag marked in the preview; inside an editable, a tag embedded in it. */
export const MARKED = '[data-gcn-tagid]';

/** Why an edit cannot be saved directly; the UI names it (`preview.save.refused.*`). */
export type RefusalReason = 'noEditable' | 'formatting' | 'unsupportedContent' | 'unclearPart' | 'pageTarget' | 'noImagePart' | 'notFound';

/** A link's new target, as the link card gives it: a URL, or a CMS page. */
export type LinkTargetChange = { type: 'url'; url: string } | { type: 'page'; pageId: number; nodeId?: number };

/**
 * The result of step 1: what to write, and how `applyPlan` finds it in the loaded page.
 * - `value`: new stored value of the editable whose stored value has the id `propertyId`.
 * - `text`: new text for the only text part of the tag `tagName`.
 * - `linkTag`: new text and/or target for the link tag `tagName` (each only when changed).
 * - `image`: another CMS image for the image tag `tagName`.
 * - `refused`: nothing to write, for `reason`.
 */
export type EditPlan =
    | { kind: 'value'; propertyId: number; value: string }
    | { kind: 'text'; tagName: string; text: string }
    | { kind: 'linkTag'; tagName: string; text?: string; target?: LinkTargetChange }
    | { kind: 'image'; tagName: string; imageId: number; nodeId: number }
    | { kind: 'refused'; reason: RefusalReason };

/** The id of an editable's stored value, read from its `GENTICS_EDITABLE_<id>`; `null` for any other element. */
export function editableIdOf(element: Element): number | null {
    const match = /^GENTICS_EDITABLE_(\d+)$/.exec(element.id);

    return match ? Number(match[1]) : null;
}

// The name of a part's tag (`data-gcn-tagname`), by which `applyPlan` finds it in the loaded page.
function tagNameOf(element: Element): string {
    return element.getAttribute('data-gcn-tagname') ?? '';
}

// --- Turning an editable back into its stored value (used by `changedCopy`) ---

/**
 * The stored value of `editable`: its HTML as the CMS keeps it. Every tag embedded directly in it (a link tag,
 * a block) becomes `<node NAME>`; a tag inside an embedded one is part of that one and goes with it. The
 * attribute the editing in place adds (`contenteditable`) is left out. Everything else is the HTML as the
 * browser writes it.
 */
export function storedValueOf(editable: Element): string {
    const copy = editable.cloneNode(true) as Element;

    copy.querySelectorAll('[contenteditable]').forEach((element) => element.removeAttribute('contenteditable'));

    // The embedded tags directly in the editable: no other marked element between them and the editable.
    const embedded = Array.from(copy.querySelectorAll(MARKED)).filter((element) => {
        const outer = element.parentElement?.closest(MARKED);

        return !outer || outer === copy;
    });

    // Placeholder comments first: inserting `<node NAME>` as text would escape it.
    for (const element of embedded) {
        element.replaceWith(copy.ownerDocument.createComment(`gtxnode:${tagNameOf(element)}`));
    }

    return copy.innerHTML.replace(/<!--gtxnode:([^>]*?)-->/g, '<node $1>');
}

// --- Step 1: plans, worked out from the page in the preview ---

// Whether `element` holds more than text and line breaks (formatting, links, embedded tags). Plain text from the
// UI would lose that, so `planTextEdit` and `planLinkEdit` refuse such an element.
function hasMarkup(element: Element): boolean {
    return Array.from(element.children).some((child) => child.localName !== 'br');
}

// Elements that hold one run of text, such as an editable's only paragraph (see `textContainerOf`).
const TEXT_CONTAINERS = ['p', 'h1', 'h2', 'h3', 'h4', 'h5', 'h6', 'blockquote', 'pre'];

/**
 * The element whose text an edit replaces: `element` itself, or, while it holds nothing but a single
 * paragraph, heading, quote or code block (and white space), that one. So an editable with one paragraph keeps
 * its `<p>`. `planTextEdit` writes into this element, and `PreviewColumn` makes the same element editable in
 * place, so what the user edits is what gets saved.
 */
export function textContainerOf(element: Element): Element {
    let container = element;

    while (container.children.length === 1) {
        const child = container.children.item(0);

        if (!child || !hasOnlyChildAndWhitespace(container, child) || !TEXT_CONTAINERS.includes(child.localName) || child.matches(MARKED)) {
            break;
        }

        container = child;
    }

    return container;
}

// The elements of a list or table that are edited on their own (see `editLeafOf`).
const LEAVES = 'li, td, th';

/**
 * What "Write it yourself" edits in `target` (the selected part or text block): in a list or table, only the item or
 * cell `clicked` is in — the innermost `li`, `td` or `th` inside `target` — so the list or table keeps its structure;
 * otherwise `target` itself. The selection stays on `target`.
 */
export function editLeafOf(target: Element, clicked: Node | null): Element {
    const element = clicked?.nodeType === 1 ? (clicked as Element) : clicked?.parentElement;
    const leaf = element?.closest(LEAVES);

    return leaf && target.contains(leaf) ? leaf : target;
}

function hasOnlyChildAndWhitespace(parent: Element, child: Element): boolean {
    return Array.from(parent.childNodes).every(
        (node) =>
            node === child ||
            (node.nodeType === 3 && !node.textContent?.trim()),
    );
}

// Replaces `element`'s content with the plain `text`, each line break as `<br>` (used in the copy, by `planTextEdit`).
function setText(element: Element, text: string) {
    const lines = text.split('\n');

    element.replaceChildren();
    lines.forEach((line, index) => {
        if (index > 0) {
            element.append(element.ownerDocument.createElement('br'));
        }

        element.append(line);
    });
}

// Where `node` sits below `root`: each element's index among its parent's children, from `root` down. With
// `nodeAt` this finds the same element again in a copy of `root` (see `changedCopy`).
function pathOf(root: Element, node: Element): number[] {
    const path: number[] = [];

    for (let current = node; current !== root; current = current.parentElement!) {
        path.unshift(Array.from(current.parentElement!.children).indexOf(current));
    }

    return path;
}

// The element at `path` below `root` (the other half of `pathOf`).
function nodeAt(root: Element, path: number[]): Element {
    return path.reduce((element, index) => element.children[index]!, root);
}

// The `value` plan for an edit inside an editable: copies `editable`, applies `change` to the element in the copy
// that `node` is in the original (the page itself is not changed), and takes the copy's stored value.
function changedCopy(editable: Element, node: Element, change: (copied: Element) => void): EditPlan {
    const copy = editable.cloneNode(true) as Element;

    change(nodeAt(copy, pathOf(editable, node)));

    return { kind: 'value', propertyId: editableIdOf(editable)!, value: storedValueOf(copy) };
}

/**
 * Step 1 for new text ("Write it yourself"). `part` is the selected part, `target` the element meant in it (the
 * part, or one of its text blocks), `text` the new plain text.
 * - In an editable: the text replaces `target`'s content (`textContainerOf`) in a copy → a `value` plan. Refused
 *   (`formatting`) if that content holds more than text and line breaks.
 * - In a part that is no editable (e.g. a block embedded in a text): the tag's own text part → a `text` plan;
 *   a text block of such a part cannot be written (`noEditable`).
 */
export function planTextEdit(part: Element, target: Element, text: string): EditPlan {
    if (!part.matches(EDITABLE)) {
        return target === part ? { kind: 'text', tagName: tagNameOf(part), text } : { kind: 'refused', reason: 'noEditable' };
    }

    const container = textContainerOf(target);

    if (hasMarkup(container)) {
        return { kind: 'refused', reason: 'formatting' };
    }

    return changedCopy(part, container, (copied) => setText(copied, text));
}

/**
 * Step 1 for content written in the inline editor ("Write it yourself", `useInlineEditor`). `part` is the selected
 * part, `target` the element meant in it (a text block, or the item or cell of `editLeafOf`), `html` its new content
 * as the editor writes it, `attributes` the attributes it had before the editing (the editor adds its own).
 * - In an editable: the content of `target`'s text container (`textContainerOf`) becomes `html` in a copy → a
 *   `value` plan; the tags embedded in `html` (kept unchanged by the editor) become `<node NAME>` (`storedValueOf`).
 * - Outside an editable: refused (`noEditable`); a part that is no editable gets plain text (`planTextEdit`).
 */
export function planHtmlEdit(part: Element, target: Element, html: string, attributes: [string, string][]): EditPlan {
    if (!part.matches(EDITABLE)) {
        return { kind: 'refused', reason: 'noEditable' };
    }

    return changedCopy(part, textContainerOf(target), (copied) => {
        for (const name of copied.getAttributeNames()) {
            copied.removeAttribute(name);
        }

        for (const [name, value] of attributes) {
            copied.setAttribute(name, value);
        }

        copied.innerHTML = html;
    });
}

/**
 * Step 1 for a link ("Edit link"). `part` is the selected part, `link` the selected link, `text` and `target`
 * what the link card changed.
 * - A link tag (the link is a marked tag itself): its own parts → a `linkTag` plan.
 * - A plain link inside an editable: its text and `href` are changed in a copy of the editable → a `value` plan.
 *   Refused for a CMS page as target (`pageTarget`: only a link tag can point to one), for new text of a link
 *   that holds formatting (`formatting`), and outside an editable (`noEditable`).
 */
export function planLinkEdit(part: Element, link: Element, { text, target }: { text?: string; target?: LinkTargetChange }): EditPlan {
    if (link.matches(MARKED)) {
        return { kind: 'linkTag', tagName: tagNameOf(link), ...(text !== undefined ? { text } : {}), ...(target ? { target } : {}) };
    }

    if (!part.matches(EDITABLE)) {
        return { kind: 'refused', reason: 'noEditable' };
    }

    if (target?.type === 'page') {
        return { kind: 'refused', reason: 'pageTarget' };
    }

    if (text !== undefined && hasMarkup(link)) {
        return { kind: 'refused', reason: 'formatting' };
    }

    return changedCopy(part, link, (copied) => {
        if (text !== undefined) {
            copied.textContent = text.replace(/\s*\n\s*/g, ' ');
        }

        if (target) {
            copied.setAttribute('href', target.url);
        }
    });
}

/** Step 1 for another CMS image ("Swap the image"): the image tag `part` gets `image` → an `image` plan. */
export function planImageEdit(part: Element, image: { imageId: number; nodeId: number }): EditPlan {
    return { kind: 'image', tagName: tagNameOf(part), ...image };
}

// --- Step 2: applying a plan to the page as the CMS has it now ---

/** The result of step 2: the one tag to save (`saveCmsPageTag`), nothing to save, or why it cannot be saved. */
export type PlanResult = { tag: CmsTag } | { unchanged: true } | { refused: RefusalReason };

// The tag to save: `tag` as loaded, with only `properties` as its parts. The loaded `active` is sent back, since
// the CMS would switch a tag without it on.
function tagWith(tag: CmsTag, properties: Record<string, CmsProperty>): PlanResult {
    return { tag: { id: tag.id, name: tag.name, constructId: tag.constructId, active: tag.active, properties } };
}

// Plain `text` as HTML for a rich text part: `&`, `<` and `>` escaped, line breaks as `<br>`.
function textAsHtml(text: string): string {
    const escaped = text.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');

    return escaped.replace(/\n/g, '<br>');
}

// The part types that hold text.
const TEXT_TYPES = ['STRING', 'RICHTEXT'];

/**
 * Step 2: applies `plan` to `tags`, the page's tags as the CMS has them now (`loadCmsPageTags`).
 * - `value`: the part whose `Property.id` is the editable's id gets the new stored value.
 * - `text`: the tag's only text part gets the text (as escaped HTML for a rich text part, and only while that
 *   part holds no markup); several or no text parts → `unclearPart`.
 * - `linkTag`: the tag's `text` part (STRING) gets the text, its `url` part (PAGE) the target — a CMS page by
 *   `pageId`, a URL as `stringValue` without a `pageId`; other parts → `unclearPart`.
 * - `image`: the tag's only image part gets the image; none or several → `noImagePart`.
 * A part or tag the page no longer has → `notFound`. A value equal to the stored one → `unchanged`.
 */
export function applyPlan(tags: Record<string, CmsTag>, plan: EditPlan): PlanResult {
    if (plan.kind === 'refused') {
        return { refused: plan.reason };
    }

    if (plan.kind === 'value') {
        for (const tag of Object.values(tags)) {
            const entry = Object.entries(tag.properties ?? {}).find(([, property]) => property.id === plan.propertyId);

            if (entry) {
                const [key, property] = entry;

                return property.stringValue === plan.value
                    ? { unchanged: true }
                    : tagWith(tag, { [key]: { type: property.type, stringValue: plan.value } });
            }
        }

        return { refused: 'notFound' };
    }

    // The other plans name the tag.
    const tag = tags[plan.tagName];

    if (!tag) {
        return { refused: 'notFound' };
    }

    const properties = Object.entries(tag.properties ?? {});

    if (plan.kind === 'text') {
        const texts = properties.filter(([, property]) => TEXT_TYPES.includes(property.type));

        if (texts.length !== 1) {
            return { refused: 'unclearPart' };
        }

        const [key, property] = texts[0]!;

        if (property.type === 'RICHTEXT' && /</.test(property.stringValue ?? '')) {
            return { refused: 'formatting' };
        }

        const value = property.type === 'RICHTEXT' ? textAsHtml(plan.text) : plan.text;

        return property.stringValue === value ? { unchanged: true } : tagWith(tag, { [key]: { type: property.type, stringValue: value } });
    }

    if (plan.kind === 'linkTag') {
        const text = tag.properties?.text;
        const url = tag.properties?.url;

        if (text?.type !== 'STRING' || url?.type !== 'PAGE') {
            return { refused: 'unclearPart' };
        }

        // Only what differs from the stored values is sent.
        const changed: Record<string, CmsProperty> = {};

        if (plan.text !== undefined && plan.text !== text.stringValue) {
            changed.text = { type: 'STRING', stringValue: plan.text };
        }

        if (plan.target?.type === 'page' && plan.target.pageId !== url.pageId) {
            changed.url = { type: 'PAGE', pageId: plan.target.pageId, ...(plan.target.nodeId !== undefined ? { nodeId: plan.target.nodeId } : {}) };
        } else if (plan.target?.type === 'url' && (url.pageId || plan.target.url !== url.stringValue)) {
            changed.url = { type: 'PAGE', stringValue: plan.target.url };
        }

        return Object.keys(changed).length === 0 ? { unchanged: true } : tagWith(tag, changed);
    }

    // An `image` plan.
    const images = properties.filter(([, property]) => property.type === 'IMAGE');

    if (images.length !== 1) {
        return { refused: 'noImagePart' };
    }

    const [key, property] = images[0]!;

    return property.imageId === plan.imageId
        ? { unchanged: true }
        : tagWith(tag, { [key]: { type: 'IMAGE', imageId: plan.imageId, nodeId: plan.nodeId } });
}
