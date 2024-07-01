export interface LoginInformation {
    username: string;
    password: string;
}

export enum TestSize {
    MINIMAL = 'minimal',
    FULL = 'full',
}

export type EntityMap = Record<string, any>;

export const ENV_KEYCLOAK_ENABLED = 'FEATURE_KEYCLOAK';
export const ENV_MULTI_CHANNELING_ENABLED = 'FEATURE_MULTI_CHANNELING';
export const ENV_FORMS_ENABLED = 'FEATURE_FORMS';
export const ENV_CONTENT_STAGING_ENABLED = 'FEATURE_CONTENT_STAGING';
