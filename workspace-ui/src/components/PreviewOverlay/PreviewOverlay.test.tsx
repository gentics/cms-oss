import { render, screen } from '@testing-library/react';
import { createRef } from 'react';
import { describe, expect, it } from 'vitest';

import { PreviewOverlay } from './PreviewOverlay';

const box = { top: 100, left: 40, width: 200, height: 30 };
const none = { dimmed: null, changed: [], selected: null, target: null, editing: null, hovered: null };

describe('PreviewOverlay', () => {
    it('dims the page around a box in four bands, leaving the outline\'s offset free', () => {
        const { container } = render(<PreviewOverlay {...none} selectedRef={createRef()} dimmed={box} />);
        const bands = Array.from(container.querySelectorAll<HTMLElement>('[data-dim]'));

        expect(bands).toHaveLength(4);
        expect(bands[0]!.style.height).toBe('98px');
        expect(bands[1]!.style.top).toBe('132px');
    });

    it('draws changed parts, the target and the edited element at their boxes, and names the hovered part', () => {
        const { container } = render(
            <PreviewOverlay
                {...none}
                selectedRef={createRef()}
                changed={[{ tagName: 'content_2', box }]}
                target={{ top: 110, left: 50, width: 80, height: 20 }}
                editing={box}
                hovered={{ box, label: 'Paragraph · Text' }}
            />,
        );

        expect(container.querySelector<HTMLElement>('[data-changed-part="content_2"]')!.style.top).toBe('100px');
        expect(container.querySelector<HTMLElement>('[data-edit-target]')!.style.width).toBe('80px');
        expect(container.querySelector('[data-editing]')).toBeInTheDocument();
        expect(screen.getByText('Paragraph · Text')).toBeInTheDocument();
        expect(container.firstElementChild).toHaveAttribute('aria-hidden', 'true');
    });

    it('gives the selection\'s element to the ref, also as a bare anchor', () => {
        const ref = createRef<HTMLDivElement>();

        render(<PreviewOverlay {...none} selectedRef={ref} selected={{ box, mode: 'anchor' }} />);

        expect(ref.current).not.toBeNull();
        expect(ref.current!.style.left).toBe('40px');
    });
});
