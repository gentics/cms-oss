
import { Component, Input, OnInit } from '@angular/core';
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
