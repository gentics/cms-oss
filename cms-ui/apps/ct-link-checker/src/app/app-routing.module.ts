import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';

//
// Currently no routing used by this custom tool, its there only for just in case.
//

const APP_ROUTES: Routes = [];

@NgModule({
    imports: [RouterModule.forRoot(APP_ROUTES, { useHash: true })],
    exports: [RouterModule],
})
export class AppRoutingModule { }
