import { render, screen } from '@testing-library/react';
import { createRef } from 'react';
import { describe, expect, it } from 'vitest';

import { ComposerTextInput } from './ComposerTextInput';

describe('ComposerTextInput', () => {
    it('is a named, editable multi-line textbox with its label as placeholder', () => {
        const ref = createRef<HTMLDivElement>();

        render(<ComposerTextInput ref={ref} label="What should happen?" isEmpty size="base" />);

        const field = screen.getByRole('textbox', { name: 'What should happen?' });

        expect(field).toBe(ref.current);
        expect(field).toHaveAttribute('contenteditable', 'true');
        expect(field).toHaveAttribute('aria-multiline', 'true');
        expect(field).toHaveAttribute('data-placeholder', 'What should happen?');
    });
});
