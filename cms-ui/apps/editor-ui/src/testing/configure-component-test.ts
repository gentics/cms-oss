import { NO_ERRORS_SCHEMA } from '@angular/core';
import { TestBed, TestModuleMetadata, getTestBed } from '@angular/core/testing';
import { I18nService } from '@gentics/cms-components';
import { MockI18nPipe, MockI18nService } from '@gentics/cms-components/testing';
import { NgxsModule } from '@ngxs/store';
import { STATE_MODULES } from '../app/state';
import { mockPipes } from '@gentics/ui-core/testing';

/**
 * Merge two arrays and remove duplicate items.
 */
function mergeUnique(a: any[], b: any[]): any[] {
    const arr1 = a instanceof Array ? a : [];
    const arr2 = b instanceof Array ? b : [];
    return arr1.concat(arr2.filter((item) => arr1.indexOf(item) < 0));
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
        declarations: [
            MockI18nPipe,
            ...mockPipes('gtxI18nDate'),
        ],
        providers: [
            { provide: I18nService, useClass: MockI18nService },
        ],
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
