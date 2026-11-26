import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';

import { ReferenceToken } from './ReferenceToken';

describe('ReferenceToken', () => {
    it('names the part, its @ icon hidden from assistive technology', () => {
        const { container } = render(<ReferenceToken>Text · content_2 · Link</ReferenceToken>);

        expect(screen.getByText('Text · content_2 · Link')).toBeInTheDocument();
        expect(container.querySelector('svg')).toHaveAttribute('aria-hidden', 'true');
    });

    it('marks a verbatim token, which is dashed (design.md §12), and shows a trailing control inside it', () => {
        const { container } = render(
            <ReferenceToken verbatim action={<span data-testid="action">×</span>}>brief.pdf</ReferenceToken>,
        );
        const token = container.firstElementChild!;

        expect(token).toHaveAttribute('data-verbatim-token');
        expect(token).toContainElement(screen.getByTestId('action'));
    });

    it('takes the tone of the user\'s own message, and is no verbatim token by default', () => {
        const { container } = render(<ReferenceToken tone="onSolid">Campaigns</ReferenceToken>);
        const token = container.firstElementChild!;

        expect(token).toHaveAttribute('data-tone', 'onSolid');
        expect(token).not.toHaveAttribute('data-verbatim-token');
    });

    it('shows a passage whole: its text, not cut to a label, as a verbatim token', () => {
        const text = 'Der Widerruf kann innerhalb von 14 Tagen erfolgen, ohne Angabe von Gründen und ohne weitere Kosten.';
        const { container } = render(<ReferenceToken verbatim passage>{text}</ReferenceToken>);
        const token = container.firstElementChild!;

        expect(token).toHaveTextContent(text);
        expect(token).toHaveAttribute('data-verbatim-token');
        expect(token).toHaveAttribute('data-passage');
    });
});
