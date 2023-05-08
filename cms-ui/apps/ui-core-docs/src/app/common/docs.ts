export type PropertyGroup = 'property' | 'input' | 'output' | 'method';
export type AccessModifer = 'public' | 'protected' | 'private';

export enum DocumentationType {
    COMPONENT = 'component',
    SERVICE = 'service',
}

export interface DocBlock {
    identifier: string;
    body: string;
    type: string;
    defaultValue?: string;
    decorator?: string;
    accessModifier?: AccessModifer;
    methodArgs?: string[];
}

export interface IDocumentation {
    type: 'component' | 'service';
    main: string;
    inputs: DocBlock[];
    outputs: DocBlock[];
    properties: DocBlock[];
    methods: DocBlock[];
}

export interface ISource {
    default: string;
}
