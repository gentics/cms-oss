import { NormalizableEntityType } from '@gentics/cms-models';
import { AdminUIModuleRoutes } from '../../routing/gcms-admin-ui-route';

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
            return createDetailLink(AdminUIModuleRoutes.USERS, ['user', id, tab]);
        case 'construct':
            return createDetailLink(AdminUIModuleRoutes.CONSTRUCTS, ['construct', id, tab]);
        case 'template':
            return createDetailLink(AdminUIModuleRoutes.TEMPLATES, ['template', nodeId, id, tab]);
        case 'folder':
            return createDetailLink(AdminUIModuleRoutes.FOLDERS, ['folder', id, tab]);
        case 'node':
            return createDetailLink(AdminUIModuleRoutes.NODES, ['node', id, tab]);
        case 'group':
            return createDetailLink(AdminUIModuleRoutes.GROUPS, ['group', id, tab]);
        case 'datasource':
            return createDetailLink(AdminUIModuleRoutes.DATA_SOURCES, ['data-source', id, tab]);
        case 'constructcategory':
            return createDetailLink(AdminUIModuleRoutes.CONSTRUCTS, ['construct-category', id, tab]);
        case 'objectprop':
            return createDetailLink(AdminUIModuleRoutes.OBJECT_PROPERTIES, ['object-propperty', id, tab]);
        case 'objectpropcategory':
            return createDetailLink(AdminUIModuleRoutes.OBJECT_PROPERTIES, ['object-property-category', id, tab]);
        case 'schedule':
        case 'schedulerschedule':
            return createDetailLink(AdminUIModuleRoutes.SCHEDULER, ['schedule', id, tab]);
        case 'scheduleTask':
        case 'schedulertask':
            return createDetailLink(AdminUIModuleRoutes.SCHEDULER, ['task', id, tab]);
        case 'contentrepository':
        case 'contentRepository':
        case 'contentRepositories':
            return createDetailLink(AdminUIModuleRoutes.CONTENT_REPOSITORIES, ['content-repository', id, tab]);
        default:
            return [];
    }
}
