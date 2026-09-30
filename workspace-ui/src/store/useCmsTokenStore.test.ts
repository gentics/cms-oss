import { beforeEach, describe, expect, it } from 'vitest';

import { type CmsToken, useCmsTokenStore } from './useCmsTokenStore';

const SECRET = 'cmstok_do_not_leak';

function cmsToken(token: string, id = 1): CmsToken {
    return { token, id, name: 'genaix-workspace-1', expires: 1_790_838_683 };
}

describe('useCmsTokenStore', () => {
    beforeEach(() => {
        useCmsTokenStore.getState().clearCmsToken();
    });

    it('holds one token, replaced by the next and removed by clear', () => {
        const { setCmsToken, clearCmsToken } = useCmsTokenStore.getState();

        expect(useCmsTokenStore.getState().cmsToken).toBeNull();

        setCmsToken(cmsToken(SECRET));
        setCmsToken(cmsToken('cmstok_second', 2));

        expect(useCmsTokenStore.getState().cmsToken).toEqual(cmsToken('cmstok_second', 2));

        clearCmsToken();

        expect(useCmsTokenStore.getState().cmsToken).toBeNull();
    });

    it('keeps the token in memory only', () => {
        useCmsTokenStore.getState().setCmsToken(cmsToken(SECRET));

        const persisted = [
            ...Object.keys(localStorage).map((key) => localStorage.getItem(key)),
            ...Object.keys(sessionStorage).map((key) => sessionStorage.getItem(key)),
            document.cookie,
        ].join();

        expect(persisted).not.toContain(SECRET);
    });
});
