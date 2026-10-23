import { NodeViewWrapper, type ReactNodeViewProps } from '@tiptap/react';
import { AtSignIcon, type LucideIcon, PaperclipIcon, SlidersHorizontalIcon, XIcon } from 'lucide-react';
import { useTranslation } from 'react-i18next';

import { Button } from '@/components/ui/button';
import { FILE_REF_NODE, SETTING_NODE, TOKEN_ATTRIBUTE, tokenLabel } from '@/helper/composerParts/composerParts';

// The icon of each kind of token (design.md §9); a reference is the default.
const ICONS: Partial<Record<string, LucideIcon>> = { [SETTING_NODE]: SlidersHorizontalIcon, [FILE_REF_NODE]: PaperclipIcon };

/** A token in the field: its icon, its label, and the × that removes it. */
export function TokenChip({ node, deleteNode }: ReactNodeViewProps) {
    const { t } = useTranslation();
    const label = tokenLabel(node);
    const removeLabel = t('composer.removeToken', { label });
    const Icon = ICONS[node.type.name] ?? AtSignIcon;

    return (
        <NodeViewWrapper as="span" {...{ [TOKEN_ATTRIBUTE]: node.type.name }} contentEditable={false}>
            <Icon size={13} aria-hidden />
            <span data-token-label="" title={label}>{label}</span>
            <Button variant="ghost" size="icon-xs" aria-label={removeLabel} title={removeLabel} onClick={deleteNode}>
                <XIcon size={12} aria-hidden />
            </Button>
        </NodeViewWrapper>
    );
}
