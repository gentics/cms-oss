import { ChangeDetectionStrategy, ChangeDetectorRef, Component, EventEmitter, Input, Output, SimpleChanges } from '@angular/core';
import { UntypedFormControl, UntypedFormGroup, Validators } from '@angular/forms';
import { ApplicationStateService, FeaturesActionsService, MarkObjectPropertiesAsModifiedAction } from '@editor-ui/app/state';
import { EditableFileProps, Feature, NodeFeature } from '@gentics/cms-models';
import { createMultiValuePatternValidator } from '@gentics/ui-core';
import { Observable, Subscription } from 'rxjs';
import { filter, map, switchMap } from 'rxjs/operators';

@Component({
    selector: 'file-properties-form',
    templateUrl: './file-properties-form.tpl.html',
    styleUrls: ['./file-properties-form.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class FilePropertiesForm {

    @Input()
    properties: EditableFileProps = {};

    @Input()
    disabled = false;

    @Output()
    changes = new EventEmitter<EditableFileProps>();

    form: UntypedFormGroup;
    changeSub: Subscription;
    contentAutoOfflineActive$: Observable<boolean>;

    // Note from Norbert -> This is configurable in the backend and might need to be updated
    // or checked async via an async-validator.
    urlPattern = '[\\w\\._\\-\\/]+';
    niceUrlsActivated = false;
    niceUrlsChecked = false;
    origFileName: string;

    constructor(
        private changeDetector: ChangeDetectorRef,
        private appState: ApplicationStateService,
        private featuresActions: FeaturesActionsService,
    ) { }

    ngOnInit(): void {
        this.contentAutoOfflineActive$ = this.appState.select(state => state.editor.nodeId).pipe(
            switchMap(nodeId => this.appState.select(state => state.features.nodeFeatures[nodeId])),
            filter(nodeFeatures => !!nodeFeatures),
            map(nodeFeatures => nodeFeatures.findIndex(value => value === NodeFeature.CONTENT_AUTO_OFFLINE) !== -1),
        );

        this.form = new UntypedFormGroup({
            name: new UntypedFormControl(this.properties.name || '', Validators.required),
            description: new UntypedFormControl(this.properties.description || ''),
            forceOnline: new UntypedFormControl(this.properties.forceOnline),
            niceUrl: new UntypedFormControl(this.properties.niceUrl || '', createMultiValuePatternValidator(this.urlPattern)),
            alternateUrls: new UntypedFormControl(this.properties.alternateUrls || [], createMultiValuePatternValidator(this.urlPattern)),
        });

        this.form.get('niceUrl').disable({ emitEvent: false });
        this.form.get('alternateUrls').disable({ emitEvent: false });

        this.featuresActions.checkFeature(Feature.NICE_URLS)
            .then(active => {
                if (active) {
                    this.form.get('niceUrl').enable({ emitEvent: false });
                    this.form.get('alternateUrls').enable({ emitEvent: false });
                    this.niceUrlsActivated = true;
                }
                this.niceUrlsChecked = true;
                this.changeDetector.markForCheck();
            });

        this.changeSub = this.form.valueChanges.subscribe(changes => {
            this.changes.emit(changes);

            // notify state about entity properties validity -> relevant for `ContentFrame.modifiedObjectPropertyValid`
            const isValid = this.form.valid;
            this.appState.dispatch(new MarkObjectPropertiesAsModifiedAction(true, isValid));
        });
    }

    ngOnChanges(changes: SimpleChanges): void {
        if (changes['properties']) {
            this.updateForm(this.properties);
        }
    }

    ngOnDestroy(): void {
        if (this.changeSub) {
            this.changeSub.unsubscribe();
        }
    }

    priorityRangeChanged(value: number | Event): void {
        if (typeof value === 'number') {
            this.form.get('priority').setValue(value);
        }
    }

    updateForm(properties: EditableFileProps): void {
        if (!this.form) {
            return;
        }
        this.origFileName = this.properties.name;
        this.form.patchValue({
            name: properties.name,
            description: properties.description,
            forceOnline: properties.forceOnline,
            niceUrl: properties.niceUrl,
            alternateUrls: properties.alternateUrls,
        }, { emitEvent: false });
    }
}
