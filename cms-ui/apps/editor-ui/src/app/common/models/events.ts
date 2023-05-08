import {
    Form,
    Language,
    Page
} from '@gentics/cms-models';

export interface ItemLanguageClickEvent<T extends Page | Form> {
    item: T;
    source: boolean;
    language: Language;
    compare: boolean;
    restore: boolean;
}
