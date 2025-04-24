/* eslint-disable @typescript-eslint/no-unnecessary-type-assertion,@typescript-eslint/no-non-null-assertion */
import { ChangeDetectionStrategy, ChangeDetectorRef, Component, EventEmitter, Input, OnChanges, OnDestroy, Output } from '@angular/core';
import { ChangesOf, NotificationService } from '@gentics/ui-core';
import { Subscription } from 'rxjs';
import { Coordinates, NewGenerationNotificationData } from '../../common/models';
import { FaceData } from '../../common/prompt';
import { PiktidAPIService } from '../../providers/piktid-api/piktid-api.service';

@Component({
    selector: 'gtxpict-editor',
    templateUrl: './editor.component.html',
    styleUrl: './editor.component.css',
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class EditorComponent implements OnChanges, OnDestroy {

    @Input()
    public imageBlob: Blob | File | null = null;

    @Input()
    public disabled = false;

    @Output()
    public imageUpload = new EventEmitter<string>();

    @Output()
    public confirmChange = new EventEmitter<number[]>();

    /** The URL of the preview image. */
    public imageUrl: string | null = null;

    /** The URL of the edited image. */
    public editedImageUrl: string | null = null;

    /** The active image. */
    public activeImage: 'original' | 'edited' = 'original';

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
    public loading = false;

    private uploadSubscription: Subscription | null = null;
    private otherSubscriptions: Subscription[] = [];

    constructor(
        private changeDetector: ChangeDetectorRef,
        private api: PiktidAPIService,
        private notificationService: NotificationService,
    ) {}

    ngOnChanges(changes: ChangesOf<this>): void {
        if (changes.imageBlob) {
            this.updateImage();
        }
    }

    ngOnDestroy(): void {
        this.uploadSubscription?.unsubscribe?.();
        this.otherSubscriptions.forEach((subscription) => subscription.unsubscribe());
    }

    public updateImage(): void {
        if (this.imageBlob != null) {
            this.imageUrl = URL.createObjectURL(this.imageBlob);
        } else {
            this.imageBlob = null;
        }

        // Reset the status of the image upload
        this.imageUploadStatus = null;
        this.imageId = null;
        this.faceDetectionStatus = null;
        this.faceIds = [];
        this.facePositions = {};
        this.faceDescriptions = {};
        this.editedImageUrl = null;
        this.activeImage = 'original';
        this.selectedGeneration = {};
        this.changeDetector.markForCheck();
    }

    public uploadImage(): void {
        // No image or already uploading
        if (!this.imageBlob || this.imageUploadStatus === 'pending') {
            return;
        }

        this.loading = true;
        this.imageUploadStatus = 'pending';
        this.faceDetectionStatus = null;
        this.changeDetector.markForCheck();

        this.uploadSubscription = this.api.uploadFile(this.imageBlob, {
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

                this.loading = false;
                this.imageUpload.emit(this.imageId);
                this.changeDetector.markForCheck();
            },
            error: (error) => {
                this.imageUploadStatus = 'error';
                this.loading = false;

                console.error(error);
                this.notificationService.show({
                    message: error.message,
                    type: 'alert',
                })

                this.changeDetector.markForCheck();
            },
        });
    }

    public handleConfirmationChange(ids: number[]): void {
        this.confirmChange.emit(ids);
    }
}
