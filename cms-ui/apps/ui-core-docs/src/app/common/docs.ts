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
    /** File path to the source-file to load the contents from. */
    sourceFile: string;
    /** Which class it extends. Used for referencing and linking. */
    extends?: string;
    /** The path where this documentation page is available at. Used in base-classes for linking. */
    path?: string;
}

export interface IDocumentation extends SourceFile {
    name: string;
    main: string;
    generics?: string[];
    inheritance?: InheritanceInfo[];
    inputs: DocBlock[];
    outputs: DocBlock[];
    properties: DocBlock[];
    methods: DocBlock[];
}

export interface ISource {
    default: string;
}
