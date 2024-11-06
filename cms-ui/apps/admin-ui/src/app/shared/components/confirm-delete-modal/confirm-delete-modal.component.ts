import { I18nService } from '@admin-ui/core';
import { Component, OnInit } from '@angular/core';
import { NormalizableEntityType } from '@gentics/cms-models';
import { IModalDialog } from '@gentics/ui-core';

@Component({
    selector: 'gtx-confirm-delete-modal',
    templateUrl: './confirm-delete-modal.component.html',
})
export class ConfirmDeleteModalComponent<T extends NormalizableEntityType> implements IModalDialog, OnInit {

    /** Name of the entity */
    entityIdentifier: T;
    entityIdentifierTranslation: string;
    entityAmount: number;
    entityNames: string[];
    confirmDeleteMessageTranslation: string;

    constructor(
        private i18n: I18nService,
    ) { }

    ngOnInit(): void {
        this.entityAmount = this.entityNames.length;
        if (this.entityAmount !== 1) {
            this.entityIdentifierTranslation = this.i18n.instant(`common.${this.entityIdentifier}_plural`);
            this.confirmDeleteMessageTranslation = this.i18n.instant(
                'modal.confirm_delete_message_plural',
                { entityName: this.entityIdentifierTranslation },
            );
        } else {
            this.entityIdentifierTranslation = this.i18n.instant(`common.${this.entityIdentifier}_singular`);
            this.confirmDeleteMessageTranslation = this.i18n.instant(
                'modal.confirm_delete_message_singular',
                { entityName: this.entityIdentifierTranslation },
            );
        }
    }

    closeFn = (deleteConfirmed: boolean) => {};
    cancelFn = () => {};

    registerCloseFn(close: (val?: any) => void): void {
        this.closeFn = (deleteConfirmed: boolean) => {
            close(deleteConfirmed);
        };
    }

    registerCancelFn(cancel: (val?: any) => void): void {
        this.cancelFn = cancel;
    }

    buttonDeleteConfirmClicked(): void {
        this.closeFn(true);
    }
}
