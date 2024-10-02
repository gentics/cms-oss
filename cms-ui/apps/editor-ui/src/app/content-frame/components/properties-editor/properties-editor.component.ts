/* eslint-disable @typescript-eslint/naming-convention */
import {
    ChangeDetectionStrategy,
    ChangeDetectorRef,
    Component,
    EventEmitter,
    Input,
    OnChanges,
    OnInit,
    Output,
    SimpleChange,
} from '@angular/core';
import { FormControl, Validators } from '@angular/forms';
import { EditableProperties } from '@editor-ui/app/common/models';
import { ApplicationStateService, MarkObjectPropertiesAsModifiedAction } from '@editor-ui/app/state';
import {
    InheritableItem,
    Language,
    Node,
    Template,
} from '@gentics/cms-models';
import { BaseFormElementComponent, generateFormProvider, setEnabled } from '@gentics/ui-core';
import { isEqual } from 'lodash-es';
import { BehaviorSubject, combineLatest } from 'rxjs';
import { debounceTime, distinctUntilChanged, map, skip, switchMap, tap } from 'rxjs/operators';
import { PermissionService } from '../../../core/providers/permissions/permission.service';
import { NodePropertiesMode } from '../node-properties/node-properties.component';

@Component({
    selector: 'gtx-properties-editor',
    templateUrl: './properties-editor.component.html',
    styleUrls: ['./properties-editor.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    providers: [generateFormProvider(PropertiesEditorComponent)],
})
export class PropertiesEditorComponent
    extends BaseFormElementComponent<EditableProperties>
    implements OnInit, OnChanges {

    public readonly NodePropertiesMode = NodePropertiesMode;

    @Input({ required: true })
    public item: InheritableItem | Node;

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

    /** Behaviour to call whenever a new permission check needs to occur */
    private permissionCheck = new BehaviorSubject<void>(undefined);

    public control: FormControl<EditableProperties>;

    constructor(
        changeDetector: ChangeDetectorRef,
        private permissions: PermissionService,
        private appState: ApplicationStateService,
    ) {
        super(changeDetector);
    }

    ngOnInit(): void {
        this.control = new FormControl({ value: this.value, disabled: true }, Validators.required);

        this.subscriptions.push(this.control.valueChanges.pipe(
            distinctUntilChanged(isEqual),
            skip(1),
        ).subscribe(value => {
            this.triggerChange(value);
        }));

        this.subscriptions.push(combineLatest([
            this.control.valueChanges,
            this.control.statusChanges,
        ]).subscribe(() => {
            this.appState.dispatch(new MarkObjectPropertiesAsModifiedAction(this.control.dirty, this.control.valid));
        }));

        this.subscriptions.push(this.permissionCheck.pipe(
            // Set the control disabled until we know the permissions
            tap(() => {
                setEnabled(this.control, false);
                this.changeDetector.markForCheck();
            }),
            // Just in case it's getting spammed
            debounceTime(50),
            switchMap(() => {
                if (this.item.type === 'folder') {
                    return this.permissions.forFolder(this.item.id, this.nodeId).pipe(
                        map(permission => {
                            return permission.folder.edit;
                        }),
                    );
                } else if (this.item.type === 'node' || this.item.type === 'channel') {
                    return this.permissions.forFolder(this.item.folderId, this.item.id).pipe(
                        map(permission => {
                            return permission.folder.edit;
                        }),
                    );
                }

                return this.permissions.forItem(this.item, this.nodeId).pipe(
                    map(permission => {
                        return permission.edit;
                    }),
                );
            }),
        ).subscribe(enabled => {
            setEnabled(this.control, enabled, { onlySelf: true });
            this.changeDetector.markForCheck();
        }));
    }

    override ngOnChanges(changes: { [K in keyof this]: SimpleChange }): void {
        super.ngOnChanges(changes);

        if (changes.item || changes.nodeId) {
            this.permissionCheck.next();
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
