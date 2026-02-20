import {
    ChangeDetectionStrategy,
    ChangeDetectorRef,
    Component,
    EventEmitter,
    Input,
    OnChanges,
    OnInit,
    Output,
} from '@angular/core';
import { FormControl, Validators } from '@angular/forms';
import {
    InheritableItem,
    ItemPermissions,
    Language,
    Node,
    Template,
} from '@gentics/cms-models';
import { BaseFormElementComponent, ChangesOf, generateFormProvider, setEnabled } from '@gentics/ui-core';
import { isEqual } from 'lodash-es';
import { combineLatest } from 'rxjs';
import { distinctUntilChanged, skip } from 'rxjs/operators';
import { EditableProperties } from '../../../common/models';
import { ApplicationStateService, MarkObjectPropertiesAsModifiedAction } from '../../../state';
import { NodePropertiesMode } from '../node-properties/node-properties.component';

@Component({
    selector: 'gtx-properties-editor',
    templateUrl: './properties-editor.component.html',
    styleUrls: ['./properties-editor.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    providers: [generateFormProvider(PropertiesEditorComponent)],
    standalone: false,
})
export class PropertiesEditorComponent
    extends BaseFormElementComponent<EditableProperties>
    implements OnInit, OnChanges {

    public readonly NodePropertiesMode = NodePropertiesMode;

    @Input({ required: true })
    public item: InheritableItem | Node;

    @Input({ required: true })
    public permissions: ItemPermissions;

    @Input({ required: true })
    public nodeId: number;

    @Input({ required: true })
    public templates: Template[] = [];

    @Input({ required: true })
    public languages: Language[] = [];

    @Input({ required: true })
    public itemClean = true;

    @Output()
    public itemCleanChange = new EventEmitter<boolean>();

    public control: FormControl<EditableProperties>;

    constructor(
        changeDetector: ChangeDetectorRef,
        private appState: ApplicationStateService,
    ) {
        super(changeDetector);
    }

    ngOnInit(): void {
        this.control = new FormControl({ value: this.value, disabled: !this.permissions?.edit }, Validators.required);

        this.subscriptions.push(this.control.valueChanges.pipe(
            distinctUntilChanged(isEqual),
            skip(1),
        ).subscribe((value) => {
            this.triggerChange(value);
        }));

        this.subscriptions.push(combineLatest([
            this.control.valueChanges,
            this.control.statusChanges,
        ]).subscribe(() => {
            this.appState.dispatch(new MarkObjectPropertiesAsModifiedAction(this.control.dirty, this.control.valid));
        }));
    }

    override ngOnChanges(changes: ChangesOf<this>): void {
        super.ngOnChanges(changes);

        if (changes.permissions && this.control) {
            setEnabled(this.control, this.permissions.edit);
            this.control.updateValueAndValidity();
        }

        if (changes.itemClean && !changes.itemClean.firstChange && this.itemClean && this.control) {
            this.control.markAsPristine();
        }
    }

    protected onValueChange(): void {
        if (this.control && !isEqual(this.value, this.control.value)) {
            this.control.setValue(this.value);
        }
    }

    forwardItemCleanChange(value: boolean): void {
        this.itemCleanChange.emit(value);
    }
}
