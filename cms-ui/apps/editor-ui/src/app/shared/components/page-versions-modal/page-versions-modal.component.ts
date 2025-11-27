import {
    AfterViewInit,
    ChangeDetectionStrategy,
    ChangeDetectorRef,
    Component,
    Input,
    OnDestroy,
} from '@angular/core';
import { I18nNotificationService } from '@gentics/cms-components';
import { EditMode } from '@gentics/cms-integration-api-models';
import { Page, PageRequestOptions, PageVersion } from '@gentics/cms-models';
import { IModalDialog } from '@gentics/ui-core';
import { Subscription } from 'rxjs';
import { Api } from '../../../core/providers/api/api.service';
import { NavigationService } from '../../../core/providers/navigation/navigation.service';
import { FolderActionsService } from '../../../state';
import { PublishableStateUtil } from '../../util/entity-states';

function sortVariantsByLanguageName(a: Page, b: Page): number {
    return a.languageName < b.languageName ? -1 : a.languageName > b.languageName ? 1 : 0;
}

function sortVersionsByDate(a: PageVersion, b: PageVersion): number {
    return b.timestamp - a.timestamp;
}

@Component({
    selector: 'page-versions-modal',
    templateUrl: './page-versions-modal.tpl.html',
    styleUrls: ['./page-versions-modal.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false,
})
export class PageVersionsModal implements IModalDialog, AfterViewInit, OnDestroy {

    @Input() page: Page;
    @Input() nodeId: number;

    loading = false;
    backgroundActivity = false;
    languageVariants: Page[];
    selectedPageVariant: Page;
    plannedVersion: string;
    plannedOnlineDate: number;
    plannedOfflineDate: number;

    private selectedLanguage: string;
    private compareBaseVersion: PageVersion;
    private subscription = new Subscription();
    private timeout: ReturnType<typeof setTimeout>;
    public current: PageVersion;
    public published: PageVersion;
    public planned: boolean;

    constructor(
        private api: Api,
        private navigationService: NavigationService,
        private folderActions: FolderActionsService,
        private notification: I18nNotificationService,
        private changeDetector: ChangeDetectorRef,
    ) { }

    ngAfterViewInit(): void {
        this.fetchFromServer();
    }

    ngOnDestroy(): void {
        this.subscription.unsubscribe();
        clearTimeout(this.timeout);
    }

    fetchFromServer(): Promise<PageVersion> {
        this.subscription.unsubscribe();
        this.loading = true;
        this.changeDetector.detectChanges();

        const options: PageRequestOptions = {
            nodeId: this.nodeId,
            versioninfo: true,
            langvars: true,
        };

        return new Promise((resolve, reject) => {
            this.subscription = this.api.folders.getItem(this.page.id, 'page', options)
                .subscribe((res) => {
                    this.loading = false;
                    this.changeDetector.markForCheck();

                    if (res.responseInfo.responseCode !== 'OK') {
                        this.notification.show({
                            message: res.responseInfo.responseMessage,
                            type: 'alert',
                            delay: 10000,
                        });
                        reject(new Error(res.responseInfo.responseMessage));
                        return;
                    }

                    const variants = (Object.values(res.page.languageVariants) as Page[])
                        .sort(sortVariantsByLanguageName)
                        .map((variant) => {
                            // if (variant.currentVersion) {
                            //     variant.currentVersion = variant.versions
                            //         .find((v) => v.timestamp === variant.currentVersion.timestamp);
                            // }

                            // if (variant.publishedVersion) {
                            //     variant.publishedVersion = variant.versions
                            //         .find((v) => v.timestamp === variant.publishedVersion.timestamp);
                            // }

                            variant.versions.sort(sortVersionsByDate);

                            return variant;
                        });

                    this.languageVariants = variants;

                    if (this.selectedLanguage) {
                        this.selectVariant(variants.filter((variant) => variant.language === this.selectedLanguage)[0]);
                        resolve(this.selectedPageVariant.currentVersion);
                        return;
                    }

                    const languageVariant = variants.filter((variant) => variant.language === res.page.language)[0];
                    if (languageVariant) {
                        this.selectVariant(languageVariant);
                        resolve(this.selectedPageVariant.currentVersion);
                        return;
                    }

                    // Node has no languages configured
                    const page = res.page;
                    page.versions.sort(sortVersionsByDate);
                    // if (page.currentVersion) {
                    //     page.currentVersion = page.versions.find((v) => v.timestamp === page.currentVersion.timestamp);
                    // }
                    // if (page.publishedVersion) {
                    //     page.publishedVersion = page.versions.find((v) => v.timestamp === page.publishedVersion.timestamp);
                    // }
                    this.selectVariant(res.page);

                    resolve(this.selectedPageVariant.currentVersion);
                }, (error) => {
                    this.loading = false;
                    this.changeDetector.markForCheck();

                    reject(error);
                });
        });
    }

    selectVariant(variant: Page): void {
        this.selectedPageVariant = variant;
        this.selectedLanguage = variant.language;
        this.compareBaseVersion = undefined;
        this.current = variant.currentVersion;
        this.published = variant.publishedVersion;
        this.planned = variant.planned;
        if (PublishableStateUtil.statePlanned(variant)) {
            this.plannedOnlineDate = variant.timeManagement.at;
            if (this.plannedOnlineDate > 0) {
                this.plannedVersion = variant.timeManagement.version.number;
            }
            this.plannedOfflineDate = variant.timeManagement.offlineAt;
        } else {
            this.plannedVersion = null;
            this.plannedOnlineDate = null;
            this.plannedOfflineDate = null;
        }
    }

    previewVersion(version: PageVersion): void {
        const pageId = this.page.id;
        const nodeId = this.nodeId;
        this.navigationService.detailOrModal(nodeId, 'page', pageId, EditMode.PREVIEW_VERSION, { version }).navigate();
        this.closeFn();
    }

    restoreVersion(version: PageVersion): void {
        const versionBeforeRestore = this.selectedPageVariant.currentVersion.number;
        this.backgroundActivity = true;
        this.folderActions.restorePageVersion(this.selectedPageVariant.id, version, false)
            .then(() => this.fetchFromServer())
            .then((currentVersion) => {
                this.backgroundActivity = false;
                if (currentVersion.number === versionBeforeRestore) {
                    this.notification.show({
                        type: 'default',
                        message: `Version ${version.number} is identical to the current version.`,
                    });
                    return;
                }

                this.notification.show({
                    type: 'success',
                    message: `Version ${version.number} was restored as version ${currentVersion.number}.`,
                });
                this.navigationService
                    .detailOrModal(this.nodeId, 'page', this.page.id, EditMode.PREVIEW)
                    .navigate()
                    .then((navigated) => {
                        this.closeFn();
                    });
            }, (error) => {
                this.backgroundActivity = false;
                this.notification.show({
                    type: 'alert',
                    message: error.message || error,
                });
            });
    }

    compareFromVersion(version: PageVersion): void {
        this.compareBaseVersion = version;
    }

    cancelComparing(): void {
        this.compareBaseVersion = undefined;
    }

    compareWithVersion(version: PageVersion): void {
        const pageId = this.page.id;
        const nodeId = this.nodeId;
        let oldVersion = this.compareBaseVersion;

        if (+oldVersion.number > +version.number) {
            const swap = oldVersion;
            oldVersion = version;
            version = swap;
        }

        this.navigationService
            .detailOrModal(nodeId, 'page', pageId, EditMode.COMPARE_VERSION_CONTENTS, { version, oldVersion })
            .navigate();
        this.closeFn();
    }

    closeFn(): void { }
    cancelFn(): void { }

    registerCloseFn(close: (val?: any) => void): void {
        this.closeFn = close;
    }

    registerCancelFn(cancel: (val?: any) => void): void {
        this.cancelFn = cancel;
    }
}
