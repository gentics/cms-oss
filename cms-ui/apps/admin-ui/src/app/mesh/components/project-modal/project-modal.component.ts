import { ProjectHandlerService } from '@admin-ui/mesh/providers/project-handler/project-handler.service';
import { ChangeDetectionStrategy, ChangeDetectorRef, Component, Input, OnInit } from '@angular/core';
import { FormControl } from '@angular/forms';
import { Project } from '@gentics/mesh-models';
import { BaseModal } from '@gentics/ui-core';
import { ProjectPropertiesMode } from '../project-properties/project-properties.component';

@Component({
    selector: 'gtx-mesh-project-modal',
    templateUrl: './project-modal.component.html',
    styleUrls: ['./project-modal.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false
})
export class ProjectModal extends BaseModal<Project> implements OnInit {

    public readonly ProjectPropertiesMode = ProjectPropertiesMode;

    @Input()
    public mode: ProjectPropertiesMode;

    @Input()
    public project: Project;

    public form: FormControl;

    constructor(
        protected changeDetector: ChangeDetectorRef,
        protected handler: ProjectHandlerService,
    ) {
        super();
    }

    ngOnInit(): void {
        this.form = new FormControl(this.project || {});
    }

    buttonCreateEntityClicked(): void {
        const val = this.form.value;
        this.form.disable();
        this.changeDetector.markForCheck();

        const op = this.mode === ProjectPropertiesMode.CREATE
            ? this.handler.create(val)
            : this.handler.update(this.project.uuid, val);

        op.then(res => {
            this.closeFn(res);
        }).catch(() => {
            this.form.enable();
            this.changeDetector.markForCheck();
        });
    }
}
