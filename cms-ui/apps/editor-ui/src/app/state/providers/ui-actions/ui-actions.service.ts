import { Injectable } from '@angular/core';
import { ExternalLinkStatistics, GcmsUiLanguage, GtxVersion, I18nLanguage, NodeFeature } from '@gentics/cms-models';
import { of } from 'rxjs';
import { filter, map, switchMap, take, tap } from 'rxjs/operators';
import { Api } from '../../../core/providers/api/api.service';
import { I18nService } from '../../../core/providers/i18n/i18n.service';
import {
    BreadcrumbLocation,
    SetAvailableUILanguageAction,
    SetBackendLanguageAction,
    SetBreadcrumbExpandedAction,
    SetBrokenLinksCountAction,
    SetCMPVersionAction,
    SetUILanguageAction,
    SetUIVersionAction,
    SetUsersnapSettingsAction,
} from '../../modules';
import { ApplicationStateService } from '../../providers';

// This var is provided by the Webpack DefinePlugin and gets
// swapped out for a hard-coded string at build-time. See webpack.config.js.
declare let GCMS_VERSION: string;

@Injectable()
export class UIActionsService {

    constructor(
        private appState: ApplicationStateService,
        private api: Api,
        private i18n: I18nService,
    ) { }

    setContentFrameBreadcrumbsExpanded(isExpanded: boolean): void {
        this.appState.dispatch(new SetBreadcrumbExpandedAction(BreadcrumbLocation.CONTENT_FRAME, isExpanded));
    }

    setItemListBreadcrumbsExpanded(isExpanded: boolean): void {
        this.appState.dispatch(new SetBreadcrumbExpandedAction(BreadcrumbLocation.ITEM_LIST, isExpanded));
    }

    setRepositoryBrowserBreadcrumbsExpanded(isExpanded: boolean): void {
        this.appState.dispatch(new SetBreadcrumbExpandedAction(BreadcrumbLocation.CONTENT_REPOSITORY, isExpanded));
    }

    setActiveUiLanguageInFrontend(language: GcmsUiLanguage): void {
        this.appState.dispatch(new SetUILanguageAction(language));
        // set lang in translation module
        this.i18n.setLanguage(language);
    }

    /**
     * Change UI langauge in backend and frontend application.
     *
     * __Important Note__: For frontend language fully changed, frontend application must reload.
     * Thus, call `windowRef.nativeWindow.location.reload()` in `callback` argument.
     *
     * @param language new ui language
     * @param callBack optional callback after successful request
     */
    setActiveUiLanguageInBackend(language: GcmsUiLanguage, callBack?: () => void): void {
        this.api.i18n.setActiveUiLanguage({ code: language }).subscribe(() => {
            this.appState.dispatch(new SetBackendLanguageAction(language));
            callBack?.();
        });
    }

    getActiveUiLanguage(): void {
        this.api.i18n.getActiveUiLanguage().pipe(
            map(response => response.code),
        ).subscribe((language: GcmsUiLanguage) => {
            this.appState.dispatch(new SetBackendLanguageAction(language));
            this.setActiveUiLanguageInFrontend(language);
        });
    }

    getAvailableUiLanguages(): void {
        this.api.i18n.getAvailableUiLanguages().pipe(
            map(response => response.items),
        ).subscribe((languages: I18nLanguage[]) => {
            this.appState.dispatch(new SetAvailableUILanguageAction(languages));
        });
    }

    getAlerts(): void {
        const featureLinkcheckerEnabled$ = this.appState.select(state => state.features.nodeFeatures).pipe(
            filter(nodeFeatures => nodeFeatures instanceof Object),
            // check if at least one node has NodeFeature.linkChecker activated
            map(nodeFeatures => {
                return Object.values(nodeFeatures).some(nodeFeaturesOfNode => {
                    return nodeFeaturesOfNode.find(feature => feature === NodeFeature.LINK_CHECKER) ? true : false;
                });
            }),
            filter(status => status),
            take(1),
        );

        featureLinkcheckerEnabled$.subscribe(linkCheckerEnabled => {
            // Only check for alerts of Link Checker if at least one Node has this enabled
            if (linkCheckerEnabled) {
                this.getAlertLinkCheckerBrokenLinks();
            }
        });
    }

    getAlertLinkCheckerBrokenLinks(): void {
        this.appState.select(state => state.folder.activeNode).pipe(
            switchMap(activeNodeId => this.appState.select(state => state.features.nodeFeatures[activeNodeId])),
            map((activeNodeFeatures: NodeFeature[]) => activeNodeFeatures && !activeNodeFeatures.some(f => f === NodeFeature.LINK_CHECKER)),
            switchMap((linkCheckerIsEnabled: boolean) => {
                if (!linkCheckerIsEnabled) {
                    return of<ExternalLinkStatistics>();
                }

                return this.api.linkChecker.getStats();
            }),
            filter(stats => !!stats),
            tap(stats => {
                this.appState.dispatch(new SetBrokenLinksCountAction(stats.invalid));
            }),
        ).toPromise();
    }

    getCmsVersion(): void {
        this.api.admin.getVersion().subscribe(res => {
            const version: GtxVersion = {
                cmpVersion: res.cmpVersion,
                version: res.version,
                nodeInfo: res.nodeInfo,
            };
            this.appState.dispatch(new SetCMPVersionAction(version));
        }, err => {
            console.error('Error while loading CMS Version!', err);
        });
    }

    getUiVersion(): void {
        this.appState.dispatch(new SetUIVersionAction(GCMS_VERSION));
    }

    getUsersnapSettings(): void {
        this.api.admin.getUsersnapSettings().subscribe(res => {
            this.appState.dispatch(new SetUsersnapSettingsAction(res.settings));
        });
    }
}
