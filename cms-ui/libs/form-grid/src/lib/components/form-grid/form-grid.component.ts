import { ChangeDetectionStrategy, ChangeDetectorRef, Component, input, model } from '@angular/core';
import { FormElementConfiguration, FormSchema, FormTypeConfiguration, FormUISchema } from '@gentics/cms-models';
import { BaseComponent } from '@gentics/ui-core';

@Component({
    selector: 'gtx-form-grid',
    templateUrl: './form-grid.component.html',
    styleUrls: ['./form-grid.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false,
})
export class FormGridComponent extends BaseComponent {

    public config = input<FormTypeConfiguration>();
    public restricted = input<boolean>();

    public schema = model<FormSchema>();
    public uiSchema = model<FormUISchema>();

    public activeElement: string | null = null;
    public isDragging = false;

    constructor(
        changeDetector: ChangeDetectorRef,
    ) {
        super(changeDetector);
    }

    public sidebarDragStart(event: DragEvent, id: string, element: FormElementConfiguration): void {
        this.isDragging = true;
    }

    public sidebarDragEnd(): void {
        this.isDragging = false;
    }
}
