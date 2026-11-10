import { NodeViewWrapper, type ReactNodeViewProps } from '@tiptap/react';
import { type LucideIcon, PaperclipIcon, SlidersHorizontalIcon, XIcon } from 'lucide-react';
import { useTranslation } from 'react-i18next';

import { ReferenceToken } from '@/components/ReferenceToken/ReferenceToken';
import { Button } from '@/components/ui/button';
import { FILE_REF_NODE, type FileRefAttrs, SETTING_NODE, TOKEN_ATTRIBUTE, tokenLabel } from '@/helper/composerParts/composerParts';

// The icon of each kind of token (design.md §9); a reference keeps `ReferenceToken`'s `@`.
const ICONS: Partial<Record<string, LucideIcon>> = { [SETTING_NODE]: SlidersHorizontalIcon, [FILE_REF_NODE]: PaperclipIcon };

/**
 * A token in the field (`ReferenceToken`): its icon, its label, and the × that removes it; a file attached as verbatim
 * dashed. The wrapper keeps the node's `data-token`, which pasting reads (`parseHTML`).
 */
export function TokenChip({ node, deleteNode }: ReactNodeViewProps) {
    const { t } = useTranslation();
    const label = tokenLabel(node);
    const removeLabel = t('composer.removeToken', { label });
    const isVerbatim = node.type.name === FILE_REF_NODE && (node.attrs as FileRefAttrs).mode === 'verbatim';

    return (
        <NodeViewWrapper as="span" {...{ [TOKEN_ATTRIBUTE]: node.type.name }} contentEditable={false}>
            <ReferenceToken
                icon={ICONS[node.type.name]}
                verbatim={isVerbatim}
                title={label}
                action={(
                    <Button variant="ghost" size="icon-xs" aria-label={removeLabel} title={removeLabel} onClick={deleteNode}>
                        <XIcon size={12} aria-hidden />
                    </Button>
                )}
            >
                {label}
            </ReferenceToken>
        </NodeViewWrapper>
    );
}
