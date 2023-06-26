import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { RouterModule } from '@angular/router';
import { SharedModule } from '../../shared/shared.module';
import {
    AssignTemplatesToFoldersModalComponent,
    AssignTemplatesToNodesModalComponent,
    CopyTemplateModal,
    CreateTemplateModalComponent,
    CreateTemplateTagModalComponent,
    EditTemplateTagModalComponent,
    TemplateDetailComponent,
    TemplateFolderLinkMasterComponent,
    TemplateMasterComponent,
    TemplatePropertiesComponent,
    TemplateTagMasterComponent,
    TemplateTagPropertiesComponent,
    TemplateTagStatusMasterComponent,
    TemplateTagStatusTableComponent,
    TemplateTagTableComponent,
} from './components';
import {
    CanActivateTemplateGuard,
    TemplateTagStatusTableLoaderService,
    TemplateTagTableLoaderService,
} from './providers';
import { TEMPLATE_ROUTES } from './template.routes';

@NgModule({
    declarations: [
        AssignTemplatesToFoldersModalComponent,
        AssignTemplatesToNodesModalComponent,
        CopyTemplateModal,
        CreateTemplateModalComponent,
        CreateTemplateTagModalComponent,
        EditTemplateTagModalComponent,
        TemplateDetailComponent,
        TemplateFolderLinkMasterComponent,
        TemplateMasterComponent,
        TemplatePropertiesComponent,
        TemplateTagMasterComponent,
        TemplateTagPropertiesComponent,
        TemplateTagStatusMasterComponent,
        TemplateTagStatusTableComponent,
        TemplateTagTableComponent,
    ],
    providers: [
        CanActivateTemplateGuard,
        TemplateTagStatusTableLoaderService,
        TemplateTagTableLoaderService,
    ],
    imports: [
        SharedModule,
        CommonModule,
        RouterModule.forChild(TEMPLATE_ROUTES),
    ],
})
export class TemplateModule {}
