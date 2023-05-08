import { SharedModule } from '@admin-ui/shared/shared.module';
import { NgModule } from '@angular/core';
import { RouterModule } from '@angular/router';
import {
    AssignLanguagesToNodeModal,
    CopyNodesModalComponent,
    CreateNodeWizardComponent,
    DeleteNodesModalComponent,
    NodeDetailComponent,
    NodeFeaturesComponent,
    NodeMasterComponent,
    NodePropertiesComponent,
    NodePublishingPropertiesComponent,
} from './components';
import { NODE_ROUTES } from './node.routes';
import { CanActivateNodeGuard } from './providers';

@NgModule({
    declarations: [
        AssignLanguagesToNodeModal,
        CopyNodesModalComponent,
        CreateNodeWizardComponent,
        DeleteNodesModalComponent,
        NodeDetailComponent,
        NodeFeaturesComponent,
        NodeMasterComponent,
        NodePropertiesComponent,
        NodePublishingPropertiesComponent,
    ],
    providers: [
        CanActivateNodeGuard,
    ],
    imports: [
        SharedModule,
        RouterModule.forChild(NODE_ROUTES),
    ],
})
export class NodeModule { }
