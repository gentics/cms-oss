import { GcmsNormalizationSchemas } from '@gentics/cms-models';

export * from './business-objects';
export * from './detail-tabs';
export * from './editors';
export * from './entities';
export * from './entity-grid-data-provider';
export * from './generic';
export * from './lists';
export * from './permissions';
export * from './routing';
export * from './tables';
export * from './tabs';
export * from './tag-map';
export * from './wizard';

// To maintain compatibility with existing code, we export various
// types and variables using their legacy variable names:

const schemas = new GcmsNormalizationSchemas();
export const userSchema = schemas.user;
