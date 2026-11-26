import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';

import { RefusalNote } from './RefusalNote';

import '@/i18n';

describe('RefusalNote', () => {
    it('says why an edit could not be saved directly, and that the chat can have it changed', () => {
        render(<RefusalNote reason="pageTarget" />);

        expect(screen.getByRole('alert')).toHaveTextContent('Only a link tag can point to a CMS page. Reference it in the chat to have it changed there.');
    });
});
