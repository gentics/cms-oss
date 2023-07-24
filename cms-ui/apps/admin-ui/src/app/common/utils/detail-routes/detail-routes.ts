import { NormalizableEntityType } from '@gentics/cms-models';
import { AdminUIEntityDetailRoutes, AdminUIModuleRoutes } from '../../routing/gcms-admin-ui-route';

const createDetailLink = (root: AdminUIModuleRoutes, segments: any[]): any[] => ['/' + root, { outlets: { detail: segments.filter(s => s != null) } } ];

export function buildEntityDetailPath(
    typeOrItem: NormalizableEntityType | any,
    id?: string | number,
    nodeIdOrTab?: number | string,
    tab?: string,
): any[] {
    let type: string;
    let nodeId: number;

    if (typeOrItem == null) {
        return [];
    }

    if (typeof typeOrItem === 'object') {
        if (typeof typeOrItem.type === 'object') {
            // Action-Log entry
            if (id == null) {
                id = typeOrItem.objId;
            }
            type = typeOrItem.type.name;
        } else {
            // Regular entities
            if (id == null) {
                id = typeOrItem.id;
            }
            type = typeOrItem.type;
        }
    } else {
        type = typeOrItem;
    }

    if (type == null || id == null) {
        return [];
    }

    if (nodeIdOrTab) {
        if (Number.isInteger(nodeIdOrTab)) {
            nodeId = nodeIdOrTab as number;
        } else if (tab) {
            nodeId = parseInt(nodeIdOrTab as string, 10);
        } else {
            tab = nodeIdOrTab as string;
        }
    }

    switch (type) {
        case 'user':
        case AdminUIEntityDetailRoutes.USER:
            return createDetailLink(AdminUIModuleRoutes.USERS, [AdminUIEntityDetailRoutes.USER, id, tab]);

        case 'construct':
        case AdminUIEntityDetailRoutes.CONSTRUCT:
            return createDetailLink(AdminUIModuleRoutes.CONSTRUCTS, [AdminUIEntityDetailRoutes.CONSTRUCT, id, tab]);

        case 'template':
        case AdminUIEntityDetailRoutes.TEMPLATE:
            return createDetailLink(AdminUIModuleRoutes.TEMPLATES, [AdminUIEntityDetailRoutes.TEMPLATE, nodeId, id, tab]);

        case 'folder':
        case AdminUIEntityDetailRoutes.FOLDER:
            return createDetailLink(AdminUIModuleRoutes.FOLDERS, [AdminUIEntityDetailRoutes.FOLDER, id, tab]);

        case 'node':
        case AdminUIEntityDetailRoutes.NODE:
            return createDetailLink(AdminUIModuleRoutes.NODES, [AdminUIEntityDetailRoutes.NODE, id, tab]);

        case 'group':
        case AdminUIEntityDetailRoutes.GROUP:
            return createDetailLink(AdminUIModuleRoutes.GROUPS, [AdminUIEntityDetailRoutes.GROUP, id, tab]);

        case 'datasource':
        case AdminUIEntityDetailRoutes.DATA_SOURCE:
            return createDetailLink(AdminUIModuleRoutes.DATA_SOURCES, [AdminUIEntityDetailRoutes.DATA_SOURCE, id, tab]);

        case 'constructcategory':
        case AdminUIEntityDetailRoutes.CONSTRUCT_CATEGORY:
            return createDetailLink(AdminUIModuleRoutes.CONSTRUCTS, [AdminUIEntityDetailRoutes.CONSTRUCT_CATEGORY, id, tab]);

        case 'objectprop':
        case AdminUIEntityDetailRoutes.OBJECT_PROPERTY:
            return createDetailLink(AdminUIModuleRoutes.OBJECT_PROPERTIES, [AdminUIEntityDetailRoutes.OBJECT_PROPERTY, id, tab]);

        case 'objectpropcategory':
        case AdminUIEntityDetailRoutes.OBJECT_PROPERTY_CATEGORY:
            return createDetailLink(AdminUIModuleRoutes.OBJECT_PROPERTIES, [AdminUIEntityDetailRoutes.OBJECT_PROPERTY_CATEGORY, id, tab]);

        case 'schedule':
        case 'schedulerschedule':
        case AdminUIEntityDetailRoutes.SCHEDULE:
            return createDetailLink(AdminUIModuleRoutes.SCHEDULER, [AdminUIEntityDetailRoutes.SCHEDULE, id, tab]);

        case 'scheduleTask':
        case 'schedulertask':
        case AdminUIEntityDetailRoutes.SCHEDULE_TASK:
            return createDetailLink(AdminUIModuleRoutes.SCHEDULER, [AdminUIEntityDetailRoutes.SCHEDULE_TASK, id, tab]);

        case 'contentrepository':
        case 'contentRepository':
        case 'contentRepositories':
        case AdminUIEntityDetailRoutes.CONTENT_REPOSITORY:
            return createDetailLink(AdminUIModuleRoutes.CONTENT_REPOSITORIES, [AdminUIEntityDetailRoutes.CONTENT_REPOSITORY, id, tab]);

        default:
            return [];
    }
}
