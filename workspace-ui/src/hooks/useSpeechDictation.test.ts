import { act, renderHook } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import { useSpeechDictation } from './useSpeechDictation';

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

    // Results as the API delivers them: a list of results, each a list of alternatives.
    say(...transcripts: string[]) {
        const results = transcripts.map((transcript) => [{ transcript }]);

        this.onresult?.({ results });
    }
}

function renderDictation() {
    const onText = vi.fn();
    const onEnd = vi.fn();
    const hook = renderHook(() => useSpeechDictation({ onText, onEnd }));

    return { ...hook, onText, onEnd };
}

describe('useSpeechDictation', () => {
    beforeEach(() => {
        FakeRecognizer.last = undefined;
    });

    afterEach(() => {
        vi.unstubAllGlobals();
        vi.useRealTimers();
    });

    it('is not supported without a recognizer', () => {
        const { result } = renderDictation();

        expect(result.current.isSupported).toBe(false);
    });

    it('uses the webkit-prefixed recognizer with the given language and interim results', () => {
        vi.stubGlobal('webkitSpeechRecognition', FakeRecognizer);

        const { result } = renderDictation();

        expect(result.current.isSupported).toBe(true);

        act(() => result.current.start('de'));

        expect(result.current.isListening).toBe(true);
        expect(FakeRecognizer.last?.start).toHaveBeenCalledTimes(1);
        expect(FakeRecognizer.last).toMatchObject({ lang: 'de', interimResults: true, continuous: false });
    });

    it('reports the growing transcript, then the final one when listening ends', () => {
        vi.stubGlobal('SpeechRecognition', FakeRecognizer);

        const { result, onText, onEnd } = renderDictation();

        act(() => result.current.start('en'));
        act(() => FakeRecognizer.last!.say('Shorten the intro'));
        act(() => FakeRecognizer.last!.say('Shorten the intro', ' to two sentences'));

        expect(onText).toHaveBeenLastCalledWith('Shorten the intro to two sentences');

        act(() => result.current.stop());

        expect(result.current.isListening).toBe(false);
        expect(onEnd).toHaveBeenCalledWith('Shorten the intro to two sentences');
    });

    it('does not report the end after cancel', () => {
        vi.stubGlobal('SpeechRecognition', FakeRecognizer);

        const { result, onEnd } = renderDictation();

        act(() => result.current.start('en'));
        act(() => FakeRecognizer.last!.say('Discard this'));
        act(() => result.current.cancel());

        expect(FakeRecognizer.last?.abort).toHaveBeenCalledTimes(1);
        expect(result.current.isListening).toBe(false);
        expect(onEnd).not.toHaveBeenCalled();
    });

    it('counts the seconds while listening', () => {
        vi.useFakeTimers();
        vi.stubGlobal('SpeechRecognition', FakeRecognizer);

        const { result } = renderDictation();

        act(() => result.current.start('en'));
        act(() => vi.advanceTimersByTime(3000));

        expect(result.current.seconds).toBe(3);
    });

    it('aborts an ongoing take on unmount', () => {
        vi.stubGlobal('SpeechRecognition', FakeRecognizer);

        const { result, unmount, onEnd } = renderDictation();

        act(() => result.current.start('en'));
        unmount();

        expect(FakeRecognizer.last?.abort).toHaveBeenCalledTimes(1);
        expect(onEnd).not.toHaveBeenCalled();
    });
});
