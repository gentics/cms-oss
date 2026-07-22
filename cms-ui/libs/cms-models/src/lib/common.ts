export type EntityType = 'page' | 'folder' | 'form' | 'image' | 'file' | 'node' | 'template';

export enum LocalizationType {
    FULL = 'FULL',
    PARTIAL = 'PARTIAL',
}

/**
 * This format is designed to store string-values subject to translation.
 */
export interface I18nString {
    [key: string]: string;
}
