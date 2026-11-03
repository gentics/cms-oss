import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';

import type { TextPart } from '@/services/apiService/genaix/types';

import { TextPartView } from './TextPartView';

import '@/i18n';

const markdown = (text: string): TextPart => ({ type: 'text', format: 'markdown', text });

describe('TextPartView', () => {
    it('renders Markdown as formatted elements', () => {
        const { container } = render(
            <TextPartView part={markdown('The **three** changes:\n\n- Shipping from `50 €`\n- Withdrawal in 14 days')} sessionId="s-1" />,
        );

        expect(container.querySelector('strong')).toHaveTextContent('three');
        expect(container.querySelector('code')).toHaveTextContent('50 €');
        expect(screen.getAllByRole('listitem').map((item) => item.textContent)).toEqual(['Shipping from 50 €', 'Withdrawal in 14 days']);
    });

    it('renders no raw HTML from the model', () => {
        const { container } = render(<TextPartView part={markdown('Hello <img src=x onerror="alert(1)"> world')} sessionId="s-1" />);

        expect(container.querySelector('img')).toBeNull();
    });

    it('shows the text as it grows while streaming, without a cursor', () => {
        const { container, rerender } = render(<TextPartView part={markdown('Ich habe')} sessionId="s-1" />);

        expect(screen.getByText('Ich habe')).toBeInTheDocument();
        expect(container.querySelector('[aria-hidden]')).toBeNull();

        rerender(<TextPartView part={markdown('Ich habe die Aktualisierung gelesen.')} sessionId="s-1" />);
        expect(screen.getByText('Ich habe die Aktualisierung gelesen.')).toBeInTheDocument();
        expect(container.querySelector('[aria-hidden]')).toBeNull();
    });

    it('keeps plain text as typed, without Markdown', () => {
        const { container } = render(<TextPartView part={{ type: 'text', format: 'plain', text: 'Not **bold**' }} sessionId="s-1" />);

        expect(container.querySelector('strong')).toBeNull();
        expect(screen.getByText('Not **bold**')).toBeInTheDocument();
    });
});
