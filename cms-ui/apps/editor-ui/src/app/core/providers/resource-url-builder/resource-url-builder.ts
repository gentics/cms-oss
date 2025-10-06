import { Injectable } from '@angular/core';
import { EditMode } from '@gentics/cms-integration-api-models';
import { File as FileModel, Folder, Form, Image, Node, Page } from '@gentics/cms-models';
import { EditorState } from '../../../common/models';
import { ALOHAPAGE_URL, API_BASE_URL, IMAGESTORE_URL } from '../../../common/utils/base-urls';
import { BLANK_PAGE, BLANK_PROPERTIES_PAGE } from '../../../content-frame/models/content-frame';
import { ApplicationStateService } from '../../../state/providers/application-state/application-state.service';

/**
 * Service which returns URLs to non-REST resources such as Alohapages or Images.
 */
@Injectable()
export class ResourceUrlBuilder {

    private sid: number;

    constructor(appState: ApplicationStateService) {
        appState.select(state => state.auth.sid)
            .subscribe(sid => this.sid = sid);
    }

    /**
     * Returns the URL for an Alohapage, which should be used as the source of an iframe.
     */
    pageEditor(pageId: number, nodeId: number): string {
        const params = this.createParamsString({
            real: 'newedit',
            realid: pageId,
            nodeid: nodeId,
        });
        return `${ALOHAPAGE_URL}?${params}`;
    }

    /**
     * Returns a URL to a live page preview.
     */
    pagePreview(pageId: number, nodeId: number): string {
        const params = this.createParamsString({
            real: 'newview',
            realid: pageId,
            nodeid: nodeId,
        });
        return `${ALOHAPAGE_URL}?${params}`;
    }

    /**
     * Returns the URL of an image thumbnail
     * if the given file type is resizable by gentics image store a url with resizing is returned
     * if no or an incompatible file type is is given, the url to the full sized original image is returned
     */
    imageThumbnail(imageId: number, width: number, height: number, nodeId: number, changeDate?: number, fileType?: string): string {
        // Append a string that changes when the image is updated to force the change detection to re-request the image.
        const cacheBust = changeDate ? String(changeDate) : Math.random().toString(36).substr(5);

        const data = {
            cachebust: cacheBust,
        };
        if (nodeId) {
            data['nodeId'] = nodeId;
        }

        const params = this.createParamsString(data);

        return `${IMAGESTORE_URL}/${Math.round(width)}/${Math.round(height)}/prop${API_BASE_URL}/file/content/load/${imageId}?${params}`;
    }

    /**
     * Returns the full-size URL of an image.
     */
    imageFullsize(imageId: number, nodeId: number, changeDate?: number): string {
        const cacheBust = changeDate ? String(changeDate) : Math.random().toString(36).substr(5);

        const data = {
            cachebust: cacheBust,
        };
        if (nodeId) {
            data['nodeId'] = nodeId;
        }
        const params = this.createParamsString(data);

        return `${API_BASE_URL}/file/content/load/${imageId}?${params}`;
    }

    /**
     * Returns the URL for downloading a file.
     */
    fileDownload(fileId: number, nodeId: number): string {
        const data = {};
        if (nodeId) {
            data['nodeId'] = nodeId;
        }
        const params = this.createParamsString(data);
        return `${API_BASE_URL}/file/content/load/${fileId}?${params}`;
    }


    /**
     * Returns the URL of the live page preview of a specific version.
     */
    previewPageVersion(nodeId: number, pageId: number, versionTimestamp: number): string {
        const params = this.createParamsString({
            version: versionTimestamp,
            nodeId: nodeId,
        });
        return `${API_BASE_URL}/page/render/content/${pageId}?${params}`;
    }

    /**
     * Returns the URL of the version-comparison page.
     */
    comparePageVersions(nodeId: number, pageId: number, oldTimestamp: number, newTimestamp: number): string {
        const params = this.createParamsString({
            old: oldTimestamp,
            new: newTimestamp,
            nodeId: nodeId,
            source: false,
            daisyDiff: true,
        });
        return `${API_BASE_URL}/page/diff/versions/${pageId}?${params}`;
    }

    /**
     * Returns the URL of the sourcecode-version-comparison page.
     */
    comparePageVersionSources(nodeId: number, pageId: number, oldTimestamp: number, newTimestamp: number): string {
        const params = this.createParamsString({
            old: oldTimestamp,
            new: newTimestamp,
            nodeId: nodeId,
            source: true,
            daisyDiff: true,
        });
        return `${API_BASE_URL}/page/diff/versions/${pageId}?${params}`;
    }

    /**
     * This URL will perform a cross-node search for pages matching the term (either name or ID). It returns an HTML
     * snippet containing the matches. Used for "quick jump" functionality.
     */
    autocomplete(term: string | number): string {
        const params = this.createParamsString({
            q: term,
            limit: 15,
        });
        return `${API_BASE_URL}/page/autocomplete?${params}`;
    }

    /**
     * Returns a GCMS url to act as the src of the iframe depending on the editMode and
     * type of item being edited.
     */
    stateToUrl(editorState: EditorState, currentItem: Page | FileModel | Folder | Form | Image | Node): string {
        if (!editorState.editorIsOpen) {
            return BLANK_PAGE;
        }

        const itemId = currentItem.id;
        const nodeId = editorState.nodeId;

        switch (editorState.editMode) {
            case EditMode.PREVIEW:
            case EditMode.EDIT:
            case EditMode.EDIT_INHERITANCE:
                if (editorState.itemType !== 'page') {
                    break;
                }
                if (editorState.editMode === EditMode.PREVIEW) {
                    return this.pagePreview(itemId, nodeId);
                } else {
                    return this.pageEditor(itemId, nodeId);
                }

            case EditMode.EDIT_PROPERTIES:
                return BLANK_PROPERTIES_PAGE;

            case EditMode.PREVIEW_VERSION:
                return this.previewPageVersion(nodeId, itemId, editorState.version.timestamp);

            case EditMode.COMPARE_VERSION_CONTENTS:
                return this.comparePageVersions(
                    nodeId,
                    itemId,
                    editorState.oldVersion.timestamp,
                    editorState.version.timestamp,
                );

            case EditMode.COMPARE_VERSION_SOURCES:
                return this.comparePageVersionSources(
                    nodeId,
                    itemId,
                    editorState.oldVersion.timestamp,
                    editorState.version.timestamp,
                );
        }

        return BLANK_PAGE;
    }

    private createParamsString(data: Record<string, any>): string {
        const params = new URLSearchParams();
        params.set('sid', `${this.sid}`);
        if (data) {
            Object.entries(data).forEach(([key, value]) => {
                // eslint-disable-next-line @typescript-eslint/restrict-template-expressions
                params.set(key, `${value}`);
            })
        }

        return params.toString();
    }
}
