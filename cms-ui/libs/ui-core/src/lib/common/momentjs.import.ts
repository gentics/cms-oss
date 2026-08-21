/*  eslint-disable import-x/no-named-as-default-member */

/**
 * This is a workaround for loading moment JS in TypeScript 3.2 and Angular 7.
 * This is based on https://github.com/rollup/rollup/issues/1267#issuecomment-446681320
 */

import moment from 'moment';
import 'moment-timezone';

// eslint-disable-next-line @typescript-eslint/no-unsafe-call
let instance: Moment = ((moment as any).default || moment as any)();

/*
 * This getter and setter are only here for the tests, as we need to setup a special
 * Instance which should be used.
 * Same for all other exports, so they can apply the correct timezone inbetween.
 */

export function getInstance(): Moment {
    return instance;
}

export function setInstance(value: Moment): void {
    instance = value;
}

export function unix(timestamp: number): Moment {
    const parsed = moment.unix(timestamp);
    const zone = instance.tz();
    if (typeof zone === 'string') {
        parsed.tz(zone);
    }
    return parsed;
}

export function locale(language?: string | string[], definition?: moment.LocaleSpecification | null | undefined): string {
    return moment.locale(language as any, definition);
}

/** The Moment class. This must be used as a type instead of momentjs.Moment. */
export type Moment = moment.Moment;
