import { type ReactNode, useEffect, useLayoutEffect, useMemo, useRef } from 'react';
import { useShallow } from 'zustand/react/shallow';

import { AgentSender, ChatMessage } from '@/components/ChatMessage/ChatMessage';
import { HandOffContext } from '@/components/EditTheseButton/handOffContext';
import { ConfirmedSettingsCard, InteractionCard } from '@/components/InteractionCard/InteractionCard';
import { PlanCard } from '@/components/PlanCard/PlanCard';
import { RunNotice } from '@/components/RunNotice/RunNotice';
import { ThinkingIndicator } from '@/components/ThinkingIndicator/ThinkingIndicator';
import { WorkflowSteps } from '@/components/WorkflowSteps/WorkflowSteps';
import { READ_ONLY_WORKFLOW } from '@/helper/workflowGuess/workflowGuess';
import { useSession, useSessionMessages } from '@/hooks/useGenaixQueries';
import { useWorkflowSteps } from '@/hooks/useWorkflowSteps';
import type { MessagePart, MessageRole, UserMessagePart, UserSettingPart } from '@/services/apiService/genaix/types';
import { type LiveMessage, type LivePlan, type LiveRun, messageKey, type ReceivedMessage, type RunNotice as Notice, selectSession, useWorkspaceEventStore } from '@/store/useWorkspaceEventStore';

import styles from './ChatStream.module.css';

// How close to the end counts as "at the end": the stream then follows new content.
const STICK_THRESHOLD_PX = 40;

type Parts = (MessagePart | UserMessagePart | undefined)[];

interface Entry {
    key: string;
    runId?: string;
    isAgent?: boolean;
    /** A user or system message: it shows its sender itself, and ends the agent's block before it. */
    hasOwnSender?: boolean;
    /** An agent message's parts, shown split where something else came in between (`splitMessage`). */
    parts?: Parts;
    isStreaming?: boolean;
    /** Everything else, shown as it is. */
    node?: ReactNode;
}

interface Item {
    key: string;
    hasOwnSender?: boolean;
    /** Only shows what the items after it say again (the agent's head): left out for assistive technology. */
    isDecorative?: boolean;
    node: ReactNode;
}

/** Something placed inside an agent message: after its first `afterPart` parts (all without one). */
interface Cut {
    seq: number;
    afterPart?: number;
    item: Item;
}

// An agent message with what came while it was streaming (`StreamAnchor`): its parts up to each cut,
// then what came there, then the rest, in the order the stream sent them. The first piece keeps the
// message's key, so it is not mounted again when a cut comes; the piece with its newest parts is the
// busy one. A message without parts yet shows nothing.
function splitMessage(entry: Entry, cuts: Cut[], sessionId: string): Item[] {
    const { parts } = entry;

    if (!parts) {
        return [{ key: entry.key, hasOwnSender: entry.hasOwnSender, node: entry.node }, ...cuts.map(({ item }) => item)];
    }

    const items: Item[] = [];
    let from = 0;

    const piece = (to: number) => {
        if (to <= from) {
            return;
        }

        const start = from;

        items.push({
            key: start === 0 ? entry.key : `${entry.key}-from-${start}`,
            node: (
                <ChatMessage
                    role="assistant"
                    parts={parts.map((part, index) => (index >= start && index < to ? part : undefined))}
                    sessionId={sessionId}
                    isStreaming={entry.isStreaming && to === parts.length}
                    hidesSender
                />
            ),
        });
        from = to;
    };

    [...cuts]
        .sort((a, b) => (a.afterPart ?? Infinity) - (b.afterPart ?? Infinity) || a.seq - b.seq)
        .forEach(({ afterPart, item }) => {
            piece(Math.min(afterPart ?? Infinity, parts.length));
            items.push(item);
        });
    piece(parts.length);

    return items;
}

// The agent's avatar and name at the top of each of its blocks: everything between two messages with a
// sender of their own (steps, plan, the agent's messages, notices, interactions, thinking). Keyed by
// what stands before the block, so the head stays put while the block grows.
function withAgentSenders(items: Item[]): Item[] {
    return items.flatMap((item, index) => {
        const previous = items[index - 1];
        const startsBlock = !item.hasOwnSender && (!previous || previous.hasOwnSender);

        return startsBlock
            ? [{ key: `sender-after-${previous?.key ?? 'start'}`, isDecorative: true, node: <AgentSender /> }, item]
            : [item];
    });
}

// The parts a sent turn was composed of; a turn sent as `content` shows as one text part.
function sentParts(message: Extract<LiveMessage, { kind: 'sent' }>): UserMessagePart[] {
    const { parts, content } = message.request;

    return parts ?? [{ type: 'text', text: content ?? '' }];
}

interface MessageInput {
    id: string;
    runId?: string;
    role: MessageRole;
    parts: Parts;
    interactionId?: string;
    isStreaming?: boolean;
}

// One message of the stream, from the history or the event stream: a confirmation as its card, an
// agent message by its parts (`splitMessage`), any other as its sender's own turn.
function messageEntry({ id, runId, role, parts, interactionId, isStreaming }: MessageInput, sessionId: string): Entry {
    if (interactionId) {
        return confirmationEntry(id, runId, parts);
    }

    if (role === 'assistant') {
        return { key: id, runId, isAgent: true, parts, isStreaming };
    }

    return { key: id, runId, hasOwnSender: true, node: <ChatMessage role={role} parts={parts} sessionId={sessionId} isStreaming={isStreaming} /> };
}

// The user message a confirmed settings review became (`Message.interaction_id`): shown as the review's
// card, confirmed, in the agent's block; it carries no sender of its own.
function confirmationEntry(id: string, runId: string | undefined, parts: Parts): Entry {
    const settings = parts.filter((part): part is UserSettingPart => part?.type === 'setting');

    return { key: id, runId, node: <ConfirmedSettingsCard settings={settings} /> };
}

// Where a plan version shows: after the message it came after (`after`), else before its run's first
// agent message (`before`), else at the end (-1 for both).
function planPlace(entries: Entry[], { runId, afterMessageId }: LivePlan): { after: number; before: number } {
    const after = afterMessageId ? entries.findIndex((entry) => entry.key === afterMessageId) : -1;

    if (after >= 0 || !runId) {
        return { after, before: -1 };
    }

    return { after: -1, before: entries.findIndex((entry) => entry.runId === runId && entry.isAgent) };
}

/** Whether the run is working, or a turn is on its way: the thinking indicator shows. */
function isWorking(run: LiveRun | null | undefined, messages: LiveMessage[]): boolean {
    if (run) {
        return run.status !== 'waiting_for_input';
    }

    return messages.some((message) => message.kind === 'sent' && message.status === 'sending');
}

/**
 * The conversation of a session: the history (`GET …/messages`, every page), then what the event
 * stream added since (`useWorkspaceEventStore`), each message once. A message the stream has is shown
 * as streamed, also where the history lists it already while it is still in progress, until the
 * refetched history replaces it (`useSessionEvents`). Where the agent is in its workflow
 * (`WorkflowSteps`) stands first in the agent's latest block, right under its avatar and name. Every version of the work plan (`PlanCard`) stands where its `plan.updated` came, and
 * a confirmed settings review (`ConfirmedSettingsCard`) where its `interaction.resolved` came; an agent
 * message that went on streaming after them is shown split there (`splitMessage`).
 * How a run ended (`RunNotice`) follows the last message of that run. At the end: the pending
 * interaction to answer and, while the agent works, the thinking indicator with its latest status.
 * The agent's avatar and name head each of its blocks (`AgentSender`), so they show first, before
 * its steps, plan and thinking. Follows new content while scrolled to the end. In a read-only session
 * the result cards can hand their objects off to a new session (`HandOffContext`).
 */
export function ChatStream({ sessionId }: { sessionId: string }) {
    const history = useSessionMessages(sessionId);
    const { messages, run, notices, interactions, plans, confirmations } = useWorkspaceEventStore(useShallow((state) => {
        const session = selectSession(sessionId)(state);

        return {
            messages: session.messages,
            run: session.run,
            notices: session.notices,
            interactions: session.interactions,
            plans: session.plans,
            confirmations: session.confirmations,
        };
    }));
    const steps = useWorkflowSteps(sessionId);
    const session = useSession(sessionId).data;
    const isReadOnly = session?.workflow === READ_ONLY_WORKFLOW;
    const nodeId = session?.context?.node_id;
    // One value while the session stays the same, so the cards do not re-render with every event.
    const handOff = useMemo(() => (isReadOnly ? { nodeId } : null), [isReadOnly, nodeId]);
    const scrollRef = useRef<HTMLDivElement>(null);
    const stickRef = useRef(true);

    // Messages list oldest first, so the whole history is every page (contract §13).
    const { hasNextPage, isFetchingNextPage, fetchNextPage } = history;

    useEffect(() => {
        if (hasNextPage && !isFetchingNextPage) {
            void fetchNextPage();
        }
    }, [hasNextPage, isFetchingNextPage, fetchNextPage]);

    const persisted = history.data?.pages.flatMap((page) => page.items) ?? [];
    const persistedIds = new Set(persisted.map((message) => message.id));
    const received = new Map(messages.flatMap((message) => (message.kind === 'received' ? [[message.id, message]] : [])));

    const receivedEntry = (message: ReceivedMessage) => messageEntry({ ...message, isStreaming: message.status === 'streaming' }, sessionId);

    const allEntries: Entry[] = [
        ...persisted.map((message) => {
            const live = received.get(message.id);

            return live
                ? receivedEntry(live)
                : messageEntry({ id: message.id, runId: message.run_id ?? undefined, role: message.role, parts: message.parts, interactionId: message.interaction_id }, sessionId);
        }),
        ...messages
            .filter((message) => !persistedIds.has(message.kind === 'sent' ? message.messageId ?? '' : message.id))
            .map((message) => (message.kind === 'sent'
                ? {
                    key: messageKey(message),
                    hasOwnSender: true,
                    node: (
                        <ChatMessage
                            role="user"
                            parts={sentParts(message)}
                            sessionId={sessionId}
                            isPending={message.status === 'sending'}
                            hasFailed={message.status === 'send_failed'}
                        />
                    ),
                }
                : receivedEntry(message))),
    ];

    // A confirmation stands where its `interaction.resolved` came, inside the agent message it came in
    // (`StreamAnchor`), not where the history lists it. Without that place (the agent had not written
    // yet, or the events are gone) it stays where it is.
    const anchoredConfirmations = new Map((confirmations ?? [])
        .filter(({ afterMessageId }) => afterMessageId && allEntries.some((entry) => entry.key === afterMessageId))
        .map((confirmation) => [confirmation.messageId, confirmation]));
    const entries = allEntries.filter((entry) => !anchoredConfirmations.has(entry.key));
    const confirmationCuts = allEntries.flatMap((entry) => {
        const confirmation = anchoredConfirmations.get(entry.key);

        return confirmation
            ? [{
                after: entries.findIndex(({ key }) => key === confirmation.afterMessageId),
                seq: confirmation.seq,
                afterPart: confirmation.afterPart,
                item: { key: entry.key, node: entry.node },
            }]
            : [];
    });

    // Each version of the plan where its `plan.updated` came; only the newest can be adjusted and accepted.
    const newestPlan = plans?.at(-1);
    const placedPlans = (plans ?? []).map((livePlan) => ({
        ...planPlace(entries, livePlan),
        seq: livePlan.seq,
        afterPart: livePlan.afterPart,
        item: {
            key: `plan-${livePlan.seq}`,
            node: <PlanCard plan={livePlan.plan} sessionId={sessionId} canAnswer={livePlan === newestPlan && !isWorking(run, messages)} />,
        },
    }));
    const cutsAfter = (index: number): Cut[] => [...placedPlans, ...confirmationCuts].filter(({ after }) => after === index);

    // Each notice after the last message of its run, else at the end.
    const placed: Item[] = [];
    const unplaced: Notice[] = [...(notices ?? [])];

    entries.forEach((entry, index) => {
        placedPlans.filter(({ before }) => before === index).forEach(({ item }) => placed.push(item));
        placed.push(...splitMessage(entry, cutsAfter(index), sessionId));

        if (entry.runId && !entries.slice(index + 1).some((next) => next.runId === entry.runId)) {
            unplaced
                .filter((notice) => notice.runId === entry.runId)
                .forEach((notice) => placed.push({ key: `notice-${notice.seq}`, node: <RunNotice notice={notice} /> }));
        }
    });

    placedPlans.filter(({ after, before }) => after < 0 && before < 0).forEach(({ item }) => placed.push(item));

    const anchoredRuns = new Set(entries.map((entry) => entry.runId).filter(Boolean));

    unplaced
        .filter((notice) => !notice.runId || !anchoredRuns.has(notice.runId))
        .forEach((notice) => placed.push({ key: `notice-${notice.seq}`, node: <RunNotice notice={notice} /> }));

    (interactions ?? []).forEach((interaction) => placed.push({
        key: `interaction-${interaction.id}`,
        node: <InteractionCard interaction={interaction} sessionId={sessionId} />,
    }));

    if (isWorking(run, messages)) {
        placed.push({ key: 'thinking', node: <ThinkingIndicator text={run?.statusText} /> });
    }

    // Where the agent is in its workflow: first in its latest block, right under its avatar and name; at
    // the end, opening the next block, while the user's turn is the last thing. One stable key, so the
    // line stays open or closed as it moves down with the agent's turns.
    if (steps.length > 0) {
        const latestBlock = placed.at(-1)?.hasOwnSender
            ? -1
            : placed.findLastIndex((item, index) => !item.hasOwnSender && (index === 0 || placed[index - 1].hasOwnSender));

        placed.splice(latestBlock < 0 ? placed.length : latestBlock, 0, { key: 'workflow-steps', node: <WorkflowSteps steps={steps} /> });
    }

    useLayoutEffect(() => {
        const element = scrollRef.current;

        if (element && stickRef.current) {
            element.scrollTop = element.scrollHeight;
        }
    });

    function handleScroll() {
        const element = scrollRef.current;

        if (element) {
            stickRef.current = element.scrollHeight - element.scrollTop - element.clientHeight < STICK_THRESHOLD_PX;
        }
    }

    return (
        <div ref={scrollRef} className={styles.stream} onScroll={handleScroll}>
            <HandOffContext value={handOff}>
                <ol className={styles.list}>
                    {withAgentSenders(placed).map(({ key, isDecorative, node }) => <li key={key} aria-hidden={isDecorative || undefined}>{node}</li>)}
                </ol>
            </HandOffContext>
        </div>
    );
}
