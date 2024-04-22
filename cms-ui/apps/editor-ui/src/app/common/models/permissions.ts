import {
    FilePermissions,
    FolderPermissions,
    FormPermissions,
    ImagePermissions,
    ItemPermissions,
    PagePermissions,
    TagTypePermissions,
    TemplatePermissions,
} from '@gentics/cms-models';

/*
 * This file contains Models for permissions as handled in our app.
 */

/**
 * Permissions used in the Editor UI, abstracted from the server logic.
 */
export interface EditorPermissions {
    /** Permission to assign user permissions to a folder */
    assignPermissions: boolean;

    /** Permissions for files */
    file: FilePermissions;

    /** Permissions for folders */
    folder: FolderPermissions;

    /** Permissions for forms */
    form: FormPermissions;

    /** Permissions for images */
    image: ImagePermissions;

    /** Permissions for pages */
    page: PagePermissions;

    /** Permission to synchronize changes to the parent node */
    synchronizeChannel: boolean;

    /** Permissions for tag types */
    tagType: TagTypePermissions;

    /** Permissions for templates */
    template: TemplatePermissions;

    /** Permission to view the wastebin and restore items from it */
    wastebin: boolean;
}

/**
 * A permission hash with no permissions.
 */
export const getNoPermissions: () => EditorPermissions = () => {
    return {
        assignPermissions: false,
        file: {
            create: false,
            delete: false,
            edit: false,
            import: false,
            inherit: false,
            localize: false,
            unlocalize: false,
            upload: false,
            view: false,
        },
        folder: {
            create: false,
            delete: false,
            edit: false,
            inherit: false,
            localize: false,
            unlocalize: false,
            view: false,
        },
        form: {
            create: false,
            delete: false,
            edit: false,
            publish: false,
            view: false,
            inherit: false,
            localize: false,
            unlocalize: false,
        },
        image: {
            create: false,
            delete: false,
            edit: false,
            import: false,
            inherit: false,
            localize: false,
            unlocalize: false,
            upload: false,
            view: false,
        },
        page: {
            create: false,
            delete: false,
            edit: false,
            import: false,
            inherit: false,
            linkTemplate: false,
            localize: false,
            publish: false,
            translate: false,
            unlocalize: false,
            view: false,
        },
        synchronizeChannel: false,
        tagType: {
            create: false,
            delete: false,
            edit: false,
            view: false,
        },
        template: {
            create: false,
            delete: false,
            edit: false,
            inherit: false,
            link: false,
            localize: false,
            unlocalize: false,
            view: false,
        },
        wastebin: false,
    };
}

/**
 * A base item permission hash with no permissions.
 */
export const noItemPermissions: ItemPermissions = {
    create: false,
    delete: false,
    edit: false,
    inherit: false,
    localize: false,
    unlocalize: false,
    view: false,
};
Object.freeze(noItemPermissions);
