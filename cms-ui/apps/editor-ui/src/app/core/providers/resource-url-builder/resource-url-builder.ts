import { Injectable } from '@angular/core';
import { ItemType } from '@gentics/cms-models';
import { ALOHAPAGE_URL, API_BASE_URL, CONTENTNODE_URL, IMAGESTORE_URL } from '../../../common/utils/base-urls';
import { ApplicationStateService } from '../../../state';

/**
 * Service which returns URLs to non-REST resources such as Alohapages or Images.
 *
 * TODO: Do we need to support http://www.gentics.com/Content.Node/guides/feature_context.html?
 */
@Injectable()
export class ResourceUrlBuilder {

    private sid: number;

    constructor(private appState: ApplicationStateService) {
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
     * Returns the URL of the image editing page.
     */
    imageEditor(imageId: number, folderId: number): string {
        const params = this.createParamsString({
            do: '15010',
            type: 'img',
            folder_id: folderId,
            id: imageId,
        });
        return `${CONTENTNODE_URL}/?${params}`;
    }

    /**
     * Returns the URL of the tagfill for editing a specific object property.
     */
    objectPropertyTagfill(tagId: number, itemId: number, folderId: number, nodeId: number, type: ItemType, tagContainsOverview: boolean): string {
        // images should be specified by FILE_ID, rather than IMAGE_ID
        const ucType = type === 'image' ? 'FILE' : type.toUpperCase();

        // If the tag contains a overvie TagPart, we need to use a different URL.
        const doId = !tagContainsOverview ? '10008' : '17001';

        const data = {
            do: doId,
            id: tagId,
            NODE_ID: nodeId,
            type: type === 'template' ? 'template' : 'obj',
            [`${ucType}_ID`]: itemId,
            keepsid: 1,
            newui: 'true',
            combpropeditor: 'true',
        };

        if (type !== 'folder' && type !== 'template' && typeof folderId === 'number') {
            data['FOLDER_ID'] = folderId;
        }

        const params = this.createParamsString(data);

        return `${CONTENTNODE_URL}/?${params}`;
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
     * Returns the URL of the split-screen language comparison page.
     */
    comparePageLanguages(nodeId: number, pageId: number, languageVariantId: number): string {
        const params = this.createParamsString({
            do: '14020',
            cmd: 'cmp',
            diff_page_id: languageVariantId,
            PAGE_ID: pageId,
            NODE_ID: nodeId,
            newui: 'true',
        });
        return `${CONTENTNODE_URL}/?${params}`;
    }

    /**
     * Returns the URL of the live page preview of a specific version.
     */
    previewPageVersion(nodeId: number, pageId: number, versionTimestamp: number): string {
        const params = this.createParamsString({
            do: '14001',
            live: pageId,
            version: versionTimestamp,
            PAGE_ID: pageId,
            NODE_ID: nodeId,
        });
        return `${CONTENTNODE_URL}/?${params}`;
    }

    /**
     * Returns the URL of the version-comparison page.
     */
    comparePageVersions(nodeId: number, pageId: number, oldTimestamp: number, newTimestamp: number): string {
        const params = this.createParamsString({
            do: '14016',
            firstdate: oldTimestamp,
            lastdate: newTimestamp,
            PAGE_ID: pageId,
            NODE_ID: nodeId,
            newui: 'true',
        });
        return `${CONTENTNODE_URL}/?${params}`;
    }

    /**
     * Returns the URL of the sourcecode-version-comparison page.
     */
    comparePageVersionSources(nodeId: number, pageId: number, oldTimestamp: number, newTimestamp: number): string {
        const params = this.createParamsString({
            do: '14016',
            firstdate: oldTimestamp,
            lastdate: newTimestamp,
            PAGE_ID: pageId,
            NODE_ID: nodeId,
            diffsource: 1,
            newui: 'true',
        });
        return `${CONTENTNODE_URL}/?${params}`;
    }

    /**
     * When a file has been selected in the tagfill dialog, the dialog window location must be updated to a particular
     * URL which tells the backend to use the specified file in the dialog.
     */
    useFileInTagFill(fileId: number, fieldToUpdate: string, formActionUrl: string): string {
        const idField = fieldToUpdate.match(/\d+/)[0];
        const doParamMatches = formActionUrl.match(/[?&]do=(\d+)/);
        const doParam = doParamMatches ? doParamMatches[1] : '10008';

        const params = this.createParamsString({
            do: doParam,
            new_mnbr: `f_p${idField}_a`,
            faction: 'Übernehmen',
            [`m_p${idField}_a`]: fileId,
        })

        return `${CONTENTNODE_URL}/?${params}`;
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

    private isImageStoreResizableFileType(fileType: string): boolean {
        const imageStoreResizeableFileTypes = [
            'image/jpg',
            'image/jpeg',
            'image/png',
        ];
        return imageStoreResizeableFileTypes.indexOf(fileType) !== -1;
    }
}
