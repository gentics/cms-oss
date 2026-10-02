import { type KeyboardEvent, type PointerEvent } from 'react';
import { useTranslation } from 'react-i18next';

import styles from './Splitter.module.css';

/** Keyboard step of a splitter in px; with Shift it is `LARGE_STEP`. */
const STEP = 32;
const LARGE_STEP = 96;

interface SplitterProps {
    className: string;
    label: string;
    hidden?: boolean;
    isDragging: boolean;
    /** Width of the column this splitter resizes, in px. */
    value: number;
    min: number;
    max: number;
    onDragChange: (isDragging: boolean) => void;
    onDrag: (clientX: number) => void;
    onStep: (delta: number) => void;
    onReset: () => void;
}

/** A vertical splitter: drag, arrow keys (Shift = larger step), Home or double-click to reset. */
export function Splitter({ className, label, hidden, isDragging, value, min, max, onDragChange, onDrag, onStep, onReset }: SplitterProps) {
    const { t } = useTranslation();

    function handlePointerDown(event: PointerEvent<HTMLDivElement>) {
        event.currentTarget.setPointerCapture(event.pointerId);
        onDragChange(true);
    }

    function handlePointerMove(event: PointerEvent<HTMLDivElement>) {
        if (isDragging) {
            onDrag(event.clientX);
        }
    }

    function handleKeyDown(event: KeyboardEvent<HTMLDivElement>) {
        const step = event.shiftKey ? LARGE_STEP : STEP;

        if (event.key === 'ArrowLeft') {
            event.preventDefault();
            onStep(-step);
        } else if (event.key === 'ArrowRight') {
            event.preventDefault();
            onStep(step);
        } else if (event.key === 'Home') {
            event.preventDefault();
            onReset();
        }
    }

    return (
        <div
            role="separator"
            aria-orientation="vertical"
            aria-label={label}
            aria-valuenow={Math.round(value)}
            aria-valuemin={min}
            aria-valuemax={max}
            tabIndex={0}
            title={t('workspace.resizeHint')}
            hidden={hidden}
            className={[styles.splitter, className, isDragging ? styles.dragging : ''].filter(Boolean).join(' ')}
            onPointerDown={handlePointerDown}
            onPointerMove={handlePointerMove}
            onPointerUp={() => onDragChange(false)}
            onPointerCancel={() => onDragChange(false)}
            onLostPointerCapture={() => onDragChange(false)}
            onDoubleClick={onReset}
            onKeyDown={handleKeyDown}
        >
            <span className={styles.grip} />
        </div>
    );
}
