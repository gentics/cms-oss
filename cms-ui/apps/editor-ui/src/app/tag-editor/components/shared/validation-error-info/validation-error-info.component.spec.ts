import { Component, Pipe, PipeTransform } from '@angular/core';
import { tick } from '@angular/core/testing';
import { FormsModule } from '@angular/forms';
import { By } from '@angular/platform-browser';
import { ApplicationStateService } from '@editor-ui/app/state';
import { TestApplicationState } from '@editor-ui/app/state/test-application-state.mock';
import { ValidationResult } from '@gentics/cms-integration-api-models';
import { GenticsUICoreModule } from '@gentics/ui-core';
import { componentTest, configureComponentTest } from '../../../../../testing';
import { ValidationErrorInfoComponent } from './validation-error-info.component';

describe('ValidationErrorInfoComponent', () => {

    beforeEach(() => {
        configureComponentTest({
            imports: [
                GenticsUICoreModule.forRoot(),
                FormsModule,
            ],
            providers: [
                { provide: ApplicationStateService, useClass: TestApplicationState },
            ],
            declarations: [
                MockI18nPipe,
                TestComponent,
                ValidationErrorInfoComponent,
            ],
        });
    });

    it('displays nothing for empty validationResult',
        componentTest(() => TestComponent, (fixture) => {
            fixture.detectChanges();
            tick();
            const errorInfoElement = fixture.debugElement.query(By.directive(ValidationErrorInfoComponent));
            expect(errorInfoElement.queryAll(By.all()).length).toBe(0);
        }),
    );

    it('displays nothing for validationResult.success = true',
        componentTest(() => TestComponent, (fixture, instance) => {
            instance.validationResult = { isSet: true, success: true };
            fixture.detectChanges();
            tick();
            const errorInfoElement = fixture.debugElement.query(By.directive(ValidationErrorInfoComponent));
            expect(errorInfoElement.queryAll(By.all()).length).toBe(0);
        }),
    );

    it('displays the errorMessage for validationResult.success = false',
        componentTest(() => TestComponent, (fixture, instance) => {
            const expectedErrorMessage = 'Error Message';
            instance.validationResult = {
                isSet: true,
                success: false,
                errorMessage: expectedErrorMessage,
            };
            fixture.detectChanges();
            tick();

            const errorInfoElement = fixture.debugElement.query(By.directive(ValidationErrorInfoComponent));
            const spanElement = errorInfoElement.query(By.css('span')).nativeElement as HTMLElement;
            expect(spanElement.innerText.trim()).toEqual(expectedErrorMessage);
        }),
    );

});

@Pipe({
    name: 'i18n',
    standalone: false,
})
class MockI18nPipe implements PipeTransform {
    transform(value: string): string {
        return value;
    }
}

@Component({
    template: `
        <validation-error-info [validationResult]="validationResult"></validation-error-info>
    `,
    standalone: false,
})
class TestComponent {
    validationResult: ValidationResult;
}
