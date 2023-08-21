import { ComponentFixture } from '@angular/core/testing';
import { FormsModule } from '@angular/forms';
import { ApplicationStateService } from '@editor-ui/app/state';
import { TestApplicationState } from '@editor-ui/app/state/test-application-state.mock';
import { GenticsUICoreModule } from '@gentics/ui-core';
import { componentTest, configureComponentTest } from '../../../../testing';
import { CapitalizePipe } from '../../pipes/capitalize/capitalize.pipe';
import { InheritanceDialog } from './inheritance-dialog.component';

describe('InheritanceDialog', () => {

    function defaultSetup(fixture: ComponentFixture<InheritanceDialog>): void {
        const instance = fixture.componentRef.instance;
        instance.item = <any> {
            id: 42,
            name: 'Test Folder',
            type: 'folder',
            inheritable: [1, 2, 3],
            disinherit: [1],
            excluded: false,
        };

        instance.nodes = <any> {
            1: { id: 1, name: 'Channel 1' },
            2: { id: 2, name: 'Channel 2' },
            3: { id: 3, name: 'Channel 3' },
        };

        instance.registerCloseFn((val: any) => {});
        instance.registerCancelFn(() => {});
        spyOn(instance, 'closeFn');
        fixture.detectChanges();
    }

    beforeEach(() => {
        configureComponentTest({
            imports: [GenticsUICoreModule, FormsModule],
            providers: [
                { provide: ApplicationStateService, useClass: TestApplicationState },
            ],
            declarations: [InheritanceDialog, CapitalizePipe],
        });
    });

    it('should set initial values',
        componentTest(() => InheritanceDialog, (fixture, instance) => {
            defaultSetup(fixture);
            expect(instance.generalInheritance).toBe(true);
            expect(instance.inheritedChannels).toEqual({
                1: false,
                2: true,
                3: true,
            });
        }),
    );

    it('should return correct value for "exclude"',
        componentTest(() => InheritanceDialog, (fixture, instance) => {
            defaultSetup(fixture);
            instance.saveSettings();
            expect(fnArgs(instance.closeFn, 0)[0]).toEqual(jasmine.objectContaining({
                exclude: false,
            }));

            instance.generalInheritance = false;
            instance.saveSettings();

            expect(fnArgs(instance.closeFn, 1)[0]).toEqual(jasmine.objectContaining({
                exclude: true,
            }));
        }),
    );

    describe('disinherit / reinherit', () => {

        it('should be empty if nothing changed',
            componentTest(() => InheritanceDialog, (fixture, instance) => {
                defaultSetup(fixture);
                instance.saveSettings();
                expect(fnArgs(instance.closeFn, 0)[0]).toEqual(jasmine.objectContaining({
                    disinherit: [],
                    reinherit: [],
                }));
            }),
        );

        it('should return correct value for "disinherit" ',
            componentTest(() => InheritanceDialog, (fixture, instance) => {
                defaultSetup(fixture);
                instance.inheritedChannels = {
                    1: false,
                    2: false,
                    3: false,
                };
                instance.saveSettings();
                expect(fnArgs(instance.closeFn, 0)[0]).toEqual(jasmine.objectContaining({
                    disinherit: [2, 3],
                    reinherit: [],
                }));
            }),
        );

        it('should return correct value for "reinherit" ',
            componentTest(() => InheritanceDialog, (fixture, instance) => {
                defaultSetup(fixture);
                instance.inheritedChannels = {
                    1: true,
                    2: true,
                    3: true,
                };
                instance.saveSettings();
                expect(fnArgs(instance.closeFn, 0)[0]).toEqual(jasmine.objectContaining({
                    reinherit: [1],
                    disinherit: [],
                }));
            }),
        );

        it('should return correct value for both "reinherit" and "disinherit"',
            componentTest(() => InheritanceDialog, (fixture, instance) => {
                defaultSetup(fixture);
                instance.inheritedChannels = {
                    1: true,
                    2: false,
                    3: false,
                };
                instance.saveSettings();
                expect(fnArgs(instance.closeFn, 0)[0]).toEqual(jasmine.objectContaining({
                    reinherit: [1],
                    disinherit: [2, 3],
                }));
            }),
        );
    });


    /**
     * Return the arguments that the spy was called with.
     */
    const fnArgs = (spy: any, callIndex: number): any[] => (<jasmine.Spy> spy).calls.argsFor(callIndex);

});
