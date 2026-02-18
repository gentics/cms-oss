import { Component } from '@angular/core';
import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { Page } from '@gentics/cms-models';
import { configureComponentTest } from '../../../../testing';
import { ApplicationStateService, FolderActionsService } from '../../../state';
import { TestApplicationState } from '../../../state/test-application-state.mock';
import { BaseLanguageIndicatorComponent } from './base-language-indicator.component';

@Component({
    template: '',
    standalone: false,
})
class TestLanguageIndicatorComponent extends BaseLanguageIndicatorComponent<Page> {
    constructor(
        appState: ApplicationStateService,
        folderActions: FolderActionsService,
    ) {
        super('page', appState, folderActions);
    }
}

describe('BaseLanguageIndicatorComponent', () => {
    let state: TestApplicationState;
    let fixture: ComponentFixture<TestLanguageIndicatorComponent>;
    let instance: TestLanguageIndicatorComponent;

    beforeEach(() => {
        configureComponentTest({
            declarations: [TestLanguageIndicatorComponent],
            providers: [
                { provide: ApplicationStateService, useClass: TestApplicationState },
                { provide: FolderActionsService, useValue: {} },
            ],
        });
        state = TestBed.inject(ApplicationStateService) as any;
        fixture = TestBed.createComponent(TestLanguageIndicatorComponent);
        instance = fixture.componentInstance;
    });

    it('should create', () => {
        expect(instance).toBeTruthy();
    });

    it('emits languageVariants$ correctly on initial load', fakeAsync(() => {
        const mockPage1 = { id: 1, globalId: '1', languages: [] } as any;
        const mockPage2 = { id: 2, globalId: '2', languages: [] } as any;

        state.mockState({
            entities: {
                page: {
                    1: mockPage1,
                    2: mockPage2,
                },
            },
        });

        instance.languageVariantsIds$.next([1, 2]);
        instance.ngOnInit();
        tick();

        let emittedVariants: any;
        instance.languageVariants$.subscribe((variants) => emittedVariants = variants);
        tick();

        expect(emittedVariants).toEqual({
            1: mockPage1,
            2: mockPage2,
        });
    }));

    it('emits languageVariants$ when store updates', fakeAsync(() => {
        const mockPage1 = { id: 1, globalId: '1', online: false } as any;

        state.mockState({
            entities: {
                page: {
                    1: mockPage1,
                },
            },
        });

        instance.languageVariantsIds$.next([1]);
        instance.ngOnInit();
        tick();

        let emittedVariants: any;
        instance.languageVariants$.subscribe((variants) => emittedVariants = variants);
        tick();

        expect(emittedVariants[1].online).toBe(false);

        const updatedPage1 = { ...mockPage1, online: true };
        state.mockState({
            entities: {
                page: {
                    1: updatedPage1,
                },
            },
        });
        tick();

        expect(emittedVariants[1].online).toBe(true);
    }));

    it('does not emit when store updates unrelated items', fakeAsync(() => {
        const mockPage1 = { id: 1, globalId: '1' } as any;

        state.mockState({
            entities: {
                page: {
                    1: mockPage1,
                },
            },
        });

        instance.languageVariantsIds$.next([1]);
        instance.ngOnInit();
        tick();

        let emissionCount = 0;
        instance.languageVariants$.subscribe(() => emissionCount++);
        tick();

        expect(emissionCount).toBe(1);

        state.mockState({
            entities: {
                page: {
                    1: mockPage1,
                    2: { id: 2 } as any,
                },
            },
        });
        tick();

        expect(emissionCount).toBe(1);
    }));
});
