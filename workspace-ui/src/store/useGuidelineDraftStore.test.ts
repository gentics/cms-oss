import { beforeEach, describe, expect, it } from 'vitest';

import { guidelineDraftKey, useGuidelineDraftStore } from './useGuidelineDraftStore';

describe('useGuidelineDraftStore', () => {
    beforeEach(() => {
        useGuidelineDraftStore.setState({ drafts: {} });
    });

    it('keys a draft by the guideline\'s package and file', () => {
        expect(guidelineDraftKey('corporate', 'guidelines/20_seo-basics.md')).toBe('corporate/guidelines/20_seo-basics.md');
    });

    it('keeps a draft per guideline and drops it again', () => {
        const { setDraft } = useGuidelineDraftStore.getState();

        setDraft('corporate/guidelines/20_seo-basics.md', '# SEO');
        setDraft('corporate/guidelines/30_legal-notes.md', '# Legal');
        setDraft('corporate/guidelines/20_seo-basics.md', undefined);

        expect(useGuidelineDraftStore.getState().drafts).toEqual({ 'corporate/guidelines/30_legal-notes.md': '# Legal' });
    });
});
