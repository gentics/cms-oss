export function dateToFileSystemString(date: Date): string {
    return `${date.getFullYear()}-${padLeft(date.getMonth(), '0', 2)}-${padLeft(date.getDate(), '0', 2)}_${padLeft(date.getHours(), '0', 2)}-${padLeft(date.getMinutes(), '0', 2)}`
}

export function padLeft(source: number | string, filler: string, size: number): string {
    if (typeof source !== 'string') {
        source = source.toString();
    }

    while (source.length < size) {
        source = filler + source;
    }

    return source;
}
