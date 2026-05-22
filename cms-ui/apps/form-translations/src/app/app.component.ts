import { ChangeDetectionStrategy, Component, OnInit } from '@angular/core';

import { AppService } from './core/services/app.service';

@Component({
  selector: 'gtx-root',
  standalone: false,
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: '<gtx-shell></gtx-shell>'
})
export class AppComponent implements OnInit {
  constructor(private readonly appService: AppService) {}

  ngOnInit(): void {
    void this.appService.initialize();
  }
}
