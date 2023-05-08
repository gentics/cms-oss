import { NormalizableEntityType } from '@gentics/cms-models';
import { AdminUIModuleRoutes } from '../../routing/gcms-admin-ui-route';

const createDetailLink = (root: AdminUIModuleRoutes, segments: any[]): any[] => ['/' + root, { outlets: { detail: segments } } ];

export function buildEntityDetailPath(typeOrItem: NormalizableEntityType | any, id?: string | number, nodeId?: number): any[] {
    let type: string;

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

    switch (type) {
        case 'user':
            return createDetailLink(AdminUIModuleRoutes.USERS, ['user', id]);
        case 'construct':
            return createDetailLink(AdminUIModuleRoutes.CONSTRUCTS, ['construct', id]);
        case 'template':
            return createDetailLink(AdminUIModuleRoutes.TEMPLATES, ['template', nodeId, id]);
        case 'folder':
            return createDetailLink(AdminUIModuleRoutes.FOLDERS, ['folder', id]);
        case 'node':
            return createDetailLink(AdminUIModuleRoutes.NODES, ['node', id]);
        case 'group':
            return createDetailLink(AdminUIModuleRoutes.GROUPS, ['group', id]);
        case 'datasource':
            return createDetailLink(AdminUIModuleRoutes.DATA_SOURCES, ['data-source', id]);
        case 'constructcategory':
            return createDetailLink(AdminUIModuleRoutes.CONSTRUCTS, ['construct-category', id]);
        case 'objectprop':
            return createDetailLink(AdminUIModuleRoutes.OBJECT_PROPERTIES, ['object-propperty', id]);
        case 'objectpropcategory':
            return createDetailLink(AdminUIModuleRoutes.OBJECT_PROPERTIES, ['object-property-category', id]);
        case 'schedule':
        case 'schedulerschedule':
            return createDetailLink(AdminUIModuleRoutes.SCHEDULER, ['schedule', id]);
        case 'scheduleTask':
        case 'schedulertask':
            return createDetailLink(AdminUIModuleRoutes.SCHEDULER, ['task', id]);
        case 'contentRepository':
        case 'contentRepositories':
            return createDetailLink(AdminUIModuleRoutes.CONTENT_REPOSITORIES, ['content-repository', id]);
        default:
            return [];
    }
}
