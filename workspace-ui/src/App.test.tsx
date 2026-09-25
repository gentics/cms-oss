import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it } from 'vitest';

import App from './App';

describe('App', () => {
    it('renders the main content', () => {
        render(<App />);

        expect(screen.getByRole('heading', { name: 'Get started' })).toBeInTheDocument();
    });

    it('increments the counter when clicked', async () => {
        const user = userEvent.setup();

        render(<App />);

        const counter = screen.getByRole('button', { name: 'Count is 0' });

        expect(counter).toBeInTheDocument();

        await user.click(counter);

        expect(screen.getByRole('button', { name: 'Count is 1' })).toBeInTheDocument();
    });
});
