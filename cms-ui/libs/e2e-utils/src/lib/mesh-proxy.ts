import { GCMSRestClient } from '@gentics/cms-rest-client';
import { MeshFetchDriver, MeshRestClient } from '@gentics/mesh-rest-client';

// TODO: Move this to `cms-components` or another lib, as we can re-use this.
// If possible, move it to the CMS client directly, but types/dependency is going
// to be an issue. Which is why it's here for now.
export function createMeshProxy(client: GCMSRestClient, crId: string | number): MeshRestClient {
    const mesh = new MeshRestClient(new MeshFetchDriver(), {
        connection: {
            ...client.config.connection,
            basePath: `${client.config.connection.basePath}/contentrepositories/${crId}/proxy/api/v2`,
        },
        interceptors: [(data) => {
            if (!data.params) {
                data.params = {};
            }
            data.params['sid'] = `${client.sid}`;
            return data;
        }],
    });

    return mesh;
}
