import type { Check, CmsObjectRef } from '@/services/apiService/genaix/types';

/** Whether a check found something to do: a warning or a failure. */
export function isFailingCheck(check: Check): boolean {
    return check.severity === 'warn' || check.severity === 'fail';
}

/** Whether `check` names `page` as its target (`Check.target`). */
export function isCheckOnPage({ target }: Check, page: CmsObjectRef): boolean {
    return target?.type === 'page' && target.id === String(page.id);
}

/**
 * Whether `check` belongs to `page` when one page of a plan is shown: it names that page as its
 * target, or no page at all, so it is about the whole plan (or about another kind of object).
 */
export function isCheckForPage(check: Check, page: CmsObjectRef): boolean {
    return check.target?.type !== 'page' || isCheckOnPage(check, page);
}
