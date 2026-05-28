import { IDataFields, IFeatures, IForm, ISchema } from "./interfaces";

export type FormgridPreviewData = {
  formId: string;
  formType: string;
  schema: ISchema;
  uiSchema: IForm;
  features?: IFeatures;
  cmsSid?: number;
  language: string;
  prefillContent: IDataFields;
  currentPage?: number;
  selectedElementId?: string;
};

/**
 * Info about the preview-form which doesn't change
 */
export interface PreviewInformation {
  dynamic: boolean;
  formType: string;
  availableLanguages: string[];
  cmsSid?: number;
  features?: IFeatures;
}

/**
 * Form-data which may change when editing a form
 */
export interface PreviewFormData {
  schema: ISchema;
  uiSchema: IForm;
}

export interface ExtendedWindow extends Window {
  // Form Data that is assigned in the page directly.
  formPreviewData: FormgridPreviewData;
}

export const PREVIEW_CONNECT_EVENT = 'preview-connect';
export const PREVIEW_INTIALIZATION_EVENT_NAME = 'preview-init';
export const PREVIEW_FORM_CHANGE_EVENT_NAME = 'preview-form-change';
export const PREVIEW_SELECTED_ELEMENT_CHANGE_EVENT_NAME = 'preview-selected-element-change';
export const PREVIEW_CURRENT_PAGE_CHANGE_EVENT_NAME = 'preview-current-page-change';
export const PREVIEW_DISPLAY_LANGUAGE_CHANGE_EVENT_NAME = 'preview-language-change';
export const PREVIEW_LOADED_EVENT_NAME = 'preview-loaded';

export enum PreviewEventReceiver {
    CONTROLLER = 'controller',
    PUPPET = 'puppet',
}

export type PreviewEvent = PreviewConnectEvent
  | PreviewInitializationEvent
  | PreviewLoadedEvent
  | PreviewFormChangeEvent
  | PreviewSelectedElementChangeEvent
  | PreviewCurrentPageChangeEvent
  | PreviewDisplayLanguageChangeEvent
  ;

export interface PreviewConnectEvent {
    eventType: typeof PREVIEW_CONNECT_EVENT;
    receiver: PreviewEventReceiver;
}

export interface PreviewInitializationEvent extends PreviewInformation {
    eventType: typeof PREVIEW_INTIALIZATION_EVENT_NAME;
    receiver: PreviewEventReceiver;

    formId: string;
    language: string;
    pageIndex: number;
    elementId?: string;
}

export interface PreviewLoadedEvent {
    eventType: typeof PREVIEW_LOADED_EVENT_NAME;
    receiver: PreviewEventReceiver;
}

export interface PreviewFormChangeEvent extends PreviewFormData {
    eventType: typeof PREVIEW_FORM_CHANGE_EVENT_NAME;
    receiver: PreviewEventReceiver;

    prefillContent: IDataFields;
}

export interface PreviewSelectedElementChangeEvent {
    eventType: typeof PREVIEW_SELECTED_ELEMENT_CHANGE_EVENT_NAME;
    receiver: PreviewEventReceiver;

    elementId: string;
}

export interface PreviewCurrentPageChangeEvent {
    eventType: typeof PREVIEW_CURRENT_PAGE_CHANGE_EVENT_NAME;
    receiver: PreviewEventReceiver;

    pageIndex: number;
}

export interface PreviewDisplayLanguageChangeEvent {
    eventType: typeof PREVIEW_DISPLAY_LANGUAGE_CHANGE_EVENT_NAME;
    receiver: PreviewEventReceiver;

    language: string;
}
