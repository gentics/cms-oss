import { ChangeDetectionStrategy, Component, Input } from '@angular/core';
import { ValidationResult } from '@gentics/cms-integration-api-models';

/**
 * Displays the appropriate error message if a TagPropery validation fails.
 * If ValidationResult.success is true, nothing is displayed.
 *
 * The following conventions should be followed with regards to the
 * display of error messages (based on https://medium.com/@andrew.burton/form-validation-best-practices-8e3bec7d0549):
 * * This component should be displayed to the right of the component(s) used to edit
 *   the TagProperty on a desktop screen and below the component(s) on a mobile screen.
 * * The validation that triggers the ValidationErrorInfo should occur when the
 *   component used to edit the TagProperty loses focus.
 */
@Component({
    selector: 'validation-error-info',
    templateUrl: './validation-error-info.component.html',
    styleUrls: ['./validation-error-info.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ValidationErrorInfoComponent {

    /**
     * The result of the last validation of the TagProperty.
     *
     * The displayed information will be taken from this object.
     * If validationResult.success = true, then nothing is displayed.
     */
    @Input()
    validationResult: ValidationResult;

}
