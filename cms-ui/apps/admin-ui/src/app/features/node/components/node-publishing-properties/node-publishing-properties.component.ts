import { FormControlOnChangeFn, ObservableStopper } from '@admin-ui/common';
import { DisableableControlValueAccessor } from '@admin-ui/shared/directives/action-allowed/action-allowed.directive';
import { SelectState } from '@admin-ui/state';
import { ChangeDetectionStrategy, ChangeDetectorRef, Component, forwardRef, OnDestroy, OnInit, OnChanges, SimpleChanges, Input } from '@angular/core';
import { ControlValueAccessor, UntypedFormControl, UntypedFormGroup, NG_VALUE_ACCESSOR } from '@angular/forms';
import { ContentRepository, GtxNodePageLanguageCode, Index, IndexById, Normalized } from '@gentics/cms-models';
import { generateFormProvider } from '@gentics/ui-core';
import { isEqual as _isEqual } from 'lodash';
import { Observable } from 'rxjs';
import { distinctUntilChanged, map, startWith, takeUntil } from 'rxjs/operators';

/**
 * Defines the data editable by the `NodePropertiesComponent`.
 *
 * To convey the validity state of the user's input, the onChange callback will
 * be called with `null` if the form data is currently invalid.
 */
export interface NodePublishingPropertiesFormData {
    disableUpdates: boolean;
    fileSystem: boolean;
    fileSystemPages: boolean;
    fileSystemPagesDir: string;
    fileSystemFiles: boolean;
    fileSystemBinaryDir: string;
    contentRepository: boolean;
    contentRepositoryPages: boolean;
    contentRepositoryFiles: boolean;
    contentRepositoryFolders: boolean;
    contentRepositoryId: number;
    urlRenderWayPages: number;
    urlRenderWayFiles: number;
    omitPageExtension: boolean;
    pageLanguageCode: GtxNodePageLanguageCode;
}

/* eslint-disable @typescript-eslint/indent */
type ContentRepositoryControlNames = keyof Pick<NodePublishingPropertiesFormData,
    'contentRepositoryFiles'
    | 'contentRepositoryFolders'
    | 'contentRepositoryPages'
>;

type FileSystemControlNames = keyof Pick<NodePublishingPropertiesFormData,
    'fileSystemBinaryDir'
    | 'fileSystemFiles'
    | 'fileSystemPages'
    | 'fileSystemPagesDir'
>;

type SeoControlNames = keyof Pick<NodePublishingPropertiesFormData,
    'omitPageExtension'
    | 'pageLanguageCode'
>;
/* eslint-enable @typescript-eslint/indent */

@Component({
    selector: 'gtx-node-publishing-properties',
    templateUrl: './node-publishing-properties.component.html',
    styleUrls: ['./node-publishing-properties.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    providers: [generateFormProvider(NodePublishingPropertiesComponent)],
})
export class NodePublishingPropertiesComponent implements OnInit, OnChanges, OnDestroy, ControlValueAccessor, DisableableControlValueAccessor {

    public readonly GtxNodePageLanguageCode = GtxNodePageLanguageCode;

    public readonly URL_MODES = {
        0: 'node.url_mode_automatic',
        1: 'node.url_mode_plink',
        2: 'node.url_mode_dynamic',
        3: 'node.url_mode_w_domain',
        4: 'node.url_mode_wo_domain',
    };

    @Input()
    public disabled = false;

    @SelectState(state => state.entity.contentRepository)
    public contentRepositories$: Observable<IndexById<ContentRepository<Normalized>>>;

    public fgPublishing: UntypedFormGroup;

    public linkInputs: boolean;

    get contentRepositoryDisabled(): boolean {
        return !this.fgPublishing.get('contentRepository').value;
    }

    get fileSystemDisabled(): boolean {
        return !this.fgPublishing.get('fileSystem').value;
    }

    get fileSystemPagesDisabled(): boolean {
        return !this.fgPublishing.get('fileSystemPages').value;
    }

    get fileSystemFilesDisabled(): boolean {
        return !this.fgPublishing.get('fileSystemFiles').value;
    }

    private fileSystemControls: Index<FileSystemControlNames, UntypedFormControl>;
    private contentrepositoriesystemControls: Index<ContentRepositoryControlNames, UntypedFormControl>;
    private seoSystemControls: Index<SeoControlNames, UntypedFormControl>;

    private stopper = new ObservableStopper();

    constructor(
        private changeDetector: ChangeDetectorRef,
    ) {}

    ngOnInit(): void {
        this.initForm();
    }

    ngOnChanges(changes: SimpleChanges): void {
        if (changes.disabled) {
            this.setDisabledState(this.disabled);
        }
    }

    ngOnDestroy(): void {
        this.stopper.stop();
    }

    fileDirChange(cr: ContentRepository): void {
        if (cr.crType === 'mesh' && !cr.projectPerNode) {
            this.fgPublishing.patchValue({
                fileSystemPagesDir: null,
                fileSystemBinaryDir: null,
            }, {
                emitEvent: false,
            });

            this.fileSystemControls.fileSystemBinaryDir.disable();
            this.fileSystemControls.fileSystemPagesDir.disable();
        } else {
            this.fileSystemControls.fileSystemBinaryDir.enable();
            this.fileSystemControls.fileSystemPagesDir.enable();
        }

        this.fgPublishing.markAsPristine();
    }

    writeValue(value: NodePublishingPropertiesFormData): void {
        if (value) {
            // CRs are loaded directly from the state. In the state, they are stored as BOs,
            // where the ID has been converted from number to a string.
            // To make it properly visible in the select, we have to convert it to a string as well.
            if (value.contentRepositoryId != null && typeof value.contentRepositoryId !== 'string') {
                (value as any).contentRepositoryId = value.contentRepositoryId + '';
            }

            this.fgPublishing.patchValue(value);
            this.linkInputs = value.fileSystemBinaryDir === value.fileSystemPagesDir;
        } else {
            this.fgPublishing.reset();
        }
        this.onFormDataChange(this.fgPublishing.value);
        this.fgPublishing.markAsPristine();
    }

    registerOnChange(fn: FormControlOnChangeFn<NodePublishingPropertiesFormData>): void {
        this.fgPublishing.valueChanges.pipe(
            map((formData: NodePublishingPropertiesFormData) => this.fgPublishing.valid ? formData : null),
            takeUntil(this.stopper.stopper$),
        ).subscribe(fn);
    }

    registerOnTouched(fn: any): void { }

    setDisabledState(isDisabled: boolean): void {
        if (isDisabled) {
            this.fgPublishing.disable({ emitEvent: false, onlySelf: true });
        } else {
            this.fgPublishing.enable({ emitEvent: false, onlySelf: true });
        }
    }

    contentRepositoryChange(value: boolean): void {
        this.fgPublishing.patchValue({
            contentRepositoryPages: value,
            contentRepositoryFiles: value,
            contentRepositoryFolders: value,
        }, {
            emitEvent: true,
        });
    }

    fileSystemChange(value: boolean): void {
        this.fgPublishing.patchValue({
            fileSystemPages: value,
            fileSystemFiles: value,
        }, {
            emitEvent: true,
        });
    }

    fileSystemDirChange(value: string): void {
        if (this.linkInputs) {
            this.fgPublishing.patchValue({
                fileSystemPagesDir: value,
                fileSystemBinaryDir: value,
            }, {
                emitEvent: false,
            });
        }
    }

    toggleLinkInputs(): void {
        if (!this.linkInputs) {
            this.linkInputs = true;
            const pageDir = this.fgPublishing.get('fileSystemPagesDir').value;
            if (pageDir !== this.fgPublishing.get('fileSystemBinaryDir').value) {
                this.fgPublishing.get('fileSystemBinaryDir').setValue(pageDir);
            }
        } else {
            this.linkInputs = false;
        }
    }

    private initForm(): void {
        this.contentrepositoriesystemControls = {
            contentRepositoryFiles: new UntypedFormControl({ disabled: true, value: false }),
            contentRepositoryFolders: new UntypedFormControl({ disabled: true, value: false }),
            contentRepositoryPages: new UntypedFormControl({ disabled: true, value: false }),
        };

        this.fileSystemControls = {
            fileSystemPages: new UntypedFormControl({ disabled: true, value: false }),
            fileSystemPagesDir: new UntypedFormControl({ disabled: true, value: false }),
            fileSystemFiles: new UntypedFormControl({ disabled: true, value: false }),
            fileSystemBinaryDir: new UntypedFormControl({ disabled: true, value: false }),
        };

        this.seoSystemControls = {
            omitPageExtension: new UntypedFormControl(true),
            pageLanguageCode: new UntypedFormControl(GtxNodePageLanguageCode.NONE),
        };

        this.fgPublishing = new UntypedFormGroup({
            disableUpdates: new UntypedFormControl(null),
            fileSystem: new UntypedFormControl(null),
            ...this.fileSystemControls,
            contentRepository: new UntypedFormControl(null),
            ...this.contentrepositoriesystemControls,
            contentRepositoryId: new UntypedFormControl(null),
            urlRenderWayPages: new UntypedFormControl(null),
            urlRenderWayFiles: new UntypedFormControl(null),
            ...this.seoSystemControls,
        });

        this.fgPublishing.valueChanges.pipe(
            startWith({}),
            distinctUntilChanged(_isEqual),
            takeUntil(this.stopper.stopper$),
        ).subscribe(formData => this.onFormDataChange(formData));
    }

    private onFormDataChange(formData: NodePublishingPropertiesFormData): void {
        if (!formData) {
            formData = {} as any;
        }

        this.contentRepositories$.subscribe(cr => {
            Object.values(cr).forEach(contentRepository => {
                if (contentRepository.id === formData.contentRepositoryId) {
                    this.fileDirChange(contentRepository);
                }
            });
        });

        const fsPubEnabled = !!formData.fileSystem;
        const fsContentRepositoryEnabled = !!formData.contentRepository;

        this.setControlEnabledState(this.fileSystemControls.fileSystemFiles, fsPubEnabled);
        this.setControlEnabledState(this.fileSystemControls.fileSystemPages, fsPubEnabled);

        this.setControlEnabledState(this.contentrepositoriesystemControls.contentRepositoryFiles, fsContentRepositoryEnabled);
        this.setControlEnabledState(this.contentrepositoriesystemControls.contentRepositoryFolders, fsContentRepositoryEnabled);
        this.setControlEnabledState(this.contentrepositoriesystemControls.contentRepositoryPages, fsContentRepositoryEnabled);

        this.fgPublishing.updateValueAndValidity();
    }

    private setControlEnabledState(control: UntypedFormControl, enabled: boolean): void {
        if (control.enabled !== enabled) {
            if (enabled) {
                control.enable();
            } else {
                control.disable();
            }
        }
    }

}
