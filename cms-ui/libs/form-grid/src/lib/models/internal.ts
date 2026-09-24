import { FormElement, FormSchemaProperty, FormSelectOptionValue } from '@gentics/cms-models';

export const ELEMENT_MIME = 'application/x-form-grid-element';
export const CLIPBOARD_MIME = 'application/x-formgrid-clipboard';
export const CLIPBOARD_STORAGE_KEY = 'formgrid-clipboard';

/**
 * String which is used as "glue"/join chars for separating container- and element-id
 */
export const UNIQUE_ID_GLUE = '@';

export const ATTR_CONTAINER_ID = 'data-drop-container-id';
export const ATTR_ELEMENT_ID = 'data-element-id';
export const ATTR_CONTEXT_ID = 'data-context-id';

export interface FormGridClipboardData {
    element: FormElement;
    elementSchema?: FormSchemaProperty;
    /** Schema properties of all nested child elements, keyed by their original element ID */
    childSchemas?: Record<string, FormSchemaProperty>;
    formId: number | null;
    formType: string;
    formName: string;
}

export interface ElementMoveData {
    elementId: string | null;
    contextId: string | null;
    elementType: string;
    inserting: boolean;
}

export interface ElementSelectionEvent {
    elementId: string;
    contextId: string;
}

export interface ElementContainerMoveEvent {
    pageIndex: number;
    elementId: string;
    fromContainerId: string;
    fromContextId: string;
    toContainerId: string;
    toContextId: string;
    targetIndex: number;
}

export interface ElementInterPageMoveEvent {
    elementId: string;
    fromPage: number;
    toPage: number;
}

export interface DefaultableFormSelectOptionValue extends FormSelectOptionValue {
    /**
     * Internally used to determine which labels are to be defaulted to the value.
     * This is needed as there's otherwise no way to distinguish a label from a user,
     * and one that has been defaulted.
     */
    // eslint-disable-next-line @typescript-eslint/naming-convention
    _defaulted?: string[];
}
