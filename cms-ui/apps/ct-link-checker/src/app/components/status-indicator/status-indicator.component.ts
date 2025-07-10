import { Component, Input, OnInit } from '@angular/core';

@Component({
    selector: 'gtxct-status-indicator',
    templateUrl: './status-indicator.component.html',
    styleUrls: ['./status-indicator.component.scss'],
    standalone: false
})
export class StatusIndicatorComponent implements OnInit {

  @Input() alert: boolean;
  @Input() info: boolean;
  @Input() success: boolean;
  @Input() iconName: string;

  constructor() { }

  ngOnInit(): void {}

}
