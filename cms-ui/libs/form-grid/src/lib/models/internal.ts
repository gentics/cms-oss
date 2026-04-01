import { FormElement } from '@gentics/cms-models';

export const PALETTE_MIME = 'application/x-andp-formgrid-palette';

export interface PaletteDropTarget {
    /** The ID of the element-container, to determine where to place the item in. */
    elementContainerId: string;
    /** The index of where to place the element within the elementContainer */
    index: number;
    /** How many columns the item spans */
    span: number;
}

export interface DropItemRect {
    index: number;
    rect: DOMRect;
}

export interface DropRow {
    top: number;
    bottom: number;
    items: DropItemRect[];
}

export interface ElementSelectionEvent {
    element: FormElement;
    containerId: string;
}
