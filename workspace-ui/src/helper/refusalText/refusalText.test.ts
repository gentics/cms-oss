import i18n from 'i18next';
import { beforeAll, describe, expect, it } from 'vitest';

import { refusalText } from './refusalText';

import '@/i18n';

beforeAll(async () => {
    await i18n.changeLanguage('en');
});

describe('refusalText', () => {
    it('names the reason, then that the chat can have it changed', () => {
        expect(refusalText(i18n.t, 'noEditable')).toBe('This spot cannot be saved directly in the CMS. Reference it in the chat to have it changed there.');
    });
});
