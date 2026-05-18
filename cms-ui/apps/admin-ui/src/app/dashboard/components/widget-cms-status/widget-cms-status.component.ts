import { SidebarItemComponent } from '@admin-ui/shared/components/sidebar-item/sidebar-item.component';
import { Component, Input } from '@angular/core';
import { I18nService } from '@gentics/cms-components';
import { Update, Version } from '@gentics/cms-models';
import { Observable } from 'rxjs';
import { filter, map } from 'rxjs/operators';
import * as semver from 'semver';
import { AppStateService, UIStateModel } from '../../../state';

interface CmsUpdates {
    hotfix?: {
        version: string;
        changelog: string;
    };
    feature?: {
        version: string;
        changelog: string;
    };
}

@Component({
    selector: 'gtx-widget-cms-status',
    templateUrl: './widget-cms-status.component.html',
    styleUrls: ['./widget-cms-status.component.scss'],
    standalone: false,
})
export class WidgetCmsStatusComponent extends SidebarItemComponent {
    // CMS Status widget title
    @Input()
    declare title: string;

    currentDocsUrl = 'https://gentics.com/Content.Node/cmp8/guides-history/';
    latestDocsUrl = 'https://gentics.com/Content.Node/cmp8/guides/';

    uiState$: Observable<UIStateModel>;
    updates$: Observable<CmsUpdates>;

    constructor(
        protected i18n: I18nService,
        protected appState: AppStateService,
    ) {
        super();

        this.uiState$ = this.appState.select((state) => state.ui);

        this.updates$ = this.uiState$.pipe(
            filter((state) => !!state.cmpVersion && !!state.cmpVersion.version),
            map((ui) => this.getUpdateVersions(ui.cmsUpdates, ui.cmpVersion)),
        );

        this.uiState$.pipe(
            filter((state) => !!state.cmpVersion && !!state.cmpVersion.version),
            map((ui) => ui.cmpVersion.version),
        ).subscribe((version) => {
            this.currentDocsUrl = `https://gentics.com/Content.Node/cmp8/guides-history/${version}/`;
        });
    }

    getUpdateVersions(updates: Update[], currentVersion: Version): CmsUpdates {
        const cleanCurrent = semver.clean(currentVersion.version);
        let cmsUpdates: CmsUpdates = {};

        updates.forEach((update) => {

            if (semver.satisfies(update.version, `~${semver.coerce(cleanCurrent)}`)) {
                cmsUpdates = {
                    ...cmsUpdates,
                    hotfix: {
                        version: update.version,
                        changelog: update.changelogUrl,
                    },
                };
            }

            if (semver.satisfies(update.version, `>${semver.major(cleanCurrent)}.${semver.minor(cleanCurrent)}`)) {
                cmsUpdates = {
                    ...cmsUpdates,
                    feature: {
                        version: update.version,
                        changelog: update.changelogUrl,
                    },
                };
            }
        });

        return cmsUpdates;
    }
}
