import { NO_ERRORS_SCHEMA, Pipe, PipeTransform } from '@angular/core';
import { TestBed, TestModuleMetadata, getTestBed } from '@angular/core/testing';
import { TypeIconPipe } from '@editor-ui/app/shared/pipes';
import { STATE_MODULES } from '@editor-ui/app/state';
import { TranslateService } from '@ngx-translate/core';
import { NgxsModule } from '@ngxs/store';
import { Observable, of } from 'rxjs';

/**
 * Merge two arrays and remove duplicate items.
 */
function mergeUnique(a: any[], b: any[]): any[] {
    const arr1 = a instanceof Array ? a : [];
    const arr2 = b instanceof Array ? b : [];
    return arr1.concat(arr2.filter(item => arr1.indexOf(item) < 0));
}

@Pipe({
    name: 'i18n',
})
class MockI18nPipe implements PipeTransform {
    transform(): void {
    }
}

export class MockTranslateService {
    onTranslationChange = of({});
    onLangChange = of({});
    get(): Observable<string> { return of('mocked i18n string'); }
}

/**
 * Wraps the TestBed.configureTestingModule() and provides a mocked implementation of the i18n pipe/service, which
 * is used it virtually every component.
 *
 * For tests which are testing non-component functionality (e.g. reducer tests), this function is
 * not needed.
 */
export function configureComponentTest(config: TestModuleMetadata): TestBed {
    const testBed = getTestBed();
    const defaultConfig: TestModuleMetadata = {
        imports: [NgxsModule.forRoot(STATE_MODULES)],
        declarations: [MockI18nPipe],
        providers: [{ provide: TranslateService, useClass: MockTranslateService }],
        schemas: [NO_ERRORS_SCHEMA],
    };

    const mergedConfig: TestModuleMetadata = {
        imports: mergeUnique(defaultConfig.imports, config.imports),
        declarations: mergeUnique(defaultConfig.declarations, config.declarations),
        providers: mergeUnique(defaultConfig.providers, config.providers),
        schemas: mergeUnique(defaultConfig.schemas, config.schemas),
    };
    testBed.configureTestingModule(mergedConfig);

    return testBed;
}
