import { I18nService } from '@admin-ui/core';
import { MockI18nServiceWithSpies } from '@admin-ui/core/providers/i18n/i18n.service.mock';
import { NO_ERRORS_SCHEMA, Pipe, PipeTransform } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NormalizableEntityTypesMap } from '@gentics/cms-models';
import { GenticsUICoreModule } from '@gentics/ui-core';
import { ConfirmDeleteModalComponent } from './confirm-delete-modal.component';

@Pipe({
    name: 'i18n',
    standalone: false,
})
class MockI18nPipe implements PipeTransform {
    transform(key: string, params: object): string {
        return key + (params ? ':' + JSON.stringify(params) : '');
    }
}

xdescribe('ConfirmDeleteModalComponent', () => {
    let i18n: MockI18nServiceWithSpies;
    let component: ConfirmDeleteModalComponent<keyof NormalizableEntityTypesMap>;
    let fixture: ComponentFixture<ConfirmDeleteModalComponent<keyof NormalizableEntityTypesMap>>;

    beforeEach(() => {
        TestBed.configureTestingModule({
            imports: [
                GenticsUICoreModule.forRoot(),
            ],
            declarations: [
                MockI18nPipe,
                ConfirmDeleteModalComponent,
            ],
            providers: [
                { provide: I18nService, useClass: MockI18nServiceWithSpies },
            ],
            schemas: [NO_ERRORS_SCHEMA],
        }).compileComponents();

        i18n = TestBed.get(I18nService);
        fixture = TestBed.createComponent(ConfirmDeleteModalComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();

        // i18n.instant.and.callFake((key, params) => {
        //     const translated = `translated-${state.now.ui.language}-${key}`;
        //     return params ? `${translated} ${JSON.stringify(params)}` : translated;
        // });
    });

    it('should display correct amount of and user names', () => {
        // TODO: write test
    });

    it('should return correct value if user cancels', () => {
        // TODO: write test
    });

    it('should return correct value if user confirms', () => {
        // TODO: write test
    });
});
