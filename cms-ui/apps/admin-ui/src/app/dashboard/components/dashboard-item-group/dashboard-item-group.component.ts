import { Component, OnInit, Input } from '@angular/core';

@Component({
  selector: 'gtx-dashboard-item-group',
  templateUrl: './dashboard-item-group.component.html',
  styleUrls: ['./dashboard-item-group.component.scss']
})
export class DashboardItemGroupComponent implements OnInit {

  @Input() title: string;

  constructor() { }

  ngOnInit(): void {}

}
