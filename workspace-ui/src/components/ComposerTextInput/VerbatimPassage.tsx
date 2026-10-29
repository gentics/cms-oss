import { NodeViewWrapper, type ReactNodeViewProps } from '@tiptap/react';
import { LockIcon, PaperclipIcon, UserIcon, XIcon } from 'lucide-react';
import { useContext } from 'react';
import { useTranslation } from 'react-i18next';

import { Button } from '@/components/ui/button';
import { DropdownMenu, DropdownMenuContent, DropdownMenuRadioGroup, DropdownMenuRadioItem, DropdownMenuTrigger } from '@/components/ui/dropdown-menu';
import { sourceAttachmentId, textContent } from '@/helper/composerParts/composerParts';
import { useSessionUploads } from '@/hooks/useGenaixQueries';

import { type VerbatimSourceOption, VerbatimSourcesContext } from './verbatimSources';

type Choose = (option: VerbatimSourceOption) => void;

// One choice of the menu; picking it stores its value and name on the passage.
function SourceItem({ option, onChoose }: { option: VerbatimSourceOption; onChoose: Choose }) {
    return (
        <DropdownMenuRadioItem value={option.value} closeOnClick onClick={() => onChoose(option)}>
            {option.label}
        </DropdownMenuRadioItem>
    );
}

// The session's uploaded files as choices; only mounted while the menu is open, so they are
// loaded then and not with every composer.
function SessionUploadItems({ sessionId, onChoose }: { sessionId: string; onChoose: Choose }) {
    const uploads = useSessionUploads(sessionId);

    return (uploads.data ?? []).map(({ id, name }) => <SourceItem key={id} option={{ value: id, label: name }} onChoose={onChoose} />);
}

/**
 * A locked passage in the field: lock icon, its text, where it was quoted from (typed, or an
 * attached or uploaded file; `VerbatimSourcesContext`), and the × that turns it back into text.
 */
export function VerbatimPassage({ node, editor, getPos, updateAttributes }: ReactNodeViewProps) {
    const { t } = useTranslation();
    const { attachments, sessionId } = useContext(VerbatimSourcesContext);
    const removeLabel = t('composer.removeVerbatim');
    const text = String(node.attrs.text);
    const source = String(node.attrs.source);
    const attachmentId = sourceAttachmentId(source);
    // A passage whose attachment was removed counts as typed, as it is sent (`useComposer`).
    const isTyped = source === 'user' || (attachmentId !== null && !attachments.some((option) => option.value === source));
    const current = isTyped ? 'user' : source;
    const currentLabel = isTyped ? t('composer.verbatimSource.user') : String(node.attrs.sourceLabel);
    const sourceLabel = t('composer.verbatimSource.label', { source: currentLabel });

    function choose({ value, label }: VerbatimSourceOption) {
        updateAttributes({ source: value, sourceLabel: value === 'user' ? '' : label });
    }

    function unmark() {
        const from = getPos();

        if (from !== undefined) {
            editor.chain().insertContentAt({ from, to: from + node.nodeSize }, textContent(text)).run();
        }
    }

    return (
        <NodeViewWrapper as="span" data-verbatim="" contentEditable={false}>
            <LockIcon size={13} aria-hidden />
            <span data-verbatim-text="">{text}</span>
            <DropdownMenu>
                <DropdownMenuTrigger render={<Button variant="ghost" size="icon-xs" aria-label={sourceLabel} title={sourceLabel} />}>
                    {isTyped ? <UserIcon size={12} aria-hidden /> : <PaperclipIcon size={12} aria-hidden />}
                </DropdownMenuTrigger>
                <DropdownMenuContent finalFocus={() => editor.view.dom}>
                    <DropdownMenuRadioGroup value={current}>
                        <SourceItem option={{ value: 'user', label: t('composer.verbatimSource.user') }} onChoose={choose} />
                        {attachments.map((option) => <SourceItem key={option.value} option={option} onChoose={choose} />)}
                        {sessionId && <SessionUploadItems sessionId={sessionId} onChoose={choose} />}
                    </DropdownMenuRadioGroup>
                </DropdownMenuContent>
            </DropdownMenu>
            <Button variant="ghost" size="icon-xs" aria-label={removeLabel} title={removeLabel} onClick={unmark}>
                <XIcon size={12} aria-hidden />
            </Button>
        </NodeViewWrapper>
    );
}
