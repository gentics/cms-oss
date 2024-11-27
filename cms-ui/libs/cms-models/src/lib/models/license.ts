export enum LicenseStatus {
    /** The license is missing */
    MISSING = 'MISSING',
    /** The license is invalid/could not be verified */
    INVALID = 'INVALID',
    /** The license is valid */
    VALID = 'VALID',
    /** The license itself is valid (?), but not available yet */
    NOT_ACTIVATED = 'NOT_ACTIVATED',
    /** The license itself is valid, but the required feature is not present */
    INSUFFICIENT = 'INSUFFICIENT',
}

export interface License {
    /** JWT issuer field. Can be ignored */
    issuer?: string;
    /** JWT subject field. Can be ignored */
    subject?: string;
    /** The ID of the license */
    uuid: string;
    /** The licensed features. Key is the Product/Feature ID, and the Value is the Name. */
    features: Record<string, string>;
    /** When the License was issued. */
    issuedAt: string;
    /** Until when the License is valid. If not set or 0, it doesn't expire. */
    validUntil?: string;
    /** Signature of the license which acts as a hash */
    signature: string;
}

export interface LicenseCheckResult {
    status: LicenseStatus;
    license: License;
}

export interface ContentRepositoryLicense {
    /** ID of the CR */
    id: number;
    /** Name of the CR */
    name: string;
    /** URL of the CR */
    url: string;
    /** If the CR is an open source version, and therefore doesn't have/need a license */
    openSource: boolean;
    /** License of the CR */
    license?: License;
    /** Status of the License */
    status: LicenseStatus;
}
