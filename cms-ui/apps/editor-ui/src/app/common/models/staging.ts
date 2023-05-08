import { File, Folder, Form, Image, Page } from '@gentics/cms-models';

export enum StagingMode {
    REGULAR,
    RECURSIVE,
    ALL_LANGUAGES
}

export type StageableItem = Folder | Page | File | Image | Form;
