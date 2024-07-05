export interface LoginInformation {
    username: string;
    password: string;
}

export enum TestSize {
    MINIMAL = 'minimal',
    FULL = 'full',
}

export type EntityMap = Record<string, any>;

export const ENV_CMS_REST_PATH = 'CMS_REST_PATH';
export const ENV_CMS_EDITOR_PATH = 'CMS_EDITOR_PATH';
export const ENV_CMS_ADMIN_PATH = 'CMS_ADMIN_PATH';
export const ENV_CMS_USERNAME = 'CMS_USERNAME';
export const ENV_CMS_PASSWORD = 'CMS_PASSWORD';

export const ENV_MESH_CR_ENABLED = 'FEATURE_MESH_CR';
export const ENV_KEYCLOAK_ENABLED = 'FEATURE_KEYCLOAK';
export const ENV_MULTI_CHANNELING_ENABLED = 'FEATURE_MULTI_CHANNELING';
export const ENV_FORMS_ENABLED = 'FEATURE_FORMS';
export const ENV_CONTENT_STAGING_ENABLED = 'FEATURE_CONTENT_STAGING';
