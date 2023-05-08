import {TestBed} from '@angular/core/testing';
import {TranslateLoader, TranslateModule, TranslateService} from '@ngx-translate/core';

import {LocalTranslateLoader} from './local-translate-loader';

describe('LocalTranslateLoader and ngx-translate', () => {

    let translator: TranslateService;

    beforeEach(() => {
        TestBed.configureTestingModule({
            imports: [
                TranslateModule.forRoot({
                    loader: { provide: TranslateLoader, useClass: LocalTranslateLoader }
                })
            ]
        }).compileComponents();

        translator = TestBed.get(TranslateService);
    });

    it('loads and provides the English translations correctly', () => {
        translator.use('en');
        expect(translator.instant('common.cancel_button')).toEqual('Cancel');
        expect(translator.instant('dashboard.log_in')).toEqual('Log In');
    });

    it('loads and provides the German translations correctly', () => {
        translator.use('de');
        expect(translator.instant('common.cancel_button')).toEqual('Abbrechen');
        expect(translator.instant('dashboard.log_in')).toEqual('Anmelden');
    });

    it('allows switching the language', () => {
        translator.use('en');
        expect(translator.instant('common.cancel_button')).toEqual('Cancel');
        translator.use('de');
        expect(translator.instant('common.cancel_button')).toEqual('Abbrechen');
    });

});
