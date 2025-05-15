/**
 * UNIT TESTS DISABLED
 */

// import { EntityExistsValidator } from '@admin-ui/shared';
import { GroupOperations, I18nService } from '@admin-ui/core';
import { MockI18nServiceWithSpies } from '@admin-ui/core/providers/i18n/i18n.service.mock';
import { NO_ERRORS_SCHEMA, Pipe, PipeTransform } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ReactiveFormsModule } from '@angular/forms';
import { GenticsUICoreModule } from '@gentics/ui-core';
import { CreateGroupModalComponent } from './create-group-modal.component';

@Pipe({
    name: 'i18n',
    standalone: false,
})
class MockI18nPipe implements PipeTransform {
    transform(key: string, params: object): string {
        return key + (params ? ':' + JSON.stringify(params) : '');
    }
}

xdescribe('CreateGroupModalComponent', () => {
    let i18n: MockI18nServiceWithSpies;
    let component: CreateGroupModalComponent;
    let fixture: ComponentFixture<CreateGroupModalComponent>;

    beforeEach(() => {
        TestBed.configureTestingModule({
            imports: [
                GenticsUICoreModule.forRoot(),
                ReactiveFormsModule,
            ],
            declarations: [
                MockI18nPipe,
                CreateGroupModalComponent,
            ],
            providers: [
                { provide: I18nService, useClass: MockI18nServiceWithSpies },
                // { provide: GroupOperations, useClass: MockGroupOperations },
                // { provide: GroupExistsValidator, useClass: MockGroupExistsValidator },
            ],
            schemas: [NO_ERRORS_SCHEMA],
        }).compileComponents();

        i18n = TestBed.get(I18nService);
        fixture = TestBed.createComponent(CreateGroupModalComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    beforeEach(() => {
        fixture = TestBed.createComponent(CreateGroupModalComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });
});
