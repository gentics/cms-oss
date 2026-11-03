import { MicIcon, SquareIcon } from 'lucide-react';
import { type CSSProperties, useEffect, useRef } from 'react';
import { useTranslation } from 'react-i18next';

import { IconButton } from '@/components/IconButton/IconButton';
import { Button } from '@/components/ui/button';
import type { SpeechDictation } from '@/hooks/useSpeechDictation';

import styles from './VoiceInput.module.css';

// Heights of the wave bars while listening (draft `voiceStart`).
const WAVE_HEIGHTS = Array.from({ length: 12 }, (_, i) => 7 + Math.round(Math.abs(Math.sin(i * 1.9)) * 17));

function formatClock(seconds: number): string {
    return `${Math.floor(seconds / 60)}:${String(seconds % 60).padStart(2, '0')}`;
}

interface VoiceInputProps {
    /** From `useSpeechDictation`, owned by the composer; its `onText` writes the words into the prompt. */
    dictation: SpeechDictation;
    /** Cancel or Esc while listening: the composer throws the take away (empties the field). */
    onCancel: () => void;
    /**
     * `sm` chat (icon button), `md` dashboard (34 px, design.md §12), `xl` the big mic of both
     * composers on phones, with its label below.
     */
    size: 'sm' | 'md' | 'xl';
}

/**
 * Voice input of both composers: speak, and the words stream into the prompt; tap again or pause
 * to stop. Nothing is sent: the user sends with Enter or the send button. While listening, Esc
 * cancels and Enter only stops listening. Without speech recognition the mic stays, disabled,
 * and its tooltip says why.
 */
export function VoiceInput({ dictation, onCancel, size }: VoiceInputProps) {
    const { t, i18n } = useTranslation();
    const { isSupported, isListening, seconds, start, stop, cancel } = dictation;
    const onCancelRef = useRef(onCancel);

    useEffect(() => {
        onCancelRef.current = onCancel;
    });

    // While listening, Esc and Enter belong to the dictation. Caught before the field sees them, so
    // Enter never sends the half-dictated prompt. The big mic leaves this to the inline one.
    useEffect(() => {
        if (!isListening || size === 'xl') {
            return;
        }

        function onKeyDown(event: KeyboardEvent) {
            if (event.key === 'Escape') {
                event.preventDefault();
                event.stopPropagation();
                cancel();
                onCancelRef.current();
            } else if (event.key === 'Enter') {
                event.preventDefault();
                event.stopPropagation();
                stop();
            }
        }

        document.addEventListener('keydown', onKeyDown, true);

        return () => document.removeEventListener('keydown', onKeyDown, true);
    }, [isListening, size, cancel, stop]);

    const toggle = () => (isListening ? stop() : start(i18n.language));

    if (size === 'xl') {
        const label = t(isListening ? 'voice.tapToStop' : 'voice.tapToSpeak');

        return (
            <div className={styles.big}>
                <span className={`${styles.pulse} ${styles.round} ${isListening ? styles.pulsing : ''}`}>
                    <Button variant={isListening ? 'primary' : 'secondary'} size="icon-xl" aria-pressed={isListening} aria-label={label} onClick={toggle}>
                        {isListening ? <SquareIcon size={24} /> : <MicIcon size={32} />}
                    </Button>
                </span>
                <span className={styles.bigLabel} aria-hidden="true">{label}</span>
            </div>
        );
    }

    let label = t(isListening ? 'voice.stop' : 'voice.start');

    if (!isSupported) {
        label = t('voice.unsupported');
    }

    return (
        <span className={styles.voice}>
            {isListening && (
                <>
                    <span className={styles.wave} aria-hidden="true">
                        {WAVE_HEIGHTS.map((height, i) => (
                            <i key={i} style={{ '--wave-bar-height': `${height}px`, animationDelay: `${i * 70}ms` } as CSSProperties} />
                        ))}
                    </span>
                    <span className={styles.clock} aria-live="polite">
                        <span className={styles.listening}>{t('voice.listening')}</span> {formatClock(seconds)}
                    </span>
                    <Button
                        variant="ghost"
                        size="sm"
                        onClick={() => {
                            cancel();
                            onCancel();
                        }}
                    >
                        {t('voice.cancel')} <kbd className={styles.kbd}>Esc</kbd>
                    </Button>
                </>
            )}
            {/* The pulse while listening sits on this wrapper, not on the layer's Button. */}
            <span className={`${styles.pulse} ${size === 'md' ? styles.roundLg : styles.roundMd} ${isListening ? styles.pulsing : ''}`}>
                <IconButton
                    variant={isListening ? 'primary' : size === 'md' ? 'secondary' : 'ghost'}
                    size={size === 'md' ? 'icon-lg' : 'icon'}
                    label={label}
                    aria-pressed={isListening}
                    disabled={!isSupported}
                    focusableWhenDisabled
                    onClick={toggle}
                >
                    {isListening ? <SquareIcon size={size === 'md' ? 16 : 14} /> : <MicIcon size={size === 'md' ? 20 : 16} />}
                </IconButton>
            </span>
        </span>
    );
}
