import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';

import { ReferenceToken } from './ReferenceToken';

describe('ReferenceToken', () => {
    it('names the part, its @ icon hidden from assistive technology', () => {
        const { container } = render(<ReferenceToken>Text · content_2 · Link</ReferenceToken>);

        expect(screen.getByText('Text · content_2 · Link')).toBeInTheDocument();
        expect(container.querySelector('svg')).toHaveAttribute('aria-hidden', 'true');
    });
});
