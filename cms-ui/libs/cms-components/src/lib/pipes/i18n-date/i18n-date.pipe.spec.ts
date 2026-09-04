import { LangChangeEvent, provideTranslateService, TranslateService } from '@ngx-translate/core';
import { Observable, Subject } from 'rxjs';
import { I18nDatePipe } from './i18n-date.pipe';
import { TestBed } from '@angular/core/testing';
import { I18nService } from '../../providers';

const originalIntl = (<any> window).Intl;
const originalNavigator = window.navigator;

/** 2016-12-31T22:59:59.000Z */
const newYears2016 = new Date(2016, 11, 31, 23, 59, 59);

/** 2016-10-01T09:55:00.000Z */
const fiveBeforeTwelve = new Date(2016, 9, 1, 11, 55, 0);

/** 2016-02-01T02:04:05.000Z */
const leadingZeroesDate = new Date(2016, 1, 1, 3, 4, 5);


class MockChangeDetectorRef {
    markForCheck = jasmine.createSpy('markForCheck');
}

class MockNavigator {
    private internalLangs = ['en'];

    userAgent = originalNavigator.userAgent;

    get language(): string {
        return this.internalLangs[0];
    }

    get languages(): string[] {
        return this.internalLangs;
    }

    mockUserAgentLanguages(...languages: string[]): void {
        this.internalLangs = languages;
    }
}

class MockDateTimeFormat implements Intl.DateTimeFormat {
    resolvedOptions: any;
    constructorArgs: any[];

    constructor() {
        this.constructorArgs = Array.from(arguments);
    }

    formatRange(startDate: number | bigint | Date, endDate: number | bigint | Date): string {
        return '';
    }

    formatRangeToParts(startDate: number | bigint | Date, endDate: number | bigint | Date): Intl.DateTimeRangeFormatPart[] {
        return [];
    }

    format(): string {
        return '';
    }

    formatToParts(date?: number | Date): Intl.DateTimeFormatPart[] {
        throw new Error('Method not implemented.');
    }
}

describe('I18nDatePipe', () => {

    let pipe: I18nDatePipe;
    let service: I18nService;

    let mockChangeDetector: MockChangeDetectorRef;
    let navigator: MockNavigator;

    beforeEach(() => {
        if (!window.Intl) {
            pending('Intl not supported by browser');
        }

        TestBed.configureTestingModule({
            providers: [
                provideTranslateService(),
                I18nService,
            ],
        });

        service = TestBed.inject(I18nService);

        mockChangeDetector = new MockChangeDetectorRef();
        navigator = new MockNavigator();
        Object.defineProperty(window, 'navigator', {
            configurable: true,
            enumerable: true,
            value: navigator,
            writable: false,
        });

        pipe = new I18nDatePipe(service, mockChangeDetector as any);
    });

    afterAll(() => {
        window.Intl = originalIntl;
        Object.defineProperty(window, 'navigator', {
            configurable: true,
            enumerable: true,
            value: originalNavigator,
            writable: false,
        });
    });

    it('transforms Date objects and timestamps with seconds and milliseconds equally', () => {
        service.setLanguage('en');

        for (const date of [newYears2016, fiveBeforeTwelve, leadingZeroesDate]) {
            const fromDate = pipe.transform(date, 'longDateTime');
            const fromMilliseconds = pipe.transform(date.getTime(), 'longDateTime');
            const fromSeconds = pipe.transform(date.getTime() / 1000, 'longDateTime');

            expect(fromDate).toBe(fromMilliseconds);
            expect(fromDate).toBe(fromSeconds);
            expect(fromSeconds).toBe(fromMilliseconds);
        }
    });

    describe('format "date"', () => {

        it('transforms "en-US" date correctly', () => {
            navigator.mockUserAgentLanguages('en-US', 'en');
            service.setLanguage('en');

            expect(pipe.transform(newYears2016, 'date')).toBe('12/31/2016');
            expect(pipe.transform(fiveBeforeTwelve, 'date')).toBe('10/1/2016');
            expect(pipe.transform(leadingZeroesDate, 'date')).toBe('2/1/2016');
        });

        it('transforms "en-GB" date correctly', () => {
            navigator.mockUserAgentLanguages('en-GB', 'en');
            service.setLanguage('en');

            expect(pipe.transform(newYears2016, 'date')).toBe('12/31/2016');
            expect(pipe.transform(fiveBeforeTwelve, 'date')).toBe('10/1/2016');
            expect(pipe.transform(leadingZeroesDate, 'date')).toBe('2/1/2016');
        });

        it('transforms "de" date correctly', () => {
            navigator.mockUserAgentLanguages('de');
            service.setLanguage('de');

            expect(pipe.transform(newYears2016, 'date')).toBe('31.12.2016');
            expect(pipe.transform(fiveBeforeTwelve, 'date')).toBe('1.10.2016');
            expect(pipe.transform(leadingZeroesDate, 'date')).toBe('1.2.2016');
        });

        it('transforms "de" date correctly when User-Agent reports ["de-AT", "de"]', () => {
            service.setLanguage('de');
            navigator.mockUserAgentLanguages('de-AT', 'de');

            expect(pipe.transform(newYears2016, 'date')).toBe('31.12.2016');
            expect(pipe.transform(fiveBeforeTwelve, 'date')).toBe('1.10.2016');
            expect(pipe.transform(leadingZeroesDate, 'date')).toBe('1.2.2016');
        });

        it('transforms "de" date correctly when User-Agent reports ["de-DE", "de"]', () => {
            service.setLanguage('de');
            navigator.mockUserAgentLanguages('de-DE', 'de');

            expect(pipe.transform(newYears2016, 'date')).toBe('31.12.2016');
            expect(pipe.transform(fiveBeforeTwelve, 'date')).toBe('1.10.2016');
            expect(pipe.transform(leadingZeroesDate, 'date')).toBe('1.2.2016');
        });

        it('transforms "de" date correctly when User-Agent prefers "en"', () => {
            service.setLanguage('de');
            navigator.mockUserAgentLanguages('en-GB', 'en-US', 'en', 'de');

            expect(pipe.transform(newYears2016, 'date')).toBe('31.12.2016');
            expect(pipe.transform(fiveBeforeTwelve, 'date')).toBe('1.10.2016');
            expect(pipe.transform(leadingZeroesDate, 'date')).toBe('1.2.2016');
        });

        it('transforms "en" date correctly when User-Agent reports "de", "en-GB"', () => {
            service.setLanguage('en');
            navigator.mockUserAgentLanguages('de', 'en-GB', 'en-US', 'en');

            expect(pipe.transform(newYears2016, 'date')).toBe('12/31/2016');
            expect(pipe.transform(fiveBeforeTwelve, 'date')).toBe('10/1/2016');
            expect(pipe.transform(leadingZeroesDate, 'date')).toBe('2/1/2016');
        });

    });

    describe('format "time"', () => {

        it('transforms "en-US" time correctly', () => {
            service.setLanguage('en');
            navigator.mockUserAgentLanguages('en-US', 'en');

            expect(pipe.transform(newYears2016, 'time')).toBe('11:59 PM');
            expect(pipe.transform(fiveBeforeTwelve, 'time')).toBe('11:55 AM');
            expect(pipe.transform(leadingZeroesDate, 'time')).toBe('3:04 AM');
        });

        it('transforms "en-GB" time correctly', () => {
            service.setLanguage('en');
            navigator.mockUserAgentLanguages('en-GB', 'en');

            expect(pipe.transform(newYears2016, 'time')).toBe('11:59 PM');
            expect(pipe.transform(fiveBeforeTwelve, 'time')).toBe('11:55 AM');
            expect(pipe.transform(leadingZeroesDate, 'time')).toBe('3:04 AM');
        });

        it('transforms "de" time correctly', () => {
            service.setLanguage('de');
            navigator.mockUserAgentLanguages('de');

            expect(pipe.transform(newYears2016, 'time')).toBe('23:59');
            expect(pipe.transform(fiveBeforeTwelve, 'time')).toBe('11:55');
            expect(pipe.transform(leadingZeroesDate, 'time')).toBe('03:04');
        });

        it('transforms "de" time correctly when User-Agent reports ["de-AT", "de"]', () => {
            service.setLanguage('de');
            navigator.mockUserAgentLanguages('de-AT', 'de');

            expect(pipe.transform(newYears2016, 'time')).toBe('23:59');
            expect(pipe.transform(fiveBeforeTwelve, 'time')).toBe('11:55');
            expect(pipe.transform(leadingZeroesDate, 'time')).toBe('03:04');
        });

        it('transforms "de" time correctly when User-Agent reports ["de-DE", "de"]', () => {
            service.setLanguage('de');
            navigator.mockUserAgentLanguages('de-DE', 'de');

            expect(pipe.transform(newYears2016, 'time')).toBe('23:59');
            expect(pipe.transform(fiveBeforeTwelve, 'time')).toBe('11:55');
            expect(pipe.transform(leadingZeroesDate, 'time')).toBe('03:04');
        });

        it('transforms "de" time correctly when User-Agent prefers "en"', () => {
            service.setLanguage('de');
            navigator.mockUserAgentLanguages('en-GB', 'en-US', 'en', 'de');

            expect(pipe.transform(newYears2016, 'time')).toBe('23:59');
            expect(pipe.transform(fiveBeforeTwelve, 'time')).toBe('11:55');
            expect(pipe.transform(leadingZeroesDate, 'time')).toBe('03:04');
        });

        it('transforms "en" time correctly when User-Agent reports "de", "en-GB"', () => {
            service.setLanguage('en');
            navigator.mockUserAgentLanguages('de', 'en-GB', 'en-US', 'en');

            expect(pipe.transform(newYears2016, 'time')).toBe('11:59 PM');
            expect(pipe.transform(fiveBeforeTwelve, 'time')).toBe('11:55 AM');
            expect(pipe.transform(leadingZeroesDate, 'time')).toBe('3:04 AM');
        });

    });

    describe('format "dateTime"', () => {

        it('transforms "en-US" dateTime correctly', () => {
            service.setLanguage('en');
            navigator.mockUserAgentLanguages('en-US', 'en');

            expect(pipe.transform(newYears2016, 'dateTime')).toBe('12/31/2016, 11:59 PM');
            expect(pipe.transform(fiveBeforeTwelve, 'dateTime')).toBe('10/1/2016, 11:55 AM');
            expect(pipe.transform(leadingZeroesDate, 'dateTime')).toBe('2/1/2016, 3:04 AM');
        });

        it('transforms "en-GB" dateTime correctly', () => {
            service.setLanguage('en');
            navigator.mockUserAgentLanguages('en-GB', 'en');

            expect(pipe.transform(newYears2016, 'dateTime')).toBe('12/31/2016, 11:59 PM');
            expect(pipe.transform(fiveBeforeTwelve, 'dateTime')).toBe('10/1/2016, 11:55 AM');
            expect(pipe.transform(leadingZeroesDate, 'dateTime')).toBe('2/1/2016, 3:04 AM');
        });

        it('transforms "de" dateTime correctly', () => {
            service.setLanguage('de');
            navigator.mockUserAgentLanguages('de');

            expect(pipe.transform(newYears2016, 'dateTime')).toBe('31.12.2016, 23:59');
            expect(pipe.transform(fiveBeforeTwelve, 'dateTime')).toBe('1.10.2016, 11:55');
            expect(pipe.transform(leadingZeroesDate, 'dateTime')).toBe('1.2.2016, 03:04');
        });

        it('transforms "de" dateTime correctly when User-Agent reports ["de-AT", "de"]', () => {
            service.setLanguage('de');
            navigator.mockUserAgentLanguages('de-AT', 'de');

            expect(pipe.transform(newYears2016, 'dateTime')).toBe('31.12.2016, 23:59');
            expect(pipe.transform(fiveBeforeTwelve, 'dateTime')).toBe('1.10.2016, 11:55');
            expect(pipe.transform(leadingZeroesDate, 'dateTime')).toBe('1.2.2016, 03:04');
        });

        it('transforms "de" dateTime correctly when User-Agent reports ["de-DE", "de"]', () => {
            service.setLanguage('de');
            navigator.mockUserAgentLanguages('de-DE', 'de');

            expect(pipe.transform(newYears2016, 'dateTime')).toBe('31.12.2016, 23:59');
            expect(pipe.transform(fiveBeforeTwelve, 'dateTime')).toBe('1.10.2016, 11:55');
            expect(pipe.transform(leadingZeroesDate, 'dateTime')).toBe('1.2.2016, 03:04');
        });

        it('transforms "de" dateTime correctly when User-Agent prefers "en"', () => {
            service.setLanguage('de');
            navigator.mockUserAgentLanguages('en-GB', 'en-US', 'en', 'de');

            expect(pipe.transform(newYears2016, 'dateTime')).toBe('31.12.2016, 23:59');
            expect(pipe.transform(fiveBeforeTwelve, 'dateTime')).toBe('1.10.2016, 11:55');
            expect(pipe.transform(leadingZeroesDate, 'dateTime')).toBe('1.2.2016, 03:04');
        });

        it('transforms "en" dateTime correctly when User-Agent reports "de", "en-GB"', () => {
            service.setLanguage('en');
            navigator.mockUserAgentLanguages('de', 'en-GB', 'en-US', 'en');

            expect(pipe.transform(newYears2016, 'dateTime')).toBe('12/31/2016, 11:59 PM');
            expect(pipe.transform(fiveBeforeTwelve, 'dateTime')).toBe('10/1/2016, 11:55 AM');
            expect(pipe.transform(leadingZeroesDate, 'dateTime')).toBe('2/1/2016, 3:04 AM');
        });

    });

    describe('format "longTime"', () => {

        it('transforms "en-US" longTime correctly', () => {
            service.setLanguage('en');
            navigator.mockUserAgentLanguages('en-US', 'en');

            expect(pipe.transform(newYears2016, 'longTime')).toBe('11:59:59 PM');
            expect(pipe.transform(fiveBeforeTwelve, 'longTime')).toBe('11:55:00 AM');
            expect(pipe.transform(leadingZeroesDate, 'longTime')).toBe('3:04:05 AM');
        });

        it('transforms "en-GB" longTime correctly', () => {
            service.setLanguage('en');
            navigator.mockUserAgentLanguages('en-GB', 'en');

            expect(pipe.transform(newYears2016, 'longTime')).toBe('11:59:59 PM');
            expect(pipe.transform(fiveBeforeTwelve, 'longTime')).toBe('11:55:00 AM');
            expect(pipe.transform(leadingZeroesDate, 'longTime')).toBe('3:04:05 AM');
        });

        it('transforms "de" longTime correctly', () => {
            service.setLanguage('de');
            navigator.mockUserAgentLanguages('de');

            expect(pipe.transform(newYears2016, 'longTime')).toBe('23:59:59');
            expect(pipe.transform(fiveBeforeTwelve, 'longTime')).toBe('11:55:00');
            expect(pipe.transform(leadingZeroesDate, 'longTime')).toBe('03:04:05');
        });

        it('transforms "de" longTime correctly when User-Agent reports ["de-AT", "de"]', () => {
            service.setLanguage('de');
            navigator.mockUserAgentLanguages('de-AT', 'de');

            expect(pipe.transform(newYears2016, 'longTime')).toBe('23:59:59');
            expect(pipe.transform(fiveBeforeTwelve, 'longTime')).toBe('11:55:00');
            expect(pipe.transform(leadingZeroesDate, 'longTime')).toBe('03:04:05');
        });

        it('transforms "de" longTime correctly when User-Agent reports ["de-DE", "de"]', () => {
            service.setLanguage('de');
            navigator.mockUserAgentLanguages('de-DE', 'de');

            expect(pipe.transform(newYears2016, 'longTime')).toBe('23:59:59');
            expect(pipe.transform(fiveBeforeTwelve, 'longTime')).toBe('11:55:00');
            expect(pipe.transform(leadingZeroesDate, 'longTime')).toBe('03:04:05');
        });

        it('transforms "de" longTime correctly when User-Agent prefers "en"', () => {
            service.setLanguage('en');
            navigator.mockUserAgentLanguages('en-GB', 'en-US', 'en', 'de');

            expect(pipe.transform(newYears2016, 'longTime')).toBe('11:59:59 PM');
            expect(pipe.transform(fiveBeforeTwelve, 'longTime')).toBe('11:55:00 AM');
            expect(pipe.transform(leadingZeroesDate, 'longTime')).toBe('3:04:05 AM');
        });

        it('transforms "en" longTime correctly when User-Agent reports "de", "en-GB"', () => {
            service.setLanguage('de');
            navigator.mockUserAgentLanguages('de', 'en-GB', 'en-US', 'en');

            expect(pipe.transform(newYears2016, 'longTime')).toBe('23:59:59');
            expect(pipe.transform(fiveBeforeTwelve, 'longTime')).toBe('11:55:00');
            expect(pipe.transform(leadingZeroesDate, 'longTime')).toBe('03:04:05');
        });

    });

});
