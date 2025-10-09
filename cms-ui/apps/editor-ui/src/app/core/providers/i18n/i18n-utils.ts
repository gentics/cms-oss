import { Form, Page, PageVersion, TimeManagement, User } from '@gentics/cms-models';
import { TranslateService } from '@ngx-translate/core';
import { from, Observable, of } from 'rxjs';
import { map } from 'rxjs/operators';
import { I18nDatePipe } from '../../../shared/pipes/i18n-date/i18n-date.pipe';
import { FolderActionsService } from '../../../state';
import { I18nService } from './i18n.service';

/**
 * In many places we need to localize the page type and status. This method allows us to use the string `folder`
 * rather than `common.type_folder`. In the case of types, it also allows the use of a `count` param to create
 * the correct pluralized translation key.
 */
export function applyShortcuts(value: string, params?: any): string {
    switch (value) {
        case 'contenttag':
        case 'file':
        case 'folder':
        case 'form':
        case 'image':
        case 'node':
        case 'object':
        case 'page':
        case 'tag':
        case 'template':
        case 'templatetag':
        case 'linkedPage':
        case 'linkedFile':
        case 'linkedImage':
        case 'variant': {
            let key = `common.type_${value}`;
            if (params && params.hasOwnProperty('count') && 1 !== params.count) {
                key += 's';
            }
            if (params && params.hasOwnProperty('dative') && params.dative) {
                key += '_dative';
            }
            return key;
        }

        case 'published':
        case 'edited':
        case 'offline':
        case 'queue':
        case 'timeframe':
        case 'publishat':
            return `common.status_${value}`;

        default:
            return value;
    }
}

export function translateParams(params: {[key: string]: any}, translate: TranslateService): {[key: string]: any} {
    const translated: {[key: string]: any} = {};
    for (const key in params) {
        if (key === '_lang' || key === '_language') {
            translated[key.substr(1)] = translate.instant(`lang.${params[key]}`);
        } else if (key[0] === '_') {
            translated[key.substr(1)] = translateParamValue(params[key], params, translate);
        } else {
            translated[key] = params[key];
        }
    }
    return translated;
}

/**
 * If a param value is one of the common pages, we translate it implicitly.
 */
function translateParamValue(value: any, params: {[key: string]: any}, translate: TranslateService): any {
    const shortCut = applyShortcuts(value, params);
    const toTranslate = shortCut || value;
    if (!toTranslate) {
        return toTranslate;
    }
    return translate.instant(toTranslate);
}

/**
 * @param versions Explicit array property from page object
 * @returns latest version string
 */
export function pageVersionsGetLatest(versions: PageVersion[]): string {
    if (!versions) return;
    if (versions.length === 0) return '0.1';
    return versions[versions.length - 1].number;
}

function formattedUser(user: User): string {
    if (user == null || typeof user !== 'object') {
        return '';
    }
    return `${user.firstName} ${user.lastName}`;
}

/**
 * @param item object to extract timemanegement translation from
 * @param field 'at' | 'offlineAt' | 'queuedPublish' | 'queuedOffline' | 'version'
 * @param currentNodeId unique ID of current node active in UI
 * @param i18n translation service instance
 * @param datePipe date pipe instance
 * @param folderActions service for folder-related API requests
 *
 * @returns human-readable translation of timemanagement properties or FALSE if
 * no timemanagement property is available
 */
export function getFormattedTimeMgmtValue(
    item: Page | Form,
    field: keyof TimeManagement,
    currentNodeId: number,
    i18n: I18nService,
    datePipe: I18nDatePipe,
    folderActions: FolderActionsService,
): Observable<string | boolean> {
    if (!item || !item.timeManagement || !item.timeManagement[field]) {
        return of(false);
    }

    // human-readable and translated scheduled action date value
    let date: string;
    // scheduled page version
    let version: string;
    // user who has set scheduled action
    let user: string;

    function toDateString(dateRaw): string {
        return dateRaw > 0
            ? datePipe.transform(dateRaw, 'dateTime')
            : i18n.translate('editor.publish_queue_date_value_immediately');
    }

    function getCompatiblityPageValue(page: Page): Observable<string> {
        // if version property is already contained
        if (page.versions) {
            version = pageVersionsGetLatest(page.versions);
            return of(i18n.translate('editor.publish_queue_date_version_value_label', {date, user, version}));
        }

        // get page data with version information
        return from(folderActions.getPage(page.id, {
            nodeId: currentNodeId,
            versioninfo: true,
            langvars: true,
        })).pipe(
            map(versionedPage => {
                // if there are mutliple versions, get latest
                if (versionedPage.versions && versionedPage.versions.length > 0) {
                    version = pageVersionsGetLatest(versionedPage.versions);
                } else {
                    console.error('No version information found in page data.');
                    version = 'unknown';
                }
                return i18n.translate('editor.publish_queue_date_version_value_label', {date, user, version});
            }),
        );
    }

    function getCompatiblityFormValue(form: Form): Observable<string> {
        if (form.version) {
            version = form.version.number;
            return of(i18n.translate('editor.publish_queue_date_version_value_label', {date, user, version}));
        }

        // get form data with version information
        return from(folderActions.getForm(form.id, {
            nodeId: currentNodeId,
        })).pipe(
            map(versionedForm => {
                // if there are mutliple versions, get latest
                if (versionedForm.version) {
                    version = versionedForm.version.number;
                } else {
                    console.error('No version information found in form data.');
                    version = 'unknown';
                }
                return i18n.translate('editor.publish_queue_date_version_value_label', {date, user, version});
            }),
        );
    }

    switch (field) {
        // PLANNED: action is scheduled and will we performed
        case 'at': {
            const timeManagement = item.timeManagement;
            date = toDateString(timeManagement.at);

            if (timeManagement.version) {
                version = timeManagement.version.number;
                user = formattedUser(item.timeManagement?.futurePublisher);
                return of(i18n.translate('editor.publish_queue_date_version_value_label', {date, user, version}));
            }

            user = i18n.translate('editor.publish_queue_migration_notice');
            // SPECIAL CASE
            // If there is no ```timeManagement.version``` despite ```timeManagement.at > 0``` in the CMS' response
            // a legal data migration issue occured documented at https://jira.gentics.com/browse/GTXPE-446 .
            // In this case, the latest page version shall be published.

            if (item.type === 'page') {
                return getCompatiblityPageValue(item);
            }

            // Precautious check
            if (item.type !== 'form') {
                // TODO: Can't really happen, but maybe a fallback value?
                return of('');
            }

            return getCompatiblityFormValue(item);
        }

        case 'offlineAt':
            date = toDateString(item.timeManagement.offlineAt);
            user = formattedUser(item.timeManagement?.futureUnpublisher);

            // if no user is available, fallback to date-only
            if (user) {
                return of(i18n.translate('editor.publish_queue_date_value_label', {date, user}));
            } else {
                return of(date);
            }

        // QUEUED: action is requested to be planned but won't be performed until approval
        case 'queuedPublish':
            date = toDateString(item.timeManagement.queuedPublish?.at);
            version = item.timeManagement.queuedPublish?.version?.number || '1.0';
            user = formattedUser(item.timeManagement?.queuedPublish?.user);
            return of(i18n.translate('editor.publish_queue_date_version_value_label', {date, user, version}));

        case 'queuedOffline':
            date = toDateString(item.timeManagement?.queuedOffline?.at);
            user = formattedUser(item.timeManagement?.queuedOffline?.user);
            return of(i18n.translate('editor.publish_queue_date_value_label', {date, user}));

        default:
            return of(false);
    }
}
