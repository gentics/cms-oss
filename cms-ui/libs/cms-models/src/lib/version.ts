/**
 * Gentics CMP Version Response data.
 */
export interface Version {
    cmpVersion: string;
    version: string;
    variant: Variant;
    nodeInfo: { [key: string]: NodeVersionInfo; };
}

export enum Variant {
    OPEN_SOURCE = 'OSS',
    ENTERPRISE = 'EE',
}

export interface NodeVersionInfo {
    meshVersion?: string;
    portalType?: string;
    portalVersion?: string;
    compatibility?: VersionCompatibility;
}

export enum VersionCompatibility {
    /** The specific node relies on software dependencies not compatible to another part of the Gentics Content Management Platform */
    NOT_SUPPORTED = 'NOT_SUPPORTED',
    /** All dependencies the specific node relies on are fully supported by Gentics Content Management Platform */
    SUPPORTED = 'SUPPORTED',
    /** The specific node relies on software dependencies unknown/not guaranteed to be compatible to Gentics Content Management Platform */
    UNKNOWN = 'UNKNOWN',
}
