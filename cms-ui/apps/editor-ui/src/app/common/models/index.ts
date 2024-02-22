import { GcmsNormalizationSchemas } from '@gentics/cms-models/models/gcms-normalizer/schemas';
import { RequestFailedError } from '@gentics/cms-rest-client';
import { FileCreateRequest, FileUploadResponse, Image, File as CMSFile } from '@gentics/cms-models';

export * from './actions';
export * from './chip-search';
export * from './events';
export * from './list';
export * from './message';
export * from './page';
export * from './page-controls';
export * from './repository-browser';
export * from './staging';

// State models
export * from './auth-state';
export * from './content-staging-state';
export * from './app-state';
export * from './editor-state';
export * from './entity-state';
export * from './favourites-state';
export * from './features-state';
export * from './folder-state';
export * from './maintenance-mode-state';
export * from './message-state';
export * from './node-settings-state';
export * from './page-language-indicators-state';
export * from './publish-queue-state';
export * from './tools-state';
export * from './ui-state';
export * from './usage-state';
export * from './user-state';
export * from './wastebin-state';

// To maintain compatibility with existing code, we export various
// types and variables using their legacy variable names:

const schemas = new GcmsNormalizationSchemas();
export const contentRepositorySchema = schemas.contentRepository;
export const contentPackageSchema = schemas.contentPackage;
export const fileSchema = schemas.file;
export const folderSchema = schemas.folder;
export const formSchema = schemas.form;
export const groupSchema = schemas.group;
export const imageSchema = schemas.image;
export const languageSchema = schemas.language;
export const messageSchema = schemas.message;
export const nodeSchema = schemas.node;
export const pageSchema = schemas.page;
export const templateSchema = schemas.template;
export const userSchema = schemas.user;

export interface UploadResponse {
    /** The file/blob that was attempted to be uploaded. May be optimized away. */
    file?: File | Blob;
    /** The request for creating a file/image. */
    request?: FileCreateRequest;
    /** If it was successfull. */
    successfull: boolean;
    error?: RequestFailedError;
    response?: FileUploadResponse;
    item?: CMSFile | Image;
}
