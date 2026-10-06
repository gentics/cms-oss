// Named aliases for the GenAIx API types. `schema.d.ts` is generated from
// `.claude/contracts/openapi.yaml` by `npm run generate:api`; do not edit it by hand.
import type { components, paths } from './schema';

export type { components, paths };

export type GenaixSchema<Name extends keyof components['schemas']> = components['schemas'][Name];

// Errors
export type Problem = GenaixSchema<'Problem'>;
export type GenaixCode = GenaixSchema<'GenaixCode'>;

// Identity and capabilities (GET /me)
export type Me = GenaixSchema<'Me'>;
export type Capabilities = GenaixSchema<'Capabilities'>;
export type McpConnectionSummary = GenaixSchema<'McpConnectionSummary'>;
export type McpAuthorizationStatus = GenaixSchema<'McpAuthorizationStatus'>;

// Sessions, messages and events
export type Session = GenaixSchema<'Session'>;
export type SessionCreate = GenaixSchema<'SessionCreate'>;
export type SessionCreated = GenaixSchema<'SessionCreated'>;
export type SessionStatus = GenaixSchema<'SessionStatus'>;
export type SessionPage = GenaixSchema<'SessionPage'>;
export type Message = GenaixSchema<'Message'>;
export type MessagePage = GenaixSchema<'MessagePage'>;
export type MessageRole = GenaixSchema<'MessageRole'>;
export type MessageCreate = GenaixSchema<'MessageCreate'>;
export type MessageAccepted = GenaixSchema<'MessageAccepted'>;
export type ContextReference = GenaixSchema<'ContextReference'>;
export type InteractionAnswer = GenaixSchema<'InteractionAnswer'>;
export type MessagePart = GenaixSchema<'MessagePart'>;
export type UserMessagePart = GenaixSchema<'UserMessagePart'>;
export type UserTextPart = GenaixSchema<'UserTextPart'>;
export type UserVerbatimPart = GenaixSchema<'UserVerbatimPart'>;
export type UserFileRefPart = GenaixSchema<'UserFileRefPart'>;
// Prefixed so it does not shadow the DOM `Event` type.
export type GenaixEvent = GenaixSchema<'Event'>;
export type GenaixEventType = GenaixSchema<'EventType'>;
export type MessageStartedEvent = GenaixSchema<'MessageStartedEvent'>;
export type MessageCompletedEvent = GenaixSchema<'MessageCompletedEvent'>;
export type PartStartedEvent = GenaixSchema<'PartStartedEvent'>;
export type PartDeltaEvent = GenaixSchema<'PartDeltaEvent'>;
export type PartCompletedEvent = GenaixSchema<'PartCompletedEvent'>;

// `MessageCreate` is generated as `unknown`: its `anyOf` branches carry only `required`, and a union
// with `unknown` collapses to `unknown`. These are its properties as generated in `schema.d.ts`, with
// the `anyOf` (at least one of `parts` and `content`) as a union.
interface MessageCreateFields {
    content?: string;
    parts?: UserMessagePart[];
    references?: ContextReference[];
    files?: string[];
    reply_to_interaction?: {
        interaction_id: string;
        answer: InteractionAnswer;
    };
    options?: {
        model?: string;
        mode: 'plan_only' | 'execute';
    };
}

export type MessageCreateBody = MessageCreateFields & ({ parts: UserMessagePart[] } | { content: string });

// `SessionCreate` with its `message` typed as `MessageCreateBody` instead of the generated `unknown`.
export type SessionCreateBody = Omit<SessionCreate, 'message'> & { message?: MessageCreateBody };

// Files (POST /sessions/{session_id}/files). Prefixed so it does not shadow the DOM `File` type.
export type SessionFile = GenaixSchema<'File'>;
export type FileMode = GenaixSchema<'FileMode'>;

// Workflows
export type Workflow = GenaixSchema<'Workflow'>;
export type WorkflowState = GenaixSchema<'WorkflowState'>;
