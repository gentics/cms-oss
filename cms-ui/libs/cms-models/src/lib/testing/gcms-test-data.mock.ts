import {
    File,
    Folder,
    Image,
    Node,
    Normalized,
    NormalizedEntityStore,
    Page,
    Raw,
    User,
} from '../models';
import { getExampleEntityStore } from './entity-store-data.mock';
import {
    getExampleFolderData,
    getExampleFolderDataNormalized,
    getExampleImageData,
    getExampleImageDataNormalized,
    getExampleNewImageData,
    getExampleNodeData,
    getExampleNodeDataNormalized,
    getExamplePageData,
    getExamplePageDataNormalized,
    getExampleUserData,
    getExampleUserDataNormalized,
    MockNodeConfig,
} from './test-data.mock';

/**
 * Exposes various mocking methods for testing.
 *
 * DO NOT use this class in production code!
 */
export class GcmsTestData {

    /** Returns actual page data from the ContentNode API for "GCN5 Demo" */
    static getExamplePageData(
        { id, userId, idVariant1 }: { id: number, userId?: number, idVariant1?: number } = { id: 95, userId: 3, idVariant1: 48 },
    ): Page<Raw> {
        return getExamplePageData({ id, userId, idVariant1 });
    }

    static getExamplePageDataNormalized(
        { id, userId, idVariant1 }: { id: number, userId?: number, idVariant1?: number } = { id: 95, userId: 3, idVariant1: 48 },
    ): Page<Normalized> {
        return getExamplePageDataNormalized({ id, userId, idVariant1 });
    }

    /** Returns actual folder data from the ContentNode API for "GCN5 Demo" */
    static getExampleFolderData({ id, userId, publishDir }: { id: number, userId?: number, publishDir?: string }
            = { id: 115, userId: 3, publishDir: '/' }): Folder<Raw> {
        return getExampleFolderData({ id, userId, publishDir });
    }

    static getExampleFolderDataNormalized({ id, userId }: { id: number, userId?: number } = { id: 115, userId: 3 }): Folder<Normalized> {
        return getExampleFolderDataNormalized({ id, userId });
    }

    /** Returns actual user data from the ContentNode API for "GCN5 Demo" */
    static getExampleImageData({ id, userId }: { id: number, userId?: number } = { id: 1, userId: 3 }): Image<Raw> {
        return getExampleImageData({ id, userId });
    }

    static getExampleImageDataNormalized({ id, userId }: { id: number, userId?: number } = { id: 1, userId: 3 }): Image<Normalized> {
        return getExampleImageDataNormalized({ id, userId });
    }

    /** Returns actual user data from the ContentNode API when editing an image in "GCN5 Demo" */
    static getExampleNewImageData({ id, userId }: { id: number, userId?: number } = { id: 1, userId: 3 }): File<Raw> {
        return getExampleNewImageData({ id, userId });
    }

    /** Returns actual user data from the ContentNode API for "GCN5 Demo" */
    static getExampleUserData({ id }: { id: number } = { id: 3 }): User<Raw> {
        return getExampleUserData({ id });
    }

    static getExampleUserDataNormalized({ id }: { id: number } = { id: 3 }): User<Normalized> {
        return getExampleUserDataNormalized({ id });
    }

    /** Returns actual node data from the ContentNode API for "GCN5 Demo" */
    static getExampleNodeData(config: MockNodeConfig = { id: 1, userId: 3 }): Node<Raw> {
        return getExampleNodeData(config);
    }

    static getExampleNodeDataNormalized(config: MockNodeConfig = { id: 1, userId: 3 }): Node<Normalized> {
        return getExampleNodeDataNormalized(config);
    }

    static getExampleEntityStore(): NormalizedEntityStore {
        return getExampleEntityStore();
    }

}
