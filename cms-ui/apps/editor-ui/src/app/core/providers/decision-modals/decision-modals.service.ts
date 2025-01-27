import { Injectable } from '@angular/core';
import { ApplicationStateService, FeaturesActionsService, FolderActionsService } from '@editor-ui/app/state';
import {
    Feature,
    File,
    Folder,
    Form,
    GcmsPermission,
    GcmsRolePrivilege,
    Image,
    InheritableItem,
    Node,
    Page,
    Raw,
} from '@gentics/cms-models';
import { ModalService } from '@gentics/ui-core';
import { take } from 'rxjs/operators';
import { itemIsLocalized } from '../../../common/utils/item-is-localized';
import {
    FormLanguageVariantMap,
    MultiDeleteModal,
    MultiDeleteResult,
    PageLanguageVariantMap,
} from '../../../shared/components/multi-delete-modal/multi-delete-modal.component';
import { MultiMoveModal } from '../../../shared/components/multi-move-modal/multi-move-modal.component';
import { MultiRestoreModalComponent } from '../../../shared/components/multi-restore-modal/multi-restore-modal.component';
import { PublishPagesModalComponent } from '../../../shared/components/publish-pages-modal/publish-pages-modal.component';
import { TakePagesOfflineModal } from '../../../shared/components/take-pages-offline-modal/take-pages-offline-modal.component';
import { EntityResolver } from '../entity-resolver/entity-resolver';
import { I18nService } from '../i18n/i18n.service';
import { LocalizationsService } from '../localizations/localizations.service';
import { PermissionService } from '../permissions/permission.service';

/**
 * A shorthand service for modals displayed in multiple places
 * where the user may pick an option to proceed depending on his choice.
 *
 * Examples:
 * - When editing an inherited page, asks the user if they want
 *   to edit the master item or create a local copy and edit that one
 * - When deleting multiple items and some are localized, asks
 *   if they should be unlocalized, as that action can not be undone.
 */
@Injectable()
export class DecisionModalsService {

    constructor(
        private appState: ApplicationStateService,
        private entityResolver: EntityResolver,
        private modalService: ModalService,
        private i18n: I18nService,
        private localizationService: LocalizationsService,
        private folderActions: FolderActionsService,
        private permissionService: PermissionService,
        private featuresActions: FeaturesActionsService,
    ) { }

    /**
     * Displays a confirmation dialog before taking an action on a possibly-inherited item to ask whether
     * the user wishes to edit the master version or create a new localization and then work on that.
     */
    showInheritedDialog(item: InheritableItem | Node, nodeId: number): Promise<{ item: InheritableItem | Node, nodeId: number }> {
        // We cannot rely on the `item.inherited` property since its value varies depending on the node context under
        // which the data was last fetched. A reliable way is to compare the inheritedFromId with the current node - this
        // should remain constant.
        const itemIsInherited = item.inheritedFromId !== nodeId;

        if (!itemIsInherited || item.type === 'node' || item.type === 'channel' || item.type === 'form') {
            return Promise.resolve({ item, nodeId });
        }

        return Promise.all([
            this.featuresActions.checkFeature(Feature.ALWAYS_LOCALIZE),
            this.permissionService.forItem(item, item.inheritedFromId).pipe(take(1)).toPromise(),
        ])
            .then(([alwaysLocalizeIsEnabled, permOriginal]) => {
                if (alwaysLocalizeIsEnabled || !permOriginal.edit) {
                    return this.localizeItemAndRefreshList(item as InheritableItem, nodeId);
                } else {
                    return this.askUserInWhichNodeToEdit(item as InheritableItem, nodeId);
                }
            });
    }

    private localizeItemAndRefreshList(item: InheritableItem, nodeId: number): Promise<{ item: InheritableItem<Raw>, nodeId: number }> {
        return this.folderActions.localizeItem(item.type, item.id, nodeId)
            .then(localizedItem => {
                // Refresh the list view to reflect the new local item.
                const parentFolderId = (localizedItem as Page | File | Image).folderId ||
                    (localizedItem as Folder).motherId;
                const currentFolderId = this.appState.now.folder.activeFolder;
                if (parentFolderId === currentFolderId) {
                    this.folderActions.getItems(parentFolderId, item.type);
                }
                return { item: localizedItem, nodeId };
            });
    }

    private askUserInWhichNodeToEdit(item: InheritableItem, nodeId: number): Promise<{ item: InheritableItem, nodeId: number }> {
        const localNodeName = this.entityResolver.getNode(nodeId).name;
        return this.modalService
            .dialog({
                title: this.i18n.translate('modal.edit_inherited_title', { _type: item.type, name: item.name }),
                body: this.i18n.translate('modal.edit_inherited_body', {
                    _type: item.type,
                    master: (item ).inheritedFrom,
                    name: item.name,
                    local: localNodeName,
                }),
                buttons: [
                    {
                        label: this.i18n.translate('common.cancel_button'),
                        type: 'secondary',
                        returnValue: '',
                        flat: true,
                    },
                    {
                        label: this.i18n.translate('modal.edit_original_button'),
                        type: 'secondary',
                        returnValue: 'editOriginal',
                    },
                    {
                        label: this.i18n.translate('modal.localize_and_edit_button'),
                        type: 'default',
                        returnValue: 'localize',
                    },
                ],
            }, { width: '800px' })
            .then(modal => modal.open())
            .then((result: string) => {
                if (result === 'editOriginal') {
                    return { item, nodeId: item.inheritedFromId };
                } else if (result === 'localize') {
                    return this.localizeItemAndRefreshList(item, nodeId) as Promise<{ item: InheritableItem, nodeId: number }>;
                }
            });
    }

    /**
     * If the user wants to translate a page into a different language and the page is inherited,
     * ask the user if they want to create the translation in the master node or a localized
     * one in the current node.
     */
    showTranslatePageDialog(page: Page, nodeId: number): Promise<number> {
        if (page.masterNodeId === nodeId) {
            return Promise.resolve(nodeId);
        }

        const localNode = this.entityResolver.getNode(nodeId);
        const masterNode = this.entityResolver.getNode(page.masterNodeId);

        return this.modalService.dialog({
            title: this.i18n.translate('modal.translate_inherited_title'),
            body: this.i18n.translate('modal.translate_inherited_body', {
                master: page.masterNode,
                local: localNode.name,
            }),
            buttons: [
                {
                    label: this.i18n.translate('common.cancel_button'),
                    type: 'secondary',
                    shouldReject: true,
                    flat: true,
                },
                {
                    label: this.i18n.translate('modal.translate_in_master_node_button'),
                    type: 'secondary',
                    returnValue: masterNode.id,
                },
                {
                    label: this.i18n.translate('modal.translate_in_local_node_button'),
                    type: 'default',
                    returnValue: localNode.id,
                },
            ],
        }, { width: '800px' })
            .then<number>(modal => modal.open());
    }

    /**
     * When publish pages, if a page has language variants, the user must choose whether to publish the current
     * variant, or all variants. If a single page is going to be published no modal at all. If publishLanguageVariants
     * is true, then always open the modal without preselections.
     *
     * Returns a promise which resolves to a list of page ids.
     */
    async selectPagesToPublish(pages: Page[], publishLanguageVariants: boolean = false): Promise<Page[]> {
        const data = this.getPermittedPageLanguages(pages, [GcmsPermission.EDIT, GcmsRolePrivilege.UPDATE_ITEMS]);

        // If the user has no permission to publish any page to begin with
        if (data.pages.length === 0) {
            return [];
        }

        const langId = this.appState.now.folder.activeLanguage;
        const currentLang = this.appState.now.entities.language[langId];

        const pageLanguages = new Set<string>(data.pages.map(page => page.language));

        // If we only want to publish pages in the current language,
        // and all final pages have the correct language, then we can
        // skip the modal and simply publish the expected variants.
        if (
            !publishLanguageVariants
            && pageLanguages.size === 1
            && pageLanguages.has(currentLang.code)
        ) {
            return Promise.resolve(data.pages);
        }

        // Show the modal to the user, so that they can select the variants they
        // wish to publish manually.
        return this.modalService.fromComponent(PublishPagesModalComponent, null, {
            pages: data.pages,
            variants: data.variants,
            selectVariants: publishLanguageVariants,
        })
            .then(modal => modal.open());
    }

    /**
     * When taking a page offline, if it has language variants, the user must choose whether to take the current
     * variant offline, or all variants.
     *
     * Returns a promise which resolves to a list of page ids.
     */
    selectPagesToTakeOffline(pages: Page[]): Promise<number[]> {
        const data = this.getPermittedPageLanguages(pages, [GcmsPermission.EDIT, GcmsRolePrivilege.UPDATE_ITEMS]);

        // If the user has no permission to un-publish any page to begin with
        if (data.pages.length === 0) {
            return Promise.resolve([]);
        }

        const pagesToTakeOffline = data.pages;

        const anyPagesHaveMultipleLanguage = Object.values(data.variants)
            .some(arr => arr.length > 1);

        if (!anyPagesHaveMultipleLanguage) {
            // In this case we are simply taking offline local items which do not have any other language
            // variations, so we can go ahead and resolve without displaying a dialog.
            return Promise.resolve(pagesToTakeOffline.map((page) => page.id));
        }

        return this.modalService.fromComponent(TakePagesOfflineModal, null, {
            pagesToTakeOffline,
            pageLanguageVariants: data.variants,
        })
            .then(modal => modal.open());
    }

    /**
     * When deleting multiple items, ask the user whether to delete localized items
     * and show a list of inherited items which can not be deleted.
     */
    selectItemsToDelete(items: InheritableItem[]): Promise<MultiDeleteResult> {
        // If there's no multichanneling enabled, we don't need any of this. Just simply delete the files
        if (!this.appState.now.features[Feature.MULTICHANNELLING]) {
            const pages = items.filter(item => item.type === 'page') as Page[];
            const pageData = this.getPermittedPageLanguages(pages, [GcmsPermission.DELETE_ITEMS]);

            return this.modalService.fromComponent(MultiDeleteModal, null, {
                otherItems: items,
                localizedItems: [],
                inheritedItems: [],
                itemLocalizations: {},
                pageLanguageVariants: pageData.variants,
                formLanguageVariants: this.createFormLanguageVariantsMap(items),
            }).then(modal => modal.open());
        }

        const inheritedItems: InheritableItem[] = [];
        const localizedItems: InheritableItem[] = [];
        const otherItems: InheritableItem[] = [];

        for (const item of items) {
            if (item.inherited) {
                inheritedItems.push(item);
            } else if (itemIsLocalized(item)) {
                localizedItems.push(item);
            } else {
                otherItems.push(item);
            }
        }

        const pages = [...otherItems, ...localizedItems].filter(item => item.type === 'page') as Page[];
        const pageData = this.getPermittedPageLanguages(pages, [GcmsPermission.DELETE_ITEMS]);
        const formLanguageVariants: FormLanguageVariantMap = this.createFormLanguageVariantsMap([...otherItems]);

        // Remove all pages which aren't allowed
        const filteredOtherItems = otherItems.filter(item => item.type !== 'page' || pageData.pages.includes(item as Page));

        return this.localizationService.getLocalizationMap(items.filter(item => item.type !== 'form'))
            .toPromise()
            .then(itemLocalizations => {
                return this.modalService.fromComponent(MultiDeleteModal, null, {
                    otherItems: filteredOtherItems,
                    localizedItems,
                    inheritedItems,
                    pageLanguageVariants: pageData.variants,
                    formLanguageVariants,
                    itemLocalizations: itemLocalizations,
                });
            })
            .then(modal => modal.open());
    }

    /**
     * When restoring multiple items, ask the user whether to restore localized items
     * and show a list of inherited items which can not be restored.
     */
    async selectItemsToRestore(
        files: File[],
        folders: Folder[],
        forms: Form[],
        images: Image[],
        pages: Page[],
    ): Promise<void | { [type: string]: number[] }> {
        return this.modalService.fromComponent(
            MultiRestoreModalComponent,
            null,
            {
                files,
                folders,
                forms,
                images,
                pages,
            },
        )
            .then(modal => modal.open());
    }

    /**
     * When moving multiple items, inform the user that inherited and localized items can not be moved.
     */
    moveMultipleItems(items: InheritableItem[], targetFolder: Folder, targetNode: Node): Promise<InheritableItem[]> {
        const hasUnmovableItems = items.some(item => item.inherited || itemIsLocalized(item));

        if (!hasUnmovableItems) {
            return Promise.resolve(items);
        }

        return this.modalService.fromComponent(MultiMoveModal, null, { items, targetFolder, targetNode })
            .then(modal => modal.open());
    }

    private getPermittedPageLanguages(
        pages: Page[],
        permissions: (GcmsPermission | GcmsRolePrivilege)[],
    ): {
            languages: Set<string>,
            pages: Page[],
            variants: PageLanguageVariantMap,
        }
    {
        const validPages: Page[] = [];
        const variationMap: PageLanguageVariantMap = {};
        const languages = new Set<string>();

        for (const currentPage of pages) {
            if (currentPage.inherited) {
                continue;
            }

            const folderPermissions = this.appState.now.entities.folder[currentPage.folderId]?.permissionsMap;

            // Check the general permissions
            const hasGeneralPerm = permissions.some(perm => folderPermissions?.permissions?.[perm]);
            if (!hasGeneralPerm) {
                let hasValidLangPerm = false;

                for (let langPage of (Object.values(currentPage.languageVariants) as Page[])) {
                    if (typeof langPage === 'number') {
                        langPage = this.entityResolver.getPage(langPage);
                    }
                    if (langPage == null || typeof langPage !== 'object') {
                        continue;
                    }

                    // Check if there's any special role permissions
                    const lang = langPage.language;
                    const rolePerms = folderPermissions?.rolePermissions?.pageLanguages?.[lang];
                    const hasRolePerm = permissions.some(perm => rolePerms?.[perm]);
                    if (!hasRolePerm) {
                        continue;
                    }

                    hasValidLangPerm = true;
                    if (!variationMap[currentPage.id]) {
                        variationMap[currentPage.id] = [];
                    }
                    variationMap[currentPage.id].push(langPage);
                    languages.add(lang);
                }

                if (!hasValidLangPerm) {
                    continue;
                }
            } else {
                for (let langPage of (Object.values(currentPage.languageVariants) as Page[])) {
                    if (typeof langPage === 'number') {
                        langPage = this.entityResolver.getPage(langPage);
                    }
                    if (langPage == null || typeof langPage !== 'object') {
                        continue;
                    }
                    if (!variationMap[currentPage.id]) {
                        variationMap[currentPage.id] = [];
                    }
                    variationMap[currentPage.id].push(langPage);
                    languages.add(langPage.language);
                }
            }

            // Edge case, for when the user has no permissions for the `currentPage`,
            // but for a language variant.
            if (!variationMap[currentPage.id].includes(currentPage)) {
                validPages.push(variationMap[currentPage.id][0]);
            } else {
                validPages.push(currentPage);
            }
        }

        return {
            languages: languages,
            pages: validPages,
            variants: variationMap,
        };
    }

    private createFormLanguageVariantsMap(items: InheritableItem[]): FormLanguageVariantMap {
        const variantsPerFormLanguage: FormLanguageVariantMap = {};
        const forms = items.filter(item => item.type === 'form') as Form[];
        forms.forEach(form => {
            variantsPerFormLanguage[form.id] = form.languages;
        });
        return variantsPerFormLanguage;
    }
}
