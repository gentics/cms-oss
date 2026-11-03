import type { ReactNode } from 'react';

/**
 * A search highlight fragment as text: `<em>` marks the hit and becomes `<mark>`, any other tag is
 * dropped. Never parsed as HTML, so the snippet cannot inject markup.
 */
export function highlight(snippet: string): ReactNode[] {
    return snippet
        .split(/(<em>|<\/em>)/i)
        .reduce<{ nodes: ReactNode[]; marked: boolean }>((state, piece, index) => {
            if (/^<em>$/i.test(piece)) {
                return { ...state, marked: true };
            }

            if (/^<\/em>$/i.test(piece)) {
                return { ...state, marked: false };
            }

            const text = piece.replace(/<[^>]*>/g, '');

            if (text) {
                state.nodes.push(state.marked ? <mark key={index}>{text}</mark> : text);
            }

            return state;
        }, { nodes: [], marked: false })
        .nodes;
}
