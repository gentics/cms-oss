import { AtSignIcon, HashIcon, ListFilterIcon, PaperclipIcon } from 'lucide-react';
import type { ReactNode } from 'react';
import { useTranslation } from 'react-i18next';

import { refText } from '@/components/MessageParts/contextReference';
import { PartRenderer } from '@/components/MessageParts/MessageParts';
import type { MessagePart, MessageRole, UserMessagePart } from '@/services/apiService/genaix/types';

import styles from './ChatMessage.module.css';

function Token({ icon, children }: { icon: ReactNode; children: ReactNode }) {
    return (
        <span className={styles.token}>
            {icon}
            <span className={styles.tokenText}>{children}</span>
        </span>
    );
}

// A part of a user turn (contract `UserMessagePart`, the six types of §5.1).
function UserPart({ part }: { part: UserMessagePart }) {
    const { t } = useTranslation();

    switch (part.type) {
        case 'text':
            return <span className={styles.userText}>{part.text}</span>;
        case 'verbatim':
            return <span className={styles.verbatim}>{part.text}</span>;
        case 'reference':
            return <Token icon={<AtSignIcon size={12} aria-hidden />}>{refText(part.ref)}</Token>;
        case 'file_ref':
            return <Token icon={<PaperclipIcon size={12} aria-hidden />}>{t(part.mode === 'verbatim' ? 'chat.fileVerbatim' : 'chat.file')}</Token>;
        case 'setting':
            return <Token icon={<HashIcon size={12} aria-hidden />}>{`${part.key}: ${part.label || part.value}`}</Token>;
        case 'filter':
            return <Token icon={<ListFilterIcon size={12} aria-hidden />}>{part.label}</Token>;
        default:
            return null;
    }
}

interface ChatMessageProps {
    role: MessageRole;
    /**
     * `UserMessagePart`s for a user turn, `MessagePart`s otherwise (contract `Message.parts`: `role`
     * says which registry). A slot not started yet is `undefined`.
     */
    parts: (MessagePart | UserMessagePart | undefined)[];
    sessionId: string;
    /** The message is still streaming (`aria-busy`). */
    isStreaming?: boolean;
    /** A sent turn the server has not accepted yet. */
    isPending?: boolean;
    /** A sent turn the server refused. */
    hasFailed?: boolean;
    /** The sender stands above the message (`AgentSender`): no avatar and name of its own, indented. */
    hidesSender?: boolean;
}

/**
 * The agent's avatar and name at the top of its turn, before whatever the turn shows (steps, plan,
 * messages, thinking). Hidden from assistive technology: each message names its sender itself.
 */
export function AgentSender() {
    const { t } = useTranslation();

    return (
        <div className={styles.agentSender} aria-hidden>
            <span className={styles.avatar} />
            <span className={styles.sender}>{t('chat.sender.assistant')}</span>
        </div>
    );
}

/**
 * One turn of the chat: the agent's on the left with the avatar, without a bubble, its parts through
 * the part registry; the user's own on the right as a bubble (design.md §12 "Chat-Blasen").
 */
export function ChatMessage({ role, parts, sessionId, isStreaming = false, isPending = false, hasFailed = false, hidesSender = false }: ChatMessageProps) {
    const { t } = useTranslation();
    const isOwn = role === 'user';
    const shown = parts.flatMap((part, index) => (part ? [{ part, index }] : []));
    const className = [
        styles.message,
        isOwn ? styles.own : '',
        hidesSender ? styles.indented : '',
        isPending ? styles.pending : '',
        hasFailed ? styles.failed : '',
    ]
        .filter(Boolean)
        .join(' ');

    return (
        <article className={className} aria-label={t(`chat.sender.${role}`)} aria-busy={isStreaming || isPending}>
            {!isOwn && !hidesSender && <span className={styles.avatar} aria-hidden />}
            <div className={styles.bubble}>
                {!hidesSender && <div className={styles.sender}>{t(`chat.sender.${role}`)}</div>}
                {isOwn
                    // A user message carries user parts (`role` decides the registry).
                    ? shown.map(({ part, index }) => <UserPart key={index} part={part as UserMessagePart} />)
                    : (
                        <div className={styles.parts}>
                            {shown.map(({ part, index }) => <PartRenderer key={index} part={part as MessagePart} sessionId={sessionId} />)}
                        </div>
                    )}
                {hasFailed && <div className={styles.error}>{t('chat.sendFailed')}</div>}
            </div>
        </article>
    );
}
