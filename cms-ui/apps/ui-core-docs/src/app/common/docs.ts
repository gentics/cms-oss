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
    inheritance?: InheritanceInfo;
}

export interface InheritanceInfo {
    type: DocumentationType;
    name: string;
    file: string;
}

export interface SourceFile {
    type: 'component' | 'service';
    sourceFile: string;
    extends?: string;
}

export interface IDocumentation extends SourceFile {
    name: string;
    main: string;
    inheritance?: InheritanceInfo[];
    inputs: DocBlock[];
    outputs: DocBlock[];
    properties: DocBlock[];
    methods: DocBlock[];
}

export interface ISource {
    default: string;
}
