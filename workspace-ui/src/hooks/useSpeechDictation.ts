import { useCallback, useEffect, useRef, useState } from 'react';

// The part of the Web Speech API recognizer this hook uses. TypeScript's DOM lib declares the
// events (`SpeechRecognitionEvent`) but not the recognizer itself.
interface SpeechRecognizer {
    lang: string;
    interimResults: boolean;
    continuous: boolean;
    onresult: ((event: SpeechRecognitionEvent) => void) | null;
    onend: (() => void) | null;
    start: () => void;
    stop: () => void;
    abort: () => void;
}

type SpeechRecognizerConstructor = new () => SpeechRecognizer;

// Chromium and Safari still ship the recognizer only with the `webkit` prefix.
function recognizerConstructor(): SpeechRecognizerConstructor | undefined {
    const speechWindow = window as Window & {
        SpeechRecognition?: SpeechRecognizerConstructor;
        webkitSpeechRecognition?: SpeechRecognizerConstructor;
    };

    return speechWindow.SpeechRecognition ?? speechWindow.webkitSpeechRecognition;
}

// The transcript so far: every result, final or interim, in order.
function transcriptOf(event: SpeechRecognitionEvent): string {
    let transcript = '';

    for (let i = 0; i < event.results.length; i++) {
        transcript += event.results[i]![0]!.transcript;
    }

    return transcript;
}

interface SpeechDictationOptions {
    /** The transcript so far, on every result while listening. */
    onText: (text: string) => void;
    /** Listening ended by a pause or by `stop()`, with the final transcript. Not called after `cancel()`. */
    onEnd: (text: string) => void;
}

/**
 * Dictation through the browser's speech recognition. One utterance per `start()`: a pause ends it,
 * as does `stop()`; `cancel()` throws the take away. `isSupported` is `false` where the browser has
 * no recognizer.
 */
export function useSpeechDictation({ onText, onEnd }: SpeechDictationOptions) {
    const [isListening, setIsListening] = useState(false);
    const [seconds, setSeconds] = useState(0);
    const recognizerRef = useRef<SpeechRecognizer | null>(null);
    const cancelledRef = useRef(false);
    const callbacksRef = useRef({ onText, onEnd });

    useEffect(() => {
        callbacksRef.current = { onText, onEnd };
    });

    const start = useCallback((lang: string) => {
        const Recognizer = recognizerConstructor();

        if (!Recognizer || recognizerRef.current) {
            return;
        }

        const recognizer = new Recognizer();
        let transcript = '';

        recognizer.lang = lang;
        recognizer.interimResults = true;
        recognizer.continuous = false;
        recognizer.onresult = (event) => {
            transcript = transcriptOf(event);
            callbacksRef.current.onText(transcript);
        };
        recognizer.onend = () => {
            recognizerRef.current = null;
            setIsListening(false);

            if (!cancelledRef.current) {
                callbacksRef.current.onEnd(transcript);
            }
        };
        cancelledRef.current = false;
        recognizerRef.current = recognizer;
        setSeconds(0);
        setIsListening(true);
        recognizer.start();
    }, []);

    const stop = useCallback(() => recognizerRef.current?.stop(), []);

    const cancel = useCallback(() => {
        cancelledRef.current = true;
        recognizerRef.current?.abort();
    }, []);

    // The clock next to the field while listening.
    useEffect(() => {
        if (!isListening) {
            return;
        }

        const timer = setInterval(() => setSeconds((value) => value + 1), 1000);

        return () => clearInterval(timer);
    }, [isListening]);

    // Leaving the page throws an ongoing take away.
    useEffect(() => cancel, [cancel]);

    return { isSupported: recognizerConstructor() !== undefined, isListening, seconds, start, stop, cancel };
}

/** The dictation state and controls `useSpeechDictation` returns, e.g. for `VoiceInput`. */
export type SpeechDictation = ReturnType<typeof useSpeechDictation>;
