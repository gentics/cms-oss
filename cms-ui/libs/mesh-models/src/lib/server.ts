export interface MeshServerInfoModel {
    /** Database structure revision hash. */
    databaseRevision?: string;
    /** Used database implementation vendor name. */
    databaseVendor?: string;
    /** Used database implementation version. */
    databaseVersion?: string;
    /** Node name of the Gentics Mesh instance. */
    meshNodeName?: string;
    /** Gentics Mesh Version string. */
    meshVersion?: string;
    /** Used search implementation vendor name. */
    searchVendor?: string;
    /** Used search implementation version. */
    searchVersion?: string;
    /** Used Vert.x version. */
    vertxVersion?: string;
}

export interface MeshStatusResponse {
    /** The current Gentics Mesh server status. */
    status: string;
}

export interface MeshClusterInstanceInfo {
    address?: string;
    name?: string;
    role?: string;
    startDate?: string;
    status?: string;
}

export interface MeshClusterStatusResponse {
    instances?: MeshClusterInstanceInfo[];
}

export interface MeshLocalConfigModel {
    /** If true, mutating requests to this instance are not allowed. */
    readOnly?: boolean;
}
