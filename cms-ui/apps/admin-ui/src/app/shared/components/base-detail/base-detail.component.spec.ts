import { FormTabHandle } from '@admin-ui/common';
import { ExtendedEntityOperationsBase } from '@admin-ui/core';
import { OperationsBase } from '@admin-ui/core/providers/operations/operations.base';
import { ExtendedEntityDataServiceBase } from '@admin-ui/shared/providers';
import { AppStateService, INITIAL_UI_STATE } from '@admin-ui/state';
import { assembleTestAppStateImports, TestAppState } from '@admin-ui/state/utils/test-app-state';
import { componentTest, configureComponentTest } from '@admin-ui/testing';
import { ChangeDetectionStrategy, Component, ViewChild } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { AnyModelType, NormalizableEntityTypesMap } from '@gentics/cms-models';
import { BaseDetailComponent } from './base-detail.component';

@Component({
    selector: 'gtx-test-detail',
    standalone: false,
    changeDetection: ChangeDetectionStrategy.OnPush,
})
class TestDetailComponent extends BaseDetailComponent<any, MockOperations>{
    entityIdentifier: keyof NormalizableEntityTypesMap<AnyModelType>;
    get isLoading(): boolean {
        return jasmine.createSpy('isLoading') as any;
    }
    get activeFormTab(): FormTabHandle {
        return jasmine.createSpy('activeFormTab') as any;
    }
    ngOnDestroy = jasmine.createSpy('ngOnDestroy');
}

@Component({
    template: '<gtx-test-detail></gtx-test-detail>',
    standalone: false,
})
class TestComponent {
    @ViewChild(TestDetailComponent) testDetail: TestDetailComponent;
}

class MockNGXLogger {}

class MockExtendedEntityDataServiceBase {}

class MockOperations extends ExtendedEntityOperationsBase<any>{
    ngOnDestroy = jasmine.createSpy('ngOnDestroy');
    getAll = jasmine.createSpy('getAll');
    get = jasmine.createSpy('get');
}

describe('TestDetailComponent', () => {
    let appState: TestAppState;

    beforeEach(() => {
        configureComponentTest({
            imports: [
                assembleTestAppStateImports(),
                RouterTestingModule,
            ],
            declarations: [
                TestDetailComponent,
                TestComponent,
            ],
            providers: [
                { provide: AppStateService, useClass: TestAppState },
                { provide: ExtendedEntityDataServiceBase, useClass: MockExtendedEntityDataServiceBase },
                { provide: OperationsBase, useClass: MockOperations },
            ],
        });
        appState = TestBed.get(AppStateService);
    });


    it('BaseDetailComponent ends',
        componentTest(() => TestComponent, (fixture, testComponent) => {
            fixture.detectChanges();
            testComponent.testDetail.ngOnDestroy();
            expect(appState.now.ui).toEqual({
                ...INITIAL_UI_STATE,
                editorIsOpen: false,
                editorIsFocused: false,
                editorTab: undefined,
                focusEntityType: undefined,
                focusEntityId: undefined,
            });
        }),
    );

});
