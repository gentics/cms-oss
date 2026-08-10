import { FormElement, FormElementConfiguration, FormSettingConfiguration, FormSettingType } from '@gentics/cms-models';
import { isSettingVisible } from './conditions';

it('simple single condition', () => {
    const SETTING: FormSettingConfiguration = {
        id: 'foobar',
        type: FormSettingType.BOOLEAN,
        labelI18n: {
            en: 'foobar',
        },
        condition: {
            source: {
                setting: 'other',
            },
            equals: true,
        },
    };
    const CONFIG: FormElementConfiguration = {
        labelI18n: {
            en: 'dummy config',
        },
        settings: [
            SETTING,
            {
                id: 'other',
                type: FormSettingType.BOOLEAN,
                labelI18n: {
                    en: 'other',
                },
            },
        ],
    };
    const ELEMENT: FormElement = {
        id: 'something',
        label: {
            en: 'dummy element',
        },
        type: 'property',
        uiSchemaPage: 0,
        formGridOptions: {
            type: 'dummy',
            other: true,
        },
    };

    expect(isSettingVisible(SETTING, CONFIG, ELEMENT)).toBe(true);

    expect(isSettingVisible(SETTING, CONFIG, {
        ...ELEMENT,
        formGridOptions: {
            type: 'dummy',
            other: false,
        },
    })).toBe(false);
});
