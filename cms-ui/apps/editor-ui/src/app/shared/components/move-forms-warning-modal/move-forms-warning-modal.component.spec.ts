import { ComponentFixture } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { Folder, Form, Node } from '@gentics/cms-models';
import { componentTest, configureComponentTest } from '../../../../testing';
import { ApplicationStateService } from '../../../state';
import { TestApplicationState } from '../../../state/test-application-state.mock';
import { TypeIconPipe } from '../../pipes/type-icon/type-icon.pipe';
import { MoveFormsWarningModal } from './move-forms-warning-modal.component';

const FORM_ONE = { id: 1, type: 'form', name: 'Contact Form' } as Form;
const FORM_TWO = { id: 2, type: 'form', name: 'Newsletter Form' } as Form;
const TARGET_FOLDER = { id: 10, type: 'folder', name: 'Target Folder' } as Folder;
const TARGET_NODE = { id: 20, type: 'node', name: 'Target Node' } as Node;

describe('MoveFormsWarningModal', () => {

    let closeSpy: jasmine.Spy;
    let cancelSpy: jasmine.Spy;

    function setup(fixture: ComponentFixture<MoveFormsWarningModal>, forms: Form[]): void {
        const instance = fixture.componentRef.instance;
        instance.forms = forms;
        instance.targetFolder = TARGET_FOLDER;
        instance.targetNode = TARGET_NODE;

        closeSpy = jasmine.createSpy('closeFn');
        cancelSpy = jasmine.createSpy('cancelFn');
        instance.registerCloseFn(closeSpy);
        instance.registerCancelFn(cancelSpy);

        fixture.detectChanges();
    }

    function getListedFormNames(fixture: ComponentFixture<MoveFormsWarningModal>): string[] {
        return fixture.debugElement
            .queryAll(By.css('.forms-to-move gtx-contents-list-item .item-name'))
            .map((el) => (el.nativeElement as HTMLElement).textContent.trim());
    }

    function getWarningText(fixture: ComponentFixture<MoveFormsWarningModal>): string {
        return (fixture.debugElement.query(By.css('.warning')).nativeElement as HTMLElement).textContent.trim();
    }

    beforeEach(() => {
        configureComponentTest({
            providers: [
                { provide: ApplicationStateService, useClass: TestApplicationState },
            ],
            declarations: [MoveFormsWarningModal, TypeIconPipe],
        });
    });

    it('lists the single form which is being moved', componentTest(() => MoveFormsWarningModal, (fixture) => {
        setup(fixture, [FORM_ONE]);

        expect(getListedFormNames(fixture)).toEqual([FORM_ONE.name]);
        expect(fixture.debugElement.query(By.css(`.forms-to-move [data-id="${FORM_ONE.id}"]`))).toBeTruthy();
    }));

    it('lists all forms which are being moved', componentTest(() => MoveFormsWarningModal, (fixture) => {
        setup(fixture, [FORM_ONE, FORM_TWO]);

        expect(getListedFormNames(fixture)).toEqual([FORM_ONE.name, FORM_TWO.name]);
    }));

    it('uses the singular warning text for a single form', componentTest(() => MoveFormsWarningModal, (fixture) => {
        setup(fixture, [FORM_ONE]);

        expect(getWarningText(fixture)).toContain('modal.move_forms_warning_body_singular');
    }));

    it('uses the plural warning text for multiple forms', componentTest(() => MoveFormsWarningModal, (fixture) => {
        setup(fixture, [FORM_ONE, FORM_TWO]);

        expect(getWarningText(fixture)).toContain('modal.move_forms_warning_body_plural');
    }));

    it('closes with the forms when the user confirms', componentTest(() => MoveFormsWarningModal, (fixture) => {
        setup(fixture, [FORM_ONE, FORM_TWO]);

        const confirmButton = fixture.debugElement.query(By.css('.modal-footer [data-action="confirm"]'));
        confirmButton.triggerEventHandler('click', null);

        expect(closeSpy).toHaveBeenCalledOnceWith([FORM_ONE, FORM_TWO]);
        expect(cancelSpy).not.toHaveBeenCalled();
    }));

    it('cancels when the user clicks cancel', componentTest(() => MoveFormsWarningModal, (fixture) => {
        setup(fixture, [FORM_ONE]);

        const cancelButton = fixture.debugElement.query(By.css('.modal-footer [data-action="cancel"]'));
        cancelButton.triggerEventHandler('click', null);

        expect(cancelSpy).toHaveBeenCalled();
        expect(closeSpy).not.toHaveBeenCalled();
    }));
});
