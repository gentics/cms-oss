import { InterfaceOf } from '@admin-ui/common';
import { I18nService } from '@admin-ui/core';
import { MockI18nServiceWithSpies } from '@admin-ui/core/providers/i18n/i18n.service.mock';
import { LanguageHandlerService } from '@admin-ui/shared';
import { NO_ERRORS_SCHEMA, Pipe, PipeTransform } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ReactiveFormsModule } from '@angular/forms';
import { GenticsUICoreModule } from '@gentics/ui-core';
import { of } from 'rxjs';
import { LanguagePropertiesComponent } from '../language-properties/language-properties.component';
import { CreateLanguageModalComponent } from './create-language-modal.component';

@Pipe({ name: 'i18n' })
class MockI18nPipe implements PipeTransform {
    transform(key: string, params: object): string {
        return key + (params ? ':' + JSON.stringify(params) : '');
    }
}

class MockLanguageOperations implements Partial<InterfaceOf<LanguageHandlerService>> {
    createMapped = jasmine.createSpy('createMapped').and.returnValue(of({}));
}

describe('CreateLanguageModalComponent', () => {
    let handler: MockLanguageOperations;
    let component: CreateLanguageModalComponent;
    let fixture: ComponentFixture<CreateLanguageModalComponent>;

    beforeEach(() => {
        TestBed.configureTestingModule({
            imports: [
                GenticsUICoreModule.forRoot(),
                ReactiveFormsModule,
            ],
            declarations: [
                MockI18nPipe,
                CreateLanguageModalComponent,
                LanguagePropertiesComponent,
            ],
            providers: [
                { provide: I18nService, useClass: MockI18nServiceWithSpies },
                { provide: LanguageHandlerService, useClass: MockLanguageOperations },
            ],
            schemas: [NO_ERRORS_SCHEMA],
        }).compileComponents();

        handler = TestBed.inject(LanguageHandlerService) as any;
        fixture = TestBed.createComponent(CreateLanguageModalComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    beforeEach(() => {
        fixture = TestBed.createComponent(CreateLanguageModalComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });

    it('should create a language', () => {
        component.buttonCreateEntityClicked();
        fixture.detectChanges();

        expect(handler.createMapped).toHaveBeenCalled();
        expect(handler.createMapped).toHaveBeenCalledWith({ name: component.form.value.name, code: component.form.value.code });
    });
});
