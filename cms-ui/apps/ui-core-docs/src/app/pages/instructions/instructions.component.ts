import { ChangeDetectionStrategy, Component } from '@angular/core';

@Component({
    templateUrl: './instructions.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false
})
export class InstructionsPage {}
