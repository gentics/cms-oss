import { act, render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import { UiProvider } from '@/components/ui/provider';
import { useSpeechDictation } from '@/hooks/useSpeechDictation';

import { VoiceInput } from './VoiceInput';

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

// A composer stand-in: the dictation writes into a text, and Enter on the field would send it.
function Harness({ size, onText, onCancel, onSend }: {
    size: 'sm' | 'md' | 'xl';
    onText: (text: string) => void;
    onCancel: () => void;
    onSend: () => void;
}) {
    const dictation = useSpeechDictation({ onText, onEnd: () => undefined });

    return (
        <>
            <input aria-label="Prompt" onKeyDown={(event) => event.key === 'Enter' && onSend()} />
            <VoiceInput dictation={dictation} onCancel={onCancel} size={size} />
        </>
    );
}

function renderVoice(size: 'sm' | 'md' | 'xl' = 'md') {
    const handlers = { onText: vi.fn(), onCancel: vi.fn(), onSend: vi.fn() };

    render(<Harness size={size} {...handlers} />, { wrapper: UiProvider });

    return handlers;
}

describe('VoiceInput', () => {
    beforeEach(() => {
        FakeRecognizer.last = undefined;
    });

    afterEach(() => {
        vi.unstubAllGlobals();
    });

    it('is visible but disabled without speech recognition, saying why', () => {
        renderVoice();

        expect(screen.getByRole('button', { name: 'Voice input is not available in this browser' })).toHaveAttribute('aria-disabled', 'true');
    });

    it('passes the spoken words on, shows that it listens and stops on a second tap', async () => {
        vi.stubGlobal('SpeechRecognition', FakeRecognizer);

        const user = userEvent.setup();
        const { onText, onSend } = renderVoice();

        await user.click(screen.getByRole('button', { name: 'Dictate' }));

        expect(screen.getByText('Listening')).toBeInTheDocument();
        expect(screen.getByText('0:00', { exact: false })).toBeInTheDocument();

        act(() => FakeRecognizer.last!.say('Shorten the intro'));

        expect(onText).toHaveBeenLastCalledWith('Shorten the intro');

        await user.click(screen.getByRole('button', { name: 'Stop dictating' }));

        expect(FakeRecognizer.last?.stop).toHaveBeenCalledTimes(1);
        expect(screen.queryByText('Listening')).not.toBeInTheDocument();
        expect(onSend).not.toHaveBeenCalled();
    });

    it('cancels with the Cancel button and with Esc', async () => {
        vi.stubGlobal('SpeechRecognition', FakeRecognizer);

        const user = userEvent.setup();
        const { onCancel } = renderVoice();

        await user.click(screen.getByRole('button', { name: 'Dictate' }));
        await user.click(screen.getByRole('button', { name: /Cancel/ }));

        expect(FakeRecognizer.last?.abort).toHaveBeenCalledTimes(1);
        expect(onCancel).toHaveBeenCalledTimes(1);

        await user.click(screen.getByRole('button', { name: 'Dictate' }));
        await user.keyboard('{Escape}');

        expect(onCancel).toHaveBeenCalledTimes(2);
    });

    it('takes Enter while listening to stop, so the prompt is not sent', async () => {
        vi.stubGlobal('SpeechRecognition', FakeRecognizer);

        const user = userEvent.setup();
        const { onSend } = renderVoice();

        await user.click(screen.getByRole('button', { name: 'Dictate' }));
        screen.getByRole('textbox', { name: 'Prompt' }).focus();
        await user.keyboard('{Enter}');

        expect(FakeRecognizer.last?.stop).toHaveBeenCalledTimes(1);
        expect(onSend).not.toHaveBeenCalled();

        // Once listening has ended, Enter reaches the field again.
        await user.keyboard('{Enter}');

        expect(onSend).toHaveBeenCalledTimes(1);
    });

    it('shows the big mic with its label in size xl', async () => {
        vi.stubGlobal('SpeechRecognition', FakeRecognizer);

        const user = userEvent.setup();

        renderVoice('xl');

        await user.click(screen.getByRole('button', { name: 'Tap to speak' }));

        expect(screen.getByRole('button', { name: 'Tap to stop' })).toHaveAttribute('aria-pressed', 'true');
    });
});
