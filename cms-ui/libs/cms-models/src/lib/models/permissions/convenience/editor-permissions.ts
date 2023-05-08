/**
 * This file contains Models for permissions as handled in our app.
 * For privilege interfaces as they are returned by the API, see privileges.ts
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
 * Permissions for items (of any type)
 */
export interface ItemPermissions {
    /** Permission to create an item of the item type */
    create: boolean;

    /** Permission to delete an item of the item type */
    delete: boolean;

    /** Permission to edit an item of the item type */
    edit: boolean;

    /** Permission to inherit folders/items */
    inherit: boolean;

    /** Permission to localize an inherited item of the item type */
    localize: boolean;

    /** Permission to delete the local version of an inherited item of the item type */
    unlocalize: boolean;

    /** Permission to view an item of the item type */
    view: boolean;
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

/**
 * Permissions for folder items, abstracted from the server logic
 */
export interface FolderPermissions extends ItemPermissions {
    /** Permission to create a folder */
    create: boolean;

    /** Permission to delete a folder */
    delete: boolean;

    /** Permission to edit a folder or its properties */
    edit: boolean;

    /** Permission to localize an inherited folder */
    localize: boolean;

    /** Permission to delete the local version of an inherited folder */
    unlocalize: boolean;

    /** Permission to view a folder and its contents */
    view: boolean;
}

/**
 * Permissions for page items, abstracted from the server logic
 */
export interface PagePermissions extends ItemPermissions {
    /** Permission to create a page */
    create: boolean;

    /** Permission to delete a page */
    delete: boolean;

    /** Permission to edit a page */
    edit: boolean;

    /** Permission to import pages */
    import: boolean;

    /** Permission to link templates */
    linkTemplate: boolean;

    /** Permission to localize an inherited page */
    localize: boolean;

    /** Permission to import pages */
    publish: boolean;

    /** Permission to delete the local version of an inherited page */
    unlocalize: boolean;

    /** Permission to translate a page into the currently active language */
    translate: boolean;

    /** Permission to view a page */
    view: boolean;
}

/**
 * Permissions for files, abstracted from the server logic
 */
export interface FilePermissions extends ItemPermissions {
    /** Permission to upload a file */
    create: boolean;

    /** Permission to delete a file */
    delete: boolean;

    /** Permission to edit a file */
    edit: boolean;

    /** Permission to localize an inherited file */
    localize: boolean;

    /** Permission to import files */
    import: boolean;

    /** Permission to delete the local version of an inherited file */
    unlocalize: boolean;

    /** Permission to upload a file */
    upload: boolean;

    /** Permission to view a file */
    view: boolean;
}

/**
 * Permissions for images, abstracted from the server logic
 */
export interface ImagePermissions extends ItemPermissions {
    /** Permission to upload an image */
    create: boolean;

    /** Permission to delete an image */
    delete: boolean;

    /** Permission to edit an image */
    edit: boolean;

    /** Permission to localize an inherited image */
    localize: boolean;

    /** Permission to import images */
    import: boolean;

    /** Permission to delete the local version of an inherited image */
    unlocalize: boolean;

    /** Permission to upload an image */
    upload: boolean;

    /** Permission to view an image */
    view: boolean;
}

/**
 * Permissions for form items, abstracted from the server logic
 */
export interface FormPermissions extends ItemPermissions {
    /** Permission to import forms */
    publish: boolean;
}

/**
 * Permissions for templates, abstracted from the server logic
 */
export interface TemplatePermissions extends ItemPermissions {
    /** Permission to create a template */
    create: boolean;

    /** Permission to delete a template */
    delete: boolean;

    /** Permission to edit a template */
    edit: boolean;

    /** Permission to link a template to a page */
    link: boolean;

    /** Permission to localize an inherited template */
    localize: boolean;

    /** Permission to delete the local version of an inherited template */
    unlocalize: boolean;

    /** Permission to view a template */
    view: boolean;
}

/**
 * Permissions for tag types, abstracted from the server logic
 */
export interface TagTypePermissions {
    /** Permission to create tag types */
    create: boolean;

    /** Permission to edit tag types */
    edit: boolean;

    /** Permission to delete tag types */
    delete: boolean;

    /** Permission to view tag types */
    view: boolean;
}
