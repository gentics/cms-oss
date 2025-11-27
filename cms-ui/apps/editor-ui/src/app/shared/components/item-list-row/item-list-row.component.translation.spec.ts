import { ChangeDetectorRef } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { I18nService } from '@gentics/cms-components';
import { MockI18nPipe } from '@gentics/cms-components/testing';
import {
    AnyModelType,
    Node,
    Normalized,
    Page,
} from '@gentics/cms-models';
import { getExamplePageDataNormalized } from '@gentics/cms-models/testing/test-data.mock';
import { ModalService } from '@gentics/ui-core';
import { WindowRef } from 'ngx-autosize';
import { ItemLanguageClickEvent } from '../../../common/models';
import { ContextMenuOperationsService } from '../../../core/providers/context-menu-operations/context-menu-operations.service';
import { DecisionModalsService } from '../../../core/providers/decision-modals/decision-modals.service';
import { EntityResolver } from '../../../core/providers/entity-resolver/entity-resolver';
import { ErrorHandler } from '../../../core/providers/error-handler/error-handler.service';
import { FavouritesService } from '../../../core/providers/favourites/favourites.service';
import { NavigationService } from '../../../core/providers/navigation/navigation.service';
import { ApplicationStateService, FolderActionsService, UsageActionsService, WastebinActionsService } from '../../../state';
import { ItemListRowComponent } from './item-list-row.component';

describe('ItemListRowComponent', () => {
    const nodeId = 1;
    let component: ItemListRowComponent;
    let fixture: ComponentFixture<ItemListRowComponent>;
    let decisionModalService: jasmine.SpyObj<DecisionModalsService>;
    let folderActionsService: jasmine.SpyObj<FolderActionsService>;
    const errorHandlerServiceSpy: jasmine.SpyObj<ErrorHandler> = jasmine.createSpyObj('ErrorHandler', ['catch']);

    beforeEach(async () => {
        const mockedNode: jasmine.SpyObj<Node<AnyModelType>> = jasmine.createSpyObj('Node', [], [
            { id: nodeId },
        ]);
        const decisionModalServiceSpy = jasmine.createSpyObj('DecisionModalsService', ['showTranslatePageDialog']);
        const folderActionServiceSpy = jasmine.createSpyObj('FolderActionsService', ['updatePageLanguage', 'refreshList']);
        const modalServiceSpy = jasmine.createSpyObj('ModalService', ['fromComponent', 'open']);

        decisionModalServiceSpy.showTranslatePageDialog.and.returnValue(Promise.resolve(nodeId));
        folderActionServiceSpy.updatePageLanguage.and.returnValue(Promise.resolve(null));
        modalServiceSpy.fromComponent.and.returnValue(Promise.resolve(modalServiceSpy));
        modalServiceSpy.open.and.returnValue(Promise.resolve({}));

        await TestBed.configureTestingModule({
            declarations: [
                ItemListRowComponent,
                MockI18nPipe,
            ],
            providers: [
                { provide: DecisionModalsService, useValue: decisionModalServiceSpy },
                { provide: ErrorHandler, useValue: errorHandlerServiceSpy },
                { provide: FolderActionsService, useValue: folderActionServiceSpy },
                { provide: ApplicationStateService, useValue: {} },
                { provide: ChangeDetectorRef, useValue: {} },
                { provide: ContextMenuOperationsService, useValue: {} },
                { provide: ModalService, useValue: modalServiceSpy },
                { provide: EntityResolver, useValue: {} },
                { provide: FavouritesService, useValue: {} },
                { provide: I18nService, useValue: {} },
                { provide: NavigationService, useValue: {} },
                { provide: UsageActionsService, useValue: {} },
                { provide: WastebinActionsService, useValue: {} },
                { provide: WindowRef, useValue: {} },
            ],
        }).compileComponents();

        fixture = TestBed.createComponent(ItemListRowComponent);
        component = fixture.componentInstance;
        component.activeNode = mockedNode;
        (component.activeNode as any).id = nodeId;
        decisionModalService = TestBed.inject(DecisionModalsService) as jasmine.SpyObj<DecisionModalsService>;
        folderActionsService = TestBed.inject(FolderActionsService) as jasmine.SpyObj<FolderActionsService>;
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });

    it('should call showTranslatePageDialog to open translate-page-modal', () => {
        const pageEn = getMockedPage();
        const languageTranslationEvent = getMockedLanguageEvent(pageEn);

        component.pageLanguageClicked(languageTranslationEvent);
        expect(decisionModalService.showTranslatePageDialog).toHaveBeenCalledWith(pageEn, nodeId);
    });

    it('should call updatePageLanguage for page without language set', () => {
        const pageEn = getMockedPage();
        pageEn.language = null;
        const languageTranslationEvent = getMockedLanguageEvent(pageEn);

        component.pageLanguageClicked(languageTranslationEvent);
        expect(folderActionsService.updatePageLanguage).toHaveBeenCalled();
    });

});

function getMockedPage(): Page {
    return {
        ...getExamplePageDataNormalized({ id: 1 }),
        languageVariants: [1],
        online: true,
        language: 'en',
        deleted: {
            at: 0,
            by: null,
        },
    };
}

function getMockedLanguageEvent(page: Page): ItemLanguageClickEvent<Page<Normalized>> {
    return {
        item: page,
        language: { id: 2, code: 'de', name: 'Deutsch (German)' },
        compare: false,
        source: true,
        restore: false,
    };
}
