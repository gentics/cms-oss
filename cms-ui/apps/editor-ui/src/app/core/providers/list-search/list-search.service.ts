import { EventEmitter, Injectable } from '@angular/core';
import { isLiveUrl } from '@editor-ui/app/common/utils/is-live-url';
import { ApplicationStateService } from '@editor-ui/app/state/providers/application-state/application-state.service';
import { FolderActionsService } from '@editor-ui/app/state/providers/folder-actions/folder-actions.service';
import { EditMode } from '@gentics/cms-integration-api-models';
import { Node, Page, Raw } from '@gentics/cms-models';
import { Observable, defer, iif, of } from 'rxjs';
import { catchError, mergeMap, take, tap } from 'rxjs/operators';
import { ErrorHandler } from '../error-handler/error-handler.service';
import { I18nNotification } from '../i18n-notification/i18n-notification.service';
import { I18nService } from '../i18n/i18n.service';
import { NavigationService } from '../navigation/navigation.service';
import { QuickJumpService } from '../quick-jump/quick-jump.service';

const patternShortCutSyntaxId = /^(?:jump):(\d+)$/;

@Injectable()
export class ListSearchService {

    /** Notifying subscribers that search has been executed */
    searchEvent$ = new EventEmitter<{ term: string, nodeId?: number }>(null);

    constructor(
        private errorHandler: ErrorHandler,
        private folderActions: FolderActionsService,
        private navigationService: NavigationService,
        private quickJumpService: QuickJumpService,
        private state: ApplicationStateService,
        private i18n: I18nService,
        private notification: I18nNotification,
    ) { }

    /** Search for a string in the folder & node currently active in the item list. */
    search(term: string, nodeId?: number): void {
        const activeNode = this.state.now.entities.node[nodeId || this.state.now.folder.activeNode];
        const hosts = Object.values(this.state.now.entities.node).map((node: Node) => node.host);
        const jumpId = patternShortCutSyntaxId.exec(term);

        if (term && activeNode && isLiveUrl(term, hosts)) {
            this.searchLiveUrl(term).pipe(
                take(1),
            ).subscribe();
        } else if (jumpId) {
            // extract number from shortcut syntax
            const entityId = parseInt(jumpId[1], 10);
            this.searchPageId(entityId, nodeId || this.state.now.folder.activeNode);
        } else {
            this.folderActions.setFilterTerm('');
            this.folderActions.setSearchTerm(term);
        }
        this.searchEvent$.emit({ term, ...(nodeId && { nodeId }) });
    }

    searchLiveUrl(liveUrl: string, hosts: string[] = []): Observable<Page<Raw> | undefined> {
        const fallbackLiveUrl = liveUrl.startsWith('www.') ? liveUrl.substring(4) : `www.${liveUrl}`;

        return this.folderActions.searchLiveUrl(liveUrl).pipe(
            take(1),
            mergeMap((page: Page<Raw> | undefined) =>
                iif(() => page === undefined && isLiveUrl(fallbackLiveUrl, hosts),
                    defer(() => this.folderActions.searchLiveUrl(fallbackLiveUrl)),
                    of(page).pipe(take(1))),
            ),
            tap((page: Page<Raw> | undefined) => {
                this.folderActions.setFilterTerm('');

                if (page) {
                    this.navigationService.instruction({
                        list: {
                            nodeId: page.inheritedFromId,
                            folderId: page.folderId,
                        },
                        detail: {
                            nodeId: page.inheritedFromId,
                            itemType: 'page',
                            itemId: page.id,
                            editMode: EditMode.PREVIEW,
                        },
                    }).navigate();
                    return;
                }

                this.notification.show({
                    message: 'message.page_liveurl_not_found',
                    translationParams: { url: liveUrl.replace(/^https?:\/\//, '') },
                });
            }),
            catchError(error => {
                this.errorHandler.catch(error);
                return of(null);
            }),
        );
    }

    searchPageId(id: number, nodeId: number): Promise<void> {
        return this.quickJumpService.searchPageById(id, nodeId)
            .then(result => {
                if (!result) {
                    this.errorHandler.catch(
                        new Error(this.i18n.translate('editor.no_page_with_id', { id })),
                        { notification: true },
                    );
                    return;
                }

                // If the page entity is not already in the state, fetch it to get the folder id
                const loadedPage = this.state.now.entities.page[result.id];
                const pageReq: Promise<Page> = loadedPage ? Promise.resolve(loadedPage) : this.folderActions.getPage(result.id, { nodeId: result.nodeId });
                return pageReq.then(page => ({
                    page,
                    nodeId: result.nodeId,
                }));
            })
            .then(page => {
                if (!page) {
                    return;
                }

                this.folderActions.setFilterTerm('');
                if (!page) {
                    return this.folderActions.setSearchTerm(id.toString());
                }

                this.navigationService.instruction({
                    list: {
                        nodeId: page.nodeId,
                        folderId: page.page.folderId,
                    },
                    detail: {
                        nodeId: page.nodeId,
                        itemType: 'page',
                        itemId: page.page.id,
                        editMode: EditMode.PREVIEW,
                    },
                }).navigate();
            });
    }

}
