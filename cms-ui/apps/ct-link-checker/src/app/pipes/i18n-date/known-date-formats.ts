export type KnownDateFormatName = 'date' | 'time' | 'dateTime' | 'longDate' | 'longTime' | 'longDateTime';

/** Known format options for browsers with Intl support */
export const knownFormats: { [key: string]: Intl.DateTimeFormatOptions } = {
    date: {
        day: 'numeric',
        month: 'numeric',
        year: 'numeric'
    },
    time: {
        hour: 'numeric',
        minute: 'numeric'
    },
    dateTime: {
        day: 'numeric',
        month: 'numeric',
        year: 'numeric',
        hour: 'numeric',
        minute: 'numeric'
    },
    longDate: {
        weekday: 'long',
        day: 'numeric',
        month: 'numeric',
        year: 'numeric'
    },
    longTime: {
        hour: 'numeric',
        minute: 'numeric',
        second: 'numeric'
    },
    longDateTime: {
        weekday: 'long',
        day: 'numeric',
        month: 'numeric',
        year: 'numeric',
        hour: 'numeric',
        minute: 'numeric',
        second: 'numeric'
    }
};
