import { ChangeDetectionStrategy, ChangeDetectorRef, Component, ElementRef, Input, OnInit, ViewChild } from '@angular/core';
import { FormControl } from '@angular/forms';
import { RENDERING_CONTEXT_DROPDOWN } from '@editor-ui/app/common/models';
import { DynamicDropdownConfiguration } from '@gentics/aloha-models';
import { ModalCloseError, ModalClosingReason } from '@gentics/cms-integration-api-models';
import { BaseComponent } from '@gentics/ui-core';
import { combineLatest } from 'rxjs';
import { filter, map } from 'rxjs/operators';
import { applyControl, focusFirst } from '../../utils';

type CloseFn<T> = (value: T) => void;
type ErrorFn = (error?: any) => void;

@Component({
    selector: 'gtx-dynamic-dropdown',
    templateUrl: './dynamic-dropdown.component.html',
    styleUrls: ['./dynamic-dropdown.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DynamicDropdownComponent<T> extends BaseComponent implements OnInit {

    public readonly RENDERING_CONTEXT_DROPDOWN = RENDERING_CONTEXT_DROPDOWN;

    @Input()
    public configuration: DynamicDropdownConfiguration<T>;

    @Input()
    public label?: string;

    @ViewChild('content')
    public element: ElementRef<HTMLElement>;

    public controlNeedsConfirm = false;
    public showOverlay = false;
    public control: FormControl<T>;
    public hideHeader = false;

    protected closeFn: CloseFn<T> = () => {};
    protected errorFn: ErrorFn = () => {};

    constructor(
        changeDetector: ChangeDetectorRef,
    ) {
        super(changeDetector);
    }

    public ngOnInit(): void {
        this.control = new FormControl(this.configuration.initialValue);

        const forwardSub = applyControl(this.control, this.configuration);
        if (forwardSub) {
            this.subscriptions.push(forwardSub);
        }

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
                this.closeIfValid(value);
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
        this.closeIfValid(this.control.value);
    }

    public handleManualComponentConfirm(): void {
        this.closeIfValid(this.control.value);
    }

    public handleHideHeader(hide: boolean): void {
        this.hideHeader = hide;
    }

    public closeIfValid(value: T): void {
        if (this.control.valid) {
            this.closeFn(value);
        }
    }

    public handleAbortClick(): void {
        this.errorFn(new ModalCloseError(ModalClosingReason.CANCEL));
    }

    public focusFirstElement(): void {
        setTimeout(() => {
            focusFirst(this.element.nativeElement);
        }, 10);
    }
}
