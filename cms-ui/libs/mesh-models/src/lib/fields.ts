import { ElasticSearchSettings } from './common';

export enum FieldType {
    BINARY = 'binary',
    BOOLEAN = 'boolean',
    DATE = 'date',
    /**
     * @deprecated Use the `STRING` type instead
     */
    HTML = 'html',
    LIST = 'list',
    MICRONODE = 'micronode',
    NODE = 'node',
    NUMBER = 'number',
    STRING = 'string',
}

export interface FieldSchema {
    /**
     * Additional search index configuration. This can be used to setup custom analyzers
     * and filters.
     */
    elasticsearch?: ElasticSearchSettings;
    /** Label of the field. */
    label?: string;
    /** Name of the field. */
    name: string;
    required?: boolean;
    /** Type of the field. */
    type: FieldType;
}

export interface FieldMap {
    [key: string]: any;
}
