import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnDestroy, OnInit } from '@angular/core';
import { NotificationService } from '@gentics/ui-core';
import { filter, interval, Subscription } from 'rxjs';
import { Coordinates, NewGenerationNotificationData, NotificationName } from '../../common/models';
import { FaceData } from '../../common/prompt';
import { PiktidAPIService } from '../../providers/piktid-api/piktid-api.service';

@Component({
    selector: 'gtxpict-piktid-editor',
    templateUrl: './piktid-editor.component.html',
    styleUrl: './piktid-editor.component.css',
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PiktidEditorComponent implements OnInit, OnDestroy {

    /** The image that is currently picked by the user. */
    public pickedImage: File | null = null;

    /** The URL of the preview image. */
    public imageUrl: string | null = null;

    /** The image-id of the uploaded image. If `null`, the image is not uploaded yet. */
    public imageId: string | null = null;

    /** The status of the image upload. If `null`, the image is not uploaded yet. */
    public imageUploadStatus: 'pending' | 'success' | 'error' | null = null;

    /** The status of the face detection. If `null`, the face detection is not started yet. */
    public faceDetectionStatus: 'pending' | 'success' | 'error' | null = null;

    /** The list of face-ids detected in the image. */
    public faceIds: number[] = [];

    /** The positions of the faces in the image. */
    public facePositions: Record<number, Coordinates> = {};

    /** The descriptions of the faces in the image. */
    public faceDescriptions: Record<number, FaceData> = {};

    /** The list of generated faces for each face-id. */
    public generatedFaces: Record<number, NewGenerationNotificationData[]> = {};

    /** The selected generation for each face-id. ID of the generation may be `-1` if no generation is selected. */
    public selectedGeneration: Record<number, number> = {};

    /** Whether the component is busy. */
    public busy = false;

    /** The list of faces that are waiting for a generation. */
    public waitingForFaces: number[] = [];

    /** The list of faces that have been confirmed. */
    public confirmedFaces: number[] = [];

    /** The ids of the notifications that have been processed. */
    private processedNotifications = new Set<number>([]);

    private uploadSubscription: Subscription | null = null;
    private notificationIntervalSubscription: Subscription | null = null;
    private notificationSubscription: Subscription | null = null;
    private otherSubscriptions: Subscription[] = [];

    constructor(
        private changeDetector: ChangeDetectorRef,
        private api: PiktidAPIService,
        private notificationService: NotificationService,
    ) {}

    ngOnInit(): void {
        this.notificationIntervalSubscription = interval(3_000).pipe(
            filter(() => this.waitingForFaces.length > 0),
        ).subscribe(() => {
            this.fetchNotifications();
        });
    }

    ngOnDestroy(): void {
        this.uploadSubscription?.unsubscribe?.();
        this.notificationIntervalSubscription?.unsubscribe?.();
        this.notificationSubscription?.unsubscribe?.();
        this.otherSubscriptions.forEach((subscription) => subscription.unsubscribe());
    }

    public onImagePicked(files: File[]): void {
        this.pickedImage = files[0];
        this.imageUrl = URL.createObjectURL(files[0]);

        // Reset the status of the image upload
        this.imageUploadStatus = null;
        this.imageId = null;
        this.faceDetectionStatus = null;
        this.faceIds = [];
        this.facePositions = {};
        this.faceDescriptions = {};

        this.changeDetector.markForCheck();
    }

    public uploadImage(): void {
        // No image or already uploading
        if (!this.pickedImage || this.imageUploadStatus === 'pending') {
            return;
        }

        this.busy = true;
        this.imageUploadStatus = 'pending';
        this.faceDetectionStatus = null;
        this.changeDetector.markForCheck();

        this.uploadSubscription = this.api.uploadFile(this.pickedImage, {
            mode: 'random',
            // eslint-disable-next-line @typescript-eslint/naming-convention
            flag_hair: true,
            // eslint-disable-next-line @typescript-eslint/naming-convention
            flag_sync: true,
        }).subscribe({
            next: (response) => {
                this.imageId = response.image_id;
                this.imageUploadStatus = 'success';

                this.selectedGeneration = {};
                this.faceIds = response.face_description_list.map((face) => {
                    this.selectedGeneration[face.f] = -1;
                    return face.f;
                });
                this.facePositions = response.faces.coordinates_list.reduce((acc, face) => {
                    acc[face.id] = face;
                    return acc;
                }, {} as Record<number, Coordinates>);
                this.faceDescriptions = response.face_description_list.reduce((acc, face) =>    {
                    acc[face.f] = face.a;
                    return acc;
                }, {} as Record<number, FaceData>);
                this.faceDetectionStatus = 'success';

                this.busy = false;
                this.changeDetector.markForCheck();

                // Fetch notifications to get the initial list of generated faces
                this.fetchNotifications();
            },
            error: (error) => {
                this.imageUploadStatus = 'error';
                this.busy = false;

                console.error(error);
                this.notificationService.show({
                    message: error.message,
                    type: 'alert',
                })

                this.changeDetector.markForCheck();
            },
        });
    }

    public generateFace(faceId: number): void {
        if (!this.imageId || !this.faceIds.includes(faceId)) {
            return;
        }

        this.otherSubscriptions.push(this.api.generateNewRandomFace(this.imageId, faceId).subscribe({
            next: (response) => {
                console.log('new expression generated', response);
                this.waitingForFaces.push(faceId);
                this.changeDetector.markForCheck();
            },
            error: (error) => {
                console.error(error);
            },
        }));
    }

    public confirmFaceGeneration(faceId: number): void {
        if (!this.imageId || !this.faceIds.includes(faceId) || this.selectedGeneration[faceId] === -1) {
            return;
        }

        this.otherSubscriptions.push(this.api.substituteFace(this.imageId, faceId, this.selectedGeneration[faceId]).subscribe({
            next: (response) => {
                console.log('face generation confirmed', response);
                this.confirmedFaces.push(faceId);
                this.changeDetector.markForCheck();
            },
            error: (error) => {
                console.error(error);
            },
        }));
    }

    public fetchNotifications(): void {
        if (!this.imageId) {
            return;
        }

        if (this.notificationSubscription) {
            this.notificationSubscription.unsubscribe();
        }

        this.notificationSubscription = this.api.getNotificationsByName(this.imageId, [
            // NotificationName.ERROR,
            NotificationName.NEW_GENERATION,
        ]).subscribe({
            next: (response) => {
                const stillInProgress = new Set<number>(this.waitingForFaces);
                const notifications = response.notifications_list || [];

                for (const singleNotif of notifications) {
                    if (this.processedNotifications.has(singleNotif.id)) {
                        continue;
                    }

                    switch (singleNotif.name) {
                        case NotificationName.NEW_GENERATION:
                            if (!this.generatedFaces[singleNotif.data.f]) {
                                this.generatedFaces[singleNotif.data.f] = [];
                            }

                            this.generatedFaces[singleNotif.data.f].push(singleNotif.data);
                            stillInProgress.delete(singleNotif.data.f);
                            break;

                        case NotificationName.ERROR:
                            this.notificationService.show({
                                message: singleNotif.data.msg,
                                type: 'alert',
                            });
                            break;
                    }

                    this.processedNotifications.add(singleNotif.id);
                }

                this.waitingForFaces = Array.from(stillInProgress);
                this.changeDetector.markForCheck();
            },
        });
    }

    public confirmEditing(): void {
        if (!this.imageId || this.imageUploadStatus !== 'success' || this.confirmedFaces.length !== this.faceIds.length) {
            return;
        }

        // TODO: Upload the image to the CMS (Emit event here and do upload in parent)
    }
}
