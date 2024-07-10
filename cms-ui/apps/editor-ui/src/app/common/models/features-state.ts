import { Feature, NodeFeature } from '@gentics/cms-models';


/** Used to indicate which features are enabled. */
export type FeaturesInfo = { [key in Feature]: boolean };

/**
 * Exposes the state of a subset of the CMS features listed at
 * https://www.gentics.com/Content.Node/cmp8/guides/feature_overview.html
 *
 * Note that the naming convention of the state keys differs from the rest of the state,
 * since they reflect the keys used by the CMS.
 */
export interface FeaturesState extends FeaturesInfo {
    /**
     * Contains an array of activated features per node.
     */
    nodeFeatures: { [id: number]: NodeFeature[] };
}
