import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { ContentPackage, IndexById } from '@gentics/cms-models';
import { BaseFormElementComponent, generateFormProvider } from '@gentics/ui-core';
import { ApplicationStateService } from '../../../state';

@Component({
    selector: 'gtx-content-package-select',
    templateUrl: './content-package-select.component.html',
    styleUrls: ['./content-package-select.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    providers: [generateFormProvider(ContentPackageSelectComponent)],
    standalone: false
})
export class ContentPackageSelectComponent extends BaseFormElementComponent<ContentPackage> implements OnInit {

    public selectedPackage: string;
    public contentPackages: IndexById<ContentPackage> = {};
    public hasPackages = false;

    constructor(
        changeDetector: ChangeDetectorRef,
        protected state: ApplicationStateService,
    ) {
        super(changeDetector);
    }

    public ngOnInit(): void {
        this.subscriptions.push(this.state.select(state => state.entities.contentPackage).subscribe(packages => {
            this.contentPackages = packages || {};
            this.hasPackages = Object.keys(this.contentPackages).length > 0;
            this.changeDetector.markForCheck();
        }));
    }

    public onSelectChange(id: string): void {
        this.selectedPackage = id;
        this.triggerChange(id ? this.contentPackages[id] : null);
    }

    protected onValueChange(): void {
        if (this.value == null) {
            this.selectedPackage = null;
        } else if (typeof this.value === 'string') {
            this.selectedPackage = this.value;
        } else {
            this.selectedPackage = this.value.name;
        }
    }
}
