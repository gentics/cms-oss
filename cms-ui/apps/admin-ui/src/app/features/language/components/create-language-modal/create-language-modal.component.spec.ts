import { NO_ERRORS_SCHEMA, Pipe, PipeTransform } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ReactiveFormsModule } from '@angular/forms';
import { GenticsUICoreModule } from '@gentics/ui-core';
import { of } from 'rxjs';
import { InterfaceOf } from '../../../../common';
import { LanguageHandlerService } from '../../../../core';
import { LanguagePropertiesComponent } from '../language-properties/language-properties.component';
import { CreateLanguageModalComponent } from './create-language-modal.component';
import { MockI18nPipe } from '@gentics/cms-components/testing';

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
