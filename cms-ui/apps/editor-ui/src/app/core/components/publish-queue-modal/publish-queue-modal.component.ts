import { ChangeDetectionStrategy, Component, OnDestroy, OnInit } from '@angular/core';
import { EditMode } from '@gentics/cms-integration-api-models';
import { Page, PageRequestOptions } from '@gentics/cms-models';
import { BaseModal } from '@gentics/ui-core';
import { Observable, Subscription } from 'rxjs';
import { map } from 'rxjs/operators';
import { ApplicationStateService, FolderActionsService, PublishQueueActionsService } from '../../../state';
import { Api } from '../../providers/api/api.service';
import { EntityResolver } from '../../providers/entity-resolver/entity-resolver';
import { ErrorHandler } from '../../providers/error-handler/error-handler.service';
import { NavigationService } from '../../providers/navigation/navigation.service';

@Component({
    selector: 'publish-queue',
    templateUrl: './publish-queue-modal.component.html',
    styleUrls: ['./publish-queue-modal.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PublishQueueModal extends BaseModal<void | Page[]> implements OnInit, OnDestroy {

    queue$: Observable<Page[]>;
    loading$: Observable<boolean>;
    selectedPages: Page[] = [];

    private subscription: Subscription;

    constructor(
        private appState: ApplicationStateService,
        private navigationService: NavigationService,
        private api: Api,
        private errorHandler: ErrorHandler,
        private publishQueueActions: PublishQueueActionsService,
        private folderActions: FolderActionsService,
        private entityResolver: EntityResolver,
    ) {
        super();
    }

    ngOnInit(): void {
        this.publishQueueActions.getQueue();
        this.loading$ = this.appState.select(state => state.publishQueue.fetching);
        this.queue$ = this.appState.select(state => state.publishQueue.pages.list).pipe(
            map(pages => pages.map(id => this.entityResolver.getPage(id))),
        );
    }

    ngOnDestroy(): void {
        if (this.subscription) {
            this.subscription.unsubscribe();
        }
    }

    /**
     * When a page is clicked we need to fetch the version history so we can display the diff view.
     */
    pageClicked(page: Page): void {
        const nodeId = page.inheritedFromId;
        const options: PageRequestOptions = {
            nodeId,
            versioninfo: true,
        };

        this.api.folders.getItem(page.id, 'page', options).toPromise()
            .then(res => {
                const pageWithVersion: Page = res.page;
                const oldVersion = this.getLastVersion(pageWithVersion);
                const version = pageWithVersion.currentVersion;

                this.closeFn(null);

                this.navigationService.instruction({
                    modal: null,
                    detail: {
                        nodeId,
                        itemType: 'page',
                        itemId: pageWithVersion.id,
                        editMode: EditMode.COMPARE_VERSION_CONTENTS,
                        options: {
                            version, oldVersion,
                        },
                    },
                }).navigate({ replaceUrl: false });
            })
            .catch(error => {
                // if the fetching of the version info failed for some reason,
                // just open the page in regular edit mode.
                this.navigationService.instruction({
                    modal: null,
                    detail: {
                        nodeId, itemType: 'page', itemId: page.id, editMode: EditMode.EDIT,
                    },
                }).navigate({ replaceUrl: false });

                this.errorHandler.catch(error, { notification: false });
            });
    }

    approve(): void {
        this.folderActions.pageQueuedApprove(this.selectedPages);
        this.closeFn(null);
    }

    approveBtnIsVisible(): boolean {
        return this.selectedPages.filter(page => {
            return page.timeManagement.queuedPublish || page.timeManagement.queuedOffline;
        }).length > 0;
    }

    assign(): void {
        this.closeFn(this.selectedPages);
    }

    /**
     * If the page has not been published before, there is will be `publishedVersion` property. In this case we want
     * to compare it with its "blank" state, ie the entire contents will be highlighted as changes. We can achieve
     * this by using a fake version with the timestamp 0.
     */
    private getLastVersion(page: Page): any {
        const initialVersion: any = { timestamp: 0, number: '0.0' };
        return page.publishedVersion ? page.publishedVersion : initialVersion;
    }
}
