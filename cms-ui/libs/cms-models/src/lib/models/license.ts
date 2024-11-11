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
    issuedAt: number;
    /** Until when the License is valid. If not set or 0, it doesn't expire. */
    validUntil?: number;
}

export interface LicenseCheckResult {
    status: LicenseStatus;
    license: License;
}
