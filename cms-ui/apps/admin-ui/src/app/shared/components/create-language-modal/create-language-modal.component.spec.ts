import { InterfaceOf } from '@admin-ui/common';
import { I18nService, LanguageOperations } from '@admin-ui/core';
import { MockI18nServiceWithSpies } from '@admin-ui/core/providers/i18n/i18n.service.mock';
import { Pipe, PipeTransform } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ReactiveFormsModule } from '@angular/forms';
import { GenticsUICoreModule } from '@gentics/ui-core';
import { of } from 'rxjs';
import { CreateLanguageModalComponent } from './create-language-modal.component';

@Pipe({ name: 'i18n' })
class MockI18nPipe implements PipeTransform {
    transform(key: string, params: object): string {
        return key + (params ? ':' + JSON.stringify(params) : '');
    }
}

class MockLanguageOperations implements Partial<InterfaceOf<LanguageOperations>> {
    createLanguage = jasmine.createSpy('createLanguage').and.returnValue(of({}));
}

describe('CreateLanguageModalComponent', () => {
    let i18n: MockI18nServiceWithSpies;
    let languageOperations: MockLanguageOperations;
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
            ],
            providers: [
                { provide: I18nService, useClass: MockI18nServiceWithSpies },
                { provide: LanguageOperations, useClass: MockLanguageOperations },
            ],
        }).compileComponents();

        i18n = TestBed.get(I18nService);
        languageOperations = TestBed.get(LanguageOperations);
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

        expect(languageOperations.createLanguage).toHaveBeenCalled();
        expect(languageOperations.createLanguage).toHaveBeenCalledWith({ name: component.form.value.name, code: component.form.value.code });
    });
});
