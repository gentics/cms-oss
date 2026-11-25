import type { TFunction } from 'i18next';

import type { ConstructDraftPart, Interaction, MessagePart, UserMessagePart } from '@/services/apiService/genaix/types';
import type { CmsTaskCreate, CmsTaskObject } from '@/services/cmsApiService/cmsTaskApi';

/**
 * The option id GenAIx reserves for "request this construct" in a `choice`, offered when the user may not create
 * constructs: picking it makes the client create a task for the group that may, before it answers.
 * Proposed to GenAIx, not yet in the contract (`openapi.yaml` has no reserved option ids).
 */
export const CONSTRUCT_REQUEST_OPTION = 'request_construct';

/** Whether `interaction` is a construct request: a `choice` with the reserved option. */
export function isConstructRequest(interaction: Interaction): boolean {
    return interaction.kind === 'choice' && (interaction.options ?? []).some(({ id }) => id === CONSTRUCT_REQUEST_OPTION);
}

/** Whether `answer` picks the reserved option of a construct request. */
export function requestsConstruct(answer: { selected?: string[] } | undefined): boolean {
    return answer?.selected?.includes(CONSTRUCT_REQUEST_OPTION) ?? false;
}

interface RunParts {
    runId?: string;
    parts?: (MessagePart | UserMessagePart | undefined)[];
}

/**
 * The construct a request asks for: the last `construct_draft` of the request's run that was not created. None
 * without a run, so a draft of another conversation turn is never taken for it.
 */
export function latestConstructDraft(entries: RunParts[], runId: string | undefined): ConstructDraftPart | undefined {
    if (!runId) {
        return undefined;
    }

    return entries
        .filter((entry) => entry.runId === runId)
        .flatMap((entry) => entry.parts ?? [])
        .filter((part): part is ConstructDraftPart => part?.type === 'construct_draft' && !part.created_ref)
        .at(-1);
}

// A localised name in the UI language, else English, else the first one.
function localised(names: Record<string, string> | undefined, language: string): string | undefined {
    if (!names) {
        return undefined;
    }

    return names[language] ?? names.en ?? Object.values(names)[0];
}

function valueText(value: unknown): string {
    return typeof value === 'string' ? value : JSON.stringify(value);
}

/** The draft as an instruction a later `construct_create` session can follow on its own. */
export function constructInstruction(draft: ConstructDraftPart, t: TFunction, language: string): string {
    const name = localised(draft.name, language) || draft.keyword;
    const description = localised(draft.description, language);
    const names = Object.entries(draft.name).map(([code, text]) => `${code}: ${text}`).join(', ');
    const parts = (draft.parts ?? []).map((part) => {
        const details = [
            t('tasks.constructRequest.partType', { typeId: part.type_id }),
            ...(part.mandatory ? [t('tasks.constructRequest.mandatory')] : []),
            ...(part.default !== undefined && part.default !== null ? [t('tasks.constructRequest.default', { value: valueText(part.default) })] : []),
        ];

        return `- ${part.keyword}${part.name ? ` (${localised(part.name, language)})` : ''}: ${details.join(', ')}`;
    });

    return [
        t('tasks.constructRequest.intro', { name, keyword: draft.keyword }),
        t('tasks.constructRequest.names', { names }),
        ...(description ? [t('tasks.constructRequest.description', { description })] : []),
        ...(draft.node_ids?.length ? [t('tasks.constructRequest.nodes', { ids: draft.node_ids.join(', ') })] : []),
        ...(parts.length > 0 ? [t('tasks.constructRequest.parts'), ...parts] : []),
        t('tasks.constructRequest.template'),
        draft.template,
    ].join('\n');
}

// The CMS object the request option names (`ref`), e.g. the page that needs the construct; only one that exists.
function requestObjects(interaction: Interaction): CmsTaskObject[] {
    const ref = interaction.options?.find(({ id }) => id === CONSTRUCT_REQUEST_OPTION)?.ref;
    const id = Number(ref?.id);

    return ref && Number.isInteger(id) && id > 0 ? [{ type: ref.type, id, nodeId: ref.node_id, name: ref.label }] : [];
}

/**
 * The task a construct request creates for `groupId`: the draft as its instruction, the object the request names,
 * and the interaction's id as `externalKey`, so trying again updates the task instead of creating a second one.
 */
export function constructRequestTask(draft: ConstructDraftPart, interaction: Interaction, groupId: number, t: TFunction, language: string): CmsTaskCreate {
    const name = localised(draft.name, language) || draft.keyword;
    const description = localised(draft.description, language);
    const objects = requestObjects(interaction);

    return {
        type: 'requestConstruct',
        title: t('tasks.constructRequest.title', { name }),
        ...(description && { description }),
        assignee: { type: 'group', id: groupId },
        ...(objects.length > 0 && { objects }),
        externalKey: interaction.id,
        genaix: { instruction: constructInstruction(draft, t, language) },
    };
}
