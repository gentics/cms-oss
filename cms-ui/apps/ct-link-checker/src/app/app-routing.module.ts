import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';

//
// Currently no routing used by this custom tool, its there only for just in case.
//

const routes: Routes = [];

@NgModule({
  imports: [RouterModule.forRoot(routes, { useHash: true })],
  exports: [RouterModule]
})
export class AppRoutingModule { }
