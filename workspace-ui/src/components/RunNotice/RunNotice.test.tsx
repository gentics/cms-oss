import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';

import type { Problem } from '@/services/apiService/genaix/types';

import { RunNotice } from './RunNotice';

import '@/i18n';

const problem = (genaixCode: string, detail?: string) => ({ type: 't', title: 'x', status: 424, genaix_code: genaixCode, detail }) as Problem;

describe('RunNotice', () => {
    it('says a cancelled run was stopped', () => {
        render(<RunNotice notice={{ seq: 9, runId: 'r-1', kind: 'cancelled' }} />);

        expect(screen.getByRole('note')).toHaveTextContent('Stopped');
    });

    it('shows why a run failed, by its genaix_code and with its detail', () => {
        render(<RunNotice notice={{ seq: 9, kind: 'failed', error: problem('cms_object_locked', 'Page 8871 is locked by another user.') }} />);

        const note = screen.getByRole('note');

        expect(note).toHaveTextContent('The run failed');
        expect(note).toHaveTextContent('Page 8871 is locked by another user.');
    });

    it('shows a recoverable error as a warning that the run continues', () => {
        render(<RunNotice notice={{ seq: 9, kind: 'error', error: problem('cms_request_failed') }} />);

        expect(screen.getByRole('note')).toHaveTextContent('Something went wrong, the run continues');
    });

    it('shows how to fix an MCP connection the run could not use', () => {
        render(<RunNotice notice={{ seq: 9, kind: 'auth', connector: 'cms', howToFix: 'Register a new CMS token, then send the message again.' }} />);

        const note = screen.getByRole('note');

        expect(note).toHaveTextContent('No access to the cms connection');
        expect(note).toHaveTextContent('Register a new CMS token, then send the message again.');
    });
});
