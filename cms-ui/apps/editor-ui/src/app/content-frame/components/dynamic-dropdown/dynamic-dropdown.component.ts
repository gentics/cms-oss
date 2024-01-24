import { ChangeDetectionStrategy, Component, Input, OnInit } from '@angular/core';
import { FormControl } from '@angular/forms';
import { AlohaCoreComponentNames } from '@gentics/aloha-models';
import { DynamicDropdownConfiguration } from '@gentics/cms-integration-api-models';
import { BaseComponent, ModalCloseError, ModalClosingReason } from '@gentics/ui-core';
import { combineLatest } from 'rxjs';
import { filter, map } from 'rxjs/operators';

type CloseFn<T> = (value: T) => void;
type ErrorFn = (error?: any) => void;

@Component({
    selector: 'gtx-dynamic-dropdown',
    templateUrl: './dynamic-dropdown.component.html',
    styleUrls: ['./dynamic-dropdown.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DynamicDropdownComponent<T> extends BaseComponent implements OnInit {

    @Input()
    public configuration: DynamicDropdownConfiguration<T>;

    @Input()
    public label?: string;

    public controlNeedsConfirm = false;
    public showOverlay = false;
    public control: FormControl<T>;

    protected closeFn: CloseFn<T> = () => {};
    protected errorFn: ErrorFn = () => {};

    public ngOnInit(): void {
        this.control = new FormControl(this.configuration.initialValue);

        this.showOverlay = this.configuration.closeOnOverlayClick == null || this.configuration.closeOnOverlayClick;

        // When we don't resolve with a confirmation button,
        // then we need to resolve as soon as we get the first value.
        this.subscriptions.push(combineLatest([
            this.control.statusChanges,
            this.control.valueChanges,
        ]).pipe(
            filter(([status]) => status === 'VALID'),
            map(([, value]) => value),
        ).subscribe(value => {
            if (!this.configuration.resolveWithConfirmButton && !this.controlNeedsConfirm) {
                this.closeFn(value);
            }
        }));
    }

    public updateNeedsConfirm(confirm: boolean): void {
        this.controlNeedsConfirm = confirm;
        this.changeDetector.markForCheck();
    }

    public registerCloseFn(fn: CloseFn<T>): void {
        this.closeFn = fn;
    }

    public registerErrorFn(fn: ErrorFn): void {
        this.errorFn = fn;
    }

    public handleOverlayClick(): void {
        this.errorFn(new ModalCloseError(ModalClosingReason.OVERLAY_CLICK));
    }

    public handleConfirmClick(): void {
        this.closeFn(this.control.value);
    }

    public handleAbortClick(): void {
        this.errorFn(new ModalCloseError(ModalClosingReason.CANCEL));
    }
}
