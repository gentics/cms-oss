/**
 * UNIT TESTS DISABLED
 */

import { GroupOperations, I18nService } from '@admin-ui/core';
import { MockI18nServiceWithSpies } from '@admin-ui/core/providers/i18n/i18n.service.mock';
import { NO_ERRORS_SCHEMA, Pipe, PipeTransform } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ReactiveFormsModule } from '@angular/forms';
import { GenticsUICoreModule } from '@gentics/ui-core';
import { CreateUserModalComponent } from './create-user-modal.component';

@Pipe({ name: 'i18n' })
class MockI18nPipe implements PipeTransform {
    transform(key: string, params: object): string {
        return key + (params ? ':' + JSON.stringify(params) : '');
    }
}

xdescribe('CreateUserModalComponent', () => {
    let i18n: MockI18nServiceWithSpies;
    let component: CreateUserModalComponent;
    let fixture: ComponentFixture<CreateUserModalComponent>;

    beforeEach(() => {
        TestBed.configureTestingModule({
            imports: [
                GenticsUICoreModule.forRoot(),
                ReactiveFormsModule,
            ],
            declarations: [
                MockI18nPipe,
                CreateUserModalComponent,
            ],
            providers: [
                { provide: I18nService, useClass: MockI18nServiceWithSpies },
                // { provide: GroupOperations, useClass: MockGroupOperations },
                // { provide: UserExistsValidator, useClass: MockUserExistsValidator },
            ],
            schemas: [NO_ERRORS_SCHEMA],
        }).compileComponents();

        i18n = TestBed.get(I18nService);
        fixture = TestBed.createComponent(CreateUserModalComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    beforeEach(() => {
        fixture = TestBed.createComponent(CreateUserModalComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });
});
