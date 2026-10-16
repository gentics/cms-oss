import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';

import { ThinkingIndicator } from './ThinkingIndicator';

import '@/i18n';

describe('ThinkingIndicator', () => {
    it('announces the latest status of the run', () => {
        render(<ThinkingIndicator text='Reading page "Garantiebedingungen"' />);

        expect(screen.getByRole('status')).toHaveTextContent('Reading page "Garantiebedingungen"');
    });

    it('says "Thinking" without a status', () => {
        render(<ThinkingIndicator />);

        expect(screen.getByRole('status')).toHaveTextContent('Thinking');
    });
});
