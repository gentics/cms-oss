import { Component } from '@angular/core';
import { ModalClosingReason } from '@gentics/cms-integration-api-models';
import { IModalDialog } from '../../common';

@Component({
    template: '',
    standalone: false
})
export abstract class BaseModal<T> implements IModalDialog {

    closeFn: (value: T, reason?: ModalClosingReason) => void = () => {};
    cancelFn: (value?: T, reason?: ModalClosingReason) => void = () => {};
    errorFn: (error: Error) => void = () => {};

    registerCloseFn(fn: (value: T) => void): void {
        this.closeFn = fn;
    }

    registerCancelFn(fn: (value?: T) => void): void {
        this.cancelFn = fn;
    }

    registerErrorFn(fn: (err: Error) => void): void {
        this.errorFn = fn;
    }
}
