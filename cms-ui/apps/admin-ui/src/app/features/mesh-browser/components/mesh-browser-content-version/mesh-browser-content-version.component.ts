
import { Component, Input, OnInit } from '@angular/core';
import { I18nService } from '@admin-ui/core';
import { PublishedState, SchemaElement } from '../../models/mesh-browser-models';

@Component({
    selector: 'gtx-mesh-browser-content-version',
    templateUrl: './mesh-browser-content-version.component.html',
    styleUrls: ['./mesh-browser-content-version.component.scss'],
})
export class MeshBrowserContentVersionComponent implements OnInit {

    @Input()
    public schemaElement: SchemaElement;

    public readonly PublishedState = PublishedState;

    public publishedState: PublishedState;

    public readonly PublishedStateTranslations = new Map<string, string>([
        [PublishedState.PUBLISHED, 'mesh.published_state_published'],
        [PublishedState.ARCHIVED, 'mesh.published_state_archived'],
        [PublishedState.DRAFT, 'mesh.published_state_draft'],
        [PublishedState.UPDATED, 'mesh.published_state_updated'],
    ]);

    public title = '';

    constructor(protected i18n: I18nService) { }


    ngOnInit(): void {
        this.publishedState = this.getPublishedState();
        this.title = `Version: ${this.schemaElement?.version}\n`;

        if (this.schemaElement?.versions?.length > 0) {
            const created = this.schemaElement.versions[0].created;
            const createdDate = new Date(created).toLocaleString()
            this.title += `${this.i18n.instant('mesh.published_state_changed')}: ${createdDate}`;
        }
    }


    private getPublishedState(): PublishedState {
        if (this.schemaElement.isPublished) {
            return PublishedState.PUBLISHED;
        }

        if (this.schemaElement?.versions?.length > 0) {
            const version = this.schemaElement.versions[0];
            const isDraft = version.draft

            if (isDraft) {
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
        }

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
