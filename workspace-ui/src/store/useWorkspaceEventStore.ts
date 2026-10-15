import { create } from 'zustand';

import type { EventStreamStatus } from '@/services/apiService/eventStream';
import type {
    ApprovalStep,
    Check,
    DerivedSettings,
    GenaixEvent,
    Interaction,
    MessageCreateBody,
    MessagePart,
    MessageRole,
    Plan,
    PlanState,
    Preview,
    Problem,
    Publication,
    RunStatus,
    Step,
    UserMessagePart,
} from '@/services/apiService/genaix/types';
import { applyMergePatch } from '@/services/apiService/mergePatch';

/** A user turn this client sent with `POST /sessions/{session_id}/messages`. */
export interface SentMessage {
    kind: 'sent';
    /** Client-side id, known before the server answers. */
    localId: string;
    /** The server's `message_id`, once the POST was accepted. */
    messageId?: string;
    request: MessageCreateBody;
    status: 'sending' | 'sent' | 'send_failed';
    error?: unknown;
}

/**
 * A message arriving on the event stream (`message.started` … `message.completed`), or the user message
 * a confirmed `settings_review` became: `interaction.resolved` with `message_id` is its only event, its
 * parts the `settings` of the answer (contract, `InteractionResolvedEvent.message_id`).
 */
export interface ReceivedMessage {
    kind: 'received';
    id: string;
    role: MessageRole;
    /** The run that produced it, from the events' `run_id`. */
    runId?: string;
    /** The `settings_review` a confirmation answers (`Message.interaction_id`). */
    interactionId?: string;
    /** By `part_index`; a part not started yet is `undefined`. User parts on a user message (`role`). */
    parts: (MessagePart | UserMessagePart | undefined)[];
    status: 'streaming' | 'completed';
}

export type LiveMessage = SentMessage | ReceivedMessage;

/** The run in progress: from `run.started` until its terminal event. */
export interface LiveRun {
    id: string;
    status: RunStatus;
    /** The plan the run works for (`Run.plan_id`); absent for a run of the session agent and a side run (rc.4). */
    planId?: string;
    /** The latest `status` event's text, for the thinking indicator. */
    statusText?: string;
}

/** Something about a run the chat shows: how it ended, or an error it reported. */
export type RunNotice = { seq: number; runId?: string } & (
    | { kind: 'cancelled' }
    | { kind: 'failed'; error: Problem }
    /** An `error` event with `recoverable: true`; an unrecoverable one is followed by `run.failed`. */
    | { kind: 'error'; error: Problem }
    | { kind: 'auth'; connector: string; howToFix: string }
);

/**
 * Where in the stream something came that is not a message of its own: after which of its run's agent
 * messages (none while the run had not answered yet), and after how many of that message's parts. A
 * message that goes on streaming afterwards (one agent message can span a whole run) is shown split
 * there.
 */
export interface StreamAnchor {
    afterMessageId?: string;
    afterPart?: number;
}

/**
 * One plan: as its latest `plan.updated` sent it, and the `seq`, run and place in the stream it first
 * came with. A plan read with `GET …/plans/{plan_id}` (`hydratePlans`) has `seq` 0 and no place.
 */
export interface LivePlan extends StreamAnchor {
    seq: number;
    plan: Plan;
    runId?: string;
}

/** The user message a confirmed `settings_review` became (`interaction.resolved` `message_id`), and where it came. */
export interface LiveConfirmation extends StreamAnchor {
    seq: number;
    messageId: string;
}

/** The CMS preview of the page the session produced, as the last `preview.updated` sent it. */
export type LivePreview = Pick<Extract<GenaixEvent, { type: 'preview.updated' }>, 'seq' | 'cms_ref' | 'preview_url' | 'mode' | 'changed_elements'>;

/** A stored preview (`PlanState.preview`, `previews`) as the preview column takes it; `seq` 0, below every live update. */
export function livePreviewOf({ cms_ref, preview_url, mode, changed_elements }: Preview): LivePreview {
    return { seq: 0, cms_ref, preview_url, mode, changed_elements };
}

/** The page a preview shows, as one string: its CMS object, with the connection that reached it (`CmsObjectRef.connection_id`). */
export function previewKey({ cms_ref: { connection_id: connectionId, type, id } }: LivePreview): string {
    return `${connectionId ?? ''}:${type}:${id}`;
}

// `previews` with `preview` as its page's latest: in place of the page's entry, or added after the others.
function withPreview(previews: LivePreview[] = [], preview: LivePreview): LivePreview[] {
    const key = previewKey(preview);

    return previews.some((live) => previewKey(live) === key)
        ? previews.map((live) => (previewKey(live) === key ? preview : live))
        : [...previews, preview];
}

/**
 * The live state of one session: what is not in `GET /sessions/{session_id}/messages` yet. The
 * history itself stays in TanStack Query.
 */
export interface SessionLiveState {
    /** The highest `seq` applied, the next `after` of the event stream. */
    lastSeq: number;
    connection: EventStreamStatus;
    messages: LiveMessage[];
    /**
     * Every run in progress, oldest first: a plan's run and, beside it, a side run of the session agent
     * (contract rc.4, `Run.mode: side`). Absent while none ran yet.
     */
    runs?: LiveRun[];
    /** The active run, the latest of `runs`; absent or `null` while none runs. */
    run?: LiveRun | null;
    /**
     * Runs the server accepted (`run_id` of `MessageAccepted`, `SessionCreated`, `TransitionAccepted`)
     * that the stream has not told about yet (`awaitRun`).
     */
    awaitedRunIds?: string[];
    /** Every run the stream has told about, so a run whose events came before its `202` is not awaited. */
    seenRunIds?: string[];
    notices?: RunNotice[];
    /** Interactions waiting for an answer (`interaction.requested`, not resolved yet). */
    interactions?: Interaction[];
    /** Each plan's main steps by plan id, then by step id, as `step.updated` last reported them. */
    steps?: Record<string, Record<string, Step>>;
    /**
     * Every plan of the session, one entry per `plan.id`, in the order they first came: each later
     * `plan.updated` of a plan replaces it in place (09-integration-guide.md, "The plan card").
     */
    plans?: LivePlan[];
    /** By interaction id: the plan an `interaction.requested` belongs to (rc.4 `plan_id`; none for the session agent's). */
    interactionPlans?: Record<string, string>;
    /** Every confirmed settings review, in the order `interaction.resolved` sent them. */
    confirmations?: LiveConfirmation[];
    /** By plan id: the publish outcome of each page the plan wrote, as `publications.updated` last sent it. */
    publications?: Record<string, Publication[]>;
    /** By plan id, then by check id: each check as `check.updated` last reported it (rc.4: per plan). */
    checks?: Record<string, Record<string, Check>>;
    /** By plan id: what the agent decided about where and how the content goes, as `derived_settings.updated` last sent it. */
    derivedSettings?: Record<string, DerivedSettings | null>;
    /** By plan id: the approval chain of its pages, as `approval_chain.updated` last sent it. */
    approvalChains?: Record<string, ApprovalStep[]>;
    /** By plan id: the `seq` of the latest event carrying that `plan_id`, so `hydratePlans` keeps what is newer. */
    planSeqs?: Record<string, number>;
    /** The CMS preview; absent until the first `preview.updated`. */
    preview?: LivePreview;
    /**
     * The latest preview of every page the session wrote, one per page (`previewKey`), in the order
     * they first came: a later `preview.updated` of a page replaces its entry in place. Absent until
     * the first `preview.updated`.
     */
    previews?: LivePreview[];
    /**
     * The session's open plan (`drafting` to `released`) and its executing one (`accepted`, `running`), as
     * the last `session.updated` said (rc.4 `Session.open_plan_id`, `executing_plan_id`); `null` for none,
     * absent until a `session.updated` came.
     */
    openPlanId?: string | null;
    executingPlanId?: string | null;
}

interface WorkspaceEventState {
    sessions: Record<string, SessionLiveState>;
    addSentMessage: (sessionId: string, localId: string, request: MessageCreateBody) => void;
    markMessageSent: (sessionId: string, localId: string, messageId: string) => void;
    markSendFailed: (sessionId: string, localId: string, error: unknown) => void;
    /**
     * Applies one event of the session's stream. Events at or below `lastSeq` are ignored, so a
     * replay is a no-op. Types not handled here only advance `lastSeq` (contract rule 1).
     */
    applyEvent: (sessionId: string, event: GenaixEvent) => void;
    setConnection: (sessionId: string, connection: EventStreamStatus) => void;
    /**
     * After the server accepted a call that starts or joins a run: the session counts as working
     * (`isSessionWorking`) until the stream tells about that run. A run it told about already is not awaited.
     */
    awaitRun: (sessionId: string, runId: string) => void;
    /** After `POST …/runs/{run_id}/cancel` was accepted: the run is `cancelling` until `run.cancelled`. */
    markRunCancelling: (sessionId: string, runId: string) => void;
    /** Drops the given messages (`persistedMessageKeys`), once the refetched history contains them. */
    clearMessages: (sessionId: string, keys: string[]) => void;
    /** After `410 events_pruned`: back to `lastSeq` 0, keeping only unsent turns. */
    resetSession: (sessionId: string) => void;
    /**
     * Puts plan snapshots (`GET …/plans/{plan_id}`, `reloadPlans`) into the live state: each plan, its
     * main steps, its publications, checks, derived settings and approval chain, the question its run
     * waits on and, while there is none, the preview.
     * A plan an event told about after `sinceSeq` (`planSeqs`) is left as the stream has it: the
     * snapshot was asked for after `sinceSeq`, so those events are at least as new as it is.
     */
    hydratePlans: (sessionId: string, states: PlanState[], sinceSeq: number) => void;
}

const EMPTY_SESSION: SessionLiveState = { lastSeq: 0, connection: 'closed', messages: [] };

// `runs` while none ran yet; one array, so an event that changes no run leaves the state as it is.
const NO_RUNS: LiveRun[] = [];

/** Selector for one session's live state; a stable empty state while there is none. */
export function selectSession(sessionId: string) {
    return (state: WorkspaceEventState): SessionLiveState => state.sessions[sessionId] ?? EMPTY_SESSION;
}

/**
 * Whether the session's agent is at work: a run that does not wait for the user, a turn on its way,
 * or a run the server accepted that the stream has not told about yet (`awaitedRunIds`).
 */
export function isSessionWorking({ run, messages, awaitedRunIds }: Pick<SessionLiveState, 'run' | 'messages' | 'awaitedRunIds'>): boolean {
    if (awaitedRunIds?.length) {
        return true;
    }

    if (run) {
        return run.status !== 'waiting_for_input';
    }

    return messages.some((message) => message.kind === 'sent' && message.status === 'sending');
}

/** `localId` of a sent message, `id` of a received one. */
export function messageKey(message: LiveMessage): string {
    return message.kind === 'sent' ? message.localId : message.id;
}

/** The messages the server has persisted: sent turns it accepted, received messages it completed. */
export function persistedMessageKeys(session: SessionLiveState): string[] {
    return session.messages
        .filter((message) => (message.kind === 'sent' ? message.status === 'sent' : message.status === 'completed'))
        .map(messageKey);
}

function updateSession(
    sessions: Record<string, SessionLiveState>,
    sessionId: string,
    update: (session: SessionLiveState) => SessionLiveState,
): { sessions: Record<string, SessionLiveState> } {
    return { sessions: { ...sessions, [sessionId]: update(sessions[sessionId] ?? EMPTY_SESSION) } };
}

function updateSent(messages: LiveMessage[], localId: string, update: (message: SentMessage) => SentMessage): LiveMessage[] {
    return messages.map((message) => (message.kind === 'sent' && message.localId === localId ? update(message) : message));
}

// Updates the received message `messageId`, created as an assistant message when its
// `message.started` was missed. Leaves `messages` unchanged for the id of a turn this client sent,
// which is already shown.
function updateReceived(
    messages: LiveMessage[],
    messageId: string,
    update: (message: ReceivedMessage) => ReceivedMessage,
    role: MessageRole = 'assistant',
): LiveMessage[] {
    if (messages.some((message) => message.kind === 'sent' && message.messageId === messageId)) {
        return messages;
    }

    const index = messages.findIndex((message) => message.kind === 'received' && message.id === messageId);

    if (index === -1) {
        return [...messages, update({ kind: 'received', id: messageId, role, parts: [], status: 'streaming' })];
    }

    return messages.map((message, i) => (i === index ? update(message as ReceivedMessage) : message));
}

function withPart(message: ReceivedMessage, partIndex: number, part: ReceivedMessage['parts'][number]): ReceivedMessage {
    const parts = [...message.parts];

    parts[partIndex] = part;

    return { ...message, parts };
}

// `part.delta`: a string appended to `text` (`append`), or a merge patch of the part (`merge_patch`).
function applyDelta(part: ReceivedMessage['parts'][number], delta: string | Record<string, unknown>, patchFormat: 'append' | 'merge_patch'): ReceivedMessage['parts'][number] {
    if (part === undefined) {
        return part;
    }

    if (patchFormat === 'merge_patch') {
        // A merge patch of a part is a part again (contract, `PartDeltaEvent`); the next
        // `part.completed` replaces it with the authoritative copy either way.
        return applyMergePatch(part, delta) as MessagePart;
    }

    if (typeof delta === 'string' && 'text' in part && typeof part.text === 'string') {
        return { ...part, text: part.text + delta };
    }

    return part;
}

// The run of a received message, kept from the first event that names one.
function withRun(message: ReceivedMessage, runId: string | null | undefined): ReceivedMessage {
    return message.runId || !runId ? message : { ...message, runId };
}

function applyToMessages(messages: LiveMessage[], event: GenaixEvent): LiveMessage[] {
    switch (event.type) {
        case 'message.started':
            return updateReceived(messages, event.message_id, (message) => withRun(message, event.run_id), event.role);
        case 'part.started':
        case 'part.completed':
            return updateReceived(messages, event.message_id, (message) => withPart(withRun(message, event.run_id), event.part_index, event.part));
        case 'part.delta':
            return updateReceived(messages, event.message_id, (message) => withPart(
                message,
                event.part_index,
                applyDelta(message.parts[event.part_index], event.delta, event.patch_format),
            ));
        case 'message.completed':
            return updateReceived(messages, event.message_id, (message) => ({ ...message, status: 'completed' }));
        case 'interaction.resolved': {
            // A confirmed `settings_review`: GenAIx stored the answer's `settings` as a user message.
            const { message_id: messageId, answer } = event;

            if (!messageId || !answer || !('settings' in answer)) {
                return messages;
            }

            return updateReceived(messages, messageId, (message) => ({
                ...withRun(message, event.run_id),
                interactionId: event.interaction_id,
                parts: answer.settings,
                status: 'completed',
            }), 'user');
        }
        default:
            return messages;
    }
}

// Updates the run an event belongs to (`run_id`), else the latest one; `runs` unchanged when `update`
// returns the run itself.
function updateRun(runs: LiveRun[], runId: string | null | undefined, update: (run: LiveRun) => LiveRun): LiveRun[] {
    const index = runId ? runs.findIndex(({ id }) => id === runId) : runs.length - 1;
    const run = runs[index];

    if (!run) {
        return runs;
    }

    const next = update(run);

    return next === run ? runs : runs.map((current, i) => (i === index ? next : current));
}

function applyToRuns(runs: LiveRun[], event: GenaixEvent): LiveRun[] {
    switch (event.type) {
        case 'run.started': {
            const { id, status, plan_id: planId } = event.run;

            return [...runs.filter((run) => run.id !== id), { id, status, ...(planId && { planId }) }];
        }
        case 'run.completed':
        case 'run.failed':
        case 'run.cancelled':
            // Only that run ends: a side run ending leaves the plan's run working (rc.4).
            return runs.some(({ id }) => id === event.run.id) ? runs.filter(({ id }) => id !== event.run.id) : runs;
        case 'status':
            return updateRun(runs, event.run_id, (run) => ({ ...run, statusText: event.text }));
        case 'part.started':
            // A `status` has no end event. Once the agent writes a part, what it said it was doing is over.
            return updateRun(runs, event.run_id, (run) => (run.statusText ? { ...run, statusText: undefined } : run));
        case 'interaction.requested':
            return updateRun(runs, event.run_id ?? event.interaction.run_id, (run) => ({ ...run, status: 'waiting_for_input' }));
        case 'interaction.resolved':
            return updateRun(runs, event.run_id, (run) => (run.status === 'waiting_for_input' ? { ...run, status: 'running' } : run));
        default:
            return runs;
    }
}

function noticeOf(event: GenaixEvent): RunNotice | undefined {
    const base = { seq: event.seq, runId: event.run_id ?? undefined };

    switch (event.type) {
        case 'run.cancelled':
            return { ...base, kind: 'cancelled' };
        case 'run.failed':
            return { ...base, kind: 'failed', error: event.error };
        case 'error':
            // An unrecoverable error is the `error` of the `run.failed` that follows at once.
            return event.recoverable ? { ...base, kind: 'error', error: event.error } : undefined;
        case 'auth.required':
            return { ...base, kind: 'auth', connector: event.connector, howToFix: event.how_to_fix };
        default:
            return undefined;
    }
}

function applyToInteractions(interactions: Interaction[] | undefined, event: GenaixEvent): Interaction[] | undefined {
    switch (event.type) {
        case 'interaction.requested':
            return [...(interactions ?? []).filter(({ id }) => id !== event.interaction.id), event.interaction];
        case 'interaction.resolved':
            return interactions?.filter(({ id }) => id !== event.interaction_id);
        case 'run.completed':
        case 'run.failed':
        case 'run.cancelled':
            // A run that ended waits for nothing any more.
            return interactions?.filter(({ run_id }) => run_id !== event.run.id);
        default:
            return interactions;
    }
}

// `step.updated` is a full replacement keyed by its plan (`plan_id`, rc.4) and its id. Only the main
// steps are kept: an update of a substep (`parent_step_id`) may be ignored by a client that shows only
// those (contract).
function applyToSteps(steps: SessionLiveState['steps'], event: GenaixEvent): SessionLiveState['steps'] {
    if (event.type !== 'step.updated' || event.parent_step_id || !event.plan_id) {
        return steps;
    }

    return { ...steps, [event.plan_id]: { ...steps?.[event.plan_id], [event.step.id]: event.step } };
}

// `plan.updated` carries the whole plan every time, never a delta, on every change of its items and of
// its status (rc.4). One entry per plan, in the order they first came, with the place it first came at:
// a later one replaces its plan there.
function applyToPlans(plans: LivePlan[] = [], { seq, plan, run_id: runId }: Extract<GenaixEvent, { type: 'plan.updated' }>, messages: LiveMessage[]): LivePlan[] {
    if (plans.some((live) => live.plan.id === plan.id)) {
        return plans.map((live) => (live.plan.id === plan.id ? { ...live, plan } : live));
    }

    return [...plans, { seq, plan, ...(runId && { runId }), ...anchorIn(messages, runId ?? undefined) }];
}

// The place of what comes now: after the agent message of the run that had started by then, if any,
// and after the parts it had by then.
function anchorIn(messages: LiveMessage[], runId: string | undefined): StreamAnchor {
    const after = messages.findLast((message): message is ReceivedMessage => message.kind === 'received' && message.role === 'assistant' && message.runId === runId);

    return after ? { afterMessageId: after.id, afterPart: after.parts.length } : {};
}

function applyToSession(session: SessionLiveState, event: GenaixEvent): SessionLiveState {
    const next: SessionLiveState = { ...session, lastSeq: event.seq, messages: applyToMessages(session.messages, event) };
    const runs = applyToRuns(session.runs ?? NO_RUNS, event);
    const notice = noticeOf(event);
    const interactions = applyToInteractions(session.interactions, event);
    const steps = applyToSteps(session.steps, event);

    if (event.type === 'plan.updated') {
        next.plans = applyToPlans(session.plans, event, session.messages);
    }

    const planId = event.type === 'plan.updated' ? event.plan.id : event.plan_id;

    if (planId) {
        next.planSeqs = { ...session.planSeqs, [planId]: event.seq };
    }

    if (event.type === 'interaction.requested' && event.plan_id) {
        next.interactionPlans = { ...session.interactionPlans, [event.interaction.id]: event.plan_id };
    }

    // A plan's checks, derived settings and approval chain: each a full replacement, keyed by the plan
    // (and a check by its id), never merged (contract §6.1, rule 3).
    if (event.type === 'check.updated' && event.plan_id) {
        next.checks = { ...session.checks, [event.plan_id]: { ...session.checks?.[event.plan_id], [event.check.id]: event.check } };
    }

    if (event.type === 'derived_settings.updated' && event.plan_id) {
        next.derivedSettings = { ...session.derivedSettings, [event.plan_id]: event.derived_settings };
    }

    if (event.type === 'approval_chain.updated' && event.plan_id) {
        next.approvalChains = { ...session.approvalChains, [event.plan_id]: event.approval_chain };
    }

    // `session.updated` carries the whole session: which plan is open and which executes (rc.4).
    if (event.type === 'session.updated') {
        next.openPlanId = event.session.open_plan_id ?? null;
        next.executingPlanId = event.session.executing_plan_id ?? null;
    }

    // `publications.updated` carries the plan's whole array every time (rc.4: per plan).
    if (event.type === 'publications.updated' && event.plan_id) {
        next.publications = { ...session.publications, [event.plan_id]: event.publications };
    }

    // `preview.updated` fires after every CMS write affecting the preview; the last one wins. Its `seq`
    // tells a reload apart from the same URL sent again.
    if (event.type === 'preview.updated') {
        const { seq, cms_ref, preview_url, mode, changed_elements } = event;

        next.preview = { seq, cms_ref, preview_url, mode, changed_elements };
        next.previews = withPreview(session.previews, next.preview);
    }

    if (event.type === 'interaction.resolved' && event.message_id) {
        next.confirmations = [
            ...(session.confirmations ?? []),
            { seq: event.seq, messageId: event.message_id, ...anchorIn(session.messages, event.run_id ?? undefined) },
        ];
    }

    if (runs !== (session.runs ?? NO_RUNS)) {
        next.runs = runs;
        next.run = runs.at(-1) ?? null;
    }

    // The stream told about this run: it is not awaited any more (`awaitRun`).
    const eventRunId = event.type === 'run.started' || event.type === 'run.completed' || event.type === 'run.failed' || event.type === 'run.cancelled'
        ? event.run.id
        : event.run_id;

    if (eventRunId && !session.seenRunIds?.includes(eventRunId)) {
        next.seenRunIds = [...(session.seenRunIds ?? []), eventRunId];
        next.awaitedRunIds = session.awaitedRunIds?.filter((id) => id !== eventRunId);
    }

    if (notice) {
        next.notices = [...(session.notices ?? []), notice];
    }

    if (interactions !== session.interactions) {
        next.interactions = interactions;
    }

    if (steps !== session.steps) {
        next.steps = steps;
    }

    return next;
}

// The plan snapshots `hydratePlans` takes: every one but those an event told about after `sinceSeq`.
function hydrateSession(session: SessionLiveState, states: PlanState[], sinceSeq: number): SessionLiveState {
    const fresh = states.filter(({ plan }) => (session.planSeqs?.[plan.id] ?? 0) <= sinceSeq);

    if (fresh.length === 0) {
        return session;
    }

    let plans = session.plans ?? [];
    let { interactions, interactionPlans } = session;
    const steps = { ...session.steps };
    const publications = { ...session.publications };
    const checks = { ...session.checks };
    const derivedSettings = { ...session.derivedSettings };
    const approvalChains = { ...session.approvalChains };

    for (const { plan, steps: planSteps, publications: planPublications, pending_interaction: pending, ...state } of fresh) {
        plans = plans.some((live) => live.plan.id === plan.id)
            ? plans.map((live) => (live.plan.id === plan.id ? { ...live, plan } : live))
            : [...plans, { seq: 0, plan }];
        steps[plan.id] = Object.fromEntries(planSteps.map((step) => [step.id, step]));
        publications[plan.id] = planPublications ?? [];
        checks[plan.id] = Object.fromEntries(state.checks.map((check) => [check.id, check]));
        derivedSettings[plan.id] = state.derived_settings ?? null;
        approvalChains[plan.id] = state.approval_chain;

        if (pending && !interactions?.some(({ id }) => id === pending.id)) {
            interactions = [...(interactions ?? []), pending];
            interactionPlans = { ...interactionPlans, [pending.id]: plan.id };
        }
    }

    // The preview of the newest plan that has one, as the last `preview.updated` would have said it.
    const latest = session.preview ? undefined : fresh.findLast((state) => state.preview)?.preview;
    const preview = latest ? livePreviewOf(latest) : session.preview;
    // Each plan's latest preview per page (`PlanState.previews`), for the pages the stream has none of yet.
    const previews = fresh
        .flatMap((state) => state.previews ?? [])
        .map(livePreviewOf)
        .reduce((all, page) => (all.some((live) => previewKey(live) === previewKey(page)) ? all : [...all, page]), session.previews ?? []);

    return {
        ...session,
        plans,
        steps,
        publications,
        checks,
        derivedSettings,
        approvalChains,
        interactions,
        interactionPlans,
        preview,
        ...(previews.length > 0 && { previews }),
    };
}

export const useWorkspaceEventStore = create<WorkspaceEventState>((set) => ({
    sessions: {},

    addSentMessage: (sessionId, localId, request) => set((state) => updateSession(state.sessions, sessionId, (session) => ({
        ...session,
        messages: [...session.messages, { kind: 'sent', localId, request, status: 'sending' }],
    }))),

    markMessageSent: (sessionId, localId, messageId) => set((state) => updateSession(state.sessions, sessionId, (session) => ({
        ...session,
        messages: updateSent(session.messages, localId, (message) => ({ ...message, messageId, status: 'sent' })),
    }))),

    markSendFailed: (sessionId, localId, error) => set((state) => updateSession(state.sessions, sessionId, (session) => ({
        ...session,
        messages: updateSent(session.messages, localId, (message) => ({ ...message, error, status: 'send_failed' })),
    }))),

    applyEvent: (sessionId, event) => set((state) => {
        const session = state.sessions[sessionId] ?? EMPTY_SESSION;

        if (event.seq <= session.lastSeq) {
            return state;
        }

        return updateSession(state.sessions, sessionId, () => applyToSession(session, event));
    }),

    setConnection: (sessionId, connection) => set((state) => updateSession(state.sessions, sessionId, (session) => ({
        ...session,
        connection,
    }))),

    awaitRun: (sessionId, runId) => set((state) => updateSession(state.sessions, sessionId, (session) => (
        session.seenRunIds?.includes(runId) || session.awaitedRunIds?.includes(runId)
            ? session
            : { ...session, awaitedRunIds: [...(session.awaitedRunIds ?? []), runId] }
    ))),

    markRunCancelling: (sessionId, runId) => set((state) => updateSession(state.sessions, sessionId, (session) => {
        if (!session.runs?.some(({ id }) => id === runId)) {
            return session;
        }

        const runs = updateRun(session.runs, runId, (run) => ({ ...run, status: 'cancelling' }));

        return { ...session, runs, run: runs.at(-1) ?? null };
    })),

    clearMessages: (sessionId, keys) => set((state) => updateSession(state.sessions, sessionId, (session) => ({
        ...session,
        messages: session.messages.filter((message) => !keys.includes(messageKey(message))),
    }))),

    resetSession: (sessionId) => set((state) => updateSession(state.sessions, sessionId, (session) => ({
        ...session,
        lastSeq: 0,
        runs: undefined,
        run: undefined,
        notices: undefined,
        interactions: undefined,
        steps: undefined,
        plans: undefined,
        interactionPlans: undefined,
        confirmations: undefined,
        publications: undefined,
        planSeqs: undefined,
        preview: undefined,
        previews: undefined,
        openPlanId: undefined,
        executingPlanId: undefined,
        checks: undefined,
        derivedSettings: undefined,
        approvalChains: undefined,
        messages: session.messages.filter((message) => message.kind === 'sent' && message.status !== 'sent'),
    }))),

    hydratePlans: (sessionId, states, sinceSeq) => set((state) => {
        const session = state.sessions[sessionId] ?? EMPTY_SESSION;
        const next = hydrateSession(session, states, sinceSeq);

        return next === session ? state : updateSession(state.sessions, sessionId, () => next);
    }),
}));
