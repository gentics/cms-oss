export class Version {
    major: number;
    minor: number;
    patch: number;
    meta: string;
    valid: boolean;

    static parse(version: string): Version {
        if (typeof version !== 'string') {
            return new Version(0, 0, 0);
        }
        const matches = version.toString().match(/^(\d+)\.(\d+)\.(\d+)(?:-(\S+))?$/);
        if (!matches) {
            return new Version(0, 0, 0);
        }

        return new Version(Number(matches[1]), Number(matches[2]), Number(matches[3]), matches[4] || '');
    }

    constructor(major: number, minor: number, patch: number, meta?: string) {
        this.major = major;
        this.minor = minor;
        this.patch = patch;
        this.meta = meta || '';
        this.valid = major !== 0 || minor !== 0 || patch !== 0;
    }

    isEqualTo(other: Version): boolean {
        return this.major === other.major
            && this.minor === other.minor
            && this.patch === other.patch
            && this.meta === other.meta
            && this.valid && other.valid;
    }

    isNewerThan(older: Version): boolean {
        if (!this.valid || !older.valid) {
            return false;
        }

        if (this.major > older.major) {
            return true;
        } else if (this.major < older.major) {
            return false;
        }

        if (this.minor > older.minor) {
            return true;
        } else if (this.minor < older.minor) {
            return false;
        }

        return this.minor > older.minor;
    }

    /**
     * Returns true if this Version is equal or newer than the passed Version.
     */
    satisfiesMinimum(other: Version): boolean {
        if (!this.valid || !other.valid) {
            return false;
        }

        if (this.major > other.major) {
            return true;
        } else if (this.major < other.major) {
            return false;
        }

        if (this.minor > other.minor) {
            return true;
        } else if (this.minor < other.minor) {
            return false;
        }

        return this.patch >= other.patch;
    }

    toString(): string {
        return `${this.major}.${this.minor}.${this.patch}` + (this.meta ? `-${this.meta}` : '');
    }
}
