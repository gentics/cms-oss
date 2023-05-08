import {
    ContentPackageBO,
    ContentRepository,
    File,
    Folder,
    Form,
    Group,
    Image,
    IndexById,
    Language,
    Message,
    ModelType,
    Node,
    NormalizableEntityTypesMap,
    Normalized,
    Page,
    Template,
    User
} from '@gentics/cms-models';

export type EntityTypesMap<T extends ModelType = Normalized> = NormalizableEntityTypesMap<T>;

export interface EntityState {
    contentRepository: IndexById<ContentRepository<Normalized>>;
    contentPackage: IndexById<ContentPackageBO<Normalized>>;
    file: IndexById<File<Normalized>>;
    folder: IndexById<Folder<Normalized>>;
    form: IndexById<Form<Normalized>>;
    group: IndexById<Group<Normalized>>;
    image: IndexById<Image<Normalized>>;
    language: IndexById<Language>;
    message: IndexById<Message<Normalized>>;
    node: IndexById<Node<Normalized>>;
    page: IndexById<Page<Normalized>>;
    template: IndexById<Template<Normalized>>;
    user: IndexById<User<Normalized>>;
}
