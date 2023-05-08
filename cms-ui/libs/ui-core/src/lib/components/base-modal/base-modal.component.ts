import { Component } from '@angular/core';
import { IModalDialog } from '../../common';

@Component({ template: '' })
export abstract class BaseModal<T> implements IModalDialog {

    closeFn: (value: T) => void;
    cancelFn: (value?: T) => void;
    errorFn: (error: Error) => void;

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
