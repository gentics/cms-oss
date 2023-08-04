export function toValidNumber(value: any, defaultValue: number | null = null): number | null {
    if (value == null) {
        return defaultValue;
    }

    if (typeof value === 'string') {
        value = Number(value);
    }

    if (typeof value !== 'number' || isNaN(value) || !isFinite(value)) {
        return defaultValue;
    }

    return value;
}
