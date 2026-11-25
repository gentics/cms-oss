import { useEditorState } from '@tiptap/react';
import { useEffect, useId, useRef, useState } from 'react';
import { useTranslation } from 'react-i18next';

import type { PageElementInput } from '@/components/Composer/Composer';
import { EditBar } from '@/components/EditBar/EditBar';
import { ElementMenu } from '@/components/ElementMenu/ElementMenu';
import { FeedbackCard } from '@/components/FeedbackCard/FeedbackCard';
import { ImageCard } from '@/components/ImageCard/ImageCard';
import { isSupportedContent } from '@/components/InlineEditor/inlineEditorSchema';
import { focusEditor, useInlineEditor } from '@/components/InlineEditor/useInlineEditor';
import { LinkCard, type LinkChange, type LinkValue } from '@/components/LinkCard/LinkCard';
import type { LinkTargetValue } from '@/components/LinkTargetPicker/LinkTargetPicker';
import { PageSettings } from '@/components/PageSettings/PageSettings';
import { PreviewHeader } from '@/components/PreviewHeader/PreviewHeader';
import { PreviewOverlay } from '@/components/PreviewOverlay/PreviewOverlay';
import { useToast } from '@/components/ui/use-toast';
import { EDITABLE, editLeafOf, type EditPlan, MARKED, planHtmlEdit, planImageEdit, planLinkEdit, planTextEdit, type RefusalReason, textContainerOf } from '@/helper/cmsEditable/cmsEditable';
import type { PageSettingsChange } from '@/helper/composerParts/composerParts';
import { elementLabel, elementLink, elementName, elementTextBlock } from '@/helper/previewElement/previewElement';
import { readOnlyPreviewUrl } from '@/helper/previewUrl/previewUrl';
import { refusalText } from '@/helper/refusalText/refusalText';
import { useSaveCmsEdit } from '@/hooks/useCmsQueries';
import { linkTargetOf, type PreviewElement, usePreviewElements } from '@/hooks/usePreviewElements';
import { usePublishedUrls } from '@/hooks/usePublishedUrls';
import type { ContextReference, Publication } from '@/services/apiService/genaix/types';
import type { CmsItem } from '@/services/cmsApiService/cmsApiService';
import { type LivePreview, previewKey } from '@/store/useWorkspaceEventStore';

import styles from './PreviewColumn.module.css';

// Whether `a` and `b` are the same part (the same element of its tag) and the same text block or link of it (or both the whole part).
function isSameElement(a: PreviewElement | null, b: PreviewElement | null): boolean {
    return a?.tagName === b?.tagName && a?.elementId === b?.elementId && a?.textBlock?.index === b?.textBlock?.index && a?.link?.index === b?.link?.index;
}

// The link card's target for an `href` read from the page (`linkTargetOf`): its CMS page, or its URL.
function linkTargetValue(href: string): LinkTargetValue {
    const target = linkTargetOf(href);

    return target.type === 'page' ? { type: 'page', page: { id: target.pageId } } : { type: 'url', url: target.url };
}

/**
 * What the preview hands on for a part: the page and the part, to reference in the chat, to send feedback on,
 * or an image uploaded for it. New text, a link and a CMS image are saved directly in the CMS instead.
 */
export interface PreviewActions {
    /** Adds the part to the chat composer, followed by `text` if given. */
    onReferenceInChat: (element: PageElementInput, text?: string) => void;
    /** Sends feedback on the part as a turn; rejects when it was not sent, so the card stays open. */
    onSendFeedback: (element: PageElementInput, text: string) => Promise<void>;
    /** Sends an image uploaded for the part as a turn, with the file; rejects when it was not sent, so the card stays open. */
    onSendImage: (element: PageElementInput, image: File) => Promise<void>;
    /** Sends the changed page settings of the page as a turn; rejects when they were not sent, so the sheet stays open. */
    onSendSettings: (page: ContextReference, changes: PageSettingsChange[]) => Promise<void>;
    /** Whether a turn can be sent or an edit saved now (feedback, new text, an image, a link or page settings): not while a run is working or waiting. */
    canSendFeedback: boolean;
}

/**
 * The preview, with all of `PreviewActions` (the session's chat) or none of them (only showing the page, as on the review page).
 * `preview` is the latest one (the last `preview.updated`); `previews` the latest of each page, when there may be several.
 * `publications` are the session's, of all its plans: once the page shown is published, its header offers it on the
 * website ("Live", `usePublishedUrls`) beside its preview ("Preview"). `isBuilding`: GenAIx is still at work, so more of
 * the page may come; a skeleton strip along the page's bottom edge says so.
 */
export type PreviewColumnProps = { preview: LivePreview; previews?: LivePreview[]; publications?: Publication[]; isBuilding?: boolean } & (PreviewActions | { [K in keyof PreviewActions]?: undefined });

/**
 * The right column: the CMS preview of the page the session produced, framed on the preview stage
 * (design.md §11 "Vorschau-Bühne"), always read-only (`readOnlyPreviewUrl`), so that showing it
 * never opens the page for editing. The browser loads the URL with its own CMS session. A CMS that
 * forbids framing leaves the frame blank, so a link to open it is always shown next to it (integration
 * guide §5). Each `preview.updated` remounts the frame, which reloads it even when
 * the URL stayed the same. The header switches the width between desktop and mobile (390 px) and
 * shows the column in full screen, which Escape leaves (final_draft.html "preview widths").
 *
 * A part of the page is selected by clicking it (`usePreviewElements`): a layer above the frame
 * outlines the part under the mouse and the selected one, the page itself stays as the CMS rendered
 * it (design.md §12 "Elemente in der Vorschau"). The header names the selection and clears it; so
 * does a click into the page outside the parts, which also closes the part's menu or card. When a
 * reload no longer has the selected part, the selection is cleared and a toast says so.
 *
 * Text blocks: the CMS often stores several text blocks in one tag, e.g. a headline and a paragraph in one Aloha
 * text. Then the text block under the mouse is outlined and selected, not the whole tag (`textBlocksOf`). The
 * menu, the cards and the editing in place act on that text block only. The UI
 * calls it e.g. "Heading 1 · Text"; the message names it by its position and the start of its text,
 * because the API has no field for a text block inside a tag.
 *
 * Links: a link in a part (`linksOf`) is outlined and selected on its own, before its text block, and is
 * called e.g. "Link · Text"; the message names it by its position and the start of its text. Its menu
 * offers "Edit link" instead of "Write it yourself": the link card there starts from the link's text
 * and target as the page has them (a CMS page for an internal link, `linkTargetOf`), the target a URL
 * or a page of the node (`LinkTargetPicker`); Apply or Enter saves what changed, an unchanged link saves
 * nothing.
 *
 * The parts the last CMS write changed (`changed_elements`), or the part a direct save just changed, appear anew and
 * are tinted a moment (`PreviewOverlay`).
 *
 * Clicking a part also opens its element menu at the part (final_draft.html "ELEMENT MENU"): "Give
 * feedback" opens the feedback card there (final_draft.html "FEEDBACK CARD AT THE ELEMENT"), which
 * sends the typed text on the part (`onSendFeedback`) or moves both into the chat; while it is open the
 * rest of the page is dimmed (design.md §12 focus mode). "Write it yourself" makes the element itself editable
 * in the frame, as plain text (final_draft.html "startEdit"; in a list or table only the item or cell that was
 * clicked, while the selection stays on the list or table), with a bar above it (character count, Cancel,
 * Apply); Apply or Ctrl/Cmd+Enter saves it, an unchanged text saves nothing, Escape, Cancel or a click
 * elsewhere in the page gives the text back. While editing, nothing else in the page is outlined or selected,
 * and an update of the preview waits until the editing ends. Text that holds formatting, links or embedded
 * tags is not edited in place: a toast says why. For a part that shows an
 * image, "Swap the image" takes the place of "Write it yourself" (final_draft.html "askImage"), when the
 * preview names its node: the image card there offers the node's images from the CMS and an image to
 * upload (`ImagePicker`); "Use image" saves a CMS image, and sends an uploaded one to GenAIx
 * (`onSendImage`), which puts it into the CMS.
 *
 * Saving is direct, in the CMS, without GenAIx (`useSaveCmsEdit`): the edit is planned from the page in the
 * frame (`nodesOf`, `cmsEditable`) — new text or a plain link inside an editable rewrites that editable,
 * a tag that is no editable (a block, a link tag, an image) gets its own part written. Saved, the card
 * closes and the frame reloads (no `preview.updated` comes); what cannot be saved directly is named in the
 * card, with the hint to reference it in the chat instead.
 * "Reference in the chat" hands the part to `onReferenceInChat` (the chat composer's `insertPageElement`).
 *
 * For a page, "Page settings" in the header drops the page settings over the preview (`PageSettings`,
 * final_draft.html "PAGE SETTINGS"); Apply sends the changed ones (`onSendSettings`), Escape or the
 * button closes them.
 *
 * Without `PreviewActions` the preview only shows the page: no part is selected, outlined or offered a
 * menu, and there are no page settings; the changed parts are still tinted.
 *
 * With `previews` of more than one page, a select next to the title switches between them: the page of the latest `preview` is shown
 * until another page is picked, and again once a newer `preview` comes. Another page clears the
 * selection and closes the menu, the cards and the page settings, which were for the page before.
 */
export function PreviewColumn(props: PreviewColumnProps) {
    const { preview: latest, previews = [], publications = [], isBuilding = false } = props;
    // The page picked, and the latest preview when it was picked: a newer one shows its own page again.
    const [picked, setPicked] = useState<{ key: string; seq: number } | null>(null);
    const preview = (picked?.seq === latest.seq && previews.find((page) => previewKey(page) === picked.key)) || latest;
    const pageKey = previewKey(preview);
    const actions: PreviewActions | undefined = props.onReferenceInChat ? props : undefined;
    const canSendFeedback = actions?.canSendFeedback ?? false;
    const { t } = useTranslation();
    // By `previewKey`, each page's name for the page select.
    const pageNames = Object.fromEntries(previews.map((page) => [
        previewKey(page),
        page.cms_ref.name || t('preview.pages.unnamed', { id: page.cms_ref.id }),
    ]));
    const titleId = useId();
    const url = readOnlyPreviewUrl(preview.preview_url);
    const publishedUrl = usePublishedUrls(publications, [preview.cms_ref])[0]?.href;
    const [isMobile, setIsMobile] = useState(false);
    const [isFullScreen, setIsFullScreen] = useState(false);
    const toast = useToast();
    const frameRef = useRef<HTMLIFrameElement>(null);
    const [selected, setSelected] = useState<PreviewElement | null>(null);
    const [isMenuOpen, setIsMenuOpen] = useState(false);
    // The selected part's outline, where its element menu opens.
    const selectedBoxRef = useRef<HTMLDivElement>(null);
    // Whether the menu closes because an action moved the focus elsewhere (the chat, the feedback card).
    const hasMovedFocusRef = useRef(false);
    const [isFeedbackOpen, setIsFeedbackOpen] = useState(false);
    const [isSending, setIsSending] = useState(false);
    // The element being edited in place in the frame ("Write it yourself", final_draft.html "startEdit"): the
    // element the inline editor is on (`textContainerOf` the target), the target the edit is planned on (the
    // selected element or, in a list or table, the item or cell, `editLeafOf`), the selection, and whether the part
    // is an editable (rich text, saved as HTML) or a tag's own text part (plain text). `structure` is the selected
    // list or table while one of its items or cells is edited: the editing can move between them (`switchEdit`), its
    // content before (given back on Cancel) and whether an item edited before the current one changed.
    const [editing, setEditing] = useState<{
        node: HTMLElement;
        target: Element;
        element: PreviewElement;
        isRich: boolean;
        structure: { element: Element; originalHtml: string; changed: boolean } | null;
    } | null>(null);
    const inlineEditor = useInlineEditor(editing?.node ?? null, {
        marks: editing?.isRich ?? false,
        fixedSelector: editing?.isRich ? MARKED : '',
        onApply: () => void applyEdit(),
        onCancel: () => stopEditing(),
    });
    // The text being edited, for the bar's character count and the Apply button (none once the editor is gone).
    const editText = useEditorState({
        editor: inlineEditor?.editor ?? null,
        selector: ({ editor }) => (editor && !editor.isDestroyed ? editor.getText({ blockSeparator: '' }) : ''),
    }) ?? '';
    const canApply = canSendFeedback && !isSending && editText.trim() !== '';
    const [isImageOpen, setIsImageOpen] = useState(false);
    // The node whose images can replace a part's image; without it, no image can be offered (nor a page as a link's target).
    const nodeId = preview.cms_ref.node_id;
    const [isLinkOpen, setIsLinkOpen] = useState(false);
    // The link's text and target when the link card opened (`LinkCard` starts from them and tells what changed).
    const [linkInitial, setLinkInitial] = useState<LinkValue>({ text: '', target: { type: 'url', url: '' } });
    // Why the open card's edit could not be saved directly in the CMS, shown in the card.
    const [refusal, setRefusal] = useState<RefusalReason | null>(null);
    const saveEdit = useSaveCmsEdit();
    // Counts the direct saves: each one reloads the frame, as no `preview.updated` comes for it.
    const [savedCount, setSavedCount] = useState(0);
    // The part the last direct save changed, while no newer `preview.updated` came: shown as changed like GenAIx's.
    const [savedChange, setSavedChange] = useState<{ seq: number; tagName: string } | null>(null);
    const isSavedChange = savedChange?.seq === preview.seq;
    const changedParts = isSavedChange ? [savedChange.tagName] : preview.changed_elements;
    // What the frame shows: the latest page (each direct save reloading it), but held while an element is edited in
    // place, so that a `preview.updated` does not reload the page under the editing; it follows once editing ends.
    const latestFrame = { key: `${pageKey}@${preview.seq}#${savedCount}`, url };
    const [heldFrame, setHeldFrame] = useState(latestFrame);

    if (!editing && (heldFrame.key !== latestFrame.key || heldFrame.url !== latestFrame.url)) {
        setHeldFrame(latestFrame);
    }

    const frame = editing ? heldFrame : latestFrame;
    // The frame last loaded. While a new one loads, this one stays visible under it, so the page never goes blank,
    // and the new one fades in once it has loaded (design.md §10): an update reads as the page growing, not reloading.
    const [shownFrame, setShownFrame] = useState<{ key: string; url: string } | null>(null);
    const isFrameShown = shownFrame?.key === frame.key;

    function handleFrameLoad() {
        onFrameLoad();
        setShownFrame(frame);
    }
    const [isSettingsOpen, setIsSettingsOpen] = useState(false);
    const settingsRef = useRef<HTMLElement>(null);
    const settingsButtonRef = useRef<HTMLButtonElement>(null);
    const [shownPageKey, setShownPageKey] = useState(pageKey);

    // Another page: what was selected or open belonged to the page before. Adjusted while rendering rather than in an effect.
    if (pageKey !== shownPageKey) {
        setShownPageKey(pageKey);
        setSelected(null);
        setIsMenuOpen(false);
        setIsFeedbackOpen(false);
        setEditing(null);
        setIsImageOpen(false);
        setIsLinkOpen(false);
        setIsSettingsOpen(false);
    }

    const { onFrameLoad, hovered, selectedBox, editingBox, targetBox, changedBoxes, textOf, hasImage, currentOf, nodesOf } = usePreviewElements(frameRef, selected, (element) => {
        // A click on a part selects it and opens its menu; Escape clears both. Only with actions to offer.
        if (!actions) {
            return;
        }

        // Another selection, or a click elsewhere in the page while editing in place: the editing is cancelled.
        stopEditing();
        setSelected(element);
        setIsMenuOpen(element !== null);
        setIsFeedbackOpen(false);
        setIsImageOpen(false);
        setIsLinkOpen(false);
    }, () => {
        // A reload without the selected part: the selection is gone.
        stopEditing();
        setSelected(null);
        setIsMenuOpen(false);
        setIsFeedbackOpen(false);
        setIsImageOpen(false);
        setIsLinkOpen(false);
        toast.add({ title: t('preview.selection.removed') });
    }, changedParts, editing?.node ?? null, switchEdit);
    // Where the editing bar sits: above the edited element, or above the list or table whose item or cell it is.
    const barBox = editing?.structure ? selectedBox : editingBox;

    function finalFocus(): false | HTMLElement | null {
        const hasMovedFocus = hasMovedFocusRef.current;

        hasMovedFocusRef.current = false;

        return hasMovedFocus ? false : frameRef.current;
    }

    // The page, as a `page` reference.
    function pageRef(): ContextReference {
        const { id, node_id: nodeId, name } = preview.cms_ref;

        return { type: 'page', id: String(id), ...(nodeId !== undefined ? { node_id: nodeId } : {}), ...(name ? { label: name } : {}) };
    }

    // The selected part with its page, as the chat and the cards send it. A text block is re-read from the page
    // first (`currentOf`), so its excerpt matches the page after a reload.
    function pageElementOf(selection: PreviewElement): PageElementInput {
        const element = currentOf(selection);

        return {
            page: pageRef(),
            tagName: element.tagName,
            constructName: element.constructName,
            ...(element.textBlock ? { textBlock: elementTextBlock(t, element) } : {}),
            ...(element.link ? { link: elementLink(t, element) } : {}),
        };
    }

    function referenceInChat(element: PreviewElement, text?: string) {
        hasMovedFocusRef.current = true;
        actions?.onReferenceInChat(pageElementOf(element), text);
        toast.add({ title: t('preview.selection.referenced') });
    }

    function openFeedback() {
        hasMovedFocusRef.current = true;
        setIsImageOpen(false);
        setIsLinkOpen(false);
        setIsFeedbackOpen(true);
    }

    // What "Write it yourself" would edit for `element`, from the page in the frame: in an editable the item or cell that
    // was clicked in a list or table (`editLeafOf`), else the element; the element the inline editor goes on; whether the part is an
    // editable. Or why it cannot be edited directly, as far as the page tells already (save-time refusals need the
    // stored page and come on Apply); `null` if the page no longer has the element. Decides the menu and `startEdit`.
    function editStartOf(element: PreviewElement):
        | { refused: RefusalReason }
        | { nodes: NonNullable<ReturnType<typeof nodesOf>>; target: Element; node: HTMLElement; isRich: boolean }
        | null {
        const nodes = nodesOf(element);

        if (!nodes) {
            return null;
        }

        const isRich = nodes.part.matches(EDITABLE);
        // An item or cell on its own only in an editable; any other part is written as a whole.
        const target = isRich ? editLeafOf(nodes.target, nodes.clicked) : nodes.target;
        const node = textContainerOf(target) as HTMLElement;
        // Outside an editable, the plain-text plan says whether the tag's text part can take it.
        const check = isRich ? null : planTextEdit(nodes.part, target, '');

        if (check?.kind === 'refused') {
            return { refused: check.reason };
        }

        if (!isSupportedContent(node, { marks: isRich, fixedSelector: isRich ? MARKED : '' })) {
            return { refused: 'unsupportedContent' as const };
        }

        return { nodes, target, node, isRich };
    }

    // Whether the menu offers "Write it yourself" for `element` (`editStartOf`).
    function canStartEdit(element: PreviewElement): boolean {
        const start = editStartOf(element);

        return start !== null && !('refused' in start);
    }

    // "Write it yourself": the element itself becomes editable in the frame with the inline editor, focused with the
    // caret at the end (final_draft.html "startEdit"); in a list or table only the item or cell that was clicked
    // (`editLeafOf`). In an editable it offers formatting and links, and keeps embedded tags as they are; a tag's own
    // text part (a block) gets plain text. Content the editor could not write back unchanged (another element, a mark
    // with attributes) is not edited: the menu does not offer it (`editStartOf`); if the page changed since the menu
    // opened, a toast says why.
    function startEdit(element: PreviewElement) {
        const start = editStartOf(element);

        if (!start) {
            return;
        }

        if ('refused' in start) {
            toast.add({ title: refusalText(t, start.refused) });

            return;
        }

        const { nodes, target, node, isRich } = start;

        hasMovedFocusRef.current = true;
        setRefusal(null);
        setIsFeedbackOpen(false);
        setIsImageOpen(false);
        setIsLinkOpen(false);
        setEditing({
            node,
            target,
            element,
            isRich,
            structure: isRich && target !== nodes.target ? { element: nodes.target, originalHtml: nodes.target.innerHTML, changed: false } : null,
        });
    }

    // A click while editing an item or cell of a list or table: on another item or cell of it, the editing moves there
    // at once, the edited one keeps its text (saved together on Apply). Returns whether the click was handled; any
    // other click ends the editing (`usePreviewElements`).
    function switchEdit(clicked: Node): boolean {
        const structure = editing?.structure;

        if (!editing || !structure || !inlineEditor || !structure.element.contains(clicked)) {
            return false;
        }

        const target = editLeafOf(structure.element, clicked);

        // Between the items (e.g. the list's own space), or the item being edited: nothing to do.
        if (target === structure.element || target === editing.target) {
            return true;
        }

        const node = textContainerOf(target) as HTMLElement;

        if (!isSupportedContent(node, { marks: true, fixedSelector: MARKED })) {
            toast.add({ title: refusalText(t, 'unsupportedContent') });

            return true;
        }

        const changed = structure.changed || inlineEditor.isChanged();

        inlineEditor.keepContent();
        setEditing({ ...editing, node, target, structure: { ...structure, changed } });

        return true;
    }

    // Once more after the element menu has closed, which would otherwise keep the focus in the Workspace.
    useEffect(() => {
        if (!inlineEditor) {
            return;
        }

        const request = requestAnimationFrame(() => focusEditor(inlineEditor.editor, 'end'));

        return () => cancelAnimationFrame(request);
    }, [inlineEditor]);

    // Ends the editing in place: the inline editor gives the element its content back (Cancel, Escape, a click
    // elsewhere), and a list or table whose items were edited one after the other gets its content from before
    // back as a whole; after a save the frame reloads anyway.
    function stopEditing() {
        if (!editing) {
            return;
        }

        if (editing.structure) {
            editing.structure.element.innerHTML = editing.structure.originalHtml;
        }

        setEditing(null);
        setRefusal(null);
    }

    function openImage() {
        hasMovedFocusRef.current = true;
        setRefusal(null);
        setIsFeedbackOpen(false);
        setIsLinkOpen(false);
        setIsImageOpen(true);
    }

    // The link card starts from the link's text and target as the page has them now.
    function openLink(element: PreviewElement) {
        const text = textOf(element);
        const target = linkTargetValue(currentOf(element).link?.href ?? '');

        hasMovedFocusRef.current = true;
        setRefusal(null);
        setLinkInitial({ text, target });
        setIsFeedbackOpen(false);
        setIsImageOpen(false);
        setIsLinkOpen(true);
    }

    // Saves an edit planned from the page directly in the CMS (`useSaveCmsEdit`). Saved: the card closes
    // (`close`), a toast says so, and the frame reloads, showing the part `tagName` as changed. Unchanged in the CMS: the card closes with the
    // unchanged note. Refused: the card stays and names the reason. A failed request leaves the card open;
    // the hook shows the error.
    async function saveDirectly(plan: EditPlan, close: () => void, tagName: string) {
        setIsSending(true);
        setRefusal(null);

        try {
            const result = await saveEdit.mutateAsync({ pageId: preview.cms_ref.id, nodeId, plan });

            if (result === 'saved') {
                close();
                toast.add({ title: t('preview.save.saved') });
                setSavedChange({ seq: preview.seq, tagName });
                setSavedCount((count) => count + 1);
            } else if (result === 'unchanged') {
                close();
                toast.add({ title: t('preview.edit.unchanged') });
            } else {
                setRefusal(result.refused);
            }
        } catch {
            // Not saved: the hook has said why; the card and its values stay for another try.
        } finally {
            setIsSending(false);
        }
    }

    // The plan for an edit of `element` made on the page as it is now; refused if the page no longer has it.
    function planFor(element: PreviewElement, plan: (nodes: { part: Element; target: Element }) => EditPlan): EditPlan {
        const nodes = nodesOf(element);

        return nodes ? plan(nodes) : { kind: 'refused', reason: 'notFound' };
    }

    // Apply in the link card, with what it changed (`LinkCard`).
    async function applyLink(element: PreviewElement, change: LinkChange) {
        // Nothing to write: the card closes without a message, as the edit card's.
        if (change.text === undefined && change.target === undefined) {
            setIsLinkOpen(false);
            toast.add({ title: t('preview.edit.unchanged') });

            return;
        }

        await saveDirectly(planFor(element, ({ part, target }) => planLinkEdit(part, target, change)), () => setIsLinkOpen(false), element.tagName);
    }

    // "Use image" in the image card, with the picked image (`ImageCard`).
    async function applyImage(element: PreviewElement, picked: CmsItem | File) {
        if (nodeId === undefined) {
            return;
        }

        // A CMS image is saved directly; an uploaded one goes to GenAIx with the turn, which puts it into the CMS.
        if (!(picked instanceof File)) {
            const image = { imageId: picked.id, nodeId };

            await saveDirectly(planFor(element, ({ part }) => planImageEdit(part, image)), () => setIsImageOpen(false), element.tagName);

            return;
        }

        setIsSending(true);

        try {
            await actions?.onSendImage(pageElementOf(element), picked);
            setIsImageOpen(false);
        } catch {
            // Not sent: the caller has said why; the card and the pick stay for another try.
        } finally {
            setIsSending(false);
        }
    }

    // Apply: saves the edited content directly in the CMS, as HTML in an editable and as plain text in a tag's own
    // text part; an unchanged content ends the editing without a save (final_draft.html "commitEdit").
    async function applyEdit() {
        if (!editing || !inlineEditor || !canApply) {
            return;
        }

        const { target, element, isRich, structure } = editing;

        // In a list or table, also the items edited before this one count.
        if (!structure?.changed && !inlineEditor.isChanged()) {
            stopEditing();
            toast.add({ title: t('preview.edit.unchanged') });

            return;
        }

        const html = inlineEditor.html();
        const text = inlineEditor.text();

        // The element planned on when the editing started: the frame does not reload while editing, so it is still on the page.
        await saveDirectly(
            planFor(element, ({ part }) => {
                if (!part.contains(target)) {
                    return { kind: 'refused', reason: 'notFound' };
                }

                return isRich ? planHtmlEdit(part, target, html, inlineEditor.attributes) : planTextEdit(part, target, text);
            }),
            stopEditing,
            element.tagName,
        );
    }

    // The latest Cancel, for the listener below.
    const cancelEditRef = useRef(() => {});

    useEffect(() => {
        cancelEditRef.current = stopEditing;
    });

    // Escape cancels in the Workspace too, where the focus is after a click in the bar (e.g. an Apply the CMS
    // refused); in the element, the inline editor's own Escape does (`onCancel`).
    useEffect(() => {
        if (!editing) {
            return;
        }

        // Before the full-screen Escape on `window`, which leaves the default-prevented one alone.
        function handleWorkspaceKeyDown(event: KeyboardEvent) {
            if (event.key === 'Escape' && !event.defaultPrevented) {
                event.preventDefault();
                cancelEditRef.current();
            }
        }

        document.addEventListener('keydown', handleWorkspaceKeyDown);

        return () => document.removeEventListener('keydown', handleWorkspaceKeyDown);
    }, [editing]);

    // "In the chat" in the feedback card, with the typed text (`FeedbackCard`).
    function feedbackToChat(element: PreviewElement, text: string) {
        setIsFeedbackOpen(false);
        referenceInChat(element, text);
    }

    // Send in the feedback card, with the typed text (`FeedbackCard`).
    async function sendFeedback(element: PreviewElement, text: string) {
        setIsSending(true);

        try {
            await actions?.onSendFeedback(pageElementOf(element), text);
            setIsFeedbackOpen(false);
        } catch {
            // Not sent: the caller has said why; the card and its text stay for another try.
        } finally {
            setIsSending(false);
        }
    }

    function closeSettings() {
        setIsSettingsOpen(false);
        settingsButtonRef.current?.focus();
    }

    async function applySettings(changes: PageSettingsChange[]) {
        await actions?.onSendSettings(pageRef(), changes);
        closeSettings();
    }

    // The opened page settings take the focus, so that Escape closes them.
    useEffect(() => {
        if (isSettingsOpen) {
            settingsRef.current?.focus();
        }
    }, [isSettingsOpen]);

    // Escape leaves full screen, unless something else (an open menu) already handled it.
    useEffect(() => {
        if (!isFullScreen) {
            return;
        }

        function handleKeyDown(event: KeyboardEvent) {
            if (event.key === 'Escape' && !event.defaultPrevented) {
                setIsFullScreen(false);
            }
        }

        window.addEventListener('keydown', handleKeyDown);

        return () => window.removeEventListener('keydown', handleKeyDown);
    }, [isFullScreen]);

    return (
        <section className={`${styles.column} ${isFullScreen ? styles.fullScreen : ''}`} aria-labelledby={titleId}>
            <PreviewHeader
                titleId={titleId}
                url={url}
                publishedUrl={publishedUrl}
                pages={previews.length > 1 ? { names: pageNames, current: pageKey, onPick: (key) => setPicked({ key, seq: latest.seq }) } : null}
                selection={selected && { name: elementName(t, selected), onClear: () => { stopEditing(); setSelected(null); setIsMenuOpen(false); } }}
                settings={actions && preview.cms_ref.type === 'page' ? { open: isSettingsOpen, onToggle: () => setIsSettingsOpen((open) => !open) } : null}
                settingsButtonRef={settingsButtonRef}
                isMobile={isMobile}
                onMobileChange={setIsMobile}
                isFullScreen={isFullScreen}
                onFullScreenChange={setIsFullScreen}
            />
            <div className={`${styles.stage} ${isMobile ? styles.mobile : ''}`}>
                {isSettingsOpen && (
                    <PageSettings
                        ref={settingsRef}
                        pageId={preview.cms_ref.id}
                        nodeId={nodeId}
                        canSend={canSendFeedback}
                        onClose={closeSettings}
                        onApply={applySettings}
                    />
                )}
                <div className={styles.page}>
                    {/* The page before, until the new one has loaded: shown only, never read or used. */}
                    {shownFrame && !isFrameShown && (
                        <iframe key={shownFrame.key} className={styles.frame} src={shownFrame.url} aria-hidden="true" tabIndex={-1} inert />
                    )}
                    <iframe
                        key={frame.key}
                        ref={frameRef}
                        className={`${styles.frame} ${isFrameShown ? styles.frameShown : styles.frameLoading}`}
                        src={frame.url}
                        title={t('preview.frameTitle')}
                        onLoad={handleFrameLoad}
                    />
                    <PreviewOverlay
                        dimmed={isFeedbackOpen ? selectedBox : null}
                        changed={changedBoxes}
                        changedRound={frame.key}
                        selected={selectedBox && { box: selectedBox, mode: editing ? 'anchor' : (isImageOpen || isLinkOpen ? 'card' : 'outline') }}
                        selectedRef={selectedBoxRef}
                        target={editing ? null : targetBox}
                        editing={editing ? editingBox : null}
                        hovered={actions && hovered && !isSameElement(hovered.element, selected) ? { box: hovered.box, label: elementName(t, hovered.element) } : null}
                    />
                    {isBuilding && <div className={styles.building} aria-hidden="true" data-building="" />}
                    {/* The bar of the editing in place: for an item or cell above its list or table, so that it never covers the other items. */}
                    {editing && barBox && (
                        <EditBar
                            box={barBox}
                            label={elementName(t, editing.element)}
                            editor={editing.isRich && inlineEditor ? inlineEditor.editor : null}
                            count={editText.trim().length}
                            refusal={refusal}
                            canApply={canApply}
                            onCancel={() => stopEditing()}
                            onApply={() => void applyEdit()}
                        />
                    )}
                </div>
            </div>
            {/* The element menu. Closed by an action, the focus stays where the action put it; otherwise it goes back to the
                page, where the next Escape clears the selection (`finalFocus`). A link gets a new text and target; an image
                part another image instead of new text, if the node to pick it from is known. */}
            {selected && (
                <ElementMenu
                    open={isMenuOpen && Boolean(selectedBox)}
                    onOpenChange={setIsMenuOpen}
                    anchor={selectedBoxRef}
                    finalFocus={finalFocus}
                    label={elementName(t, selected)}
                    offers={{
                        link: Boolean(selected.link),
                        image: !selected.link && hasImage(selected) && nodeId !== undefined,
                        write: !selected.link && !hasImage(selected) && canStartEdit(selected),
                    }}
                    onFeedback={openFeedback}
                    onEditLink={() => openLink(selected)}
                    onSwapImage={openImage}
                    onWrite={() => startEdit(selected)}
                    onReference={() => referenceInChat(selected)}
                />
            )}
            {/* The feedback card, at the part like the element menu; not modal either. */}
            {selected && (
                <FeedbackCard
                    open={isFeedbackOpen && Boolean(selectedBox)}
                    onOpenChange={setIsFeedbackOpen}
                    anchor={selectedBoxRef}
                    title={elementName(t, selected)}
                    reference={elementLabel(t, selected)}
                    disabled={!canSendFeedback || isSending}
                    onSend={(text) => void sendFeedback(selected, text)}
                    onToChat={(text) => feedbackToChat(selected, text)}
                />
            )}
            {/* The image card, at the part like the other cards; not modal either. */}
            {selected && nodeId !== undefined && (
                <ImageCard
                    open={isImageOpen && Boolean(selectedBox)}
                    onOpenChange={setIsImageOpen}
                    anchor={selectedBoxRef}
                    title={elementName(t, selected)}
                    reference={elementLabel(t, selected)}
                    nodeId={nodeId}
                    refusal={refusal}
                    disabled={!canSendFeedback || isSending}
                    onUse={(picked) => void applyImage(selected, picked)}
                />
            )}
            {/* The link card, at the link like the image card; not modal either. */}
            {selected?.link && (
                <LinkCard
                    open={isLinkOpen && Boolean(selectedBox)}
                    onOpenChange={setIsLinkOpen}
                    anchor={selectedBoxRef}
                    title={elementName(t, selected)}
                    reference={elementLabel(t, selected)}
                    initial={linkInitial}
                    nodeId={nodeId}
                    pageId={preview.cms_ref.id}
                    refusal={refusal}
                    disabled={!canSendFeedback || isSending}
                    onApply={(change) => void applyLink(selected, change)}
                />
            )}
        </section>
    );
}
