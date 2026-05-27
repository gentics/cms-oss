import { Platform } from "react-native";

const PREVIEW_LOADED_EVENT_NAME = 'preview-loaded';
const PREVIEW_ELEMENT_SELECT_EVENT_NAME = 'preview-select-element';
const PREVIEW_PAGE_CHANGE_EVENT_NAME = 'preview-change-page';

interface PreviewLoadedEvent {
  eventType: typeof PREVIEW_LOADED_EVENT_NAME;
}

interface PreviewElementSelectEvent {
  eventType: typeof PREVIEW_ELEMENT_SELECT_EVENT_NAME;
  elementId: string;
}

interface PreviewPageChangeEvent {
  eventType: typeof PREVIEW_PAGE_CHANGE_EVENT_NAME;
  pageIndex: number;
}

export function emitSelected(elementId: string): void {
  if (Platform.OS !== 'web') return;
  if (typeof window === 'undefined') return;

  const event: PreviewElementSelectEvent = {
    eventType: PREVIEW_ELEMENT_SELECT_EVENT_NAME,
    elementId,
  }
  window.postMessage(event, '/');
}

export function emitPageChange(pageIndex: number): void {
  if (Platform.OS !== 'web') return;
  if (typeof window === 'undefined') return;

  const event: PreviewPageChangeEvent = {
    eventType: PREVIEW_PAGE_CHANGE_EVENT_NAME,
    pageIndex,
  };
  window.postMessage(event, '/');
}

export function emitLoadedEvent(): void {
    if (Platform.OS !== 'web') return;
  if (typeof window === 'undefined') return;

  const event: PreviewLoadedEvent = {
    eventType: PREVIEW_LOADED_EVENT_NAME,
  };
  window.postMessage(event, '/');
}