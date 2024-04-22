import { Location } from '@angular/common';
import { Injectable } from '@angular/core';
import { NavigationExtras, Router } from '@angular/router';
import { EditMode } from '@gentics/cms-integration-api-models';
import { FolderItemType } from '@gentics/cms-models';
import { EditorStateUrlOptions } from '../../../state';

export type ListUrlParams = {
    nodeId: number;
    folderId: number;
    searchTerm?: string;
    searchFilters?: any;
};

export type DetailUrlParams = {
    nodeId: number;
    itemType: FolderItemType | 'node' | 'channel';
    itemId: number;
    editMode: EditMode;
    options?: EditorStateUrlOptions;
};

export type ModalByTypeUrlParams = {
    type: string
};

export interface NavigationInstruction {
    list?: ListUrlParams;
    detail?: DetailUrlParams | null;
    modal?: DetailUrlParams | null;
    modalByType?: ModalByTypeUrlParams | null;
}

export interface InstructionActions {
    /** Navigate to the configured location. */
    navigate(extras?: NavigationExtras): Promise<boolean>;
    /** Navigate to the configured location only if the current URL is not an editor route */
    navigateIfNotSet(extras?: NavigationExtras): Promise<boolean>;
    /** Return the Router commands for the configured location. */
    commands(): any[];
}

/**
 * A wrapper around the Angular router which provides a type-safe method of navigating the UI router states.
 */
@Injectable()
export class NavigationService {

    // An empty object, in JSON and Base64 encoded.
    emptyObjectSerialized = btoa(JSON.stringify({}));

    constructor(
        private router: Router,
        private location: Location,
    ) { }

    /**
     * This is the generic method for generating commands based on the NavigationInstruction config object.
     */
    instruction(instruction: NavigationInstruction): InstructionActions {
        const commands = this.commands(instruction);

        return {
            navigate: (extras?: NavigationExtras) => {
                return this.router.navigate(commands, extras);
            },
            navigateIfNotSet: (extras?: NavigationExtras) => {
                if (!/^\/editor\//.test(this.location.path())) {
                    return this.router.navigate(commands, extras);
                } else {
                    return Promise.resolve(false);
                }
            },
            commands: () => {
                return commands;
            },
        };
    }

    /**
     * Shortcut method for navigating to a list route.
     */
    list(
        nodeId: number,
        folderId: number,
        searchTerm?: string,
        searchFilters?: any,
    ): InstructionActions {
        return this.instruction({
            list: {
                nodeId,
                folderId,
                searchTerm,
                searchFilters,
            },
        });
    }

    detailOrModal(
        nodeId: number,
        itemType: FolderItemType | 'node' | 'channel',
        itemId: number,
        editMode: EditMode,
        options?: EditorStateUrlOptions,
    ): InstructionActions {
        // Image Editing is provided by EditorOverlay
        // This helper funcion would help to move from ContentFrame editors
        if (itemType === 'image' && editMode === EditMode.EDIT) {
            return this.modal(nodeId, itemType, itemId, editMode, options);
        } else {
            return this.detail(nodeId, itemType, itemId, editMode, options);
        }
    }

    /**
     * Shortcut method for navigating to a detail route.
     */
    detail(
        nodeId: number,
        itemType: FolderItemType | 'node' | 'channel',
        itemId: number,
        editMode: EditMode,
        options?: EditorStateUrlOptions,
    ): InstructionActions {
        return this.instruction({
            detail: {
                nodeId,
                itemType,
                itemId,
                editMode,
                options,
            },
        });
    }

    /**
     * Shortcut method for navigating to a modal route.
     */
    modal(nodeId: number,
        itemType: FolderItemType | 'node' | 'channel',
        itemId: number,
        editMode: EditMode,
        options?: EditorStateUrlOptions): InstructionActions {
        return this.instruction({
            modal: {
                nodeId,
                itemType,
                itemId,
                editMode,
                options,
            },
        });
    }

    modalByType(type: string): InstructionActions {
        return this.instruction({
            modalByType: { type },
        });
    }

    navigateToNoNodes(): Promise<boolean> {
        return this.router.navigate(['/no-nodes']);
    }

    /**
     * Decode an object which was encoded by serializeOptions()
     */
    deserializeOptions<T>(options: string): T {
        return JSON.parse(atob(options));
    }

    /**
     * Encode an options object into a url-safe string
     */
    serializeOptions(options: any): { options: string } {
        const serializedOptions = btoa(JSON.stringify(options || {}));
        return { options: serializedOptions };
    }

    /**
     * Converts the NavigationInstruction object into a router commands array.
     */
    private commands(instruction: NavigationInstruction): any[] {
        const outlets = {} as any;

        if (instruction.detail === null) {
            outlets.detail = null;
        }

        if (instruction.modal === null) {
            outlets.modal = null;
        }

        if (instruction.detail) {
            const { nodeId, itemType, itemId, editMode, options } = instruction.detail;
            const optionsArg = this.serializeOptions(options);
            outlets.detail = ['node', nodeId, itemType, itemId, editMode, optionsArg];
        }

        if (instruction.modal) {
            const { nodeId, itemType, itemId, editMode, options } = instruction.modal;
            const optionsArg = this.serializeOptions(options);
            outlets.modal = ['node', nodeId, itemType, itemId, editMode, optionsArg];
        }

        if (instruction.modalByType) {
            const { type } = instruction.modalByType;
            outlets.modal = ['type', type];
        }

        if (instruction.list) {
            const { nodeId, folderId, searchTerm, searchFilters } = instruction.list;
            outlets.list = ['node', nodeId, 'folder', folderId];

            const searchFiltersArg = this.serializeOptions(searchFilters).options;
            const searchTermText = searchTerm || '';
            const searchArgs = {} as any;

            if (searchTermText !== '') {
                searchArgs.searchTerm = searchTermText;
            }
            if (searchFiltersArg !== this.emptyObjectSerialized) {
                searchArgs.searchFilters = searchFiltersArg;
            }
            if (searchArgs.searchTerm || searchArgs.searchFilters) {
                outlets.list.push(searchArgs);
            }
        }

        return ['/editor', { outlets }];
    }
}
