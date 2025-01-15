import { ChangeDetectionStrategy, Component, ElementRef, HostBinding, Input, OnDestroy, OnInit, ViewChild } from '@angular/core';
import { FormControl, FormGroup } from '@angular/forms';
import { RENDERING_CONTEXT_MODAL } from '@editor-ui/app/common/models';
import { DynamicFormModalConfiguration } from '@gentics/aloha-models';
import { BaseModal, FormProperties } from '@gentics/ui-core';
import { Subscription } from 'rxjs';
import { applyControl, focusFirst } from '../../utils';

@Component({
    selector: 'gtx-dynamic-form-modal',
    templateUrl: './dynamic-form-modal.component.html',
    styleUrls: ['./dynamic-form-modal.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DynamicFormModal<T> extends BaseModal<T> implements OnInit, OnDestroy {

    public readonly RENDERING_CONTEXT_MODAL = RENDERING_CONTEXT_MODAL;

    @Input()
    public configuration: DynamicFormModalConfiguration<any>;

    @HostBinding('attr.data-ref')
    public reference?: string;

    @ViewChild('content')
    public contentEl: ElementRef<HTMLElement>;

    public control: FormGroup<FormProperties<T>>;

    protected subscriptions: Subscription[] = [];

    public ngOnInit(): void {
        const group: FormProperties<T> = {} as any;
        Object.entries(this.configuration.controls).forEach(([name, ctl]) => {
            group[name] = new FormControl(this.configuration.initialValue?.[name]);
            const sub = applyControl(group[name], ctl);
            if (sub) {
                this.subscriptions.push(sub);
            }
        });
        this.control = new FormGroup(group);
        const sub = applyControl(this.control as any, this.configuration as any);
        if (sub) {
            this.subscriptions.push(sub);
        }
        this.reference = this.configuration?.openerReference;
    }

    public ngOnDestroy(): void {
        this.subscriptions.forEach(s => s.unsubscribe());
    }

    public focusFirstElement(): void {
        setTimeout(() => {
            focusFirst(this.contentEl.nativeElement);
        }, 100);
    }

    public handleConfirmClick(): void {
        this.closeFn(this.control.value as any);
    }

    public handleAbortClick(): void {
        this.cancelFn();
    }
}
