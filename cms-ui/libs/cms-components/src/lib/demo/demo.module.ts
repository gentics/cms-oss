import { NgModule } from '@angular/core';

import { CoreModule } from '../core/core.module';

import { DemoComponent } from './components';

export const DECLARATIONS: any[] = [
    DemoComponent,
];

@NgModule({
    declarations: [...DECLARATIONS],
    imports: [
        CoreModule,
    ],
    exports: [...DECLARATIONS],
})
export class DemoModule { }
