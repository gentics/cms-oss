import { FormElement, FormSchemaProperty } from '@gentics/cms-models';

export const PALETTE_MIME = 'application/x-andp-formgrid-palette';
export const ELEMENT_MIME = 'application/x-form-grid-element';
export const CLIPBOARD_MIME = 'application/x-formgrid-clipboard';
export const CLIPBOARD_STORAGE_KEY = 'formgrid-clipboard';

export interface FormGridClipboardData {
    element: FormElement;
    elementSchema?: FormSchemaProperty;
    /** Schema properties of all nested child elements, keyed by their original element ID */
    childSchemas?: Record<string, FormSchemaProperty>;
    formId: number | null;
    formType: string;
    formName: string;
}

export interface PaletteDropTarget {
    /** The ID of the element-container, to determine where to place the item in. */
    elementContainerId: string;
    /** The index of where to place the element within the elementContainer */
    index: number;
    /** How many columns the item spans */
    span: number;
    /** Neighbor that dynamically yields size during a drop simulation (prevents wrapping without flickering) */
    resizeNeighbor?: {
        index: number;
        span: number;
    };
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

export interface ElementInterPageMoveEvent {
    elementId: string;
    fromPage: number;
    toPage: number;
}
