/* eslint-disable @typescript-eslint/no-unsafe-call, @typescript-eslint/no-unsafe-assignment, @typescript-eslint/no-unsafe-member-access */

/**
 * This is a workaround for loading moment JS in TypeScript 3.2 and Angular 7.
 * This is based on https://github.com/rollup/rollup/issues/1267#issuecomment-446681320
 */

import * as moment_ from 'moment';
import 'moment-timezone';

let instance: Moment = (moment_ as any).default || moment_;

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
    const parsed = moment_.unix(timestamp);
    parsed.tz(instance.tz());
    return parsed;
}

export function locale(language?: string | string[], definition?: moment_.LocaleSpecification | null | undefined): string {
    return moment_.locale(language as any, definition);
}


/** The Moment class. This must be used as a type instead of momentjs.Moment. */
export type Moment = moment_.Moment;
