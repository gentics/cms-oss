import { FormControlOnChangeFn, FormControlOnTouchedFn } from '@admin-ui/common';
import { AppStateService } from '@admin-ui/state';
import { ChangeDetectionStrategy, ChangeDetectorRef, Component, Input, OnChanges, OnDestroy, OnInit, SimpleChanges } from '@angular/core';
import { ControlValueAccessor, UntypedFormControl, UntypedFormGroup, Validators } from '@angular/forms';
import { Node, Normalized } from '@gentics/cms-models';
import { generateFormProvider } from '@gentics/ui-core';
import { Observable, Subscription } from 'rxjs';
import { map } from 'rxjs/operators';
import { EntityManagerService } from '../../../../core';

/**
 * Defines the data editable by the `NodePropertiesComponent`.
 *
 * To convey the validity state of the user's input, the onChange callback will
 * be called with `null` if the form data is currently invalid.
 */
export interface NodePropertiesFormData {
    name: string;
    inheritedFromId?: number;
    description: string;
    https: boolean;
    hostname: string;
    meshPreviewUrl: string;
    insecurePreviewUrl: boolean;
    defaultFileFolderId: number;
    defaultImageFolderId: number;
    pubDirSegment: boolean;
}

@Component({
    selector: 'gtx-node-properties',
    templateUrl: './node-properties.component.html',
    styleUrls: ['./node-properties.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    providers: [generateFormProvider(NodePropertiesComponent)],
})
export class NodePropertiesComponent implements OnInit, OnChanges, OnDestroy, ControlValueAccessor {

    /**
     * Determines if changing the `inheritedFromId` is allowed.
     * This is normally only the case when creating a new node.
     */
    @Input()
    public allowChangingInheritedFrom = false;

    @Input()
    public disabled = false;

    /**
     * Determines if changing the default upload folders for files and images is allowed.
     *
     * For an existing node, this is normally allowed, while in the create node wizard it is not.
     */
    @Input()
    public allowEditingDefaultUploadFolders = true;

    /**
     * If global feature "pub_dir_segment" is activated, node will have this property.
     *
     * @see https://www.gentics.com/Content.Node/guides/feature_pub_dir_segment.html
     */
    @Input()
    public pubDirSegmentActivated: boolean;

    isChildNode: boolean;

    fgProperties: UntypedFormGroup;

    nodes$: Observable<Node<Normalized>[]>;

    /**
     * The node, from which the current node is inherited (if it is a channel).
     */
    inheritedFromNode$?: Observable<Node>;

    multiChannelingEnabled = false;

    private subscriptions: Subscription[] = [];

    constructor(
        private changeDetector: ChangeDetectorRef,
        private entityManager: EntityManagerService,
        private appState: AppStateService,
    ) { }

    ngOnInit(): void {
        this.nodes$ = this.entityManager.watchNormalizedEntitiesList('node');

        this.subscriptions.push(this.appState.select(state => state.features.global.multichanneling).subscribe(featureEnabled => {
            this.multiChannelingEnabled = featureEnabled;
            this.changeDetector.markForCheck();
        }));

        this.fgPropertiesInit();
    }

    ngOnChanges(changes: SimpleChanges): void {
        if (changes.disabled) {
            this.setDisabledState(this.disabled);
        }
    }

    ngOnDestroy(): void {
        this.subscriptions.forEach(s => s.unsubscribe());
    }

    writeValue(value: NodePropertiesFormData): void {
        if (value) {
            this.fgProperties.patchValue(value);
            if (typeof value.inheritedFromId === 'number') {
                this.inheritedFromNode$ = this.entityManager.getEntity('node', value.inheritedFromId);
            } else {
                this.inheritedFromNode$ = null;
            }
        } else {
            this.fgProperties.reset();
        }
        this.fgProperties.markAsPristine();
    }

    registerOnChange(fn: FormControlOnChangeFn<NodePropertiesFormData>): void {
        this.subscriptions.push(this.fgProperties.valueChanges.pipe(
            map((formData: NodePropertiesFormData) => {
                if (formData && typeof formData.inheritedFromId === 'number') {
                    this.isChildNode = true;
                } else {
                    this.isChildNode = false;
                }
                return this.fgProperties.valid ? formData : null;
            }),
        ).subscribe(fn));
    }

    registerOnTouched(fn: FormControlOnTouchedFn): void { }

    setDisabledState(isDisabled: boolean): void {
        if (isDisabled) {
            this.fgProperties.disable({ emitEvent: false });
        } else {
            this.fgProperties.enable({ emitEvent: false });
        }

        // ToDo: GTXPE-845
        // Until we have the repository browser in the Admin UI, we need to disable the default upload folders.
        this.fgProperties.controls.defaultFileFolderId.disable({ emitEvent: false });
        this.fgProperties.controls.defaultImageFolderId.disable({ emitEvent: false });
    }

    /**
     * Initialize form 'Properties'
     */
    private fgPropertiesInit(): void {
        this.fgProperties = new UntypedFormGroup({
            name: new UntypedFormControl(null, [ Validators.required ]),
            inheritedFromId: new UntypedFormControl(null),
            description: new UntypedFormControl(null),
            https: new UntypedFormControl(null),
            hostname: new UntypedFormControl(null, [ Validators.required ]),
            meshPreviewUrl: new UntypedFormControl(null),
            insecurePreviewUrl: new UntypedFormControl(null),
            pubDirSegment: new UntypedFormControl(null),

            // ToDo: GTXPE-845
            // Until we have the repository browser in the Admin UI, we need to disable the default upload folders.
            defaultFileFolderId: new UntypedFormControl({ value: null, disabled: true }),
            defaultImageFolderId: new UntypedFormControl({ value: null, disabled: true }),
        });
    }

}
