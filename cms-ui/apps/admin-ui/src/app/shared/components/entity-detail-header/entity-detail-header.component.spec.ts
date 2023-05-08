import { InterfaceOf, USER_ACTION_PERMISSIONS, USER_ACTION_PERMISSIONS_DEF, UserActionPermissions } from '@admin-ui/common';
import { PermissionsService } from '@admin-ui/core/providers';
import { I18nService } from '@admin-ui/core/providers/i18n';
import { MockI18nService } from '@admin-ui/core/providers/i18n/i18n.service.mock';
import { AppStateService } from '@admin-ui/state';
import { TestAppState } from '@admin-ui/state/utils/test-app-state';
import { MockStore } from '@admin-ui/state/utils/test-app-state/test-store.mock';
import { componentTest, configureComponentTest } from '@admin-ui/testing';
import { Component } from '@angular/core';
import { ComponentFixture, TestBed, tick } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { Store } from '@ngxs/store';
import { GenticsUICoreModule } from '@gentics/ui-core';
import { BehaviorSubject } from 'rxjs';
import { delay } from 'rxjs/operators';
import { ActionAllowedDirective } from '../../directives/action-allowed/action-allowed.directive';
import { EntityDetailHeaderComponent } from './entity-detail-header.component';

class MockPermissionsService implements Partial<InterfaceOf<PermissionsService>> {
    // Return all permisions to be granted by default
    private currPermissions = new BehaviorSubject<boolean>(true);

    checkPermissions = jasmine.createSpy('checkPermissions').and
        .returnValue(this.currPermissions.pipe(delay(0)));

    mockPermissionsGranted(value: boolean): void {
        this.currPermissions.next(value);
    }

    public getUserActionPermsForId(actionId: string): UserActionPermissions {
        if (!actionId) {
            return null;
        }

        const parts = actionId.split('.');
        if (parts.length !== 2) {
            throw new Error(`Malformed user action ID provided to gtxActionAllowed directive: '${actionId}'.` +
                'Make sure that you use the format \'<module>.actionId');
        }

        const module = USER_ACTION_PERMISSIONS_DEF[parts[0]];
        if (!module) {
            throw new Error(`User Action module '${parts[0]}' does not exist.`);
        }
        const reqPerms = module[parts[1]];
        if (!reqPerms) {
            throw new Error(`User Action '${parts[1]}' does not exist within module '${parts[0]}'.`);
        }
        return reqPerms;
    }
}

@Component({
    template: `
        <gtx-entity-detail-header
            [title]="title"
            [saveActionAllowedId]="saveActionAllowedId"
            [saveDisabled]="saveDisabled"
            (saveClick)="onSaveClick()"
            (cancelClick)="onCancelClick()"
        ></gtx-entity-detail-header>
    `,
})
class TestComponent {
    title = 'TEST_TITLE';
    saveDisabled = false;
    saveActionAllowedId = 'user.updateUser';

    onSaveClick = jasmine.createSpy('onSaveClick');
    onCancelClick = jasmine.createSpy('onCancelClick');
}

function getTitle<T>(fixture: ComponentFixture<T>): string {
    return fixture.debugElement.query(By.css('.gtx-entity-details-tab-content-header-title')).nativeElement.textContent;
}

function getSaveButtonElement<T>(fixture: ComponentFixture<T>): HTMLButtonElement {
    return fixture.debugElement.query(By.css('.gtx-save-button button')).nativeElement;
}

describe('EntityDetailHeaderComponent', () => {
    let permissionService: MockPermissionsService;

    beforeEach(() => {
        configureComponentTest({
            imports: [
                GenticsUICoreModule.forRoot(),
            ],
            declarations: [
                ActionAllowedDirective,
                EntityDetailHeaderComponent,
                TestComponent,
            ],
            providers: [
                { provide: Store, useClass: MockStore },
                { provide: I18nService, useClass: MockI18nService },
                { provide: USER_ACTION_PERMISSIONS, useValue: USER_ACTION_PERMISSIONS_DEF },
                { provide: PermissionsService, useClass: MockPermissionsService },
                { provide: AppStateService, useClass: TestAppState },
            ],
        });

        permissionService = TestBed.get(PermissionsService);
    });

    it('should create', componentTest(() => TestComponent, (fixture, instance) => {
        fixture.detectChanges();
        tick();
        expect(instance).toBeTruthy();
    }));

    // @TODO: Re-enable after fixing it
    xit('should display correct title', componentTest(() => TestComponent, (fixture, instance) => {
        const title = getTitle(fixture);
        fixture.detectChanges();
        tick();
        expect(title).toBeTruthy(instance.title);
    }));

    it('should display save button as enabled if required permissions are granted', componentTest(() => TestComponent, (fixture, instance) => {
        fixture.detectChanges();
        tick();
        const saveButton = getSaveButtonElement(fixture);

        expect(saveButton.disabled).toBe(false);
    }));

    it('should display save button as enabled if required permissions are NOT granted', componentTest(() => TestComponent, (fixture, instance) => {
        permissionService.mockPermissionsGranted(false);

        fixture.detectChanges();
        tick();

        fixture.detectChanges();
        tick();

        const saveButton = getSaveButtonElement(fixture);

        expect(saveButton.disabled).toBe(true);
    }));

    it(
        'should display save button as disabled if required permissions are granted but disabled property is set TRUE',
        componentTest(() => TestComponent, (fixture, instance) => {
            permissionService.mockPermissionsGranted(true);
            instance.saveDisabled = true;

            fixture.detectChanges();
            tick();

            fixture.detectChanges();
            tick();

            const saveButton = getSaveButtonElement(fixture);

            expect(saveButton.disabled).toBe(true);
        },
    ));

});
