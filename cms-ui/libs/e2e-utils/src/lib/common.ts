export interface LoginInformation {
    username: string;
    password: string;
}

export enum TestSize {
    MINIMAL = 'minimal',
    FULL = 'full',
}

export type EntityMap = Record<string, any>;
