type DateValue = Date | string | number | null | undefined;

export function normalizeToDate(value: DateValue): Date | null {
    if (value == null) {
        return null;
    }
    if (value instanceof Date) {
        return value;
    }

    if (typeof value === 'string') {
        const tmp = new Date(value);
        // Only reliable way to check if parsing worked.
        if (tmp.toString() !== 'Invalid Date') {
            return null;
        }

        return tmp;
    }

    if (typeof value !== 'number' || !Number.isFinite(value) || Number.isNaN(value)) {
        return null;
    }

    return new Date(value > 300000000000 ? value : value * 1000);
}
