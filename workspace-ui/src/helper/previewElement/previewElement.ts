import type { TFunction } from 'i18next';

import { pageElementLabel, type PageElementLink, type PageElementTextBlock } from '@/helper/composerParts/composerParts';
import type { PreviewElement } from '@/hooks/usePreviewElements';

/*
 * The names of an element selected in the preview (`PreviewElement`): for the header, the menu, the outlines, the
 * cards, the editing bar, and the chat.
 */

/** The preview's text block in the form the chat uses (`PageElementTextBlock`), its kind translated, e.g. "Heading 1"; `undefined` for a whole part. */
export function elementTextBlock(t: TFunction, { textBlock }: PreviewElement): PageElementTextBlock | undefined {
    return textBlock && { index: textBlock.index, count: textBlock.count, label: t(`preview.textBlock.${textBlock.kind}`), excerpt: textBlock.excerpt };
}

/** The preview's link in the form the chat uses (`PageElementLink`), labelled "Link"; `undefined` when no link is meant. */
export function elementLink(t: TFunction, { link }: PreviewElement): PageElementLink | undefined {
    return link && { index: link.index, count: link.count, label: t('preview.link.label'), excerpt: link.excerpt };
}

/**
 * Short name on the hover outline, the menu, the header and the card titles: the construct name (else the tag name),
 * with the text block kind or "Link" in front for a text block or link, e.g. "Heading 1 · Aloha Text (lang)".
 */
export function elementName(t: TFunction, element: PreviewElement): string {
    const name = element.constructName || element.tagName;
    const kind = elementLink(t, element)?.label ?? elementTextBlock(t, element)?.label;

    return kind ? `${kind} · ${name}` : name;
}

/** Full name on the cards' @-token, the same as on the token in the chat (`pageElementLabel`), e.g. "Aloha Text (lang) · pagecontent · Heading 1". */
export function elementLabel(t: TFunction, element: PreviewElement): string {
    return pageElementLabel({ ...element, textBlock: elementTextBlock(t, element), link: elementLink(t, element) });
}
