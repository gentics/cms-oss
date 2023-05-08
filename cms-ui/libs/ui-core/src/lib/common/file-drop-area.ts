import { InjectionToken } from "@angular/core";

export interface IFileDropAreaOptions {
    /**
     * A list of mime types accepted by the drop area. Defaults to "*".
     * Some mime types will not be reported by the client, they get matched as "unknown/unknown".
     * @example
     *   { accept: 'image/*, !image/gif' }
     *   { accept: 'text/*' }
     *   { accept: 'video/*, unknown/*' }
     */
    accept?: string;

    /**
     * Set to true to prevent interaction with the drop area.
     */
    disabled?: boolean;

    /**
     * Allow multiple files to be dropped on the drop area. Defaults to true.
     */
    multiple?: boolean;
}

export const DEFAULT_FILE_DROP_AREA_OPTIONS: IFileDropAreaOptions = {
    accept: '*',
    disabled: false,
    multiple: true
};

export interface IDraggedFile {
    type: string;
}

/**
 * A token that can be used to inject a mock into the directive
 * @internal
 */
export const FILE_DROPAREA_DRAG_EVENT_TARGET = new InjectionToken('FILE_DROPAREA_DRAG_EVENT_TARGET');

