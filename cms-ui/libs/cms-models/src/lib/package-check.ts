import { ListResponse } from './response';


export interface BasePackageDependency{
    name: string;
    globalId: string;
    keyword?: string;
    dependencyType: DependencyType;
}

export interface PackageDependency extends BasePackageDependency {
    referenceDependencies: ReferenceDependency[]
}

export interface ReferenceDependency extends BasePackageDependency {
    isInPackage: boolean;
    isInOtherPackage: boolean;
    isContained: boolean;
    foundInPackage: string;
}

export type PackageDependencyEntity = PackageDependency | ReferenceDependency

export interface PackageCheckResult extends ListResponse<PackageDependency> {
    items: PackageDependency[];
    isComplete: boolean;
}

export enum DependencyType {
    CONSTRUCT='CONSTRUCT',
    DATASOURCE='DATASOURCE',
    OBJECT_TAG_DEFINITION='OBJECT_TAG_DEFINITION',
    TEMPLATE='TEMPLATE',
}
