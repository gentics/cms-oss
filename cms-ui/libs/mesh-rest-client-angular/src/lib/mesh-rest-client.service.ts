import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import {
    MeshAuthAPI,
    MeshBranchAPI,
    MeshClientConnection,
    MeshClusterAPI,
    MeshCoordinatorAPI,
    MeshGroupAPI,
    MeshMicroschemaAPI,
    MeshNodeAPI,
    MeshPluginAPI,
    MeshProjectAPI,
    MeshProjectMicroschemaAPI,
    MeshProjectSchemaAPI,
    MeshRestClient,
    MeshRoleAPI,
    MeshSchemaAPI,
    MeshServerAPI,
    MeshUserAPI,
} from '@gentics/mesh-rest-client';
import { AngularMeshClientDriver } from './angular-mesh-client-driver';

@Injectable()
export class MeshRestClientService {

    private driver: AngularMeshClientDriver;
    private config: MeshClientConnection;
    private client: MeshRestClient;

    constructor(
        http: HttpClient,
    ) {
        this.driver = new AngularMeshClientDriver(http);
    }

    init(config: MeshClientConnection, apiKey?: string): void {
        this.client = new MeshRestClient(this.driver, config, apiKey);
        this.config = config;
    }

    isInitialized(): boolean {
        return this.client != null;
    }

    getConfig(): MeshClientConnection {
        return this.config;
    }

    get auth(): MeshAuthAPI {
        return this.client.auth;
    }

    get users(): MeshUserAPI {
        return this.client.users;
    }

    get roles(): MeshRoleAPI {
        return this.client.roles;
    }

    get groups(): MeshGroupAPI {
        return this.client.groups;
    }

    get projects(): MeshProjectAPI {
        return this.client.projects;
    }

    get schemas(): MeshSchemaAPI {
        return this.client.schemas;
    }

    get microschemas(): MeshMicroschemaAPI {
        return this.client.microschemas;
    }

    get branches(): MeshBranchAPI {
        return this.client.branches;
    }

    get nodes(): MeshNodeAPI {
        return this.client.nodes;
    }

    get projectSchemas(): MeshProjectSchemaAPI {
        return this.client.projectSchemas;
    }

    get projectMicroschemas(): MeshProjectMicroschemaAPI {
        return this.client.projectMicroschemas;
    }

    get server(): MeshServerAPI {
        return this.client.server;
    }

    get coordinator(): MeshCoordinatorAPI {
        return this.client.coordinator;
    }

    get cluster(): MeshClusterAPI {
        return this.client.cluster;
    }

    get plugins(): MeshPluginAPI {
        return this.client.plugins;
    }
}
