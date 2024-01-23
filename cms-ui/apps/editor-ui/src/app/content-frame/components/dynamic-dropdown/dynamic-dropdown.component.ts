import { ChangeDetectionStrategy, Component, Input, OnInit } from '@angular/core';
import { DynamicDropdownConfiguration } from '@gentics/cms-integration-api-models';

@Component({
    selector: 'gtx-dynamic-dropdown',
    templateUrl: './dynamic-dropdown.component.html',
    styleUrls: ['./dynamic-dropdown.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DynamicDropdownComponent<T> implements OnInit {

    @Input()
    public configuration: DynamicDropdownConfiguration<T>;

    public ngOnInit(): void {

    }
}
