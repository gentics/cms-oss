import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';

import { UiProvider } from '@/components/ui/provider';

import { AgentSender, ChatMessage } from './ChatMessage';

import '@/i18n';

describe('ChatMessage', () => {
    it('shows an assistant message with its parts through the part registry', () => {
        render(
            <ChatMessage
                role="assistant"
                sessionId="s-1"
                parts={[
                    { type: 'text', format: 'markdown', text: 'Found **two** pages.' },
                    undefined,
                    { type: 'table', label: 'Pages', columns: [{ key: 'page', label: 'Page', type: 'string' }], rows: [{ page: 'Garantiebedingungen' }] },
                ]}
            />,
            { wrapper: UiProvider },
        );

        const message = screen.getByRole('article', { name: 'Assistant' });

        expect(message).toHaveTextContent('Found two pages.');
        expect(screen.getByRole('region', { name: 'Pages' })).toBeInTheDocument();
    });

    it('shows a user turn with its text, verbatim passage, file and setting', () => {
        render(
            <ChatMessage
                role="user"
                sessionId="s-1"
                parts={[
                    { type: 'text', text: 'Create the page from ' },
                    { type: 'verbatim', text: 'Der Widerruf kann innerhalb von 14 Tagen erfolgen.', source: 'typed' },
                    { type: 'file_ref', file_id: 'f-1', mode: 'verbatim' },
                    { type: 'setting', key: 'template', value: '17', label: 'Kampagnen-Landingpage' },
                ]}
            />,
            { wrapper: UiProvider },
        );

        const message = screen.getByRole('article', { name: 'You' });

        expect(message).toHaveTextContent('Create the page from');
        expect(message).toHaveTextContent('Der Widerruf kann innerhalb von 14 Tagen erfolgen.');
        expect(message).toHaveTextContent('File, word for word');
        expect(message).toHaveTextContent('template: Kampagnen-Landingpage');
    });

    it('shows a file sent word for word as a verbatim token, a file sent as a source as a plain one', () => {
        const { container } = render(
            <ChatMessage
                role="user"
                sessionId="s-1"
                parts={[
                    { type: 'file_ref', file_id: 'f-1', mode: 'verbatim' },
                    { type: 'file_ref', file_id: 'f-2', mode: 'source' },
                ]}
            />,
            { wrapper: UiProvider },
        );

        const tokens = container.querySelectorAll('[data-tone="onSolid"]');

        expect(tokens).toHaveLength(2);
        expect(tokens[0]).toHaveAttribute('data-verbatim-token');
        expect(tokens[0]).toHaveTextContent('File, word for word');
        expect(tokens[1]).not.toHaveAttribute('data-verbatim-token');
    });

    it('shows a setting with the composer\'s settings icon (design.md §9 "Einstellungen")', () => {
        const { container } = render(
            <ChatMessage role="user" sessionId="s-1" parts={[{ type: 'setting', key: 'template', value: '17', label: 'Kampagnen-Landingpage' }]} />,
            { wrapper: UiProvider },
        );

        const token = container.querySelector('[data-tone="onSolid"]');

        expect(token).toHaveTextContent('template: Kampagnen-Landingpage');
        expect(token?.querySelector('svg')).toHaveClass('lucide-sliders-horizontal');
    });

    it('marks a streaming message as busy, without a cursor in its text', () => {
        const { container } = render(
            <ChatMessage
                role="assistant"
                sessionId="s-1"
                isStreaming
                parts={[{ type: 'text', format: 'plain', text: 'First.' }, { type: 'text', format: 'plain', text: 'Second' }]}
            />,
            { wrapper: UiProvider },
        );

        const paragraphs = container.querySelectorAll('p');

        expect(screen.getByRole('article', { name: 'Assistant' })).toHaveAttribute('aria-busy', 'true');
        expect(paragraphs[0]!.querySelector('[aria-hidden]')).toBeNull();
        expect(paragraphs[1]!.querySelector('[aria-hidden]')).toBeNull();
    });

    it('leaves out its avatar and name when the sender stands above it, and still names its sender', () => {
        render(
            <ChatMessage role="assistant" sessionId="s-1" hidesSender parts={[{ type: 'text', format: 'plain', text: 'Two pages are offline.' }]} />,
            { wrapper: UiProvider },
        );

        const message = screen.getByRole('article', { name: 'Assistant' });

        expect(message).toHaveTextContent(/^Two pages are offline\.$/);
        expect(message.querySelector('[aria-hidden]')).toBeNull();
    });

    it('says when a sent turn failed', () => {
        render(<ChatMessage role="user" sessionId="s-1" hasFailed parts={[{ type: 'text', text: 'Hello' }]} />, { wrapper: UiProvider });

        expect(screen.getByText('The message could not be sent')).toBeInTheDocument();
    });
});

describe('AgentSender', () => {
    it('shows the agent\'s avatar and name, hidden from assistive technology', () => {
        const { container } = render(<AgentSender />, { wrapper: UiProvider });

        expect(container.firstElementChild).toHaveAttribute('aria-hidden', 'true');
        expect(container).toHaveTextContent('Assistant');
    });
});
