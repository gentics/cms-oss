import { ChangeDetectionStrategy, Component } from '@angular/core';

@Component({
    templateUrl: './icons-demo.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false
})
export class IconsDemoPage {}
