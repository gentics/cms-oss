import type { ContextReference } from '@/services/apiService/genaix/types';

// `Manual · /Manual/`: the wording, then the object's path when GenAIx sent one (contract
// `ContextReference.path`), so two folders of the same name can be told apart.
export function withPath(label: string, ref?: ContextReference): string {
    return ref?.path ? `${label} · ${ref.path}` : label;
}
