import { NodeViewWrapper, type ReactNodeViewProps } from '@tiptap/react';
import { LockIcon, XIcon } from 'lucide-react';
import { useTranslation } from 'react-i18next';

import { Button } from '@/components/ui/button';
import { textContent } from '@/helper/composerParts/composerParts';

/** A locked passage in the field: lock icon, its text, and the × that turns it back into text. */
export function VerbatimPassage({ node, editor, getPos }: ReactNodeViewProps) {
    const { t } = useTranslation();
    const removeLabel = t('composer.removeVerbatim');
    const text = String(node.attrs.text);

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
            <Button variant="ghost" size="icon-xs" aria-label={removeLabel} title={removeLabel} onClick={unmark}>
                <XIcon size={12} aria-hidden />
            </Button>
        </NodeViewWrapper>
    );
}
