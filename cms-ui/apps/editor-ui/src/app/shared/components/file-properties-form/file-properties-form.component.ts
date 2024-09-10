import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { FormControl, FormGroup, Validators } from '@angular/forms';
import { ApplicationStateService, MarkObjectPropertiesAsModifiedAction } from '@editor-ui/app/state';
import { BasePropertiesComponent, FormProperties } from '@gentics/cms-components';
import { EditableFileProps, Feature, NodeFeature } from '@gentics/cms-models';
import { generateFormProvider, generateValidatorProvider, setControlsEnabled } from '@gentics/ui-core';
import { filter, map, switchMap } from 'rxjs/operators';

@Component({
    selector: 'file-properties-form',
    templateUrl: './file-properties-form.tpl.html',
    styleUrls: ['./file-properties-form.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    providers: [
        generateFormProvider(FilePropertiesFormComponent),
        generateValidatorProvider(FilePropertiesFormComponent),
    ],
})
export class FilePropertiesFormComponent extends BasePropertiesComponent<EditableFileProps> implements OnInit {

    contentAutoOfflineEnabled = false;
    niceUrlsEnabled = false;

    constructor(
        changeDetector: ChangeDetectorRef,
        private appState: ApplicationStateService,
    ) {
        super(changeDetector);
    }

    public override ngOnInit(): void {
        super.ngOnInit();

        this.subscriptions.push(this.form.valueChanges.subscribe(() => {
            // notify state about entity properties validity -> relevant for `ContentFrame.modifiedObjectPropertyValid`
            this.appState.dispatch(new MarkObjectPropertiesAsModifiedAction(this.form.dirty, this.form.valid));
        }));

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
            name: new FormControl(this.value?.name, Validators.required),
            description: new FormControl(this.value?.description),
            niceUrl: new FormControl(this.value?.niceUrl),
            forceOnline: new FormControl(this.value?.forceOnline),
            online: new FormControl(this.value?.online),
            alternateUrls: new FormControl(this.value?.alternateUrls),
        });
    }

    protected configureForm(value: EditableFileProps, loud?: boolean): void {
        const options = { emitEvent: loud };
        setControlsEnabled(this.form, ['forceOnline'], this.contentAutoOfflineEnabled, options);
        setControlsEnabled(this.form, ['niceUrl', 'alternateUrls'], this.niceUrlsEnabled, options);
    }

    protected assembleValue(value: EditableFileProps): EditableFileProps {
        return value;
    }
}
