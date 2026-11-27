import { describe, expect, it } from 'vitest';

import { collapseWhiteSpace } from './collapseWhiteSpace';

describe('collapseWhiteSpace', () => {
    it('makes runs of white space one space and drops it at the ends', () => {
        expect(collapseWhiteSpace('  Ab dem \n 1. Juli\tgilt ')).toBe('Ab dem 1. Juli gilt');
        expect(collapseWhiteSpace('')).toBe('');
    });
});
