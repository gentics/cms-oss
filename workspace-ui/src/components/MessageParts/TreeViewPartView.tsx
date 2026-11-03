import {
    ChevronDownIcon,
    ChevronRightIcon,
    FileTextIcon,
    FolderIcon,
    GlobeIcon,
    ImageIcon,
    LayersIcon,
    ListTreeIcon,
    type LucideIcon,
} from 'lucide-react';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';

import { Button } from '@/components/ui/button';
import { Checkbox } from '@/components/ui/checkbox';
import type { TreeNode, TreeViewPart } from '@/services/apiService/genaix/types';

import { PartCard } from './PartCard';
import type { PartViewProps } from './partProps';

import styles from './MessageParts.module.css';

// The node's CMS object type (`TreeNode.type`), design.md §9; anything else is a layer of the page.
const ICONS: Record<string, LucideIcon> = { node: GlobeIcon, folder: FolderIcon, page: FileTextIcon, image: ImageIcon, file: FileTextIcon };

function selectedIds(nodes: TreeNode[], into = new Set<string>()): Set<string> {
    nodes.forEach((node) => {
        if (node.selected) {
            into.add(node.id);
        }

        selectedIds(node.children ?? [], into);
    });

    return into;
}

interface TreeLevelProps {
    nodes: TreeNode[];
    depth: number;
    collapsed: Set<string>;
    selected: Set<string>;
    onToggleOpen: (id: string) => void;
    onToggleSelected: (id: string, checked: boolean) => void;
}

function TreeLevel({ nodes, depth, collapsed, selected, onToggleOpen, onToggleSelected }: TreeLevelProps) {
    const { t } = useTranslation();

    return (
        <ul className={depth === 0 ? styles.tree : styles.treeKids}>
            {nodes.map((node) => {
                const children = node.children ?? [];
                const isOpen = !collapsed.has(node.id);
                const Icon = ICONS[node.type ?? ''] ?? LayersIcon;

                return (
                    <li key={node.id}>
                        <div className={styles.treeNode}>
                            <span className={styles.treeTwist}>
                                {children.length > 0 && (
                                    <Button
                                        variant="ghost"
                                        size="icon-xs"
                                        aria-expanded={isOpen}
                                        aria-label={t(isOpen ? 'parts.tree.collapse' : 'parts.tree.expand', { label: node.label })}
                                        onClick={() => onToggleOpen(node.id)}
                                    >
                                        {isOpen ? <ChevronDownIcon size={14} /> : <ChevronRightIcon size={14} />}
                                    </Button>
                                )}
                            </span>
                            <label className={styles.treeLabel}>
                                <Checkbox checked={selected.has(node.id)} onCheckedChange={(checked) => onToggleSelected(node.id, checked)} />
                                <Icon size={14} aria-hidden />
                                <span className={depth === 0 ? styles.treeRoot : undefined}>{node.label}</span>
                            </label>
                        </div>
                        {children.length > 0 && isOpen && (
                            <TreeLevel
                                nodes={children}
                                depth={depth + 1}
                                collapsed={collapsed}
                                selected={selected}
                                onToggleOpen={onToggleOpen}
                                onToggleSelected={onToggleSelected}
                            />
                        )}
                    </li>
                );
            })}
        </ul>
    );
}

/**
 * `tree_view`: where in the content tree the agent works, as in the approved draft
 * (09-workspace-complete `treeHTML`): per node a chevron, a checkbox, the type icon and the label,
 * children indented along a line. Every level starts open; the selection starts from `selected` and
 * stays in this card.
 */
export function TreeViewPartView({ part }: PartViewProps<TreeViewPart>) {
    const { t } = useTranslation();
    const nodes = part.nodes ?? [];
    const [collapsed, setCollapsed] = useState<Set<string>>(() => new Set());
    const [selected, setSelected] = useState<Set<string>>(() => selectedIds(nodes));

    function toggleOpen(id: string) {
        setCollapsed((current) => {
            const next = new Set(current);

            if (!next.delete(id)) {
                next.add(id);
            }

            return next;
        });
    }

    function toggleSelected(id: string, checked: boolean) {
        setSelected((current) => {
            const next = new Set(current);

            if (checked) {
                next.add(id);
            } else {
                next.delete(id);
            }

            return next;
        });
    }

    return (
        <PartCard icon={ListTreeIcon} title={part.label || t('parts.tree.title')}>
            <TreeLevel
                nodes={nodes}
                depth={0}
                collapsed={collapsed}
                selected={selected}
                onToggleOpen={toggleOpen}
                onToggleSelected={toggleSelected}
            />
        </PartCard>
    );
}
