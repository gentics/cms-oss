import {ChangeDetectionStrategy, Component} from '@angular/core';

@Component({
    templateUrl: './grid-demo.component.html',
    styleUrls: ['./grid-demo.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false
})
export class GridDemoPage {}
