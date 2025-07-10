import { ChangeDetectionStrategy, ChangeDetectorRef, Component, Input, OnDestroy, OnInit } from '@angular/core';
import { Form, FormRequestOptions, Page, PageRequestOptions, QueuedActionRequestClear, TimeManagement } from '@gentics/cms-models';
import { BaseModal } from '@gentics/ui-core';
import { BehaviorSubject, Observable, Subscription, of } from 'rxjs';
import { map } from 'rxjs/operators';
import { ErrorHandler } from '../../../core/providers/error-handler/error-handler.service';
import { getFormattedTimeMgmtValue, pageVersionsGetLatest } from '../../../core/providers/i18n/i18n-utils';
import { I18nService } from '../../../core/providers/i18n/i18n.service';
import { PermissionService } from '../../../core/providers/permissions/permission.service';
import { FolderActionsService } from '../../../state';
import { I18nDatePipe } from '../../pipes/i18n-date/i18n-date.pipe';

enum VersionManagement {
    KEEP_VERSION = 'keep',
    NEW_VERSION = 'new',
}

@Component({
    selector: 'time-management-modal',
    templateUrl: './time-management-modal.tpl.html',
    styleUrls: ['./time-management-modal.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    providers: [I18nDatePipe],
    standalone: false
})
export class TimeManagementModal extends BaseModal<TimeManagement> implements OnDestroy, OnInit {

    public readonly VersionManagement = VersionManagement;

    /** The Item for which the time-management should be maneged. */
    @Input()
    public item: Page | Form;

    /** ID of the node the editor is currently navigating in. */
    @Input()
    public currentNodeId: number;

    /** Which version is valid for the new time-management settings */
    versionSelect: VersionManagement;

    /* ==================================================== */

    /** If the current item has publish at already set */
    hasExistingPublishAt = false;

    /** "Publish at" value for the existing version */
    existingPublishAt: number = null;

    /** Version of timemanagement for publishAt if already set */
    existingPublishAtVersion: string = null;

    /** Publish At timestamp for the new Version*/
    publishAt: number = null;

    /* ==================================================== */

    /** Value of timemanagement for takeOfflineAt if already set */
    hasExistingTakeOfflineAt = false;

    /** "Take offline at" value for the existing version */
    existingTakeOfflineAt: number = null;

    /** Take offline at for the current item version */
    takeOfflineAt: number = null;

    /* ==================================================== */

    /** Current page node version */
    latestItemVersion: string;

    /** Page node version where a ```queuedPublish```property has already been set */
    existingItemVersion: string;

    /** If the `latestItemVersion` and `existingItemVersion` are set and different. */
    keepVersionVisible = false;

    /* ==================================================== */

    /** TRUE if user is allowed to publish */
    userHasPublishPermission: boolean;

    /** TRUE if the form is valid */
    formValid = false;

    /** If TRUE request is in progress */
    requesting$: BehaviorSubject<boolean> = new BehaviorSubject<boolean>(false);

    selectedLanguageVariants: { [pageId: number]: number[] } = {};

    itemsToBeModified: (Page | Form)[] = [];

    private subscriptions: Subscription[] = [];

    constructor(
        private changeDetector: ChangeDetectorRef,
        private folderActions: FolderActionsService,
        private errorHandler: ErrorHandler,
        private i18n: I18nService,
        private i18nDate: I18nDatePipe,
        private permissions: PermissionService,
    ) {
        super();
    }

    ngOnInit(): void {

        // `currentNodeId` required for reliable functioning
        if (!this.currentNodeId) {
            throw new Error('Missing property "currentNodeId" is required.');
        }

        let permissionLoad$: Observable<boolean>;

        if (this.item.type === 'page') {
            // if language variants (for Nodes with multiple languages)
            if (this.item.languageVariants && Object.keys(this.item.languageVariants).length > 0) {
                // eslint-disable-next-line guard-for-in
                for (const key in this.item.languageVariants) {
                    let pageID = this.item.languageVariants[key];
                    if (pageID == null) {
                        continue;
                    } else if (typeof pageID === 'object') {
                        pageID = pageID.id;
                    }

                    this.folderActions.getPage(pageID)
                        .then(page => {
                            this.itemsToBeModified.push(page);
                        });
                }
            } else {
                // if no language variants (for Nodes with only one language)
                this.itemsToBeModified.push(this.item);
            }

            this.selectedLanguageVariants[this.item.id] = [this.item.id];

            permissionLoad$ = this.permissions.forItem(this.item.id, 'page', this.currentNodeId).pipe(
                map(permissions => permissions.publish),
            )
        }

        if (this.item.type === 'form') {
            this.itemsToBeModified.push(this.item);

            this.selectedLanguageVariants[this.item.id] = [this.item.id];

            permissionLoad$ = this.permissions.forItem(this.item.id, 'form', this.currentNodeId).pipe(
                map(permissions => permissions.publish),
            );
        }


        if (permissionLoad$) {
            this.subscriptions.push(permissionLoad$.subscribe(hasPermission => {
                this.userHasPublishPermission = hasPermission;
                this.changeDetector.markForCheck();
            }));
        }

        this.componentDataSet();
    }

    ngOnDestroy(): void {
        this.subscriptions.forEach(s => s.unsubscribe());
    }

    async componentDataSet(): Promise<void> {
        if (this.item.type === 'page') {
            await this.loadPageData();
        } else if (this.item.type === 'form') {
            await this.loadFormData();
        } else {
            // eslint-disable-next-line @typescript-eslint/restrict-template-expressions
            throw new Error(`Cannot load data from invalid item type "${(this.item as any).type}"!`);
        }

        this.keepVersionVisible = (
            !!this.latestItemVersion
            && !!this.existingItemVersion
            && this.existingItemVersion !== this.latestItemVersion
        );

        this.versionSelect = this.keepVersionVisible
            ? VersionManagement.KEEP_VERSION
            : VersionManagement.NEW_VERSION;

        // notify change detection as all initial properties should be set by now
        this.changeDetector.detectChanges();
    }

    private async loadPageData(): Promise<void> {
        const pageOptions: PageRequestOptions = {
            nodeId: this.currentNodeId,
            versioninfo: true,
            langvars: true,
        };

        // get page data with version information
        this.item = await this.folderActions.getPage(this.item.id, pageOptions);

        // prepare form
        this.setFormValues(this.item);

        // if there are mutliple versions, get latest
        if (this.item.versions && this.item.versions.length > 0) {
            // sort to by highest version number
            this.latestItemVersion = pageVersionsGetLatest(this.item.versions);
        } else {
            this.latestItemVersion = null;
        }

        // check if timemanagement has been set for an older page version
        this.existingItemVersion = this.item?.timeManagement?.version?.number;
    }

    private async loadFormData(): Promise<void> {
        const formOptions: FormRequestOptions = {
            nodeId: this.currentNodeId,
        };

        // get form data with version information
        this.item = await this.folderActions.getForm(this.item.id, formOptions);

        // prepare form
        this.setFormValues(this.item);

        this.latestItemVersion = this.item.version?.number;

        // check if timemanagement has been set for an older form version
        this.existingItemVersion = this.item?.timeManagement?.version?.number;
    }

    public updateFormValidity(): void {
        this.formValid = this.isFormValid();
    }

    private isFormValid(): boolean {
        // No languages selected
        if (this.selectedLanguageVariants[this.item.id]?.length === 0) {
            return false;
        }

        switch (this.versionSelect) {
            case VersionManagement.KEEP_VERSION:
                return (this.existingPublishAt != null && this.existingPublishAt > 0)
                    || (this.existingTakeOfflineAt != null && this.existingTakeOfflineAt > 0);

            case VersionManagement.NEW_VERSION:
                return (this.publishAt != null && this.publishAt > 0)
                    || (this.takeOfflineAt != null && this.takeOfflineAt > 0);
        }
    }

    async btnOkayClicked(): Promise<void> {
        // any change requests to be collected here
        const changesToSave: Promise<any>[] = [];

        // prepare clear request data
        const requestClear: QueuedActionRequestClear = {
            page: {
                id: this.item.id,
            },
            unlock: true,
            clearPublishAt: false,
            clearOfflineAt: false,
        };

        const itemIDs: number[] = [];

        this.itemsToBeModified.forEach(item => {
            // eslint-disable-next-line @typescript-eslint/no-for-in-array, guard-for-in
            for (const key in this.selectedLanguageVariants[item.id]) {
                itemIDs.push(this.selectedLanguageVariants[item.id][key]);
            }
        });

        for (const item of this.itemsToBeModified) {
            if (itemIDs.indexOf(item.id) === -1) {
                continue;
            }

            const publishDate = this.getPublishAtDate();
            const takeOfflineDate = this.getTakeItemOfflineAtDate();

            // if settings are different from current for publishAt
            if (
                (this.versionSelect === VersionManagement.KEEP_VERSION
                    // if form value differs from value already set in page properties
                    && this.existingPublishAt !== item.timeManagement.at
                    && this.existingPublishAt > 0
                    && (
                        // if form value differs from value requested by an unprivileged user
                        !item.timeManagement.queuedPublish
                        || this.existingPublishAt !== item.timeManagement.queuedPublish.at
                    )
                ) || !this.keepVersionVisible
            ) {
                // if user has date set, request timemanagement
                if (publishDate) {
                    changesToSave.push(this.publishItemAtSet(publishDate, item));
                }
            }

            // if settings are different from current for takeOfflineAt
            if (
                (this.versionSelect === VersionManagement.KEEP_VERSION
                    // if form value differs from value already set in page properties
                    && this.existingTakeOfflineAt !== item.timeManagement.offlineAt
                    && this.existingTakeOfflineAt > 0
                    && (
                        // if form value differs from value requested by an unprivileged user
                        !item.timeManagement.queuedOffline
                        || this.existingTakeOfflineAt !== item.timeManagement.queuedOffline.at
                    )
                ) || !this.keepVersionVisible
            ) {
                // if user has date set, request timemanagement
                if (takeOfflineDate) {
                    changesToSave.push(this.takeItemOfflineAtSet(takeOfflineDate, item));
                }
            }

            requestClear.clearPublishAt = publishDate == null;
            requestClear.clearOfflineAt = takeOfflineDate == null;

            // if any value has been cleared, request it
            if (requestClear.clearPublishAt || requestClear.clearOfflineAt) {
                this.itemTimeManagementClear(requestClear);
            }
        }

        try {
            // Request changes
            if (!changesToSave.length) {
                this.requesting$.next(false);
                this.closeFn(this.item.timeManagement);
                return;
            }

            await Promise.all(changesToSave);
            await this.componentDataSet();

            // close modal
            this.requesting$.next(false);
            this.closeFn(this.item.timeManagement);
        } catch (error) {
            this.requesting$.next(false);
            this.errorHandler.catch(error);
        }
    }

    /**
     * Handles changes to the language variants selection for pages.
     */
    onLanguageSelectionChange(pageId: number, variantIds: number[]): void {
        this.selectedLanguageVariants[pageId] = variantIds;
        this.updateFormValidity();
    }


    timeMgmtExists(): boolean {
        return this.item.timeManagement.at > 0 || this.item.timeManagement.offlineAt > 0;
    }

    timeMgmtQueuedRequestsExists(): boolean {
        return (
            this.item.timeManagement.queuedPublish instanceof Object
            || this.item.timeManagement.queuedOffline instanceof Object
        );
    }

    getFormattedTimeMgmtValue(field: keyof TimeManagement): Observable<string | boolean> {
        if (!this.currentNodeId) {
            return of(false);
        }
        return getFormattedTimeMgmtValue(this.item, field, this.currentNodeId, this.i18n, this.i18nDate, this.folderActions);
    }

    protected setFormValues(item: Page | Form): void {
        this.hasExistingPublishAt = item.timeManagement.at != null && item.timeManagement.at > 0;
        this.existingPublishAt = item.timeManagement.at;
        this.existingPublishAtVersion = this.getPublishAtVersion(item);
        this.publishAt = item.timeManagement.at;

        this.hasExistingTakeOfflineAt = item.timeManagement.offlineAt != null && item.timeManagement.offlineAt > 0;
        this.existingTakeOfflineAt = item.timeManagement.offlineAt;
        this.takeOfflineAt = item.timeManagement.offlineAt;
    }

    getPublishAtVersion(item: Page | Form): string {
        let version: string;

        if (item.timeManagement.version) {
            version = item.timeManagement.version.number;
        } else {
            if (item.type === 'page') {
                // SPECIAL CASE
                // If there is no ```timeManagement.version``` despite ```timeManagement.at > 0``` in the CMS' response
                // a legal data migration issue occured documented at https://jira.gentics.com/browse/GTXPE-446 .
                // In this case, the latest page version shall be published.

                // if planned version is not available, fallback to latest page version
                version = pageVersionsGetLatest(item.versions);
            }

            if (item.type === 'form') {
                version = item.version.number;
            }
        }

        return version;
    }

    protected async publishItemAtSet(value: number, item: Page | Form): Promise<void> {
        if (this.item.type === 'page') {
            await this.folderActions.publishPageAt(
                item.id,
                value,
                this.versionSelect === VersionManagement.KEEP_VERSION,
            );

            // refresh current folder view
            this.folderActions.refreshList('page');
        }

        if (this.item.type === 'form') {
            await this.folderActions.publishFormAt(
                item.id,
                value,
                this.versionSelect === VersionManagement.KEEP_VERSION,
            );

            // refresh current folder view
            this.folderActions.refreshList('form');
        }
    }

    protected getPublishAtDate(): number | null {
        return this.keepVersionVisible && this.versionSelect === VersionManagement.KEEP_VERSION
            ? this.existingPublishAt
            : this.publishAt;
    }

    /**
     * @returns the appropriate date for the item to be taken offline automatically if configured
     */
    protected getTakeItemOfflineAtDate(): number | null {
        return this.keepVersionVisible && this.versionSelect === VersionManagement.KEEP_VERSION
            ? this.existingTakeOfflineAt
            : this.takeOfflineAt;
    }

    /**
     * Set the date of the page to get taken offline
     *
     * @param value as date integer defining the date the page / the form shall be go offline automatically
     */
    protected async takeItemOfflineAtSet(value: number, page: Page | Form): Promise<void> {
        if (this.item.type === 'page') {
            await this.folderActions.takePageOfflineAt(
                page.id,
                value,
            );

            // refresh current folder view
            this.folderActions.refreshList('page');
        }

        if (this.item.type === 'form') {
            await this.folderActions.takeFormOfflineAt(
                this.item.id,
                value,
            );

            // refresh current folder view
            this.folderActions.refreshList('form');
        }
    }

    protected async itemTimeManagementClear(payload: QueuedActionRequestClear): Promise<void> {
        if (this.item.type === 'page') {
            await this.folderActions.pageTimeManagementClear(this.item.id, payload);
            // refresh current folder view
            this.folderActions.refreshList('page');
        }

        if (this.item.type === 'form') {
            await this.folderActions.formTimeManagementClear(this.item.id, payload);
            // refresh current folder view
            this.folderActions.refreshList('form');
        }
    }

}
