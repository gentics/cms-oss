import { Form, InheritableItem, Item, Page } from '@gentics/cms-models';

/**
 * @description Utility class to get if a page has a certain state
 */
export class EntityStateUtil {

    /**
     * @param entity object to be checked
     * @returns TRUE if this entity is deleted because another user has removed it
     */
    static stateDeleted<T extends Item>(entity: T): boolean {
        if (!entity) {
            throw new Error('Entity object is invalid');
        }
        return entity.deleted && Number.isInteger(entity.deleted.at) && entity.deleted.at > 0;
    }

}

export class InheritableEntityStateUtil extends EntityStateUtil {

    /**
     * @param entity object to be checked
     * @returns TRUE if this entity is inherited from a master node entity language variant
     */
    static stateInherited<T extends InheritableItem>(entity: T): boolean {
        if (!entity) {
            throw new Error('Entity object is invalid');
        }
        return entity.inherited === true;
    }

    /**
     * @param entity object to be checked
     * @returns TRUE if this entity is inherited but has autonomous content
     */
    static stateLocalized<T extends InheritableItem>(entity: T): boolean {
        if (!entity) {
            throw new Error('Entity object is invalid');
        }
        if (entity.inheritedFromId) {
            return entity.inheritedFromId !== entity.masterNodeId;
        } else {
            return entity.inheritedFrom !== entity.masterNode;
        }
    }

    /**
     * @param entity object to be checked
     * @returns TRUE if this entity is deleted because another user has removed it
     */
    static stateDeleted<T extends Item>(entity: T): boolean {
        if (!entity) {
            throw new Error('Entity object is invalid');
        }
        return entity.deleted && entity.deleted.at && Number.isInteger(entity.deleted.at);
    }

}

/**
 * @description Utility class to get if an that can be published item has a certain state.
 */
export class PublishableStateUtil extends InheritableEntityStateUtil {

    /**
     * @param item item object to be checked
     * @returns TRUE if this item is locked because another user is currently editing it
     */
    static stateLocked(item: Page | Form): boolean {
        if (!item) {
            throw new Error('Page object is invalid');
        }
        return item.locked === true;
    }

    /**
     * @param item item object to be checked
     * @returns TRUE if this item has been edited by a user
     */
    static stateModified(item: Page | Form): boolean {
        if (!item) {
            throw new Error('Page object is invalid');
        }
        return item.modified === true;
    }

    /**
     * @param item item object to be checked
     * @returns TRUE if this item is online
     */
    static statePublished(item: Page | Form): boolean {
        if (!item) {
            throw new Error('Page object is invalid');
        }
        return item.online === true;
    }

    /**
     * @param item item object to be checked
     * @returns TRUE if this item is offline
     */
    static stateOffline(item: Page | Form): boolean {
        if (!item) {
            throw new Error('Page object is invalid');
        }
        return item.online === false;
    }

    /**
     * @param item item object to be checked
     * @returns TRUE if this item has been requested for release
     */
    static stateInQueue(item: Page | Form): boolean {
        if (!item) {
            throw new Error('Page object is invalid');
        }
        return item.queued === true;
    }

    /**
     * @param item item object to be checked
     * @returns TRUE if this item is scheduled for an automated action
     */
    static statePlanned(item: Page | Form): boolean {
        if (!item) {
            throw new Error('Page object is invalid');
        }
        return item.planned === true;
    }

    /**
     * @param item item object to be checked
     * @returns TRUE if this item is scheduled for publishing
     */
    static statePlannedOnline(item: Page | Form): boolean {
        if (!item) {
            throw new Error('Page object is invalid');
        }
        return item.timeManagement.at > 0;
    }

    /**
     * @param item item object to be checked
     * @returns TRUE if this item is scheduled for taking offline
     */
    static statePlannedOffline(item: Page | Form): boolean {
        if (!item) {
            throw new Error('Page object is invalid');
        }
        return item.timeManagement.offlineAt > 0;
    }

}
