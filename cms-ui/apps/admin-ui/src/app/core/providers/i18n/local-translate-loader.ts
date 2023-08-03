import { Injectable } from '@angular/core';
import { IndexByKey } from '@gentics/cms-models';
import { TranslateLoader } from '@ngx-translate/core';
import { Observable, of as observableOf } from 'rxjs';
import * as COMMON_TRANSLATIONS from './translations/common.translations.json';
import * as CONSTRUCT_CATEGORY_TRANSLATIONS from './translations/construct-category.translations.json';
import * as CONSTRUCT_TRANSLATIONS from './translations/construct.translations.json';
import * as CONTENTMAINTENANCE_TRANSLATIONS from './translations/content-maintenance.translations.json';
import * as CRFRAGMENT_TRANSLATIONS from './translations/content-repository-fragment.translations.json';
import * as CONTENTREPOSITORY_TRANSLATIONS from './translations/content-repository.translations.json';
import * as CONTENT_STAGING_TRANSLATIONS from './translations/content-staging.translations.json';
import * as DASHBOARD_TRANSLATIONS from './translations/dashboard.translations.json';
import * as DATASOURCE_ENTRY_TRANSLATIONS from './translations/data-source-entry.translations.json';
import * as DATASOURCE_TRANSLATIONS from './translations/data-source.translations.json';
import * as ELASTICSEARCHINDEX_TRANSLATIONS from './translations/elastic-search-index.translations.json';
import * as GROUP_TRANSLATIONS from './translations/group.translations.json';
import * as LOGS_TRANSLATIONS from './translations/logs.translations.json';
import * as MAINTENANCEMODE_TRANSLATIONS from './translations/maintenance-mode.translations.json';
import * as MESH_TRANSLATIONS from './translations/mesh.translations.json';
import * as MODAL_TRANSLATIONS from './translations/modal.translations.json';
import * as NODE_TRANSLATIONS from './translations/node.translations.json';
import * as OBJECTPROPERTY_TRANSLATIONS from './translations/object-property.translations.json';
import * as PACKAGE_TRANSLATIONS from './translations/package.translations.json';
import * as ROLE_TRANSLATIONS from './translations/role.translations.json';
import * as SCHEDULER_TRANSLATIONS from './translations/scheduler.translations.json';
import * as SHARED_TRANSLATIONS from './translations/shared.translations.json';
import * as TAGMAPENTRY_TRANSLATIONS from './translations/tagmap-entry.translations.json';
import * as TEMPLATE_TAG_TRANSLATIONS from './translations/template-tag.translations.json';
import * as TEMPLATE_TRANSLATIONS from './translations/template.translations.json';
import * as USER_TRANSLATIONS from './translations/user.translations.json';
import * as WIDGET_TRANSLATIONS from './translations/widget.translations.json';

function getTranslations(jsonModule: any): any {
    return jsonModule.default;
}

export const ALL_TRANSLATIONS = {
    common: getTranslations(COMMON_TRANSLATIONS),
    construct: getTranslations(CONSTRUCT_TRANSLATIONS),
    construct_category: getTranslations(CONSTRUCT_CATEGORY_TRANSLATIONS),
    content_staging: getTranslations(CONTENT_STAGING_TRANSLATIONS),
    contentmaintenance: getTranslations(CONTENTMAINTENANCE_TRANSLATIONS),
    contentRepository: getTranslations(CONTENTREPOSITORY_TRANSLATIONS),
    contentRepositoryFragment: getTranslations(CRFRAGMENT_TRANSLATIONS),
    dashboard: getTranslations(DASHBOARD_TRANSLATIONS),
    dataSource: getTranslations(DATASOURCE_TRANSLATIONS),
    dataSourceEntry: getTranslations(DATASOURCE_ENTRY_TRANSLATIONS),
    elasticSearchIndex: getTranslations(ELASTICSEARCHINDEX_TRANSLATIONS),
    group: getTranslations(GROUP_TRANSLATIONS),
    logs: getTranslations(LOGS_TRANSLATIONS),
    maintenancemode: getTranslations(MAINTENANCEMODE_TRANSLATIONS),
    mesh: getTranslations(MESH_TRANSLATIONS),
    modal: getTranslations(MODAL_TRANSLATIONS),
    node: getTranslations(NODE_TRANSLATIONS),
    objectProperty: getTranslations(OBJECTPROPERTY_TRANSLATIONS),
    package: getTranslations(PACKAGE_TRANSLATIONS),
    role: getTranslations(ROLE_TRANSLATIONS),
    scheduler: getTranslations(SCHEDULER_TRANSLATIONS),
    shared: getTranslations(SHARED_TRANSLATIONS),
    tagmapEntry: getTranslations(TAGMAPENTRY_TRANSLATIONS),
    template: getTranslations(TEMPLATE_TRANSLATIONS),
    templateTag: getTranslations(TEMPLATE_TAG_TRANSLATIONS),
    user: getTranslations(USER_TRANSLATIONS),
    widget: getTranslations(WIDGET_TRANSLATIONS),
};

/** Translations for a single language. */
interface SingleLanguageTranslationsSet {
    [section: string]: IndexByKey<string>;
}

@Injectable({
    providedIn: 'root',
})
export class LocalTranslateLoader implements TranslateLoader {

    /**
     * Gets the translation object for the specified language.
     */
    getTranslation(lang: string): Observable<any> {
        return observableOf(this.getTranslationsForLanguage(lang));
    }

    private getTranslationsForLanguage(lang: string): SingleLanguageTranslationsSet {
        const translation: SingleLanguageTranslationsSet = {};

        Object.keys(ALL_TRANSLATIONS).forEach(section => {
            const srcSection = ALL_TRANSLATIONS[section];
            const destSection: IndexByKey<string> = {};

            Object.keys(srcSection).forEach(key => {
                destSection[key] = srcSection[key][lang];
            });

            translation[section] = destSection;
        });

        return translation;
    }

}
