import { act, render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { createRef } from 'react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import { UiProvider } from '@/components/ui/provider';

import { Composer, type ComposerHandle } from './Composer';

import '@/i18n';

// Stands in for the browser's recognizer: the test plays the results and the end of listening.
class FakeRecognizer {
    static last: FakeRecognizer | undefined;
    lang = '';
    interimResults = false;
    continuous = true;
    onresult: ((event: unknown) => void) | null = null;
    onend: (() => void) | null = null;
    start = vi.fn();
    stop = vi.fn(() => this.onend?.());
    abort = vi.fn(() => this.onend?.());

    constructor() {
        FakeRecognizer.last = this;
    }

    say(transcript: string) {
        this.onresult?.({ results: [[{ transcript }]] });
    }
}

function renderComposer(isSubmitting = false) {
    const onSubmit = vi.fn();
    const ref = createRef<ComposerHandle>();
    // UiProvider: tooltips and toasts.
    const view = render(<Composer variant="start" onSubmit={onSubmit} isSubmitting={isSubmitting} ref={ref} />, { wrapper: UiProvider });

    return { ...view, onSubmit, ref, field: screen.getByRole('textbox', { name: 'What would you like to do?' }) };
}

// Selects the characters `start` … `start + length` of the field's first text node.
function select(field: HTMLElement, start: number, length: number) {
    const range = document.createRange();

    range.setStart(field.firstChild!, start);
    range.setEnd(field.firstChild!, start + length);
    document.getSelection()!.removeAllRanges();
    document.getSelection()!.addRange(range);
}

describe('Composer', () => {
    beforeEach(() => {
        FakeRecognizer.last = undefined;
    });

    afterEach(() => {
        vi.unstubAllGlobals();
        vi.useRealTimers();
    });

    it('submits the typed text as a text part on Enter', async () => {
        const user = userEvent.setup();
        const { field, onSubmit } = renderComposer();

        await user.click(field);
        await user.keyboard('Which pages are offline?{Enter}');

        expect(onSubmit).toHaveBeenCalledWith({ parts: [{ type: 'text', text: 'Which pages are offline?' }], files: [] });
    });

    it('does not submit on Shift+Enter', async () => {
        const user = userEvent.setup();
        const { field, onSubmit } = renderComposer();

        await user.click(field);
        await user.keyboard('First line{Shift>}{Enter}{/Shift}');

        expect(onSubmit).not.toHaveBeenCalled();
    });

    it('disables send while the field is empty and while starting', async () => {
        const user = userEvent.setup();
        const { field, rerender, onSubmit, ref } = renderComposer();
        const send = screen.getByRole('button', { name: 'Start a session' });

        expect(send).toBeDisabled();

        await user.click(field);
        await user.keyboard('Hello');

        expect(send).toBeEnabled();

        rerender(<Composer variant="start" onSubmit={onSubmit} isSubmitting ref={ref} />);

        expect(send).toBeDisabled();

        await user.keyboard('{Enter}');

        expect(onSubmit).not.toHaveBeenCalled();
    });

    it('submits on the send button', async () => {
        const user = userEvent.setup();
        const { field, onSubmit } = renderComposer();

        await user.click(field);
        await user.keyboard('Hello');
        await user.click(screen.getByRole('button', { name: 'Start a session' }));

        expect(onSubmit).toHaveBeenCalledWith({ parts: [{ type: 'text', text: 'Hello' }], files: [] });
    });

    it('setText fills the field, as a starter suggestion does', async () => {
        const user = userEvent.setup();
        const { field, ref, onSubmit } = renderComposer();

        act(() => ref.current!.setText('Create a new landing page'));

        expect(field).toHaveTextContent('Create a new landing page');

        await user.click(screen.getByRole('button', { name: 'Start a session' }));

        expect(onSubmit).toHaveBeenCalledWith({ parts: [{ type: 'text', text: 'Create a new landing page' }], files: [] });
    });

    describe('verbatim', () => {
        it('locks the selected passage and submits it as a verbatim part', async () => {
            const user = userEvent.setup();
            const { field, ref, onSubmit } = renderComposer();

            act(() => ref.current!.setText('Use this title: Review now'));
            select(field, 16, 10);
            await user.click(screen.getByRole('button', { name: 'Verbatim' }));
            await user.click(screen.getByRole('button', { name: 'Start a session' }));

            expect(onSubmit).toHaveBeenCalledWith({
                parts: [
                    { type: 'text', text: 'Use this title: ' },
                    { type: 'verbatim', text: 'Review now', source: 'user' },
                ],
                files: [],
            });
        });

        it('asks for a selection first when nothing is selected', async () => {
            const user = userEvent.setup();

            renderComposer();

            await user.click(screen.getByRole('button', { name: 'Verbatim' }));

            expect(await screen.findByText('Select the passage that should stay unchanged first.')).toBeInTheDocument();
        });

        it('turns a passage back into text with its remove button', async () => {
            const user = userEvent.setup();
            const { field, ref, onSubmit } = renderComposer();

            act(() => ref.current!.setText('Keep this'));
            select(field, 5, 4);
            await user.click(screen.getByRole('button', { name: 'Verbatim' }));
            await user.click(screen.getByRole('button', { name: 'Remove verbatim' }));
            await user.click(screen.getByRole('button', { name: 'Start a session' }));

            expect(onSubmit).toHaveBeenCalledWith({ parts: [{ type: 'text', text: 'Keep this' }], files: [] });
        });
    });

    describe('files', () => {
        it('attaches files as a source, switches one to verbatim and submits both', async () => {
            const user = userEvent.setup();
            const { onSubmit } = renderComposer();
            const brief = new File(['a'], 'brief.pdf', { type: 'application/pdf' });
            const notes = new File(['b'], 'notes.txt', { type: 'text/plain' });

            await user.upload(screen.getByLabelText('File'), [brief, notes]);

            expect(screen.getAllByText('as a source')).toHaveLength(2);

            await user.click(screen.getByRole('button', { name: 'verbatim / as a source: brief.pdf' }));

            expect(screen.getByText('verbatim')).toBeInTheDocument();

            // A start with files and no text is allowed.
            await user.click(screen.getByRole('button', { name: 'Start a session' }));

            expect(onSubmit).toHaveBeenCalledWith({
                parts: [],
                files: [{ file: brief, mode: 'verbatim' }, { file: notes, mode: 'source' }],
            });
        });

        it('removes an attached file', async () => {
            const user = userEvent.setup();

            renderComposer();

            await user.upload(screen.getByLabelText('File'), new File(['a'], 'brief.pdf'));
            await user.click(screen.getByRole('button', { name: 'Remove: brief.pdf' }));

            expect(screen.queryByText('brief.pdf')).not.toBeInTheDocument();
            expect(screen.getByRole('button', { name: 'Start a session' })).toBeDisabled();
        });
    });

    describe('voice', () => {
        it('shows a disabled mic button without speech recognition, saying why', async () => {
            const user = userEvent.setup();

            renderComposer();

            const mic = screen.getByRole('button', { name: 'Voice input is not available in this browser' });

            expect(mic).toHaveAttribute('aria-disabled', 'true');

            await user.click(mic);

            expect(screen.queryByText('Listening')).not.toBeInTheDocument();
        });

        it('dictates into the field and leaves sending to the user', async () => {
            vi.stubGlobal('SpeechRecognition', FakeRecognizer);

            const user = userEvent.setup();
            const { field, onSubmit } = renderComposer();

            await user.click(screen.getByRole('button', { name: 'Dictate' }));

            expect(screen.getByText('Listening')).toBeInTheDocument();
            expect(FakeRecognizer.last?.lang).toBe('en');

            act(() => FakeRecognizer.last!.say('Shorten the intro'));

            expect(field).toHaveTextContent('Shorten the intro');

            await user.click(screen.getByRole('button', { name: 'Stop dictating' }));

            expect(screen.queryByText('Listening')).not.toBeInTheDocument();
            expect(field).toHaveTextContent('Shorten the intro');
            expect(onSubmit).not.toHaveBeenCalled();

            field.focus();
            await user.keyboard('{Enter}');

            expect(onSubmit).toHaveBeenCalledWith({ parts: [{ type: 'text', text: 'Shorten the intro' }], files: [] });
        });

        // The big mic shows only up to 640 px; jsdom applies no media query, so it counts as hidden
        // here and is queried with `hidden: true`. Its visibility is tested in e2e/DashboardMobile.spec.ts.
        describe('voice first (mobile)', () => {
            it('offers the big mic and "Type instead" only with speech recognition', () => {
                renderComposer();

                expect(screen.queryByRole('button', { name: 'Tap to speak', hidden: true })).not.toBeInTheDocument();
                expect(screen.queryByRole('button', { name: 'Type instead', hidden: true })).not.toBeInTheDocument();
            });

            it('starts listening from the big mic and stops it again', async () => {
                vi.stubGlobal('SpeechRecognition', FakeRecognizer);

                const user = userEvent.setup();

                renderComposer();

                await user.click(screen.getByRole('button', { name: 'Tap to speak', hidden: true }));

                expect(FakeRecognizer.last?.start).toHaveBeenCalledTimes(1);
                expect(screen.getByText('Listening')).toBeInTheDocument();
                // While dictating, the text prompt is open for the words, so "Type instead" is gone.
                expect(screen.queryByRole('button', { name: 'Type instead', hidden: true })).not.toBeInTheDocument();

                await user.click(screen.getByRole('button', { name: 'Tap to stop', hidden: true }));

                expect(FakeRecognizer.last?.stop).toHaveBeenCalledTimes(1);
            });

            it('opens the text prompt with "Type instead" and focuses the field', async () => {
                vi.stubGlobal('SpeechRecognition', FakeRecognizer);

                const user = userEvent.setup();
                const { field } = renderComposer();

                await user.click(screen.getByRole('button', { name: 'Type instead', hidden: true }));

                expect(field).toHaveFocus();
                expect(screen.queryByRole('button', { name: 'Type instead', hidden: true })).not.toBeInTheDocument();
                expect(screen.getByRole('button', { name: 'Tap to speak', hidden: true })).toBeInTheDocument();
            });

            it('opens the text prompt when a starter fills it', () => {
                vi.stubGlobal('SpeechRecognition', FakeRecognizer);

                const { ref, field } = renderComposer();

                act(() => ref.current!.setText('Create a new landing page'));

                expect(field).toHaveFocus();
                expect(screen.queryByRole('button', { name: 'Type instead', hidden: true })).not.toBeInTheDocument();
            });
        });

        it('throws the take away on Escape while listening', async () => {
            vi.stubGlobal('SpeechRecognition', FakeRecognizer);

            const user = userEvent.setup();
            const { field, onSubmit } = renderComposer();

            await user.click(screen.getByRole('button', { name: 'Dictate' }));
            act(() => FakeRecognizer.last!.say('Discard this'));
            field.focus();
            await user.keyboard('{Escape}');

            expect(FakeRecognizer.last?.abort).toHaveBeenCalled();
            expect(field).toHaveTextContent('');
            expect(onSubmit).not.toHaveBeenCalled();
        });
    });
});
