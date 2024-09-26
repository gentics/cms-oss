import { Form, FormRequestOptions, Page, PageRequestOptions, PageVersion, TimeManagement, User } from '@gentics/cms-models';
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
            if (params && params.hasOwnProperty('count') && 1 != params.count) {
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
    let translated: {[key: string]: any} = {};
    for (let key in params) {
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

/**
 * @param item object to extract timemanegement translation from
 * @param field 'at' | 'offlineAt' | 'queuedPublish' | 'queuedOffline' | 'version'
 * @param i18n translation service instance
 * @param currentNodeId unique ID of current node active in UI
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
    if (!item || !item.timeManagement || !item.timeManagement[field]) return of(false);

    // unix timestamp of scheduled action date value
    let dateRaw: number;
    // human-readable and translated scheduled action date value
    let date: string;
    // scheduled page version
    let version: string;
    // user who has set scheduled action
    let user: string;
    // final translation of scheduled action information
    let translation: string;

    switch (field) {
        // PLANNED: action is scheduled and will we performed
        case 'at': {
            dateRaw = item.timeManagement.at;
            date = dateRaw > 0 ? datePipe.transform(dateRaw, 'dateTime') : i18n.translate('editor.publish_queue_date_value_immediately');
            const timeManagement = item.timeManagement;
            if (timeManagement.version) {
                version = timeManagement.version.number;
                user = `${item.timeManagement?.futurePublisher.firstName} ${item.timeManagement?.futurePublisher.lastName}`;
                translation = i18n.translate('editor.publish_queue_date_version_value_label', {date, user, version});
            } else {
                user = i18n.translate('editor.publish_queue_migration_notice');
                // SPECIAL CASE
                // If there is no ```timeManagement.version``` despite ```timeManagement.at > 0``` in the CMS' response
                // a legal data migration issue occured documented at https://jira.gentics.com/browse/GTXPE-446 .
                // In this case, the latest page version shall be published.

                // if version property is already contained
                if (item.type === 'page' && item.versions) {
                    version = pageVersionsGetLatest(item.versions);
                    return of(i18n.translate('editor.publish_queue_date_version_value_label', {date, user, version}));
                }
                if (item.type === 'form' && item.version) {
                    version = item.version.number;
                    return of(i18n.translate('editor.publish_queue_date_version_value_label', {date, user, version}));
                }

                // otherwise fetch it
                const pageOptions: PageRequestOptions = {
                    nodeId: currentNodeId,
                    versioninfo: true,
                    langvars: true,
                };
                const formOptions: FormRequestOptions = {
                    nodeId: currentNodeId,
                };

                if (item.type === 'page') {
                    // get page data with version information
                    return from(folderActions.getPage(item.id, pageOptions)).pipe(
                        map(versionedPage => {
                            // if there are mutliple versions, get latest
                            if (versionedPage.versions && versionedPage.versions.length > 0) {
                                version = pageVersionsGetLatest(versionedPage.versions);
                            } else {
                                throw new Error('No version information found in page data.');
                            }
                            return i18n.translate('editor.publish_queue_date_version_value_label', {date, user, version});
                        }),
                    );
                }
                if (item.type === 'form') {
                    // get form data with version information
                    return from(folderActions.getForm(item.id, formOptions)).pipe(
                        map(versionedForm => {
                            // if there are mutliple versions, get latest
                            if (versionedForm.version) {
                                version = versionedForm.version.number;
                            } else {
                                throw new Error('No version information found in form data.');
                            }
                            return i18n.translate('editor.publish_queue_date_version_value_label', {date, user, version});
                        }),
                    );
                }
            }
            break;
        }

        case 'offlineAt':
            dateRaw = item.timeManagement.offlineAt;
            date = dateRaw > 0 ? datePipe.transform(dateRaw, 'dateTime') : i18n.translate('editor.publish_queue_date_value_immediately');
            user = item.timeManagement.futureUnpublisher && `${item.timeManagement.futureUnpublisher.firstName} ${item.timeManagement.futureUnpublisher.lastName}`;

            // if no user is available, fallback to date-only
            if (user) {
                translation = i18n.translate('editor.publish_queue_date_value_label', {date, user})
            } else {
                translation = date;
            }
            break;

        // QUEUED: action is requested to be planned but won't be performed until approval
        case 'queuedPublish':
            dateRaw = item.timeManagement.queuedPublish['at'];
            date = dateRaw > 0 ? datePipe.transform(dateRaw, 'dateTime') : i18n.translate('editor.publish_queue_date_value_immediately');
            version = item.timeManagement.queuedPublish['version'] ? item.timeManagement.queuedPublish['version']['number'] : '1.0';
            user = item.timeManagement.queuedPublish['user']['firstName'] + ' ' + item.timeManagement.queuedPublish['user']['lastName'];
            translation = i18n.translate('editor.publish_queue_date_version_value_label', {date, user, version});
            break;

        case 'queuedOffline':
            dateRaw = item.timeManagement.queuedOffline['at'];
            date = dateRaw > 0 ? datePipe.transform(dateRaw, 'dateTime') : i18n.translate('editor.publish_queue_date_value_immediately');
            user = item.timeManagement.queuedOffline['user']['firstName'] + ' ' + item.timeManagement.queuedOffline['user']['lastName'];
            translation = i18n.translate('editor.publish_queue_date_value_label', {date, user})
            break;

        default:
            return of(false);
    }

    return of(translation);
}
