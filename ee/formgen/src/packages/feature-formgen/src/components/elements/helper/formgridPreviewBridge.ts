import { Platform } from "react-native";
import {
  PREVIEW_CONNECT_EVENT,
  PREVIEW_CURRENT_PAGE_CHANGE_EVENT_NAME,
  PREVIEW_LOADED_EVENT_NAME,
  PREVIEW_SELECTED_ELEMENT_CHANGE_EVENT_NAME,
  PreviewEvent,
  PreviewEventReceiver
} from "../../../types/preview";

export function sendPreviewConnectEvent(): void {
  sendEvent({
    eventType: PREVIEW_CONNECT_EVENT,
    receiver: PreviewEventReceiver.CONTROLLER,
  });
}

export function sendPreviewSelectedElementEvent(elementId: string): void {
  sendEvent({
    eventType: PREVIEW_SELECTED_ELEMENT_CHANGE_EVENT_NAME,
    receiver: PreviewEventReceiver.CONTROLLER,
    elementId,
  });
}

export function sendPreviewPageChangeEvent(pageIndex: number): void {
  sendEvent({
    eventType: PREVIEW_CURRENT_PAGE_CHANGE_EVENT_NAME,
    receiver: PreviewEventReceiver.CONTROLLER,
    pageIndex,
  });
}

export function sendPreviewLoadedEvent(): void {
  sendEvent({
    eventType: PREVIEW_LOADED_EVENT_NAME,
    receiver: PreviewEventReceiver.CONTROLLER,
  });
}

function sendEvent(event: PreviewEvent): void {
  if (Platform.OS !== 'web') return;
  if (typeof window === 'undefined') return;

  console.debug('>> FormGen', event.eventType);

  window.postMessage(event, '/');
}
