import type { ContextReference, MessageCreateBody, UserMessagePart } from '@/services/apiService/genaix/types';

/** One step of a plan the user accepts: its label and, where the agent named one, its target. */
export interface PlanStepInput {
    label: string;
    target?: ContextReference;
}

/**
 * The message that accepts the agent's plan. The contract has no route for it: a plan is answered with
 * an ordinary message in `execute` mode (`MessageCreate.options.mode`). Accepted as proposed, that is
 * the plain `acceptText`. Adjusted, it carries the plan as a numbered list after `intro`, one step per
 * line with its target as a `reference` part inside the line, the shape the Workspace UI and the GenAIx
 * mock agreed on for rc.2 (mock README, "The adjusted plan is a new execute message").
 */
export function planMessage(steps: PlanStepInput[] | undefined, { acceptText, intro }: { acceptText: string; intro: string }): MessageCreateBody {
    if (!steps) {
        return { parts: [{ type: 'text', text: acceptText }], options: { mode: 'execute' } };
    }

    const parts: UserMessagePart[] = [];
    let text = intro;

    steps.forEach((step, index) => {
        text += `\n${index + 1}. ${step.label}`;

        if (step.target) {
            parts.push({ type: 'text', text: `${text} ` }, { type: 'reference', ref: step.target });
            text = '';
        }
    });

    if (text) {
        parts.push({ type: 'text', text });
    }

    return { parts, options: { mode: 'execute' } };
}
