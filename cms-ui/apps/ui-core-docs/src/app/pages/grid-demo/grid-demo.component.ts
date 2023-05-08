import {ChangeDetectionStrategy, Component} from '@angular/core';

@Component({
    templateUrl: './grid-demo.component.html',
    styleUrls: ['./grid-demo.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class GridDemoPage {}
