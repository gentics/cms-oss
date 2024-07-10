import { TagPartProperty, TagPropertyType } from '@gentics/cms-models';

type PossibleNodeIdValue<T extends string> = {
    nodeId?: number;
} & { [K in T]?: number; };

type NodeIdValue<T extends string> = {
    nodeId: number;
} & { [K in T]: number; };

function withNodeIdIfPresent<T extends string & keyof V, V extends PossibleNodeIdValue<T>>(value: V, prop: T): NodeIdValue<T> | number {
    if (value.nodeId != null) {
        return {
            nodeId: value.nodeId,
            [prop]: value[prop],
        } as any;
    }

    return value[prop];
}

export function getTagPartPropertyValue(property: TagPartProperty): any {
    switch (property.type) {
        case TagPropertyType.BOOLEAN:
            return property.booleanValue;

        case TagPropertyType.STRING:
        case TagPropertyType.RICHTEXT:
            return property.stringValue;

        case TagPropertyType.IMAGE:
            return withNodeIdIfPresent(property, 'imageId');

        case TagPropertyType.FILE:
            return withNodeIdIfPresent(property, 'fileId');

        case TagPropertyType.FOLDER:
            return withNodeIdIfPresent(property, 'folderId');

        case TagPropertyType.NODE:
            return property.nodeId;

        case TagPropertyType.OVERVIEW:
            return property.overview;

        case TagPropertyType.PAGE:
            if (property.pageId) {
                return withNodeIdIfPresent(property, 'pageId');
            }
            return property.stringValue;

        case TagPropertyType.SELECT:
        case TagPropertyType.MULTISELECT:
            return {
                datasourceId: property.datasourceId,
                options: property.options,
                selectedOptions: property.selectedOptions,
            };

        case TagPropertyType.TEMPLATETAG:
            return {
                templateId: property.templateId,
                templateTagId: property.templateTagId,
            };

        case TagPropertyType.PAGETAG: {
            const res: any = { pageId: property.pageId };
            if (property.contentTagId != null) {
                res.contentTagId = property.contentTagId;
            }

            return res;
        }

        case TagPropertyType.LIST:
            return {
                booleanValue: property.booleanValue,
                stringValues: property.stringValues,
            };

        case TagPropertyType.ORDEREDLIST:
        case TagPropertyType.UNORDEREDLIST:
            return {
                stringValues: property.stringValues,
            };

        case TagPropertyType.DATASOURCE:
            return {
                options: property.options,
            };

        case TagPropertyType.FORM:
        case TagPropertyType.CMSFORM:
            return withNodeIdIfPresent(property, 'formId');
    }
}
