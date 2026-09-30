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
export type Message = GenaixSchema<'Message'>;
export type MessageCreate = GenaixSchema<'MessageCreate'>;
export type MessagePart = GenaixSchema<'MessagePart'>;
export type UserMessagePart = GenaixSchema<'UserMessagePart'>;
// Prefixed so it does not shadow the DOM `Event` type.
export type GenaixEvent = GenaixSchema<'Event'>;
export type GenaixEventType = GenaixSchema<'EventType'>;

// Workflows
export type Workflow = GenaixSchema<'Workflow'>;
export type WorkflowState = GenaixSchema<'WorkflowState'>;
