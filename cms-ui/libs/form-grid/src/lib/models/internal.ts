import { FormElement, FormSchemaProperty } from '@gentics/cms-models';

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

export const ATTR_CONTAINER_ID = 'data-drop-container-id';
export const ATTR_ELEMENT_ID = 'data-element-id';

export interface ElementMoveData {
    elementType: string;
    inserting: boolean;
}

export interface ElementSelectionEvent {
    element: FormElement;
    containerId: string;
}

export interface ElementContainerMoveEvent {
    pageIndex: number;
    elementId: string;
    fromContainerId: string;
    toContainerId: string;
    targetIndex: number;
}

export interface ElementInterPageMoveEvent {
    elementId: string;
    fromPage: number;
    toPage: number;
}
