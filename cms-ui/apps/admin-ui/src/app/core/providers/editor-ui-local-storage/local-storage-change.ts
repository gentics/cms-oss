export class LocalStorageChange {
    /** The key of the changed LocalStorage entry without prefix */
    key: string;

    /** The old value of the LocalStorage entry */
    get oldValue(): any {
        const value = JSON.parse(this._oldValue);
        Object.defineProperty(this, 'oldValue', { value, configurable: true, enumerable: true });
        return value;
    }

    /** The new value of the LocalStorage entry */
    get newValue(): any {
        const value = JSON.parse(this._newValue);
        Object.defineProperty(this, 'newValue', { value, configurable: true, enumerable: true });
        return value;
    }

    private _oldValue: string;
    private _newValue: string;

    constructor(event: LocalStorageProps) {
        this.key = event.key;
        this._oldValue = event.oldValue;
        this._newValue = event.newValue;
    }
}

export interface LocalStorageProps {
    key?: string;
    oldValue?: string;
    newValue?: string;
}
