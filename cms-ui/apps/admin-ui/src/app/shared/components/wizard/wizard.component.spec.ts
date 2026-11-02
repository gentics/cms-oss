import { WizardStepNextClickFn } from '@admin-ui/common';
import { ChangeDetectorRef, QueryList } from '@angular/core';
import { fakeAsync, flush } from '@angular/core/testing';
import { WizardStepDirective } from '../../directives/wizard-step/wizard-step.directive';
import { WizardComponent } from './wizard.component';

type Step = WizardStepDirective<any, any>;

function createStep(nextEnabled = true, nextClick?: WizardStepNextClickFn<any>): Step {
    const step = new WizardStepDirective<any, any>();
    step.nextEnabled = nextEnabled;
    step.nextClick = nextClick;
    return step;
}

describe('WizardComponent', () => {

    let wizard: WizardComponent;
    let queryList: QueryList<Step>;

    /** Needs to be called inside of `fakeAsync()`. */
    function initWizard(...steps: Step[]): void {
        const changeDetector = jasmine.createSpyObj<ChangeDetectorRef>('ChangeDetectorRef', ['markForCheck']);
        wizard = new WizardComponent(changeDetector);
        queryList = new QueryList<Step>();
        queryList.reset(steps);
        wizard.steps = queryList;
        wizard.ngAfterContentInit();
        // The first step is activated in a `setTimeout()`.
        flush();
    }

    function updateSteps(...steps: Step[]): void {
        queryList.reset(steps);
        queryList.notifyOnChanges();
    }

    afterEach(() => {
        wizard?.ngOnDestroy();
        wizard = null;
    });

    describe('isStepSelectable()', () => {

        it('allows the current and all previous steps', fakeAsync(() => {
            initWizard(createStep(), createStep(), createStep(false));
            wizard.onNextButtonClick();
            flush();
            wizard.onNextButtonClick();
            flush();

            expect(wizard.currentStepIndex).toBe(2);
            expect(wizard.isStepSelectable(0)).toBeTrue();
            expect(wizard.isStepSelectable(1)).toBeTrue();
            expect(wizard.isStepSelectable(2)).toBeTrue();
        }));

        it('allows a later step only if "Next" is enabled on the current step and every step in between', fakeAsync(() => {
            initWizard(createStep(true), createStep(true), createStep(false), createStep(true));

            expect(wizard.isStepSelectable(1)).toBeTrue();
            expect(wizard.isStepSelectable(2)).toBeTrue();
            expect(wizard.isStepSelectable(3)).toBeFalse();
        }));

        it('does not allow a later step if "Next" is disabled on the current step', fakeAsync(() => {
            initWizard(createStep(false), createStep(true), createStep(true));

            expect(wizard.isStepSelectable(1)).toBeFalse();
            expect(wizard.isStepSelectable(2)).toBeFalse();
        }));

    });

    describe('onTabSelect()', () => {

        it('goes back to a previous step without executing any nextClick handler', fakeAsync(() => {
            const nextClick = jasmine.createSpy('nextClick').and.returnValue(Promise.resolve());
            initWizard(createStep(true, nextClick), createStep(), createStep());
            wizard.onNextButtonClick();
            flush();
            nextClick.calls.reset();

            wizard.onTabSelect(0);
            flush();

            expect(wizard.currentStepIndex).toBe(0);
            expect(nextClick).not.toHaveBeenCalled();
        }));

        it('goes forward like clicking "Next" repeatedly', fakeAsync(() => {
            const steps = [
                createStep(true, () => Promise.resolve('output 0')),
                createStep(true, () => Promise.resolve('output 1')),
                createStep(),
            ];
            initWizard(...steps);
            const activate1 = spyOn(steps[1].activate, 'emit');
            const activate2 = spyOn(steps[2].activate, 'emit');

            wizard.onTabSelect(2);
            flush();

            expect(wizard.currentStepIndex).toBe(2);
            expect(activate1).toHaveBeenCalledOnceWith('output 0');
            expect(activate2).toHaveBeenCalledOnceWith('output 1');
            expect(wizard.nextClickHandlerExecuting).toBeFalse();
        }));

        it('ignores a later step which is not selectable', fakeAsync(() => {
            const nextClick = jasmine.createSpy('nextClick').and.returnValue(Promise.resolve());
            initWizard(createStep(true, nextClick), createStep(false), createStep());

            wizard.onTabSelect(2);
            flush();

            expect(wizard.currentStepIndex).toBe(0);
            expect(nextClick).not.toHaveBeenCalled();
        }));

        it('stops at a step whose "Next" button got disabled on the way', fakeAsync(() => {
            const steps: Step[] = [];
            steps.push(
                createStep(true, () => {
                    steps[1].nextEnabled = false;
                    return Promise.resolve();
                }),
                createStep(),
                createStep(),
            );
            initWizard(...steps);

            wizard.onTabSelect(2);
            flush();

            expect(wizard.currentStepIndex).toBe(1);
        }));

        it('stays on the current step if a nextClick handler rejects', fakeAsync(() => {
            initWizard(createStep(true, () => Promise.reject(new Error('failed'))), createStep(), createStep());

            wizard.onTabSelect(2);
            flush();

            expect(wizard.currentStepIndex).toBe(0);
            expect(wizard.nextClickHandlerExecuting).toBeFalse();
        }));

        it('stops if the target step is removed on the way', fakeAsync(() => {
            const steps: Step[] = [];
            steps.push(
                createStep(true, () => {
                    updateSteps(steps[0], steps[1], steps[2]);
                    return Promise.resolve();
                }),
                createStep(),
                createStep(),
                createStep(),
            );
            initWizard(...steps);

            wizard.onTabSelect(3);
            flush();

            expect(wizard.currentStepIndex).toBe(1);
        }));

        it('never executes the nextClick handler of the last step and never finishes the wizard', fakeAsync(() => {
            const lastNextClick = jasmine.createSpy('lastNextClick').and.returnValue(Promise.resolve());
            initWizard(createStep(), createStep(), createStep(true, lastNextClick));
            const finish = spyOn(wizard.wizardFinish, 'emit');

            wizard.onTabSelect(2);
            flush();

            expect(wizard.currentStepIndex).toBe(2);
            expect(lastNextClick).not.toHaveBeenCalled();
            expect(finish).not.toHaveBeenCalled();
        }));

        it('stops if the wizard is destroyed while a nextClick handler is executing', fakeAsync(() => {
            let resolveNextClick: () => void;
            const steps = [
                createStep(true, () => new Promise<void>((resolve) => resolveNextClick = resolve)),
                createStep(),
                createStep(),
            ];
            initWizard(...steps);
            const activate1 = spyOn(steps[1].activate, 'emit');

            wizard.onTabSelect(2);
            flush();
            wizard.ngOnDestroy();
            resolveNextClick();
            flush();

            expect(wizard.currentStepIndex).toBe(0);
            expect(activate1).not.toHaveBeenCalled();
        }));

    });

    describe('while a nextClick handler is executing', () => {

        let resolveNextClick: () => void;

        function startPendingNextClick(): void {
            initWizard(
                createStep(),
                createStep(true, () => new Promise<void>((resolve) => resolveNextClick = resolve)),
                createStep(),
            );
            wizard.onNextButtonClick();
            flush();
            expect(wizard.currentStepIndex).toBe(1);

            wizard.onNextButtonClick();
            flush();
            expect(wizard.nextClickHandlerExecuting).toBeTrue();
        }

        it('ignores the "Previous" button', fakeAsync(() => {
            startPendingNextClick();

            wizard.onPrevButtonClick();
            resolveNextClick();
            flush();

            expect(wizard.currentStepIndex).toBe(2);
        }));

        it('ignores tab selections', fakeAsync(() => {
            startPendingNextClick();

            wizard.onTabSelect(0);
            resolveNextClick();
            flush();

            expect(wizard.currentStepIndex).toBe(2);
        }));

    });

    describe('onNextButtonClick()', () => {

        it('finishes the wizard on the last step with the output of its nextClick handler', fakeAsync(() => {
            initWizard(createStep(), createStep(true, () => Promise.resolve('result')));
            const finish = spyOn(wizard.wizardFinish, 'emit');
            wizard.onNextButtonClick();
            flush();

            wizard.onNextButtonClick();
            flush();

            expect(finish).toHaveBeenCalledOnceWith('result');
        }));

    });

});
