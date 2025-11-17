import { getTestBed, TestModuleMetadata } from '@angular/core/testing';
import { GenticsUICoreModule } from '@gentics/ui-core';
import { mockPipe } from '@gentics/ui-core/testing';

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
export function configureComponentTest(config: TestModuleMetadata): void {
    const testBed = getTestBed();
    const defaultConfig: TestModuleMetadata = {
        imports: [GenticsUICoreModule.forRoot()],
        declarations: [mockPipe('translate')],
        providers: [],
    };

    const mergedConfig: TestModuleMetadata = {
        imports: mergeUnique(defaultConfig.imports, config.imports),
        declarations: mergeUnique(defaultConfig.declarations, config.declarations),
        providers: mergeUnique(defaultConfig.providers, config.providers),
        schemas: mergeUnique(defaultConfig.schemas, config.schemas),
    };
    testBed.configureTestingModule(mergedConfig);
}
