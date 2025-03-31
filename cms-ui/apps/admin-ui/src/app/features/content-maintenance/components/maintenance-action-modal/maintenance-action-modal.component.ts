import { ContentRepositoryBO } from '@admin-ui/common';
import { ContentRepositoryHandlerService } from '@admin-ui/core';
import { AdminOperations } from '@admin-ui/core/providers/operations/admin/admin.operations';
import { ChangeDetectionStrategy, Component, Input, OnDestroy, OnInit } from '@angular/core';
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { ContentMaintenanceType, Response } from '@gentics/cms-models';
import { BaseModal } from '@gentics/ui-core';
import { Observable, Subscription } from 'rxjs';
import { map } from 'rxjs/operators';

export enum MaintenanceActionModalAction {
    REPUBLISH_OBJECTS = 'republish_objects',
    DELAY_OBJECTS = 'delay_objects',
    REPUBLISH_DELAYED_OBJECTS = 'republish_delayed_objects',
    MARK_OBJECTS_AS_PUBLISHED = 'mark_objects_as_published',
}

@Component({
    selector: 'gtx-maintenance-action-modal',
    templateUrl: './maintenance-action-modal.component.html',
    styleUrls: ['./maintenance-action-modal.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MaintenanceActionModalComponent extends BaseModal<boolean> implements OnInit, OnDestroy {

    public readonly TYPE_LIST: ContentMaintenanceType[] = [
        ContentMaintenanceType.file,
        ContentMaintenanceType.folder,
        ContentMaintenanceType.page,
        ContentMaintenanceType.form,
    ];

    @Input()
    public modalAction: MaintenanceActionModalAction;

    @Input()
    public selectedNodeIds: number[] = [];

    formGroup: UntypedFormGroup;

    allContentRespositories$: Observable<ContentRepositoryBO[]>;

    private subscriptions: Subscription[] = [];

    constructor(
        private adminOperations: AdminOperations,
        private formBuilder: UntypedFormBuilder,
        private contentRepositoryHandler: ContentRepositoryHandlerService,
    ) {
        super();
    }

    ngOnInit(): void {
        // fetch form data options
        this.allContentRespositories$ = this.contentRepositoryHandler.listMapped().pipe(
            map(list => list.items),
        );

        // init form
        this.formGroup = this.formBuilder.group({
            types: [null, Validators.required],
            contentRepositories: [null],
            attributes: [{ value: '', disabled: this.modalAction !== MaintenanceActionModalAction.REPUBLISH_OBJECTS }],
            clearPublishCache: [{ value: false, disabled: this.modalAction !== MaintenanceActionModalAction.REPUBLISH_OBJECTS }],
            limitToDateRange: [false],
            start: [ ],
            end: [ ],
        });

        // listen to form value changes
        this.subscriptions.push(this.formGroup.valueChanges.subscribe(value => {
            this.configureFormControls(value);
        }));
    }

    ngOnDestroy(): void {
        this.subscriptions.forEach(s => s.unsubscribe());
    }

    /**
     * If user clicks "OK"
     */
    buttonOkClicked(): void {
        this.performAction()
            .then(() => this.closeFn(true));
    }

    /**
     * Alter FormGroup depending from values, which input fields appear and disappear or change validation logic.
     *
     * @param value values of active fields to be to (re-)initialized
     */
    private configureFormControls(value: any): void {
        // TODO: Fix this mess
    }

    /**
     * After all data is set by user and valid this action assembles payload and executes action request.
     *
     * @returns action request response
     */
    private performAction(): Promise<Response> {
        const value = this.formGroup.value;

        // Disable form to prevent the user from modifying stuff
        this.formGroup.disable({ emitEvent: false });

        const payload: any = {
            nodes: this.selectedNodeIds,
            types: value.types,
            // eslint-disable-next-line @typescript-eslint/no-unsafe-call
            contentRepositories: (value.contentRepositories || []).map(crId => parseInt(crId, 10)),
        };

        if (value.limitToDateRange) {
            payload.start = value.start;
            payload.end = value.end;

            if (!payload.end) {
                payload.end = Math.floor(new Date().getTime() / 1000);
            }
        }

        let req: Observable<Response>;

        switch (this.modalAction) {
            case MaintenanceActionModalAction.REPUBLISH_OBJECTS: {
                const republishPayload = {
                    ...payload,
                    attributes: [],
                    clearPublishCache: value.clearPublishCache,
                };

                // extract comma-separated values from form data string
                const attributesRaw = String(value.attributes);
                // remove spaces
                const attributesSanitized = attributesRaw.replace( /\s/g, '');
                // convert to array
                const attributesParsed = attributesSanitized && attributesSanitized.split(',');
                if (attributesParsed?.length > 0) {
                    republishPayload.attributes = attributesParsed;
                }

                req = this.adminOperations.republishObjects(republishPayload);
                break;
            }

            case MaintenanceActionModalAction.DELAY_OBJECTS:
                req = this.adminOperations.delayObjects(payload);
                break;

            case MaintenanceActionModalAction.REPUBLISH_DELAYED_OBJECTS:
                req = this.adminOperations.republishDelayedObjects(payload);
                break;

            case MaintenanceActionModalAction.MARK_OBJECTS_AS_PUBLISHED:
                req = this.adminOperations.markObjectsAsPublished(payload);
                break;

            default:
                throw new Error(`Maintenance action "${this.modalAction}" not defined.`);
        }

        return req.toPromise()
            .finally(() => {
                this.formGroup.enable({ emitEvent: false });
            });
    }
}
