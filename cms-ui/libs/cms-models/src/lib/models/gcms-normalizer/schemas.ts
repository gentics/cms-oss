import { schema, Schema } from 'normalizr';
import { NormalizableEntity, Normalized, normalizrPreProcessEntity } from '../type-util';
import { NormalizableEntityType } from './gcms-normalizer-types';

/**
 * Manages the normalization schemas used by `GcmsNormalizer`.
 *
 * This class is not exported from `@gentics/cms-models`, because `GcmsNormalizer` should
 * be used for normalization/denormalization.
 */
export class GcmsNormalizationSchemas implements Record<NormalizableEntityType, Schema> {

    readonly construct = new schema.Entity('construct', {}, { processStrategy: normalizrPreProcessEntity });
    readonly constructCategory = new schema.Entity('constructCategory', {}, { processStrategy: normalizrPreProcessEntity });
    readonly contentPackage = new schema.Entity('contentPackage', {}, { processStrategy: normalizrPreProcessEntity });
    readonly contentPackageImport = new schema.Entity('contentPackageImport', {}, { processStrategy: normalizrPreProcessEntity });
    readonly contentRepository = new schema.Entity('contentRepository', {}, { processStrategy: normalizrPreProcessEntity });
    readonly contentRepositoryFragment = new schema.Entity('contentRepositoryFragment', {}, { processStrategy: normalizrPreProcessEntity });
    readonly dataSource = new schema.Entity('dataSource', {}, { processStrategy: normalizrPreProcessEntity });
    readonly dataSourceEntry = new schema.Entity('dataSourceEntry', {}, { processStrategy: normalizrPreProcessEntity });
    readonly elasticSearchIndex = new schema.Entity('elasticSearchIndex', {}, { processStrategy: normalizrPreProcessEntity });
    readonly file = new schema.Entity('file', {}, { processStrategy: normalizrPreProcessEntity });
    readonly folder = new schema.Entity('folder', {}, { processStrategy: normalizrPreProcessEntity });
    readonly form = new schema.Entity('form', {}, { processStrategy: normalizrPreProcessEntity });
    readonly group = new schema.Entity('group', {}, { processStrategy: normalizrPreProcessEntity });
    readonly image = new schema.Entity('image', {}, { processStrategy: normalizrPreProcessEntity });
    readonly language = new schema.Entity('language');
    readonly logs = new schema.Entity('logs', {}, { processStrategy: normalizrPreProcessEntity });
    readonly markupLanguage = new schema.Entity('markupLanguage', {}, { processStrategy: normalizrPreProcessEntity });
    readonly message = new schema.Entity('message', {}, { processStrategy: normalizrPreProcessEntity });
    readonly node = new schema.Entity('node', {}, { processStrategy: normalizrPreProcessEntity });
    readonly objectProperty = new schema.Entity('objectProperty', {}, { processStrategy: normalizrPreProcessEntity });
    readonly objectPropertyCategory = new schema.Entity('objectPropertyCategory', {}, { processStrategy: normalizrPreProcessEntity });
    readonly package = new schema.Entity('package', {}, { processStrategy: normalizrPreProcessEntity });
    readonly page = new schema.Entity('page', {}, { processStrategy: normalizrPreProcessEntity });
    readonly languageVariantPage = new schema.Entity('page', {}, { processStrategy: GcmsNormalizationSchemas.pageProcessStrategy });
    readonly role = new schema.Entity('role', {}, { processStrategy: normalizrPreProcessEntity });
    readonly schedule = new schema.Entity('schedule', {}, { processStrategy: normalizrPreProcessEntity });
    readonly scheduleExecution = new schema.Entity('scheduleExecution', {}, { processStrategy: normalizrPreProcessEntity });
    readonly scheduleTask = new schema.Entity('scheduleTask', {}, { processStrategy: normalizrPreProcessEntity });
    readonly tagmapEntry = new schema.Entity('tagmapEntry', {}, { processStrategy: normalizrPreProcessEntity });
    readonly template = new schema.Entity('template', {}, { processStrategy: normalizrPreProcessEntity });
    readonly templateTag = new schema.Entity('templateTag', {}, { processStrategy: normalizrPreProcessEntity });
    readonly templateTagStatus = new schema.Entity('templateTagStatus', {}, { processStrategy: normalizrPreProcessEntity });
    readonly user = new schema.Entity('user', {}, { processStrategy: normalizrPreProcessEntity });

    constructor() {
        // ContentRepository
        this.contentRepository.define({});

        // Construct
        this.construct.define({});

        // ConstructCategory
        this.constructCategory.define({
            constructs: new schema.Array(this.construct),
        });

        // Content Package
        this.contentPackage.define({});

        // CR_Fragment
        this.contentRepositoryFragment.define({});

        // DataSource
        this.dataSource.define({});

        // DataSourceEntry
        this.dataSourceEntry.define({});

        // Objectproperty
        this.objectProperty.define({
            category: this.objectPropertyCategory,
            construct: this.construct,
        });

        // ObjectpropertyCategory
        this.objectPropertyCategory.define({});

        // Package
        this.package.define({});

        // Elastic Search index
        this.elasticSearchIndex.define({});

        // File
        this.file.define({
            creator: this.user,
            editor: this.user,
            folder: this.folder,
        });

        // Folder
        this.folder.define({
            creator: this.user,
            editor: this.user,
            subfolders: new schema.Array(this.folder),
        });

        // Form
        this.form.define({
            creator: this.user,
            editor: this.user,
            folder: this.folder,
            lockedBy: this.user,
            publisher: this.user,
        });

        // Group
        this.group.define({
            children: new schema.Array(this.group),
        });

        // Image
        this.image.define({
            creator: this.user,
            editor: this.user,
            folder: this.folder,
        });

        // Language
        // Schema does not need any content.

        // Logs
        this.logs.define({});

        this.markupLanguage.define({});

        // Message
        this.message.define({
            sender: this.user,
        });

        // Node
        this.node.define({
            creator: this.user,
            editor: this.user,
        });

        // Page
        this.page.define({
            creator: this.user,
            editor: this.user,
            folder: this.folder,
            languageVariants: new schema.Values(this.languageVariantPage),
            lockedBy: this.user,
            pageVariants: new schema.Array(this.page),
            publisher: this.user,
            template: this.template,
        });

        // Language Variant Page
        this.languageVariantPage.define({
            creator: this.user,
            editor: this.user,
            folder: this.folder,
            languageVariants: new schema.Values(this.languageVariantPage),
            lockedBy: this.user,
            pageVariants: new schema.Array(this.page),
            publisher: this.user,
            template: this.template,
        });

        // Role
        this.role.define({});

        // Scheduler
        this.schedule.define({});
        this.scheduleExecution.define({});
        this.scheduleTask.define({});

        // Tagmapentry
        this.tagmapEntry.define({});

        // Template
        this.template.define({
            creator: this.user,
            editor: this.user,
        });

        // Template Tag
        this.templateTag.define({});

        // Template Tag Status
        this.templateTagStatus.define({});

        // Group
        this.user.define({
            groups: new schema.Array(this.group),
        });
    }

    private static pageProcessStrategy(value: any, parent: any, key: string): NormalizableEntity<Normalized> {
        if (!!key && !!parent[key] && !value.languageVariants) {
            value = {
                ...value,
                // since a group of pages that are language variants act as a clique, we can add all variants to every page of the group
                languageVariants: parent,
            };
        }
        return normalizrPreProcessEntity(value);
    }

}
