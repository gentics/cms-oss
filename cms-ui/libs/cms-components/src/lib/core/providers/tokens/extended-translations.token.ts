import { InjectionToken } from '@angular/core';

/**
 * Angular InjectionToken<JSON> to provide translations data from child to parent module.
 *
 * Provide InjectionToken in your child module like the following and import to your parent module calling `MyLibraryModule.forRoot()`.
 * @example
 * export class MyLibraryModule {
 *    static forRoot(): ModuleWithProviders<MyLibraryModule> {
 *        return {
 *            ngModule: MyLibraryModule,
 *            providers: [
 *                // provide this module's translations to the i18n solution in parent module
 *                { provide: GTX_TOKEN_EXTENDED_TRANSLATIONS, useValue: MODULE_TRANSLATIONS, multi: true },
 *            ],
 *        }
 *    }
 * }
 */
export const GTX_TOKEN_EXTENDED_TRANSLATIONS = new InjectionToken<JSON>('GtxExtendedTranslations');
