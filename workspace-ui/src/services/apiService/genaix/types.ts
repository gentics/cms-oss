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

// MCP connections and session authorizations (GET /mcp/connections, /sessions/{session_id}/authorizations)
export type McpConnection = GenaixSchema<'McpConnection'>;
export type SessionAuthorization = GenaixSchema<'SessionAuthorization'>;
export type SessionAuthorizationCreate = GenaixSchema<'SessionAuthorizationCreate'>;
export type SessionAuthorizationRequest = GenaixSchema<'SessionAuthorizationRequest'>;

// Sessions, messages and events
export type Session = GenaixSchema<'Session'>;
export type SessionCreate = GenaixSchema<'SessionCreate'>;
export type SessionContext = GenaixSchema<'SessionContext'>;
export type SessionCreated = GenaixSchema<'SessionCreated'>;
export type SessionStatus = GenaixSchema<'SessionStatus'>;
export type SessionPage = GenaixSchema<'SessionPage'>;
export type Message = GenaixSchema<'Message'>;
export type MessagePage = GenaixSchema<'MessagePage'>;
export type MessageRole = GenaixSchema<'MessageRole'>;
export type MessageCreate = GenaixSchema<'MessageCreate'>;
export type MessageAccepted = GenaixSchema<'MessageAccepted'>;
export type ContextReference = GenaixSchema<'ContextReference'>;
export type MessagePart = GenaixSchema<'MessagePart'>;
export type PartType = GenaixSchema<'PartType'>;
export type UnknownPart = GenaixSchema<'UnknownPart'>;
export type TextPart = GenaixSchema<'TextPart'>;
export type StatusNotePart = GenaixSchema<'StatusNotePart'>;
export type TreeViewPart = GenaixSchema<'TreeViewPart'>;
export type TreeNode = GenaixSchema<'TreeNode'>;
export type SelectableListPart = GenaixSchema<'SelectableListPart'>;
export type PropertiesListPart = GenaixSchema<'PropertiesListPart'>;
export type ImageGridPart = GenaixSchema<'ImageGridPart'>;
export type TablePart = GenaixSchema<'TablePart'>;
export type PageStructurePart = GenaixSchema<'PageStructurePart'>;
export type ConstructDraftPart = GenaixSchema<'ConstructDraftPart'>;
export type ApiCallLogPart = GenaixSchema<'ApiCallLogPart'>;
export type CitationPart = GenaixSchema<'CitationPart'>;
export type FileRefPart = GenaixSchema<'FileRefPart'>;
export type CmsObjectRef = GenaixSchema<'CmsObjectRef'>;
export type UserMessagePart = GenaixSchema<'UserMessagePart'>;
export type UserTextPart = GenaixSchema<'UserTextPart'>;
export type UserVerbatimPart = GenaixSchema<'UserVerbatimPart'>;
export type UserFileRefPart = GenaixSchema<'UserFileRefPart'>;
export type UserSettingPart = GenaixSchema<'UserSettingPart'>;
export type UserPlanItem = GenaixSchema<'UserPlanItem'>;
// Prefixed so it does not shadow the DOM `Event` type.
export type GenaixEvent = GenaixSchema<'Event'>;
export type GenaixEventType = GenaixSchema<'EventType'>;
export type MessageStartedEvent = GenaixSchema<'MessageStartedEvent'>;
export type MessageCompletedEvent = GenaixSchema<'MessageCompletedEvent'>;
export type PartStartedEvent = GenaixSchema<'PartStartedEvent'>;
export type PartDeltaEvent = GenaixSchema<'PartDeltaEvent'>;
export type PartCompletedEvent = GenaixSchema<'PartCompletedEvent'>;
export type StatusEvent = GenaixSchema<'StatusEvent'>;
export type ErrorEvent = GenaixSchema<'ErrorEvent'>;
export type AuthRequiredEvent = GenaixSchema<'AuthRequiredEvent'>;

// Runs and interactions
export type Run = GenaixSchema<'Run'>;
export type RunStatus = GenaixSchema<'RunStatus'>;
export type Interaction = GenaixSchema<'Interaction'>;
export type InteractionKind = GenaixSchema<'InteractionKind'>;

// `InteractionAnswer` is generated as `unknown` since the rc.5 addition: `ChoiceAnswer`'s `anyOf` branches
// carry only `required`, and a union with `unknown` collapses to `unknown`. These are its members as
// generated in `schema.d.ts`, with `ChoiceAnswer`'s `anyOf` (at least one selected id, or `other`) as a union.
/** The answer to a `choice`: the selected option ids, or with `other_allowed` the user's own text (`other`). */
export type ChoiceAnswer = { selected?: string[]; other?: string; comment?: string } & ({ selected: string[] } | { other: string });
export type InteractionAnswer =
    | { text: string }
    | ChoiceAnswer
    | { approved: boolean; comment?: string }
    | { values: Record<string, unknown> }
    | { settings: UserSettingPart[]; comment?: string };

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
        mode: 'plan_only' | 'execute' | 'revise';
        /** The plan a `revise` message revises (rc.5); default `Session.open_plan_id`. */
        plan_id?: string;
    };
}

export type MessageCreateBody = MessageCreateFields & ({ parts: UserMessagePart[] } | { content: string });

// `SessionCreate` with its `message` typed as `MessageCreateBody` instead of the generated `unknown`.
export type SessionCreateBody = Omit<SessionCreate, 'message'> & { message?: MessageCreateBody };

// Files (POST /sessions/{session_id}/files). Prefixed so it does not shadow the DOM `File` type.
export type SessionFile = GenaixSchema<'File'>;
export type FilePage = GenaixSchema<'FilePage'>;
export type FileMode = GenaixSchema<'FileMode'>;

// Workflows
export type Workflow = GenaixSchema<'Workflow'>;
export type WorkflowState = GenaixSchema<'WorkflowState'>;
export type Step = GenaixSchema<'Step'>;
export type StepTemplate = GenaixSchema<'StepTemplate'>;
export type Plan = GenaixSchema<'Plan'>;
export type PlanStatus = GenaixSchema<'PlanStatus'>;

// Plan reads (GET /sessions/{session_id}/plans, /plans/{plan_id})
export type PlanState = GenaixSchema<'PlanState'>;
export type PlanSummary = GenaixSchema<'PlanSummary'>;
export type PlanPage = GenaixSchema<'PlanPage'>;

// Plan transitions (POST /sessions/{session_id}/plans/{plan_id}/transitions) and their outcome
export type TransitionRequest = GenaixSchema<'TransitionRequest'>;
export type TransitionAccepted = GenaixSchema<'TransitionAccepted'>;
export type Publication = GenaixSchema<'Publication'>;
export type ApprovalStep = GenaixSchema<'ApprovalStep'>;
export type Check = GenaixSchema<'Check'>;
export type PlanItem = GenaixSchema<'PlanItem'>;
export type PlannedObject = GenaixSchema<'PlannedObject'>;
export type Preview = GenaixSchema<'Preview'>;
export type DerivedSettings = GenaixSchema<'DerivedSettings'>;
