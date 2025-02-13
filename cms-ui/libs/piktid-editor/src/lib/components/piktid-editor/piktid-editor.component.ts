/* eslint-disable @typescript-eslint/no-unnecessary-type-assertion,@typescript-eslint/no-non-null-assertion */
import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnDestroy, OnInit } from '@angular/core';
import { FormControl, FormGroup, Validators } from '@angular/forms';
import { FormProperties, NotificationService } from '@gentics/ui-core';
import { filter, interval, map, Subscription, switchMap } from 'rxjs';
import { Coordinates, NewGenerationNotificationData, Notification, NotificationName, UserInfoResponse } from '../../common/models';
import { FaceData } from '../../common/prompt';
import { PiktidAPIService } from '../../providers/piktid-api/piktid-api.service';

interface LoginFormProperties {
    username: string;
    password: string;
}

const LOCAL_STORAGE_KEY = 'piktid-editor-auth';

@Component({
    selector: 'gtxpict-piktid-editor',
    templateUrl: './piktid-editor.component.html',
    styleUrl: './piktid-editor.component.css',
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PiktidEditorComponent implements OnInit, OnDestroy {

    /** Whether the user is logged in. */
    public loggedIn = false;

    /** The user info. */
    public userInfo: UserInfoResponse | null = null;

    public loginForm: FormGroup<FormProperties<LoginFormProperties>> | null = null;

    /** The image that is currently picked by the user. */
    public pickedImage: File | null = null;

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
    public busy = false;

    private uploadSubscription: Subscription | null = null;
    private otherSubscriptions: Subscription[] = [];

    constructor(
        private changeDetector: ChangeDetectorRef,
        private api: PiktidAPIService,
        private notificationService: NotificationService,
    ) {}

    ngOnInit(): void {
        this.loginForm = new FormGroup<FormProperties<LoginFormProperties>>({
            username: new FormControl('', Validators.required),
            password: new FormControl('', Validators.required),
        });

        const storedAuth = localStorage.getItem(LOCAL_STORAGE_KEY);
        if (storedAuth) {
            this.api.setAuth(storedAuth.split(':')[0], storedAuth.split(':')[1]);
        }

        this.loggedIn = this.api.isLoggedIn();

        if (this.loggedIn) {
            this.otherSubscriptions.push(this.api.getUserInfo().subscribe({
                next: (response) => {
                    this.userInfo = response;
                    this.changeDetector.markForCheck();
                },
            }));
        }
    }

    ngOnDestroy(): void {
        this.uploadSubscription?.unsubscribe?.();
        this.otherSubscriptions.forEach((subscription) => subscription.unsubscribe());
    }

    public onLogin(): void {
        this.busy = true;
        this.changeDetector.markForCheck();

        this.otherSubscriptions.push(this.api.authenticate(this.loginForm!.value.username!, this.loginForm!.value.password!).pipe(
            switchMap(auth => this.api.getUserInfo().pipe(
                map((userInfo) => ({ auth, userInfo })),
            )),
        ).subscribe({
            next: ({ auth, userInfo }) => {
                this.loggedIn = true;
                this.busy = false;
                this.userInfo = userInfo;
                this.changeDetector.markForCheck();

                localStorage.setItem(LOCAL_STORAGE_KEY, `${auth.access_token}:${auth.refresh_token}`);
            },
            error: (error) => {
                this.busy = false;
                console.error(error);
                this.changeDetector.markForCheck();
            },
        }));
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
        this.editedImageUrl = null;
        this.activeImage = 'original';
        this.selectedGeneration = {};
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

    // public confirmEditing(): void {
    //     if (!this.imageId || this.imageUploadStatus !== 'success' || this.confirmedFaces.size !== this.faceIds.length) {
    //         return;
    //     }

    //     // TODO: Upload the image to the CMS (Emit event here and do upload in parent)
    // }
}
