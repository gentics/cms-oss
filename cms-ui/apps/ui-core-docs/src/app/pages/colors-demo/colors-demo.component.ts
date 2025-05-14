import { ChangeDetectionStrategy, Component } from '@angular/core';

@Component({
    templateUrl: './colors-demo.component.html',
    styleUrls: ['./colors-demo.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false
})
export class ColorsDemoPage {}
