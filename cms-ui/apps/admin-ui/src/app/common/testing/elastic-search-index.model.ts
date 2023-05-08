import { ElasticSearchIndex, GcmsNormalizer, Normalized, Raw } from '@gentics/cms-models';

// PREPARE TEST DATA
export function convertRawToNormalizedArray(rawEntities: ElasticSearchIndex<Raw>[]): ElasticSearchIndex<Normalized>[] {
    const normalizer = new GcmsNormalizer();
    const indexedEntities = normalizer.normalize('elasticSearchIndex', rawEntities).entities.elasticSearchIndex;
    const normalizedEntities = Object.keys(indexedEntities).map(key => indexedEntities[key]);
    return normalizedEntities;
}
export const MOCK_ENTITIES_RAW: ElasticSearchIndex<Raw>[] = [
    {
        id: 'index_one',
        name: 'index_one',
        found: true,
        settingsValid: true,
        mappingValid: true,
        indexed: 23,
        objects: 42,
        queued: 5,
    },
    {
        id: 'index_two',
        name: 'index_two',
        found: false,
        settingsValid: false,
        mappingValid: false,
        indexed: 46,
        objects: 84,
        queued: 10,
    },
];
export const MOCK_ENTITIES_NORMALIZED: ElasticSearchIndex<Normalized>[] = convertRawToNormalizedArray(MOCK_ENTITIES_RAW);
