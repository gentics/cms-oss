import { ListResponse } from './response';


export interface BasePackageDependency{
    name: string;
    globalId: number;
    keyword?: string;
}

export interface PackageDependency extends BasePackageDependency {
    referenceDependencies: PackageDependency[]
}

export interface ReferenceDependency extends BasePackageDependency {
    isInPackage: boolean;
    isInOtherPackage: boolean;
}

export interface PackageCheckResult extends ListResponse<PackageDependency> {
    items: PackageDependency[];
    isComplete: boolean;
}
