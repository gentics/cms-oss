import { ArrayNormalizationResult, NormalizableEntityType, NormalizableEntityTypesMapBO, Raw } from '@gentics/cms-models';


export interface NormalizationWorkerRequest<T extends NormalizableEntityType, E extends NormalizableEntityTypesMapBO<Raw>[T]> {
    id: number;
    entityType: T;
    rawEntities: E[];
}

export interface NormalizationWorkerResponse {
    id: number;
    result: ArrayNormalizationResult;
    error?: Error;
}
