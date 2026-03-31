import { SharedModule } from '@admin-ui/shared/shared.module';
import { NgModule } from '@angular/core';
import { provideRouter, RouterModule, withComponentInputBinding } from '@angular/router';
import {
    AssignLanguagesToNodeModal,
    CopyNodesModalComponent,
    CreateNodeWizardComponent,
    DeleteNodesModalComponent,
    NodeEditorComponent,
    NodeFeaturesComponent,
    NodeMasterComponent,
    NodePropertiesComponent,
    NodePublishingPropertiesComponent,
    NodeFormsAssignmentComponent
} from './components';
import { NODE_ROUTES } from './node.routes';

@NgModule({
    declarations: [
        AssignLanguagesToNodeModal,
        CopyNodesModalComponent,
        CreateNodeWizardComponent,
        DeleteNodesModalComponent,
        NodeEditorComponent,
        NodeFeaturesComponent,
        NodeMasterComponent,
        NodePropertiesComponent,
        NodePublishingPropertiesComponent,
    ],
    providers: [
        provideRouter(NODE_ROUTES, withComponentInputBinding()),
    ],
    imports: [
        SharedModule,
        RouterModule.forChild(NODE_ROUTES),
        NodeFormsAssignmentComponent,
    ],
})
export class NodeModule { }
