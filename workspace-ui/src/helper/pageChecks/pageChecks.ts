import type { Check, CmsObjectRef } from '@/services/apiService/genaix/types';

/**
 * Whether a quality gate produced `check` (`gate_id`), so it counts for `release`, `publish` and
 * `request_review` (`03-genaix-api.md` §4.2). Not `source.type: gate`: a gate whose manifest names a
 * guideline keeps `source.type: guideline` (`alt_text`). An agent's own `report_check` carries no
 * `gate_id` and never refuses a transition.
 */
export function isGateCheck(check: Check): boolean {
    return check.gate_id !== undefined;
}

/**
 * What a check found, in one sentence: the gate's verdict summary (`evidence.summary`, 08 §7.3), not
 * its `message`, into which the gate folds every finding line by line. A check without one says its
 * `message`.
 */
export function checkSummary(check: Check): string {
    const summary = check.evidence?.summary;

    return typeof summary === 'string' && summary ? summary : check.message;
}

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
