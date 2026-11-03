import { useMutation, useQueryClient } from '@tanstack/react-query';

import { errorMessageKey } from '@/helper/errorMapper/errorMapper';
import { type Guideline, guidelinePath, planGuideline } from '@/helper/guidelines/guidelines';
import { cmsKeys, useCmsGuidelines } from '@/hooks/useCmsQueries';
import { useSessionNodeId } from '@/hooks/useSessionNodeId';
import type { Plan } from '@/services/apiService/genaix/types';
import { deleteCmsPackageInternalFile } from '@/services/cmsApiService/cmsApiService';
import { HttpError } from '@/services/httpService/httpService';
import { useErrorNotificationStore } from '@/store/useErrorNotificationStore';

/** The guideline a plan created, and how to delete its file (`useCreatedGuideline`). */
export interface CreatedGuideline {
    guideline: Guideline;
    /** Deletes the guideline's file; resolves once it is gone. */
    deleteFile: () => Promise<void>;
}

/**
 * The guideline `plan` created (`planGuideline` with `isCreated`), found among the guidelines of the
 * session's node (`useSessionNodeId`), with `deleteFile`: a create of a guideline that is discarded
 * deletes the file (ticket). Asks nothing for a plan that created none; undefined until the file is
 * found.
 *
 * Deleting goes directly through the CMS. A file that is already gone counts as deleted: a restoring
 * discard may have had GenAIx remove it first. Any other failure is an error notification; it is
 * reported also once the plan's head is gone, as the mutation, not its caller, reports it.
 */
export function useCreatedGuideline(sessionId: string, plan: Plan): CreatedGuideline | undefined {
    const queryClient = useQueryClient();
    const created = planGuideline(plan);
    const slug = created?.isCreated ? created.slug : undefined;
    const nodeId = useSessionNodeId(sessionId, slug !== undefined);
    const guideline = useCmsGuidelines(slug === undefined ? undefined : nodeId).data?.find((candidate) => candidate.slug === slug);
    const remove = useMutation({
        mutationFn: async (file: Guideline) => {
            try {
                await deleteCmsPackageInternalFile(file.package, guidelinePath(file));
            } catch (error) {
                if (!(error instanceof HttpError && error.status === 404)) {
                    throw error;
                }
            }
        },
        onSuccess: () => (nodeId === undefined ? undefined : queryClient.invalidateQueries({ queryKey: cmsKeys.guidelines(nodeId) })),
        onError: (error) => {
            useErrorNotificationStore.getState().addError({ messageKey: 'guidelineColumn.deleteFailed', detailKey: errorMessageKey(error) });
        },
        retry: false,
    });

    return guideline && { guideline, deleteFile: () => remove.mutateAsync(guideline) };
}
