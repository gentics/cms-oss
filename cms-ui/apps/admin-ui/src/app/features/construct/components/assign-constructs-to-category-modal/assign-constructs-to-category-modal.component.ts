import { ConstructCategoryDataService } from '@admin-ui/shared';
import { ChangeDetectionStrategy, Component, OnInit } from '@angular/core';
import { UntypedFormControl, Validators } from '@angular/forms';
import { ConstructCategoryBO } from '@gentics/cms-models';
import { BaseModal } from '@gentics/ui-core';
import { Observable } from 'rxjs';

@Component({
    selector: 'gtx-assign-constructs-to-category-modal',
    templateUrl: './assign-constructs-to-category-modal.component.html',
    styleUrls: ['./assign-constructs-to-category-modal.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AssignConstructsToCategoryModalComponent extends BaseModal<false | ConstructCategoryBO[]> implements OnInit {

    public form = new UntypedFormControl(null, Validators.required);
    public categories$: Observable<ConstructCategoryBO[]>;

    constructor(
        private categoryData: ConstructCategoryDataService,
    ) {
        super();
    }

    ngOnInit(): void {
        this.categories$ = this.categoryData.watchAllEntities();
    }
}
