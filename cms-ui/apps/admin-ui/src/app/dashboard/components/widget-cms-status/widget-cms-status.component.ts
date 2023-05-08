import { GUIDES_URL } from '@admin-ui/common';
import { SidebarItemComponent } from '@admin-ui/shared/components/sidebar-item/sidebar-item.component';
import { Component, Input } from '@angular/core';
import { Observable } from 'rxjs';
import { filter, map } from 'rxjs/operators';
import { I18nService } from '../../../core/providers/i18n/i18n.service';
import { AppStateService } from '../../../state';

import * as semver from 'semver';

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
})
export class WidgetCmsStatusComponent extends SidebarItemComponent {
    // CMS Status widget title
    @Input() title = this.i18n.get('widget.cms_status_title');

    currentDocsUrl = GUIDES_URL;
    latestDocsUrl = `https://gentics.com/Content.Node/guides/`;
    changelogTpl = `https://gentics.com/Content.Node/changelog/{major}.{minor}.0/{major}.{minor}.{patch}.html`;
    changelogFrTpl = `https://gentics.com/Content.Node/changelog/{major}.{minor}.0/merged_changelog.html`;

    uiState$ = this.appState.select(state => state.ui);
    updates$: Observable<CmsUpdates>;

    constructor(
        protected i18n: I18nService,
        protected appState: AppStateService,
    ) {
        super(i18n);

        this.updates$ = this.uiState$.pipe(
            filter(state => !!state.cmpVersion && !!state.cmpVersion.version),
            map(ui => this.getUpdateVersions(ui.cmsUpdates, ui.cmpVersion.version)),
        );
    }

    getUpdateVersions(updates: string[], currentVersion: string): CmsUpdates {
        const _updates = semver.sort([...updates]);
        const cleanCurrent = semver.clean(currentVersion);
        let cmsUpdates: CmsUpdates = {};

        _updates.forEach(version => {
            if (semver.satisfies(version, '~' + semver.coerce(cleanCurrent))) {
                cmsUpdates = {
                    ...cmsUpdates,
                    hotfix: {
                        version,
                        changelog: this.replaceUrlTemplate(version, this.changelogTpl),
                    },
                };
            }

            if (semver.satisfies(version, '>' + semver.major(cleanCurrent) + '.' + semver.minor(cleanCurrent))) {
                cmsUpdates = {
                    ...cmsUpdates,
                    feature: {
                        version,
                        changelog: this.replaceUrlTemplate(version, this.changelogFrTpl),
                    },
                };
            }
        });

        return cmsUpdates;
    }

    replaceUrlTemplate(version: string, template: string): string {
        return template.replace(/\{major\}/g, semver.major(version).toString())
                       .replace(/\{minor\}/g, semver.minor(version).toString())
                       .replace(/\{patch\}/g, semver.patch(version).toString());
    }
}
