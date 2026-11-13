import * as DE_TRANSLATIONS from '../../../../public/i18n/de.json';
import * as EN_TRANSLATIONS from '../../../../public/i18n/en.json';

describe('Portal Implementation translations', () => {
    const translations: [string, any][] = [
        ['de', DE_TRANSLATIONS],
        ['en', EN_TRANSLATIONS],
    ];

    for (const [language, translation] of translations) {
        it(`contain the entity names used in the delete confirmation (${language})`, () => {
            const common = translation.common ?? translation.default?.common;

            // used by the ConfirmDeleteModal with the entity identifier "portalImplementation"
            expect(common.portalImplementation_singular).toBeTruthy();
            expect(common.portalImplementation_plural).toBeTruthy();
        });
    }
});
