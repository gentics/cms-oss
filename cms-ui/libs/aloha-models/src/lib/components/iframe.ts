import { AlohaComponent, AlohaCoreComponentNames } from './base-component';

export enum AlohaIFrameEventNames {
    INIT = 'aloha.iframe-component.init',
    UPDATE_VALUE = 'aloha.iframe-component.update-value',
    UPDATE_OPTIONS = 'aloha.iframe-component.update-options',
    DISABLED = 'aloha.iframe-component.disabled',

    WINDOW_SIZE = 'aloha.iframe-component.size',
    CHANGE = 'aloha.iframe-component.change',
    TOUCH = 'aloha.iframe-component.touch',
}

interface WindowSize {
    width: number;
    height: number;
}

export interface AlohaIFrameInitEvent<T = any> {
    eventName: AlohaIFrameEventNames.INIT;
    value: {
        id: string;
        value: T;
        disabled: boolean;
        options?: any;
        size: WindowSize;
    };
}

export interface AlohaIFrameUpdateValueEvent<T = any> {
    eventName: AlohaIFrameEventNames.UPDATE_VALUE;
    id: string;
    value: T;
}

export interface AlohaIFrameUpdateOptionsEvent {
    eventName: AlohaIFrameEventNames.UPDATE_OPTIONS;
    id: string;
    value: any;
}

export interface AlohaIFrameDisabledEvent {
    eventName: AlohaIFrameEventNames.DISABLED;
    id: string;
    value: boolean;
}

export interface AlohaIFrameWindowSizeEvent {
    eventName: AlohaIFrameEventNames.WINDOW_SIZE;
    id: string;
    value: WindowSize;
}

export interface AlohaIFrameChangeEvent<T = any> {
    eventName: AlohaIFrameEventNames.CHANGE;
    id: string;
    value: T;
}

export interface AlohaIFrameTouchEvent {
    eventName: AlohaIFrameEventNames.TOUCH;
    id: string;
}

export type AlohaIFrameEvent = AlohaIFrameInitEvent
| AlohaIFrameUpdateValueEvent
| AlohaIFrameUpdateOptionsEvent
| AlohaIFrameDisabledEvent
| AlohaIFrameWindowSizeEvent
| AlohaIFrameChangeEvent
| AlohaIFrameTouchEvent
    ;

export interface AlohaIFrameComponent extends AlohaComponent {
    type: AlohaCoreComponentNames.IFRAME;

    url: string;
    value: any;
    options?: any;

    setOptions: (options: any) => void;
    setUrl: (url: string) => void;

    onFrameLoad: (elem: HTMLIFrameElement) => void;
    onFrameInit: (elem: HTMLIFrameElement) => void;
}
