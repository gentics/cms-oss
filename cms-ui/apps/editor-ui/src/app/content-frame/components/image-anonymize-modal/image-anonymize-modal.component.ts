import { HttpClient } from '@angular/common/http';
import { ChangeDetectionStrategy, ChangeDetectorRef, Component, Input, OnDestroy, OnInit } from '@angular/core';
import { I18nNotification } from '@editor-ui/app/core/providers/i18n-notification/i18n-notification.service';
import { ResourceUrlBuilder } from '@editor-ui/app/core/providers/resource-url-builder/resource-url-builder';
import { Image } from '@gentics/cms-models';
import { GCMSRestClientService } from '@gentics/cms-rest-client-angular';
import { PiktidAPIService } from '@gentics/picktid-editor';
import { BaseModal } from '@gentics/ui-core';
import { map, Subscription, switchMap } from 'rxjs';

@Component({
    selector: 'gtx-image-anonymize-modal',
    templateUrl: './image-anonymize-modal.component.html',
    styleUrls: ['./image-anonymize-modal.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ImageAnonymizeModal extends BaseModal<void> implements OnInit, OnDestroy {

    @Input()
    public imageId: number | string;

    @Input()
    public nodeId: number | string;

    public loadedImage: Image;

    public imageBlob: Blob;

    public loading = false;

    public piktidImageId: string;
    public confirmedIds: number[] = [];

    private subscriptions: Subscription[] = [];

    constructor(
        private changeDetector: ChangeDetectorRef,
        private resourceUrlBuilder: ResourceUrlBuilder,
        private api: GCMSRestClientService,
        private piktid: PiktidAPIService,
        private http: HttpClient,
        private notifications: I18nNotification,
    ) {
        super();
    }

    ngOnInit(): void {
        this.subscriptions.push(this.api.image.get(this.imageId, { nodeId: this.nodeId, update: false }).subscribe(res => {
            this.loadedImage = res.image;
            this.changeDetector.markForCheck();
        }));

        const originalImageUrl = this.resourceUrlBuilder.imageFullsize(this.imageId, this.nodeId);
        this.subscriptions.push(this.http.get(originalImageUrl, {
            observe: 'body',
            responseType: 'blob',
        }).subscribe(downloadedImage => {
            this.imageBlob = downloadedImage;
            this.changeDetector.markForCheck();
        }));
    }

    ngOnDestroy(): void {
        this.subscriptions.forEach(s => s.unsubscribe());
    }

    public updateImageId(id: string): void {
        this.piktidImageId = id;
    }

    public updateConfirmation(ids: number[]): void {
        this.confirmedIds = ids;
    }

    public processImage(): void {
        if (this.piktidImageId == null || this.confirmedIds?.length === 0) {
            return;
        }

        this.loading = true;

        // Generate the final version of the anonymized image
        this.subscriptions.push(this.piktid.getImageDownloadLink(this.piktidImageId, {
            flag_watermark: 0,
            flag_png: 0,
            flag_quality: 0,
        }).pipe(
            // Download the generated image
            switchMap(links => this.http.get(links.l, {
                observe: 'body',
                responseType: 'blob',
            })),
            // Upload the generated image to the CMS
            switchMap(newImageBlob => {
                (newImageBlob as any).name = `anonymized_${this.loadedImage.name}`;

                return this.api.file.upload(newImageBlob, {
                    folderId: this.loadedImage.folderId,
                    nodeId: this.nodeId,
                });
            }),
            // Delete the image from piktid after successful upload
            switchMap(uploaded => {
                return this.piktid.deleteImage(this.piktidImageId).pipe(
                    map(() => uploaded),
                );
            }),
        ).subscribe({
            next: res => {
                this.loading = false;
                this.changeDetector.markForCheck();

                console.log(res);

                this.notifications.show({
                    message: 'image successfully anoonymized',
                    type: 'success',
                })
            },
            error: err => {
                this.loading = false;
                this.changeDetector.markForCheck();

                console.error(err);
                this.notifications.show({
                    message: err?.message ?? err,
                    type: 'alert',
                });
            },
        }));
    }
}
