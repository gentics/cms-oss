import { ChangeDetectionStrategy, Component } from '@angular/core';

@Component({
    templateUrl: './typography-demo.component.html',
    styleUrls: ['./typography-demo.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false
})
export class TypographyDemoPage { }
