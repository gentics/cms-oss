import { type CSSProperties, type ReactNode, useEffect, useRef, useState } from 'react';
import { useTranslation } from 'react-i18next';

import { Splitter } from '@/components/Splitter/Splitter';

import {
    clampColumnWidths,
    type ColumnWidths,
    MIN_LEFT_WIDTH,
    MIN_RIGHT_WIDTH,
    SPLITTER_WIDTH,
} from './columnWidths';

import styles from './WorkspaceLayout.module.css';

type Side = keyof ColumnWidths;

interface WorkspaceLayoutProps {
    left: ReactNode;
    center: ReactNode;
    right: ReactNode;
    /** Hides the left column and its splitter; the content stays mounted. Default `true`. */
    isLeftColumnVisible?: boolean;
}

/**
 * The three-column workspace: left · center · right, with a splitter between each pair. Dragging or
 * the arrow keys resize the side columns; double-click or Home resets one to its default width.
 * The center column takes the rest.
 */
export function WorkspaceLayout({ left, center, right, isLeftColumnVisible = true }: WorkspaceLayoutProps) {
    const { t } = useTranslation();
    const gridRef = useRef<HTMLDivElement>(null);
    const leftRef = useRef<HTMLDivElement>(null);
    const rightRef = useRef<HTMLDivElement>(null);
    const [requested, setRequested] = useState<ColumnWidths>({ left: null, right: null });
    const [measured, setMeasured] = useState({ total: 0, left: 0, right: 0 });
    const [dragging, setDragging] = useState<Side | null>(null);

    // Measures the grid and both side columns whenever one of them changes size: window resize,
    // a hidden column, a new width. The widths are clamped against these numbers.
    useEffect(() => {
        const grid = gridRef.current!;
        const leftColumn = leftRef.current!;
        const rightColumn = rightRef.current!;
        const observer = new ResizeObserver(() => {
            // The track widths the browser resolved. Not the columns' boxes: those reach 4 px under the
            // splitters on each side and are 8 px wider than their track.
            const tracks = getComputedStyle(grid).gridTemplateColumns.split(' ').map(Number.parseFloat);

            setMeasured({
                total: grid.getBoundingClientRect().width,
                left: tracks[0]!,
                right: tracks[tracks.length - 1]!,
            });
        });

        observer.observe(grid);
        observer.observe(leftColumn);
        observer.observe(rightColumn);

        return () => observer.disconnect();
    }, []);

    const widths = clampColumnWidths({
        total: measured.total,
        isLeftVisible: isLeftColumnVisible,
        requested,
        rendered: measured,
    });

    // Before the first measurement the CSS defaults apply.
    const style: CSSProperties & Record<'--w-left' | '--w-right', string | undefined> = {
        '--w-left': measured.total > 0 && widths.left !== null ? `${widths.left}px` : undefined,
        '--w-right': measured.total > 0 && widths.right !== null ? `${widths.right}px` : undefined,
    };

    function setWidth(side: Side, width: number | null) {
        setRequested((current) => ({ ...current, [side]: width }));
    }

    function handleDrag(side: Side, clientX: number) {
        const box = gridRef.current!.getBoundingClientRect();

        setWidth(side, side === 'left'
            ? clientX - box.left - SPLITTER_WIDTH / 2
            : box.right - clientX - SPLITTER_WIDTH / 2);
    }

    // `delta` moves the splitter: positive to the right, which widens the left and narrows the right column.
    function handleStep(side: Side, delta: number) {
        setWidth(side, side === 'left' ? measured.left + delta : measured.right - delta);
    }

    return (
        <div
            ref={gridRef}
            className={[
                styles.layout,
                isLeftColumnVisible ? '' : styles.leftHidden,
                dragging ? styles.resizing : '',
            ].filter(Boolean).join(' ')}
            style={style}
        >
            <div ref={leftRef} className={`${styles.column} ${styles.left}`} hidden={!isLeftColumnVisible}>
                {left}
            </div>
            <Splitter
                className={styles.splitterLeft}
                label={t('workspace.resizeLeft')}
                hidden={!isLeftColumnVisible}
                isDragging={dragging === 'left'}
                value={measured.left}
                min={MIN_LEFT_WIDTH}
                max={widths.maxLeft}
                onDragChange={(isDragging) => setDragging(isDragging ? 'left' : null)}
                onDrag={(clientX) => handleDrag('left', clientX)}
                onStep={(delta) => handleStep('left', delta)}
                onReset={() => setWidth('left', null)}
            />
            <div className={`${styles.column} ${styles.center}`}>
                {center}
            </div>
            <Splitter
                className={styles.splitterRight}
                label={t('workspace.resizeRight')}
                isDragging={dragging === 'right'}
                value={measured.right}
                min={MIN_RIGHT_WIDTH}
                max={widths.maxRight}
                onDragChange={(isDragging) => setDragging(isDragging ? 'right' : null)}
                onDrag={(clientX) => handleDrag('right', clientX)}
                onStep={(delta) => handleStep('right', delta)}
                onReset={() => setWidth('right', null)}
            />
            <div ref={rightRef} className={`${styles.column} ${styles.right}`}>
                {right}
            </div>
        </div>
    );
}
