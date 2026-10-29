import { render, screen, within } from '@testing-library/react';
import { afterEach, describe, expect, it, vi } from 'vitest';

import { UiProvider } from '@/components/ui/provider';

import { Catalogue } from './Catalogue';
import { partSampleGroups } from './partSamples';

import '@/i18n';

describe('Catalogue', () => {
    afterEach(() => {
        vi.restoreAllMocks();
    });

    it('shows every part type with each of its variants, through the chat\'s part registry', () => {
        // The broken sample makes its renderer throw on purpose; React reports that before the fallback
        // shows. Nothing else may report an error.
        const errors = vi.spyOn(console, 'error').mockImplementation(() => {});

        render(<Catalogue />, { wrapper: UiProvider });

        expect(errors).toHaveBeenCalled();
        errors.mock.calls.forEach((args) => expect(args.map(String).join(' ')).toMatch(/columns\.map is not a function|<TablePartView>/));

        const section = screen.getByRole('region', { name: 'Message parts' });

        [
            'text',
            'status_note',
            'tree_view',
            'selectable_list',
            'properties_list',
            'image_grid',
            'table',
            'page_structure',
            'construct_draft',
            'api_call_log',
            'citation',
            'file_ref',
            'Fallback',
        ].forEach((type) => expect(within(section).getByRole('heading', { level: 3, name: type })).toBeInTheDocument());
        expect(within(section).getByText('Known type that fails to render')).toBeInTheDocument();
        expect(within(section).getByText('Diese Tabelle konnte nicht dargestellt werden.')).toBeInTheDocument();
        expect(within(section).getByRole('region', { name: 'Seiten mit dem alten Versandschwellenwert' })).toBeInTheDocument();
    });

    it('has a label in both languages for every variant', async () => {
        const [en, de] = await Promise.all([import('@/i18n/locales/en/common.json'), import('@/i18n/locales/de/common.json')]);
        const ids = partSampleGroups.flatMap(({ samples }) => samples.map(({ id }) => id));

        expect(Object.keys(en.catalogue.parts.variants).sort()).toEqual([...ids].sort());
        expect(Object.keys(de.catalogue.parts.variants).sort()).toEqual([...ids].sort());
    });
});
