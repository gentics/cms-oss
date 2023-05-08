import { ObservableStopper } from '@admin-ui/common/utils';
import { Component, OnDestroy, OnInit } from '@angular/core';
import { AbstractControl, UntypedFormArray, UntypedFormBuilder, UntypedFormControl, UntypedFormGroup, Validators } from '@angular/forms';
import { Node, NodeCopyRequest, Raw } from '@gentics/cms-models';
import { IModalDialog } from '@gentics/ui-core';
import { takeUntil } from 'rxjs/operators';

@Component({
    selector: 'gtx-copy-nodes-modal',
    templateUrl: './copy-nodes-modal.component.html',
    styleUrls: ['./copy-nodes-modal.component.scss'],
})
export class CopyNodesModalComponent implements IModalDialog, OnDestroy, OnInit {

    nodesToBeCopied: Node<Raw>[];

    formGroup: UntypedFormGroup;

    defaultActionRequest: NodeCopyRequest = {
        pages: true,
        templates: true,
        files: true,
        workflows: true,
        copies: 1,
    };

    userInput: { nodeId: number, requestPayload: NodeCopyRequest }[] = [];

    private stopper = new ObservableStopper();

    constructor(
        private formBuilder: UntypedFormBuilder,
    ) { }

    ngOnInit(): void {
        this.initForm();
    }

    ngOnDestroy(): void {
        this.stopper.stop();
    }

    closeFn = (userInput: { nodeId: number, requestPayload: NodeCopyRequest }[]) => {};
    cancelFn = () => {};

    registerCloseFn(close: (userInput: { nodeId: number, requestPayload: NodeCopyRequest }[]) => void): void {
        this.closeFn = close;
    }

    registerCancelFn(cancel: (val?: any) => void): void {
        this.cancelFn = cancel;
    }

    onBtnOkayClicked(): void {
        this.closeFn(this.userInput);
    }

    getSubForms(): AbstractControl[] {
        return this.formGroup && (this.formGroup.get('subForms') as UntypedFormArray).controls;
    }

    getNodeName(control: UntypedFormControl): string | null {
        const c = control.get('nodeName');
        return c ? c.value : null;
    }

    removeNodeFromList(index: number): void {
        (this.formGroup.get('subForms') as UntypedFormArray).removeAt(index);
        // if no node is left for copy, close modal
        if (this.getSubForms().length === 0) {
            this.cancelFn();
        }
    }

    private initForm(): void {
        this.formGroup = this.formBuilder.group({
            subForms: this.formBuilder.array(this.createSubForms()),
        });

        this.formGroup.valueChanges.pipe(
            takeUntil(this.stopper.stopper$),
        ).subscribe((formValue: { subForms: any[] }) => {
            this.userInput = formValue.subForms.map(subForm => ({
                nodeId: subForm.nodeId,
                requestPayload: {
                    pages: subForm.pages,
                    templates: subForm.templates,
                    files: subForm.files,
                    workflows: subForm.workflows,
                    copies: subForm.copies,
                },
            }));
        });

        // trigger form at least once
        this.formGroup.updateValueAndValidity({ onlySelf: false, emitEvent: true });
    }

    private createSubForms(): UntypedFormGroup[] {
        return this.nodesToBeCopied.map(node => {
            return this.formBuilder.group({
                nodeId: [ node.id ],
                nodeName: [ node.name ],
                pages: [ this.defaultActionRequest.pages ],
                templates: [ this.defaultActionRequest.templates ],
                files: [ this.defaultActionRequest.files ],
                workflows: [ this.defaultActionRequest.workflows ],
                copies: [ this.defaultActionRequest.copies, Validators.required ],
            });
        });
    }

}
