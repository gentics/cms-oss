import { Component } from '@angular/core';
import { ComponentFixture, TestBed, tick } from '@angular/core/testing';
import { BrowserDynamicTestingModule } from '@angular/platform-browser-dynamic/testing';
import { OverlayHostComponent } from '../../components/overlay-host/overlay-host.component';
import { ToastComponent } from '../../components/toast/toast.component';
import { IconDirective } from '../../directives/icon/icon.directive';
import { componentTest } from '../../testing';
import { OverlayHostService } from '../overlay-host/overlay-host.service';
import { NotificationService } from './notification.service';

let notificationService: NotificationService;

describe('Notification Service', () => {

    beforeEach(() => {
        TestBed.configureTestingModule({
            declarations: [
                IconDirective,
                OverlayHostComponent,
                TestComponent,
                ToastComponent,
            ],
            providers: [
                NotificationService,
                OverlayHostService,
            ],
            teardown: { destroyAfterEach: false },
        });

        TestBed.overrideModule(BrowserDynamicTestingModule, {
            set: {
                declarations: [ToastComponent],
            },
        });

        notificationService = TestBed.inject(NotificationService);
    });

    describe('show():', () => {

        /**
         * Call tick() for each async operation resulting from the show() method.
         */
        function runShowAsyncTasks(fixture: ComponentFixture<TestComponent>): void {
            tick(); // loadNextToLocation()
            fixture.detectChanges();
        }

        /**
         * Clean up async tasks and destroy the fixture
         */
        function cleanUp(fixture: ComponentFixture<TestComponent>): void {
            fixture.destroy();
            tick(500); // dismissing animation delay
            tick(); // setTimeout() from positionOpenToasts()
        }

        const getToastElement = (fixture: ComponentFixture<TestComponent>): HTMLElement => fixture.nativeElement.querySelector('.gtx-toast');

        it('should return an object with a dismiss() method',
            componentTest(() => TestComponent, fixture => {
                fixture.detectChanges();
                const toast = notificationService.show({ message: 'test', delay: 0 });
                runShowAsyncTasks(fixture);

                expect(toast.dismiss).toBeDefined();
                cleanUp(fixture);
            }),
        );

        it('should add Toast component to DOM',
            componentTest(() => TestComponent, fixture => {
                fixture.detectChanges();
                notificationService.show({ message: 'test', delay: 0 });
                runShowAsyncTasks(fixture);

                const toast = getToastElement(fixture);

                expect(toast).not.toBeNull();
                cleanUp(fixture);
            }),
        );

        it('Toast should contain correct message',
            componentTest(() => TestComponent, fixture => {
                fixture.detectChanges();
                notificationService.show({ message: 'test', delay: 0 });
                runShowAsyncTasks(fixture);

                const toast = getToastElement(fixture);

                expect(toast.innerHTML).toContain('test');
                cleanUp(fixture);
            }),
        );

        it('should remove Toast when dismiss() is invoked.',
            componentTest(() => TestComponent, fixture => {
                fixture.detectChanges();
                const toast = notificationService.show({ message: 'test', delay: 0 });
                runShowAsyncTasks(fixture);

                expect(getToastElement(fixture)).not.toBeNull();

                toast.dismiss();
                tick(500);

                expect(getToastElement(fixture)).toBeNull();
                cleanUp(fixture);
            }),
        );

        it('should remove Toast after timeout specified in "delay" option.',
            componentTest(() => TestComponent, fixture => {
                fixture.detectChanges();
                notificationService.show({ message: 'test', delay: 500 });
                runShowAsyncTasks(fixture);

                expect(getToastElement(fixture)).not.toBeNull();

                tick(500); // delay timeout
                tick(500); // animate away

                expect(getToastElement(fixture)).toBeNull();
                cleanUp(fixture);
            }),
        );

        it('should not dismiss on click if "dismissOnClick" set to false.',
            componentTest(() => TestComponent, fixture => {
                fixture.detectChanges();
                notificationService.show({ message: 'test', delay: 0, dismissOnClick: false });
                runShowAsyncTasks(fixture);

                const toastElement = getToastElement(fixture);
                toastElement.click();
                tick(500);

                expect(getToastElement(fixture)).not.toBeNull();
                cleanUp(fixture);
            }),
        );

        it('should dismiss on click if "dismissOnClick" set to true.',
            componentTest(() => TestComponent, fixture => {
                fixture.detectChanges();
                notificationService.show({ message: 'test', delay: 0, dismissOnClick: true });
                runShowAsyncTasks(fixture);

                const toastElement = getToastElement(fixture);
                toastElement.click();
                tick(500);

                expect(getToastElement(fixture)).toBeNull();
                cleanUp(fixture);
            }),
        );

        describe('action option:', () => {

            it('displays the action label',
                componentTest(() => TestComponent, fixture => {
                    fixture.detectChanges();
                    notificationService.show({
                        message: 'test',
                        delay: 0,
                        action: {
                            label: 'testLabel',
                        },
                    });
                    runShowAsyncTasks(fixture);

                    const actionDiv: HTMLElement = fixture.nativeElement.querySelector('.action');

                    expect(actionDiv.textContent).toContain('testLabel');
                    cleanUp(fixture);
                }),
            );

            it('calls the onClick method when clicked',
                componentTest(() => TestComponent, fixture => {
                    fixture.detectChanges();
                    const spy = jasmine.createSpy('spy');
                    notificationService.show({
                        message: 'test',
                        delay: 0,
                        action: {
                            label: 'testLabel',
                            onClick: spy,
                        },
                    });
                    runShowAsyncTasks(fixture);

                    const actionButton: HTMLElement = fixture.nativeElement.querySelector('button');

                    expect(spy).not.toHaveBeenCalled();
                    actionButton.click();
                    tick();
                    tick(500);

                    expect(spy.calls.count()).toBe(1);
                    cleanUp(fixture);
                }),
            );

        });

    });
});

@Component({
    template: '<gtx-overlay-host></gtx-overlay-host>',
})
class TestComponent { }
