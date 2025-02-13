import { ChangeDetectionStrategy, ChangeDetectorRef, Component, EventEmitter, Input, OnChanges, OnDestroy, OnInit, Output } from '@angular/core';
import { ChangesOf, NotificationService } from '@gentics/ui-core';
import { filter, interval, Subscription } from 'rxjs';
import { Coordinates, ImageLink, NewGenerationNotificationData, Notification, NotificationName } from '../../common/models';
import { PiktidAPIService } from '../../providers';

@Component({
    selector: 'gtxpict-anonymization-editor',
    templateUrl: './anonymization-editor.component.html',
    styleUrls: ['./anonymization-editor.component.css'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AnonymizationEditorComponent implements OnInit, OnChanges,OnDestroy {

    @Input()
    public imageUrl: string | null = null;

    @Input()
    public imageId: string | null = null;

    @Input()
    public faceIds: number[] = [];

    @Input()
    public facePositions: Record<number, Coordinates> = {};

    @Input()
    public uploading = false;

    @Output()
    public uploadImage = new EventEmitter<void>();

    @Output()
    public confirm = new EventEmitter<ImageLink>();

    /** If the component is currently working on something. */
    public busy = false;

    /** The list of generated faces for each face-id. */
    public faceGenerations: Record<number, NewGenerationNotificationData[]> = {};

    /** The selected generation for each face-id. ID of the generation may be `-1` if no generation is selected. */
    public selectedGeneration: Record<number, number> = {};

    /** The URL of the edited image. */
    public editedImageUrl: string | null = null;

    /** The currently active image. */
    public activeImage: 'original' | 'edited' = 'original';

    /** The list of faces that are waiting for a generation. */
    public waitingForFaces =new Set<number>();

    /** The list of faces that have been confirmed. */
    public confirmedFaces = new Set<number>();

    /** The progress of the generation for each face. */
    public faceProgress: Record<number, number> = {};

    /** The ids of the notifications that have been processed. */
    private processedNotifications = new Set<number>([]);

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
            filter(() => this.waitingForFaces.size > 0),
        ).subscribe(() => {
            this.fetchNotifications();
        });
    }

    ngOnChanges(changes: ChangesOf<this>): void {
        if (changes.imageId && this.imageId) {
            // Fetch notifications to get the initial list of generated faces
            this.fetchNotifications();
        }
        if (changes.faceIds) {
            this.selectedGeneration = this.faceIds.reduce((acc, faceId) => {
                acc[faceId] = this.selectedGeneration[faceId] ?? -1;
                return acc;
            }, {} as Record<number, number>);
        }
    }

    ngOnDestroy(): void {
        this.notificationIntervalSubscription?.unsubscribe?.();
        this.notificationSubscription?.unsubscribe?.();
        this.otherSubscriptions.forEach((subscription) => subscription.unsubscribe());
    }

    public triggerUpload(): void {
        this.uploadImage.emit();
    }

    public onTabChange(id: string): void {
        this.activeImage = id as 'original' | 'edited';
        this.changeDetector.markForCheck();
    }

    public selectGeneration(faceId: number, generationId: number): void {
        if (this.selectedGeneration[faceId] === generationId) {
            this.selectedGeneration[faceId] = -1;
        } else {
            this.selectedGeneration[faceId] = generationId;
        }
        this.selectedGeneration = { ...this.selectedGeneration };

        this.changeDetector.markForCheck();
    }

    public confirmFaceGeneration(faceId: number): void {
        if (!this.imageId || !this.faceIds.includes(faceId) || this.busy) {
            return;
        }

        this.confirmedFaces.add(faceId);
        this.confirmedFaces = new Set(this.confirmedFaces);

        if (this.selectedGeneration[faceId] === -1) {
            return;
        }

        this.busy = true;
        this.changeDetector.markForCheck();

        this.otherSubscriptions.push(this.api.substituteFace(this.imageId, faceId, this.selectedGeneration[faceId]).subscribe({
            next: (response) => {
                this.editedImageUrl = response.l;
                this.activeImage = 'edited';
                this.busy = false;
                this.changeDetector.markForCheck();
            },
            error: (error) => {
                this.busy = false;
                this.confirmedFaces.delete(faceId);
                this.confirmedFaces = new Set(this.confirmedFaces);
                this.changeDetector.markForCheck();

                console.error(error);
                this.notificationService.show({
                    message: error.message,
                    type: 'alert',
                });
            },
        }));
    }

    public generateFace(faceId: number): void {
        if (!this.imageId || !this.faceIds.includes(faceId)) {
            return;
        }

        this.waitingForFaces.add(faceId);
        this.waitingForFaces = new Set(this.waitingForFaces);
        this.changeDetector.markForCheck();

        this.otherSubscriptions.push(this.api.generateNewRandomFace(this.imageId, faceId).subscribe({
            next: () => {
                this.changeDetector.markForCheck();
            },
            error: (error) => {
                this.waitingForFaces.delete(faceId);
                this.waitingForFaces = new Set(this.waitingForFaces);
                this.changeDetector.markForCheck();
                console.error(error);
            },
        }));
    }

    public undoFaceGeneration(faceId: number): void {
        if (!this.imageId || !this.faceIds.includes(faceId) || this.busy) {
            return;
        }

        this.busy = true;
        this.confirmedFaces.delete(faceId);
        this.confirmedFaces = new Set(this.confirmedFaces);
        this.waitingForFaces.add(faceId);
        this.waitingForFaces = new Set(this.waitingForFaces);
        this.changeDetector.markForCheck();

        this.otherSubscriptions.push(this.api.substituteFace(this.imageId, faceId, this.selectedGeneration[faceId], {
            flag_reset: 1,
            flag_reset_single_face: 1,
        }).subscribe({
            next: (res) => {
                // If there are no confirmed faces, we are back to the original image
                if (this.confirmedFaces.size === 0) {
                    this.editedImageUrl = null;
                    this.activeImage = 'original';
                } else {
                    this.editedImageUrl = res.l;
                    this.activeImage = 'edited';
                }

                this.busy = false;
                this.waitingForFaces.delete(faceId);
                this.waitingForFaces = new Set(this.waitingForFaces);
                this.selectedGeneration[faceId] = -1;
                this.selectedGeneration = { ...this.selectedGeneration };

                this.changeDetector.markForCheck();
            },
            error: (error) => {
                this.busy = false;
                this.waitingForFaces.delete(faceId);
                this.waitingForFaces = new Set(this.waitingForFaces);
                this.confirmedFaces.add(faceId);
                this.confirmedFaces = new Set(this.confirmedFaces);
                this.changeDetector.markForCheck();
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
            NotificationName.ERROR,
            NotificationName.PROGRESS,
            NotificationName.NEW_GENERATION,
        ]).subscribe({
            next: (response) => {
                const stillInProgress = new Set<number>(this.waitingForFaces);
                let notifications: Notification[];

                if (Array.isArray(response)) {
                    notifications = response;
                } else {
                    notifications = response.notifications_list || [];
                }

                for (const singleNotif of notifications) {
                    if (this.processedNotifications.has(singleNotif.id)) {
                        continue;
                    }

                    switch (singleNotif.name) {
                        case NotificationName.NEW_GENERATION:
                            if (!this.faceGenerations[singleNotif.data.f]) {
                                this.faceGenerations[singleNotif.data.f] = [];
                            }

                            this.faceGenerations[singleNotif.data.f].push(singleNotif.data);
                            stillInProgress.delete(singleNotif.data.f);
                            break;

                        case NotificationName.ERROR:
                            this.notificationService.show({
                                message: singleNotif.data.msg,
                                type: 'alert',
                            });
                            break;

                        case NotificationName.PROGRESS:
                            this.faceProgress[singleNotif.data.f] = singleNotif.data.progress;
                            break;
                    }

                    this.processedNotifications.add(singleNotif.id);
                }

                this.waitingForFaces = stillInProgress;
                this.changeDetector.markForCheck();
            },
        });
    }

    public confirmChanges(): void {
        if (!this.imageUrl || this.busy || this.uploading || this.confirmedFaces.size === 0 ) {
            return;
        }

        this.confirm.emit();
    }
}
