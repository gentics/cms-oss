import { Component, OnInit } from '@angular/core';
import { AppService } from '../../services/app/app.service';
import { FilterService } from '../../services/filter/filter.service';

@Component({
    selector: 'gtxct-root',
    templateUrl: './app.component.html',
    styleUrls: ['./app.component.scss'],
    standalone: false
})
export class AppComponent implements OnInit {
    constructor(
        private app: AppService,
        private filter: FilterService
    ) { }

    ngOnInit(): void {
        this.app.init();
        this.filter.init();
    }
}
