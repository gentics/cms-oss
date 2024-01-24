import { ChangeDetectionStrategy, ChangeDetectorRef, Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { FormControl } from '@angular/forms';
import { AlohaComponent, AlohaCoreComponentNames } from '@gentics/aloha-models';
import { BaseFormElementComponent, generateFormProvider } from '@gentics/ui-core';

@Component({
    selector: 'gtx-aloha-component-renderer',
    templateUrl: './aloha-component-renderer.component.html',
    styleUrls: ['./aloha-component-renderer.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    providers: [generateFormProvider(AlohaComponentRendererComponent)],
})
export class AlohaComponentRendererComponent<T> extends BaseFormElementComponent<T> implements OnInit {

    public readonly AlohaCoreComponentNames = AlohaCoreComponentNames;

    @Input()
    public slot?: string;

    @Input()
    public component?: AlohaComponent;

    @Input()
    public type?: string;

    @Input()
    public settings?: Record<string, any>;

    /**
     * @see BaseAlohaRendererComponent.requiresConfirm
     */
    @Output()
    public requiresConfirm = new EventEmitter<boolean>();

    /**
     * @see BaseAlohaRendererComponent.manualConfirm
     */
    @Output()
    public manualConfirm = new EventEmitter<void>();

    public control: FormControl<T>;

    constructor(
        changeDetector: ChangeDetectorRef,
    ) {
        super(changeDetector);
    }

    public ngOnInit(): void {
        this.control = new FormControl(this.value);
        this.subscriptions.push(this.control.valueChanges.subscribe(value => {
            if (value !== this.value) {
                this.triggerChange(value);
            }
        }));
    }

    public forwardRequiresConfirm(confirm: boolean): void {
        this.requiresConfirm.emit(confirm);
    }

    public forwardManualConfirm(): void {
        this.manualConfirm.emit();
    }

    protected onValueChange(): void {
        if (this.control.value !== this.value) {
            this.control.setValue(this.value);
        }
    }
}
