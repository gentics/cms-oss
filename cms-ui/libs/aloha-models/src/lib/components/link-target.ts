import { AlohaComponent, AlohaCoreComponentNames } from './base-component';

export interface ExtendedLinkTarget extends LinkTarget {
    isInternal: boolean;
    internalTargetLabel?: string;
    internalTargetId?: number;
    internalTargetType?: string;
    internalTargetLang?: string;
    internalTargetNodeId?: number;
}

export interface LinkTarget {
    target: string;
    anchor: string;
}

export interface AlohaLinkTargetComponent extends AlohaComponent {
    type: AlohaCoreComponentNames.LINK_TARGET;

    value: LinkTarget;
    targetLabel?: string;
    anchorLabel?: string;

    // "Undocumented" properties for GCMS-UI integration.
    // Not set unless opened via the GCN Plugin
    showPicker?: boolean;
    pickerTitle?: string;
}
