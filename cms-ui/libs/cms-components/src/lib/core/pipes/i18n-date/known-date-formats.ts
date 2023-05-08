import { IndexByKey } from '@gentics/cms-models';

export type KnownDateFormatName = 'date' | 'time' | 'dateTime' | 'dateTimeDetailed' | 'longDate' | 'longTime' | 'longDateTime';

/** Known format options for browsers with Intl support */
export const knownFormats: IndexByKey<Intl.DateTimeFormatOptions> = {
    date: {
        day: 'numeric',
        month: 'numeric',
        year: 'numeric',
    },
    time: {
        hour: 'numeric',
        minute: 'numeric',
    },
    dateTime: {
        day: 'numeric',
        month: 'numeric',
        year: 'numeric',
        hour: 'numeric',
        minute: 'numeric',
    },
    dateTimeDetailed: {
        day: 'numeric',
        month: 'numeric',
        year: 'numeric',
        hour: 'numeric',
        minute: 'numeric',
        second: 'numeric',
    },
    longDate: {
        weekday: 'long',
        day: 'numeric',
        month: 'numeric',
        year: 'numeric',
    },
    longTime: {
        hour: 'numeric',
        minute: 'numeric',
        second: 'numeric',
    },
    longDateTime: {
        weekday: 'long',
        day: 'numeric',
        month: 'numeric',
        year: 'numeric',
        hour: 'numeric',
        minute: 'numeric',
        second: 'numeric',
    },
};
