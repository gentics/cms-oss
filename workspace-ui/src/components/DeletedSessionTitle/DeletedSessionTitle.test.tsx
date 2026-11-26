import { render } from '@testing-library/react';
import i18n from 'i18next';
import { beforeAll, describe, expect, it } from 'vitest';

import { DeletedSessionTitle } from './DeletedSessionTitle';

import '@/i18n';

describe('DeletedSessionTitle', () => {
    beforeAll(async () => {
        await i18n.changeLanguage('en');
    });

    it('names the session in an element of its own, between the quotes and before "deleted"', () => {
        const { container } = render(<DeletedSessionTitle title="Landing page" />);

        expect(container).toHaveTextContent('“Landing page” deleted');
        expect(container.querySelector('span span')).toHaveTextContent(/^Landing page$/);
    });

    it('shows a name with angle brackets as it is, not as markup', () => {
        const { container } = render(<DeletedSessionTitle title="Use <b>bold</b> <0>here</0>" />);

        expect(container).toHaveTextContent('“Use <b>bold</b> <0>here</0>” deleted');
        expect(container.querySelector('b')).toBeNull();
    });

    it('is German in German', async () => {
        await i18n.changeLanguage('de');

        const { container } = render(<DeletedSessionTitle title="Landingpage" />);

        expect(container).toHaveTextContent('„Landingpage“ gelöscht');
        await i18n.changeLanguage('en');
    });
});
