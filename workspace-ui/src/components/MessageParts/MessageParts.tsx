import { Component, type ComponentType, type ReactNode } from 'react';

import type { MessagePart, PartType, UnknownPart } from '@/services/apiService/genaix/types';

import { ApiCallLogPartView } from './ApiCallLogPartView';
import { CitationPartView } from './CitationPartView';
import { ConstructDraftPartView } from './ConstructDraftPartView';
import { FileRefPartView } from './FileRefPartView';
import { ImageGridPartView } from './ImageGridPartView';
import { PageStructurePartView } from './PageStructurePartView';
import type { PartViewProps } from './partProps';
import { PropertiesListPartView } from './PropertiesListPartView';
import { SelectableListPartView } from './SelectableListPartView';
import { StatusNotePartView } from './StatusNotePartView';
import { TablePartView } from './TablePartView';
import { TextPartView } from './TextPartView';
import { TreeViewPartView } from './TreeViewPartView';
import { UnknownPartView } from './UnknownPartView';

type PartOf<Type extends PartType> = Extract<MessagePart, { type: Type }>;

/** The part registry: one renderer per member of the contract's `PartType`. */
const PART_RENDERERS: { [Type in PartType]: ComponentType<PartViewProps<PartOf<Type>>> } = {
    text: TextPartView,
    status_note: StatusNotePartView,
    tree_view: TreeViewPartView,
    selectable_list: SelectableListPartView,
    properties_list: PropertiesListPartView,
    image_grid: ImageGridPartView,
    table: TablePartView,
    page_structure: PageStructurePartView,
    construct_draft: ConstructDraftPartView,
    api_call_log: ApiCallLogPartView,
    citation: CitationPartView,
    file_ref: FileRefPartView,
};

function isKnownPart(part: MessagePart | UnknownPart): part is MessagePart {
    return Object.hasOwn(PART_RENDERERS, part.type);
}

/**
 * Keeps one part that fails to render (a shape the contract does not allow, half-streamed) from
 * taking the message down: it then falls back to the `UnknownPart` rule.
 */
class PartBoundary extends Component<{ part: MessagePart | UnknownPart; children: ReactNode }, { failed: boolean }> {
    state = { failed: false };

    static getDerivedStateFromError() {
        return { failed: true };
    }

    componentDidUpdate(previous: { part: MessagePart | UnknownPart }) {
        // The next delta or `part.completed` may well render; try again with it.
        if (this.state.failed && previous.part !== this.props.part) {
            this.setState({ failed: false });
        }
    }

    render() {
        return this.state.failed ? <UnknownPartView part={this.props.part} /> : this.props.children;
    }
}

interface PartRendererProps extends Omit<PartViewProps<unknown>, 'part'> {
    part: MessagePart | UnknownPart;
}

/**
 * Renders one assistant or system message part with the renderer its `type` maps to. A type outside
 * the registry follows the contract's `UnknownPart` rule (its `text`, else nothing) and never fails
 * the message.
 */
export function PartRenderer({ part, ...props }: PartRendererProps) {
    if (!isKnownPart(part)) {
        return <UnknownPartView part={part} />;
    }

    // The registry maps each `type` to the renderer of exactly that part, so this one takes `part`.
    const Renderer = PART_RENDERERS[part.type] as ComponentType<PartViewProps<MessagePart>>;

    return (
        <PartBoundary part={part}>
            <Renderer part={part} {...props} />
        </PartBoundary>
    );
}
