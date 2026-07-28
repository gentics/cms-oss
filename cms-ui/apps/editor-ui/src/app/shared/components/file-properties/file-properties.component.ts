import { ChangeDetectionStrategy, ChangeDetectorRef, Component, Input, OnInit } from '@angular/core';
import { FormControl, FormGroup, Validators } from '@angular/forms';
import { BasePropertiesComponent } from '@gentics/cms-components';
import { EditableFileProps, Feature, FileOrImage, NodeFeature } from '@gentics/cms-models';
import { FormProperties, generateFormProvider, generateValidatorProvider, setControlsEnabled } from '@gentics/ui-core';
import { filter, map, switchMap } from 'rxjs/operators';
import { ApplicationStateService } from '../../../state';

@Component({
    selector: 'gtx-file-properties',
    templateUrl: './file-properties.component.html',
    styleUrls: ['./file-properties.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    providers: [
        generateFormProvider(FilePropertiesComponent),
        generateValidatorProvider(FilePropertiesComponent),
    ],
    standalone: false
})
export class FilePropertiesComponent extends BasePropertiesComponent<EditableFileProps> implements OnInit {

    @Input()
    public item: FileOrImage;

    public contentAutoOfflineEnabled = false;
    public niceUrlsEnabled = false;

    constructor(
        changeDetector: ChangeDetectorRef,
        private appState: ApplicationStateService,
    ) {
        super(changeDetector);
    }

    public override ngOnInit(): void {
        super.ngOnInit();

        this.subscriptions.push(this.appState.select(state => state.editor.nodeId).pipe(
            switchMap(nodeId => this.appState.select(state => state.features.nodeFeatures[nodeId])),
            filter(nodeFeatures => !!nodeFeatures),
            map(nodeFeatures => nodeFeatures.includes(NodeFeature.CONTENT_AUTO_OFFLINE)),
        ).subscribe(enabled => {
            this.contentAutoOfflineEnabled = enabled;
            this.configureForm(this.form.value, true);
            this.changeDetector.markForCheck();
        }));

        this.subscriptions.push(this.appState.select(state => state.features[Feature.NICE_URLS]).subscribe(enabled => {
            this.niceUrlsEnabled = enabled;
            this.configureForm(this.form.value, true);
            this.changeDetector.markForCheck();
        }));
    }

    protected createForm(): FormGroup {
        return new FormGroup<FormProperties<EditableFileProps>>({
            name: new FormControl(this.safeValue('name'), Validators.required),
            description: new FormControl(this.safeValue('description')),
            niceUrl: new FormControl(this.safeValue('niceUrl')),
            forceOnline: new FormControl(this.safeValue('forceOnline')),
            alternateUrls: new FormControl(this.safeValue('alternateUrls')),
            customCdate: new FormControl(this.safeValue('customCdate')),
            customEdate: new FormControl(this.safeValue('customEdate')),
        });
    }

    protected configureForm(value: EditableFileProps, loud?: boolean): void {
        const options = { emitEvent: loud };
        setControlsEnabled(this.form, ['forceOnline'], !this.disabled && this.contentAutoOfflineEnabled, options);
        setControlsEnabled(this.form, ['niceUrl', 'alternateUrls'], !this.disabled && this.niceUrlsEnabled, options);
    }

    protected assembleValue(value: EditableFileProps): EditableFileProps {
        return {
            ...value,
            customCdate: value?.customCdate || 0,
            customEdate: value?.customEdate || 0,
        };
    }

    protected override onValueChange(): void {
        if (this.form) {
            const tmpObj = {};
            Object.keys(this.form.controls).forEach((controlName: keyof EditableFileProps) => {
                if (this.value != null && this.value.hasOwnProperty(controlName)) {
                    // Edge case for custom dates - The API requires them to be not-null to not be ignored during updates.
                    // However, a `0` would still be a valid timestamp, so we check it here explicitly and mark it as null.
                    if (
                        (controlName === 'customCdate' || controlName === 'customEdate')
                        && (this.value[controlName] === 0 || this.value[controlName] == null)
                    ) {
                        tmpObj[controlName] = null;
                    } else {
                        tmpObj[controlName] = this.value[controlName];
                    }
                }
            });
            this.form.patchValue(tmpObj);
        }
    }
}
