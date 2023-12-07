
import { Component, Input, OnInit } from '@angular/core';
import { PublishedState, SchemaElement, SchemaElementVersion } from '../../models/mesh-browser-models';

@Component({
    selector: 'gtx-mesh-browser-content-version',
    templateUrl: './mesh-browser-content-version.component.html',
    styleUrls: ['./mesh-browser-content-version.component.scss'],
})
export class MeshBrowserContentVersionComponent implements OnInit {

    @Input()
    public schemaElement: SchemaElement;

    public publishedState: PublishedState;

    public title = '';

    constructor() { }

    ngOnInit(): void {
        this.publishedState = this.getPublishedState();

        this.title = `version: ${this.schemaElement?.version}`;

        if (this.schemaElement?.versions?.length > 0) {
            const created = this.schemaElement.versions[0].created;
            const createdDate = new Date(created).toLocaleString()
            this.title += ` last modified at: ${createdDate}`;
        }
    }


    getPublishedState(): PublishedState {
        if (this.schemaElement.isPublished) {
            return PublishedState.PUBLISHED;
        }

        if (this.schemaElement?.versions?.length > 0) {
            const version = this.schemaElement.versions[0];
            const isDraft = version.draft

            if (isDraft) {
                if (this.getPublishedVersion()) {
                    return PublishedState.UPDATED;
                }

                return PublishedState.DRAFT;
            }

            if (version.published) {
                return PublishedState.PUBLISHED;
            }
        }

        return PublishedState.ARCHIVED;
    }


    private getPublishedVersion(): SchemaElementVersion {
        for (const version of this.schemaElement.versions) {
            if (version.published) {
                return version;
            }
        }

        return null;
    }


}
