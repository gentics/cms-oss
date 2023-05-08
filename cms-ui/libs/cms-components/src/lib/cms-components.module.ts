import { NgModule } from '@angular/core';
import { CoreModule } from './core/core.module';
import { DemoModule } from './demo/demo.module';

@NgModule({
    imports: [
        CoreModule,
        DemoModule,
    ],
    exports: [
        CoreModule,
    ],
})
export class CmsComponentsModule { }
