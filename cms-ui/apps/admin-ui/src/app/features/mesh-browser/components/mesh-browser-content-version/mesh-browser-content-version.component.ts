
import { I18nService } from '@admin-ui/core';
import { Component, Input, OnInit } from '@angular/core';
import { GtxI18nDatePipe } from '@gentics/cms-components';
import { PublishedState, SchemaElement } from '../../models/mesh-browser-models';

const ICONS: Record<PublishedState, string> = {
    [PublishedState.PUBLISHED]: 'cloud',
    [PublishedState.ARCHIVED]: 'cloud',
    [PublishedState.DRAFT]: 'cloud_off',
    [PublishedState.UPDATED]: 'cloud_upload',
};

const TRANSLATIONS: Record<PublishedState, string> = {
    [PublishedState.PUBLISHED]: 'mesh.published_state_published',
    [PublishedState.ARCHIVED]: 'mesh.published_state_archived',
    [PublishedState.DRAFT]: 'mesh.published_state_draft',
    [PublishedState.UPDATED]: 'mesh.published_state_updated',
};
@Component({
    selector: 'gtx-mesh-browser-content-version',
    templateUrl: './mesh-browser-content-version.component.html',
    styleUrls: ['./mesh-browser-content-version.component.scss'],
    standalone: false
})
export class MeshBrowserContentVersionComponent implements OnInit {

    @Input()
    public schemaElement: SchemaElement;

    public currentState: PublishedState;

    public title = '';
    public text: string;
    public icon: string;

    constructor(
        protected i18n: I18nService,
        protected i18nDate: GtxI18nDatePipe,
    ) { }

    ngOnInit(): void {
        this.currentState = this.getPublishedState();
        this.text = TRANSLATIONS[this.currentState];
        this.icon = ICONS[this.currentState];
        this.title = this.i18n.instant('mesh.version_of', { version: this.schemaElement?.version });

        if (this.schemaElement?.versions?.length > 0) {
            const created = this.schemaElement.versions[0].created;
            const createdDate = this.i18nDate.transform(new Date(created), 'dateTime');
            this.title += '\n' + this.i18n.instant('mesh.published_state_changed', { date: createdDate });
        }
    }

    private getPublishedState(): PublishedState {
        if (this.schemaElement.isPublished) {
            return PublishedState.PUBLISHED;
        }

        const version = this.schemaElement?.versions?.[0];

        if (!version) {
            return PublishedState.DRAFT;
        }

        if (version.draft) {
            if (this.hasPublishedVersion()) {
                return PublishedState.UPDATED;
            }
            if (RegExp('.0$').exec(version.version)) {
                // no published but major version => ARCHIVED
                return PublishedState.ARCHIVED
            }

            return PublishedState.DRAFT;
        }

        if (version.published) {
            return PublishedState.PUBLISHED;
        }

        // Should never reach, but just in case
        return PublishedState.DRAFT;
    }

    private hasPublishedVersion(): boolean {
        for (const version of this.schemaElement.versions) {
            if (version.published) {
                return true;
            }
        }

        return false;
    }
}
