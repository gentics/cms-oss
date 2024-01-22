import { Component } from '@angular/core';
import { IModalDialog, ModalClosingReason } from '../../common';

@Component({ template: '' })
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
