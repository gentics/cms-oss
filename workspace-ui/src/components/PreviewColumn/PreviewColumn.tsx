import { ArrowUpIcon, AtSignIcon, CheckIcon, ExternalLinkIcon, ImagesIcon, Maximize2Icon, MessagesSquareIcon, Minimize2Icon, MonitorIcon, PencilIcon, SlidersHorizontalIcon, SmartphoneIcon, SparklesIcon, XIcon } from 'lucide-react';
import { type CSSProperties, type KeyboardEvent as ReactKeyboardEvent, useEffect, useId, useRef, useState } from 'react';
import { useTranslation } from 'react-i18next';

import type { PageElementInput } from '@/components/Composer/Composer';
import { ImagePicker } from '@/components/ImagePicker/ImagePicker';
import { PageSettings } from '@/components/PageSettings/PageSettings';
import { Button } from '@/components/ui/button';
import { DropdownMenu, DropdownMenuContent, DropdownMenuGroup, DropdownMenuItem, DropdownMenuLabel } from '@/components/ui/dropdown-menu';
import { Label } from '@/components/ui/label';
import { Popover, PopoverClose, PopoverContent, PopoverTitle } from '@/components/ui/popover';
import { Textarea } from '@/components/ui/textarea';
import { useToast } from '@/components/ui/use-toast';
import type { PageSettingsChange } from '@/helper/composerParts/composerParts';
import { type ElementBox, type PreviewElement, usePreviewElements } from '@/hooks/usePreviewElements';
import type { ContextReference } from '@/services/apiService/genaix/types';
import type { CmsItem } from '@/services/cmsApiService/cmsApiService';
import type { LivePreview } from '@/store/useWorkspaceEventStore';

import styles from './PreviewColumn.module.css';

// The query parameters of a read-only CMS preview, each with the value it must have.
const READ_ONLY_PARAMS = { mode: 'view', real: 'view' } as const;

// `url` (`preview_url` of `preview.updated`) as a read-only preview: `mode=view` as in the contract
// example, and `real=view`, which is what the CMS's `/alohapage` reads (`AlohaPageServlet`; without
// it, it renders for editing and locks the page). A parameter that is missing or has another value
// is set; everything else stays. A URL that already has both is returned unchanged.
function readOnlyPreviewUrl(url: string): string {
    const parsed = new URL(url, window.location.href);
    const entries = Object.entries(READ_ONLY_PARAMS);

    if (entries.every(([key, value]) => parsed.searchParams.get(key) === value)) {
        return url;
    }

    entries.forEach(([key, value]) => parsed.searchParams.set(key, value));

    return parsed.toString();
}

// Where a box sits over the page, as inline style.
function boxStyle({ top, left, width, height }: ElementBox): CSSProperties {
    return { top, left, width, height };
}

// `text` with runs of white space as one space and without it at the ends: two texts that read the same compare equal.
function collapseWhiteSpace(text: string): string {
    return text.replace(/\s+/g, ' ').trim();
}

// How far the outlines stand off a part (`outline-offset` in the CSS module); the focus mode leaves that much room.
const OUTLINE_OFFSET = 2;

// The four bands around `box` that the focus mode covers: above, below, left and right of it.
function dimBands({ top, left, width, height }: ElementBox): CSSProperties[] {
    const gap = OUTLINE_OFFSET;
    const bottom = top + height + gap;
    const right = left + width + gap;

    return [
        { top: 0, left: 0, right: 0, height: Math.max(0, top - gap) },
        { top: bottom, left: 0, right: 0, bottom: 0 },
        { top: top - gap, left: 0, width: Math.max(0, left - gap), height: height + 2 * gap },
        { top: top - gap, left: right, right: 0, height: height + 2 * gap },
    ];
}

/** What the preview hands on for a part: the page and the part, to reference in the chat, to send feedback on, to give new text or another image. */
export interface PreviewActions {
    /** Adds the part to the chat composer, followed by `text` if given. */
    onReferenceInChat: (element: PageElementInput, text?: string) => void;
    /** Sends feedback on the part as a turn; rejects when it was not sent, so the card stays open. */
    onSendFeedback: (element: PageElementInput, text: string) => Promise<void>;
    /** Sends the part's new text, written by hand, as a turn; rejects when it was not sent, so the card stays open. */
    onSendEdit: (element: PageElementInput, text: string) => Promise<void>;
    /** Sends the CMS image picked for the part as a turn; rejects when it was not sent, so the card stays open. */
    onSendImage: (element: PageElementInput, image: ContextReference) => Promise<void>;
    /** Sends the changed page settings of the page as a turn; rejects when they were not sent, so the sheet stays open. */
    onSendSettings: (page: ContextReference, changes: PageSettingsChange[]) => Promise<void>;
    /** Whether a turn can be sent now (feedback, new text, an image or page settings): not while a run is working or waiting. */
    canSendFeedback: boolean;
}

/** The preview, with all of `PreviewActions` (the session's chat) or none of them (only showing the page, as on the review page). */
export type PreviewColumnProps = { preview: LivePreview } & (PreviewActions | { [K in keyof PreviewActions]?: undefined });

/**
 * The right column: the CMS preview of the page the session produced, framed on the preview stage
 * (design.md §11 "Vorschau-Bühne"), always read-only (`readOnlyPreviewUrl`), so that showing it
 * never opens the page for editing. The browser loads the URL with its own CMS session. A CMS that
 * forbids framing leaves the frame blank, so the URL and a link to open it are always shown next to
 * it (integration guide §5). Each `preview.updated` remounts the frame, which reloads it even when
 * the URL stayed the same. The header switches the width between desktop and mobile (390 px) and
 * shows the column in full screen, which Escape leaves (final_draft.html "preview widths").
 *
 * A part of the page is selected by clicking it (`usePreviewElements`): a layer above the frame
 * outlines the part under the mouse and the selected one, the page itself stays as the CMS rendered
 * it (design.md §12 "Elemente in der Vorschau"). The header names the selection and clears it. When a
 * reload no longer has the selected part, the selection is cleared and a toast says so.
 *
 * The parts the last CMS write changed (`changed_elements`) are tinted until the next update.
 *
 * Clicking a part also opens its element menu at the part (final_draft.html "ELEMENT MENU"): "Give
 * feedback" opens the feedback card there (final_draft.html "FEEDBACK CARD AT THE ELEMENT"), which
 * sends the typed text on the part (`onSendFeedback`) or moves both into the chat; while it is open the
 * rest of the page is dimmed (design.md §12 focus mode). "Write it yourself" opens the edit card there
 * (implementation plan, "Hand edit form"): the part's current text, read from the frame, to change as
 * plain text; Apply or Ctrl/Cmd+Enter sends it (`onSendEdit`) to be written word for word, an unchanged
 * text sends nothing. For a part that shows an image, "Swap the image" takes the place of "Write it
 * yourself" (final_draft.html "askImage"), when the preview names its node: the image card there offers
 * the node's images from the CMS (`ImagePicker`), and "Use image" sends the picked one (`onSendImage`).
 * "Reference in the chat" hands the part to `onReferenceInChat` (the chat composer's `insertPageElement`).
 *
 * For a page, "Page settings" in the header drops the page settings over the preview (`PageSettings`,
 * final_draft.html "PAGE SETTINGS"); Apply sends the changed ones (`onSendSettings`), Escape or the
 * button closes them.
 *
 * Without `PreviewActions` the preview only shows the page: no part is selected, outlined or offered a
 * menu, and there are no page settings; the changed parts are still tinted.
 */
export function PreviewColumn(props: PreviewColumnProps) {
    const { preview } = props;
    const actions: PreviewActions | undefined = props.onReferenceInChat ? props : undefined;
    const canSendFeedback = actions?.canSendFeedback ?? false;
    const { t } = useTranslation();
    const titleId = useId();
    const url = readOnlyPreviewUrl(preview.preview_url);
    const [isMobile, setIsMobile] = useState(false);
    const [isFullScreen, setIsFullScreen] = useState(false);
    const fullScreenLabel = t(isFullScreen ? 'preview.leaveFullScreen' : 'preview.fullScreen');
    const toast = useToast();
    const frameRef = useRef<HTMLIFrameElement>(null);
    const [selected, setSelected] = useState<PreviewElement | null>(null);
    const [isMenuOpen, setIsMenuOpen] = useState(false);
    // The selected part's outline, where its element menu opens.
    const selectedBoxRef = useRef<HTMLDivElement>(null);
    // Whether the menu closes because an action moved the focus elsewhere (the chat, the feedback card).
    const hasMovedFocusRef = useRef(false);
    const [isFeedbackOpen, setIsFeedbackOpen] = useState(false);
    const [feedback, setFeedback] = useState('');
    const [isSending, setIsSending] = useState(false);
    const feedbackFieldId = useId();
    const feedbackFieldRef = useRef<HTMLTextAreaElement>(null);
    const canSend = canSendFeedback && !isSending && feedback.trim() !== '';
    const [isEditOpen, setIsEditOpen] = useState(false);
    const [editText, setEditText] = useState('');
    // The part's text when the edit card opened, to tell whether anything changed.
    const [editOriginal, setEditOriginal] = useState('');
    const editFieldId = useId();
    const editFieldRef = useRef<HTMLTextAreaElement>(null);
    const canApply = canSendFeedback && !isSending && editText.trim() !== '';
    const [isImageOpen, setIsImageOpen] = useState(false);
    const [pickedImage, setPickedImage] = useState<CmsItem | null>(null);
    const canUseImage = canSendFeedback && !isSending && pickedImage !== null;
    // The node whose images can replace a part's image; without it, no image can be offered.
    const nodeId = preview.cms_ref.node_id;
    const [isSettingsOpen, setIsSettingsOpen] = useState(false);
    const settingsRef = useRef<HTMLElement>(null);
    const settingsButtonRef = useRef<HTMLButtonElement>(null);
    const { onFrameLoad, hovered, selectedBox, changedBoxes, textOf, hasImage } = usePreviewElements(frameRef, selected?.tagName ?? null, (element) => {
        // A click on a part selects it and opens its menu; Escape clears both. Only with actions to offer.
        if (!actions) {
            return;
        }

        setSelected(element);
        setIsMenuOpen(element !== null);
        setIsFeedbackOpen(false);
        setIsEditOpen(false);
        setIsImageOpen(false);
    }, () => {
        // A reload without the selected part: the selection is gone.
        setSelected(null);
        setIsMenuOpen(false);
        setIsFeedbackOpen(false);
        setIsEditOpen(false);
        setIsImageOpen(false);
        toast.add({ title: t('preview.selection.removed') });
    }, preview.changed_elements);

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

    // The part with its page, as the chat and the feedback take it.
    function pageElementOf(element: PreviewElement): PageElementInput {
        return {
            page: pageRef(),
            tagName: element.tagName,
            constructName: element.constructName,
        };
    }

    function referenceInChat(element: PreviewElement, text?: string) {
        hasMovedFocusRef.current = true;
        actions?.onReferenceInChat(pageElementOf(element), text);
        toast.add({ title: t('preview.selection.referenced') });
    }

    function openFeedback() {
        hasMovedFocusRef.current = true;
        setFeedback('');
        setIsEditOpen(false);
        setIsImageOpen(false);
        setIsFeedbackOpen(true);
    }

    function openEdit(element: PreviewElement) {
        const text = textOf(element.tagName);

        hasMovedFocusRef.current = true;
        setEditOriginal(text);
        setEditText(text);
        setIsFeedbackOpen(false);
        setIsImageOpen(false);
        setIsEditOpen(true);
    }

    function openImage() {
        hasMovedFocusRef.current = true;
        setPickedImage(null);
        setIsFeedbackOpen(false);
        setIsEditOpen(false);
        setIsImageOpen(true);
    }

    async function applyImage(element: PreviewElement) {
        if (!canUseImage || nodeId === undefined) {
            return;
        }

        setIsSending(true);

        try {
            await actions?.onSendImage(pageElementOf(element), { type: 'image', id: String(pickedImage.id), node_id: nodeId, label: pickedImage.name });
            setIsImageOpen(false);
        } catch {
            // Not sent: the caller has said why; the card and the pick stay for another try.
        } finally {
            setIsSending(false);
        }
    }

    async function applyEdit(element: PreviewElement) {
        if (!canApply) {
            return;
        }

        // Nothing to write: the card closes without a message (final_draft.html "commitEdit").
        if (collapseWhiteSpace(editText) === collapseWhiteSpace(editOriginal)) {
            setIsEditOpen(false);
            toast.add({ title: t('preview.edit.unchanged') });

            return;
        }

        setIsSending(true);

        try {
            await actions?.onSendEdit(pageElementOf(element), editText);
            setIsEditOpen(false);
        } catch {
            // Not sent: the caller has said why; the card and its text stay for another try.
        } finally {
            setIsSending(false);
        }
    }

    // Ctrl/Cmd+Enter applies; Enter alone starts a new line, as the text may have several (final_draft.html "startEdit").
    function handleEditKeyDown(event: ReactKeyboardEvent<HTMLTextAreaElement>, element: PreviewElement) {
        if (event.key === 'Enter' && (event.ctrlKey || event.metaKey)) {
            event.preventDefault();
            void applyEdit(element);
        }
    }

    function feedbackToChat(element: PreviewElement) {
        setIsFeedbackOpen(false);
        referenceInChat(element, feedback.trim());
    }

    async function sendFeedback(element: PreviewElement) {
        if (!canSend) {
            return;
        }

        setIsSending(true);

        try {
            await actions?.onSendFeedback(pageElementOf(element), feedback.trim());
            setIsFeedbackOpen(false);
        } catch {
            // Not sent: the caller has said why; the card and its text stay for another try.
        } finally {
            setIsSending(false);
        }
    }

    // Enter sends, Shift+Enter starts a new line (final_draft.html "spotIn").
    function handleFeedbackKeyDown(event: ReactKeyboardEvent<HTMLTextAreaElement>, element: PreviewElement) {
        if (event.key === 'Enter' && !event.shiftKey) {
            event.preventDefault();
            void sendFeedback(element);
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
            <header className={styles.head}>
                <h2 id={titleId} className={styles.title}>{t('preview.title')}</h2>
                <span className={styles.url} title={url}>{url}</span>
                {/* A link in the look of a button, as "New" in the left column. */}
                <Button
                    size="sm"
                    variant="ghost"
                    nativeButton={false}
                    role={undefined}
                    render={<a href={url} target="_blank" rel="noopener noreferrer" />}
                >
                    <ExternalLinkIcon size={14} aria-hidden="true" />
                    {t('preview.open')}
                </Button>
                {selected && (
                    <span className={styles.selection}>
                        {t('preview.selection.selected', { name: selected.constructName || selected.tagName })}
                        <Button variant="ghost" size="icon-xs" aria-label={t('preview.selection.clear')} title={t('preview.selection.clear')} onClick={() => { setSelected(null); setIsMenuOpen(false); }}>
                            <XIcon size={12} />
                        </Button>
                    </span>
                )}
                {actions && preview.cms_ref.type === 'page' && (
                    <Button ref={settingsButtonRef} variant="secondary" size="sm" aria-expanded={isSettingsOpen} onClick={() => setIsSettingsOpen((open) => !open)}>
                        <SlidersHorizontalIcon size={14} />
                        {t('preview.settings.open')}
                    </Button>
                )}
                <div className={styles.viewports} role="group" aria-label={t('preview.viewport')}>
                    <Button variant="ghost" size="icon" aria-label={t('preview.desktop')} title={t('preview.desktop')} aria-pressed={!isMobile} onClick={() => setIsMobile(false)}>
                        <MonitorIcon size={16} />
                    </Button>
                    <Button variant="ghost" size="icon" aria-label={t('preview.mobile')} title={t('preview.mobile')} aria-pressed={isMobile} onClick={() => setIsMobile(true)}>
                        <SmartphoneIcon size={16} />
                    </Button>
                    <Button variant="ghost" size="icon" aria-label={fullScreenLabel} title={fullScreenLabel} aria-pressed={isFullScreen} onClick={() => setIsFullScreen((on) => !on)}>
                        {isFullScreen ? <Minimize2Icon size={16} /> : <Maximize2Icon size={16} />}
                    </Button>
                </div>
            </header>
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
                    <iframe key={preview.seq} ref={frameRef} className={styles.frame} src={url} title={t('preview.frameTitle')} onLoad={onFrameLoad} />
                    <div className={styles.overlay} aria-hidden="true">
                        {isFeedbackOpen && selectedBox && dimBands(selectedBox).map((band, index) => <div key={index} className={styles.dim} style={band} data-dim="" />)}
                        {changedBoxes.map(({ tagName, box }) => <div key={tagName} className={styles.changed} style={boxStyle(box)} data-changed-part={tagName} />)}
                        {selectedBox && <div ref={selectedBoxRef} className={`${styles.selected} ${isEditOpen || isImageOpen ? styles.editing : ''}`} style={boxStyle(selectedBox)} />}
                        {actions && hovered && hovered.element.tagName !== selected?.tagName && (
                            <div className={styles.hovered} style={boxStyle(hovered.box)}>
                                <span className={styles.label}>{hovered.element.constructName || hovered.element.tagName}</span>
                            </div>
                        )}
                    </div>
                </div>
            </div>
            {/* The element menu. Not modal: the page and the chat stay usable while it is open, as with the draft's popover. */}
            {selected && (
                <DropdownMenu open={isMenuOpen && Boolean(selectedBox)} onOpenChange={setIsMenuOpen} modal={false}>
                    {/* Closed by an action, the focus stays where the action put it; otherwise it goes back to the
                        page, where the next Escape clears the selection. */}
                    <DropdownMenuContent anchor={selectedBoxRef} finalFocus={finalFocus}>
                        <DropdownMenuGroup>
                            <DropdownMenuLabel>{selected.constructName || selected.tagName}</DropdownMenuLabel>
                            <DropdownMenuItem onClick={openFeedback}>
                                <SparklesIcon size={16} />
                                {t('preview.feedback.give')}
                            </DropdownMenuItem>
                            {/* An image part gets another image instead of new text, if the node to pick it from is known. */}
                            {hasImage(selected.tagName)
                                ? nodeId !== undefined && (
                                    <DropdownMenuItem onClick={openImage}>
                                        <ImagesIcon size={16} />
                                        {t('preview.image.swap')}
                                    </DropdownMenuItem>
                                )
                                : (
                                    <DropdownMenuItem onClick={() => openEdit(selected)}>
                                        <PencilIcon size={16} />
                                        {t('preview.edit.write')}
                                    </DropdownMenuItem>
                                )}
                            <DropdownMenuItem onClick={() => referenceInChat(selected)}>
                                <AtSignIcon size={16} />
                                {t('preview.selection.referenceInChat')}
                            </DropdownMenuItem>
                        </DropdownMenuGroup>
                    </DropdownMenuContent>
                </DropdownMenu>
            )}
            {/* The feedback card, at the part like the element menu; not modal either. */}
            {selected && (
                <Popover open={isFeedbackOpen && Boolean(selectedBox)} onOpenChange={setIsFeedbackOpen} modal={false}>
                    <PopoverContent anchor={selectedBoxRef} initialFocus={feedbackFieldRef} finalFocus={false}>
                        <div className={styles.cardHead}>
                            <SparklesIcon size={16} className={styles.cardIcon} aria-hidden="true" />
                            <PopoverTitle className={styles.cardTitle}>{selected.constructName || selected.tagName}</PopoverTitle>
                            <PopoverClose render={<Button variant="ghost" size="icon" aria-label={t('preview.feedback.cancel')} title={t('preview.feedback.cancel')} />}>
                                <XIcon size={16} />
                            </PopoverClose>
                        </div>
                        <div className={styles.cardBody}>
                            <span className={styles.token}>
                                <AtSignIcon size={13} aria-hidden="true" />
                                {selected.constructName ? `${selected.constructName} · ${selected.tagName}` : selected.tagName}
                            </span>
                            <Label htmlFor={feedbackFieldId}>{t('preview.feedback.label')}</Label>
                            <Textarea
                                id={feedbackFieldId}
                                ref={feedbackFieldRef}
                                value={feedback}
                                onChange={(event) => setFeedback(event.target.value)}
                                onKeyDown={(event) => handleFeedbackKeyDown(event, selected)}
                            />
                        </div>
                        <div className={styles.cardFoot}>
                            <Button variant="secondary" size="sm" onClick={() => feedbackToChat(selected)}>
                                <MessagesSquareIcon size={14} />
                                {t('preview.feedback.toChat')}
                            </Button>
                            <Button variant="primary" size="sm" disabled={!canSend} onClick={() => void sendFeedback(selected)}>
                                <ArrowUpIcon size={14} />
                                {t('preview.feedback.send')}
                            </Button>
                        </div>
                    </PopoverContent>
                </Popover>
            )}
            {/* The edit card, at the part like the feedback card; not modal either. */}
            {selected && (
                <Popover open={isEditOpen && Boolean(selectedBox)} onOpenChange={setIsEditOpen} modal={false}>
                    <PopoverContent anchor={selectedBoxRef} initialFocus={editFieldRef} finalFocus={false}>
                        <div className={styles.cardHead}>
                            <PencilIcon size={16} className={styles.cardIcon} aria-hidden="true" />
                            <PopoverTitle className={styles.cardTitle}>{selected.constructName || selected.tagName}</PopoverTitle>
                            <PopoverClose render={<Button variant="ghost" size="icon" aria-label={t('preview.edit.cancel')} title={t('preview.edit.cancel')} />}>
                                <XIcon size={16} />
                            </PopoverClose>
                        </div>
                        <div className={styles.cardBody}>
                            <span className={styles.token}>
                                <AtSignIcon size={13} aria-hidden="true" />
                                {selected.constructName ? `${selected.constructName} · ${selected.tagName}` : selected.tagName}
                            </span>
                            <Label htmlFor={editFieldId}>{t('preview.edit.label')}</Label>
                            <Textarea
                                id={editFieldId}
                                ref={editFieldRef}
                                value={editText}
                                onChange={(event) => setEditText(event.target.value)}
                                onKeyDown={(event) => handleEditKeyDown(event, selected)}
                            />
                        </div>
                        <div className={styles.cardFoot}>
                            <Button variant="primary" size="sm" disabled={!canApply} onClick={() => void applyEdit(selected)}>
                                <CheckIcon size={14} />
                                {t('preview.edit.apply')}
                            </Button>
                        </div>
                    </PopoverContent>
                </Popover>
            )}
            {/* The image card, at the part like the other cards; not modal either. */}
            {selected && nodeId !== undefined && (
                <Popover open={isImageOpen && Boolean(selectedBox)} onOpenChange={setIsImageOpen} modal={false}>
                    <PopoverContent anchor={selectedBoxRef} finalFocus={false}>
                        <div className={styles.cardHead}>
                            <ImagesIcon size={16} className={styles.cardIcon} aria-hidden="true" />
                            <PopoverTitle className={styles.cardTitle}>{selected.constructName || selected.tagName}</PopoverTitle>
                            <PopoverClose render={<Button variant="ghost" size="icon" aria-label={t('preview.image.cancel')} title={t('preview.image.cancel')} />}>
                                <XIcon size={16} />
                            </PopoverClose>
                        </div>
                        <div className={styles.cardBody}>
                            <span className={styles.token}>
                                <AtSignIcon size={13} aria-hidden="true" />
                                {selected.constructName ? `${selected.constructName} · ${selected.tagName}` : selected.tagName}
                            </span>
                            <ImagePicker nodeId={nodeId} value={pickedImage} onValueChange={setPickedImage} />
                        </div>
                        <div className={styles.cardFoot}>
                            <Button variant="primary" size="sm" disabled={!canUseImage} onClick={() => void applyImage(selected)}>
                                <CheckIcon size={14} />
                                {t('preview.image.use')}
                            </Button>
                        </div>
                    </PopoverContent>
                </Popover>
            )}
        </section>
    );
}
