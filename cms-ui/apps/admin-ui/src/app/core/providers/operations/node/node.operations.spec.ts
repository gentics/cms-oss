import { InterfaceOf, ObservableStopper } from '@admin-ui/common';
import { AppStateService } from '@admin-ui/state';
import { TestAppState, assembleTestAppStateImports } from '@admin-ui/state/utils/test-app-state';
import { createDelayedError, createDelayedObservable, subscribeSafely } from '@admin-ui/testing';
import { TestBed, fakeAsync, flush, tick } from '@angular/core/testing';
import {
    GcmsTestData,
    Index,
    Language,
    Node,
    NodeCopyRequest,
    NodeCreateRequest,
    NodeFeature,
    NodeFeatureListRequestOptions,
    NodeFeatureModel,
    NodeSaveRequest,
    Raw,
    RecursivePartial,
} from '@gentics/cms-models';
import { GcmsApi } from '@gentics/cms-rest-clients-angular';
import { of } from 'rxjs';
import { first, takeUntil } from 'rxjs/operators';
import { ActivityManagerService, GtxActivityManagerActivity } from '../../activity-manager';
import { EntityManagerService } from '../../entity-manager';
import { MockEntityManagerService } from '../../entity-manager/entity-manager.service.mock';
import { ErrorHandler } from '../../error-handler';
import { MockErrorHandler } from '../../error-handler/error-handler.mock';
import { I18nService } from '../../i18n';
import { I18nNotificationService } from '../../i18n-notification';
import { MockI18nNotificationService } from '../../i18n-notification/i18n-notification.service.mock';
import { MockI18nServiceWithSpies } from '../../i18n/i18n.service.mock';
import { PermissionsService } from '../../permissions';
import { NodeOperations } from './node.operations';

class MockApi implements RecursivePartial<InterfaceOf<GcmsApi>> {
    node = {
        getNodes: jasmine.createSpy('getNodes').and.stub(),
        getNode: jasmine.createSpy('getNode').and.stub(),
        addNode: jasmine.createSpy('addNode').and.stub(),
        removeNode: jasmine.createSpy('removeNode').and.stub(),
        copyNode: jasmine.createSpy('copyNode').and.stub(),
        updateNode: jasmine.createSpy('updateNode').and.stub(),
        activateNodeFeature: jasmine.createSpy('activateNodeFeature').and.stub(),
        deactivateNodeFeature: jasmine.createSpy('deactivateNodeFeature').and.stub(),
        getNodeFeatureList: jasmine.createSpy('getNodeFeatureList').and.stub(),
        getNodeFeatures: jasmine.createSpy('getNodeFeatures').and.stub(),
        getNodeLanguageList: jasmine.createSpy('getNodeLanguageList').and.stub(),
        updateNodeLanguages: jasmine.createSpy('updateNodeLanguages').and.stub(),
    };
}

class MockPermissionsService {}

const NODE_ID = 2;

describe('NodeOperations', () => {

    let api: MockApi;
    let entityManager: MockEntityManagerService;
    let errorHandler: MockErrorHandler;
    let nodeOps: NodeOperations;
    let notification: MockI18nNotificationService;
    let stopper: ObservableStopper;
    let activityManager: ActivityManagerService;
    let appState: TestAppState;

    beforeEach(() => {
        TestBed.configureTestingModule({
            imports: [
                assembleTestAppStateImports(),
            ],
            providers: [
                { provide: I18nService, useClass: MockI18nServiceWithSpies },
                ActivityManagerService,
                NodeOperations,
                { provide: AppStateService, useClass: TestAppState },
                { provide: EntityManagerService, useClass: MockEntityManagerService },
                { provide: ErrorHandler, useClass: MockErrorHandler },
                { provide: GcmsApi, useClass: MockApi },
                { provide: I18nNotificationService, useClass: MockI18nNotificationService },
                { provide: PermissionsService, useClass: MockPermissionsService },
            ],
        });

        api = TestBed.get(GcmsApi);
        entityManager = TestBed.get(EntityManagerService);
        errorHandler = TestBed.get(ErrorHandler);
        nodeOps = TestBed.get(NodeOperations);
        notification = TestBed.get(I18nNotificationService);
        activityManager = TestBed.get(ActivityManagerService);
        appState = TestBed.get(AppStateService);
        stopper = new ObservableStopper();
    });

    afterEach(() => {
        stopper.stop();
    });

    describe('getAll()', () => {

        it('fetches nodes and adds them to the EntityState', fakeAsync(() => {
            const mockNodes = [
                GcmsTestData.getExampleNodeData({ id: 1 }),
                GcmsTestData.getExampleNodeData({ id: 2 }),
            ];
            api.node.getNodes.and.returnValue(
                createDelayedObservable({ items: mockNodes }),
            );

            let result: Node<Raw>[];
            nodeOps.getAll().pipe(
                takeUntil(stopper.stopper$),
            ).subscribe(nodes => result = nodes);

            tick();
            expect(api.node.getNodes).toHaveBeenCalledTimes(1);
            expect(result).toBe(mockNodes);

            expect(entityManager.addEntities).toHaveBeenCalledTimes(1);
            expect(entityManager.addEntities).toHaveBeenCalledWith('node', mockNodes);
        }));

        it('notifies the user about errors and rethrows them', fakeAsync(() => {
            const error = new Error('Test Error');
            api.node.getNodes.and.returnValue(createDelayedError(error));
            errorHandler.assertNotifyAndRethrowIsCalled(nodeOps.getAll(), error);
        }));

    });

    describe('get()', () => {

        it('fetches a node and adds it to the EntityState', fakeAsync(() => {
            const mockNode = GcmsTestData.getExampleNodeData({ id: NODE_ID });
            api.node.getNode.and.returnValue(
                createDelayedObservable({ node: mockNode }),
            );

            let result: Node<Raw>;
            nodeOps.get(NODE_ID).pipe(
                takeUntil(stopper.stopper$),
            ).subscribe(node => result = node);

            tick();
            expect(api.node.getNode).toHaveBeenCalledTimes(1);
            expect(api.node.getNode).toHaveBeenCalledWith(NODE_ID);
            expect(result).toBe(mockNode);

            expect(entityManager.addEntity).toHaveBeenCalledTimes(1);
            expect(entityManager.addEntity).toHaveBeenCalledWith('node', mockNode);
        }));

        it('notifies the user about errors and rethrows them', fakeAsync(() => {
            const error = new Error('Test Error');
            api.node.getNode.and.returnValue(createDelayedError(error));
            errorHandler.assertNotifyAndRethrowIsCalled(nodeOps.get(1), error);
        }));

    });

    describe('addNode()', () => {

        it('creates a node and adds it to the entity state', fakeAsync(() => {
            const mockNode = GcmsTestData.getExampleNodeData({ id: NODE_ID });
            api.node.addNode.and.returnValue(
                createDelayedObservable({ node: mockNode }),
            );

            const nodeCreateReq: NodeCreateRequest = {
                node: {
                    name: mockNode.name,
                    host: mockNode.host,
                },
            };

            let result: Node<Raw> | string;
            subscribeSafely(
                nodeOps.addNode(nodeCreateReq),
                stopper,
                newNode => result = newNode,
            );

            tick();
            expect(api.node.addNode).toHaveBeenCalledTimes(1);
            expect(api.node.addNode).toHaveBeenCalledWith(nodeCreateReq);
            expect(result).toBe(mockNode);

            expect(entityManager.addEntity).toHaveBeenCalledTimes(1);
            expect(entityManager.addEntity).toHaveBeenCalledWith('node', mockNode);
            expect(notification.show).toHaveBeenCalledWith({
                type: 'success',
                message: 'shared.item_created',
                translationParams: { name: mockNode.name },
            });
        }));

        it('notifies the user about errors and rethrows them', fakeAsync(() => {
            const nodeCreateReq: NodeCreateRequest = {
                node: {
                    name: 'test',
                    host: 'test.com',
                },
            };

            const error = new Error('Test Error');
            api.node.addNode.and.returnValue(createDelayedError(error));
            errorHandler.assertNotifyAndRethrowIsCalled(nodeOps.addNode(nodeCreateReq), error);
        }));

    });

    describe('removeNode()', () => {

        it('deletes an existing node and removes it from the entity state', fakeAsync(() => {
            const mockNode = GcmsTestData.getExampleNodeData({ id: NODE_ID });
            appState.mockState({
                entity: {
                    node: {
                        [NODE_ID]: mockNode,
                    } as any,
                },
            });
            api.node.removeNode.and.returnValue(of('test').pipe(first()));
            let activitiesQueue: GtxActivityManagerActivity[];
            nodeOps.removeNode(mockNode.id);

            tick();
            expect(api.node.removeNode).toHaveBeenCalledTimes(1);
            expect(api.node.removeNode).toHaveBeenCalledWith(mockNode.id, undefined);

            tick();
            expect(entityManager.deleteEntities).toHaveBeenCalledTimes(1);
            expect(entityManager.deleteEntities).toHaveBeenCalledWith('node', [mockNode.id]);
            expect(notification.show).toHaveBeenCalledWith({
                type: 'success',
                message: 'shared.item_singular_deleted',
                translationParams: { name: mockNode.name },
            });

            subscribeSafely(
                activityManager.activities$,
                stopper,
                activities => activitiesQueue = activities,
            );

            tick();
            expect(activitiesQueue.length).toBe(1);

            flush();
        }));

    });

    describe('copyNode()', () => {

        it('copies an existing node', fakeAsync(() => {
            const mockNode = GcmsTestData.getExampleNodeData({ id: NODE_ID });
            api.node.copyNode.and.returnValue(of());

            let activitiesQueue: GtxActivityManagerActivity[];
            const request: NodeCopyRequest = {
                pages: false,
                templates: false,
                files: false,
                workflows: false,
                copies: 2,
            };
            nodeOps.copyNode(mockNode.id, request);

            tick();
            expect(api.node.copyNode).toHaveBeenCalledTimes(1);
            expect(api.node.copyNode).toHaveBeenCalledWith(mockNode.id, request, undefined);

            subscribeSafely(
                activityManager.activities$,
                stopper,
                activities => activitiesQueue = activities,
            );

            tick();
            expect(activitiesQueue.length).toBe(1);

            flush();
        }));

    });

    describe('update()', () => {

        it('updates a node and refetches it using get()', fakeAsync(() => {
            const updatedNode = GcmsTestData.getExampleNodeData({ id: NODE_ID });
            api.node.updateNode.and.returnValue(
                createDelayedObservable(null),
            );
            const getSpy = spyOn(nodeOps, 'get').and.returnValue(
                createDelayedObservable(updatedNode),
            );

            const saveReq: NodeSaveRequest = {
                node: {
                    name: updatedNode.name,
                    host: updatedNode.host,
                },
            };

            let result: Node<Raw>;
            subscribeSafely(
                nodeOps.update(NODE_ID, saveReq),
                stopper,
                node => result = node,
            );

            tick();
            expect(api.node.updateNode).toHaveBeenCalledTimes(1);
            expect(api.node.updateNode).toHaveBeenCalledWith(NODE_ID, saveReq);
            expect(result).toBe(updatedNode);

            expect(getSpy).toHaveBeenCalledTimes(1);
            expect(getSpy).toHaveBeenCalledWith(NODE_ID);
            expect(notification.show).toHaveBeenCalledWith({
                type: 'success',
                message: 'shared.item_updated',
                translationParams: { name: updatedNode.name },
            });
        }));

        it('notifies the user about errors and rethrows them', fakeAsync(() => {
            const saveReq: NodeSaveRequest = {
                node: {
                    name: 'test',
                    host: 'test',
                },
            };

            const error = new Error('Test Error');
            api.node.updateNode.and.returnValue(createDelayedError(error));
            errorHandler.assertNotifyAndRethrowIsCalled(nodeOps.update(NODE_ID, saveReq), error);
        }));

    });

    describe('updateNodeFeatures()', () => {

        let featuresUpdate: Partial<Index<NodeFeature, boolean>>;

        beforeEach(() => {
            featuresUpdate = {
                [NodeFeature.newTagEditor]: true,
                [NodeFeature.contentAutoOffline]: false,
                [NodeFeature.alwaysLocalize]: true,
            };
        });

        it('enables and disables node features and refetches the node\'s complete feature list afterwards', fakeAsync(() => {
            const activatedFeatures: NodeFeature[] = [
                NodeFeature.newTagEditor,
                NodeFeature.alwaysLocalize,
            ];
            const getSpy = spyOn(nodeOps, 'getNodeFeatures').and.returnValue(
                createDelayedObservable(activatedFeatures),
            );

            api.node.activateNodeFeature.and.callFake(
                () => createDelayedObservable(null),
            );
            api.node.deactivateNodeFeature.and.callFake(
                () => createDelayedObservable(null),
            );

            let result: NodeFeature[];
            subscribeSafely(
                nodeOps.updateNodeFeatures(NODE_ID, featuresUpdate),
                stopper,
                node => result = node,
            );

            tick();

            expect(api.node.activateNodeFeature).toHaveBeenCalledTimes(2);
            expect(api.node.activateNodeFeature.calls.argsFor(0)).toEqual([NODE_ID, NodeFeature.newTagEditor]);
            expect(api.node.activateNodeFeature.calls.argsFor(1)).toEqual([NODE_ID, NodeFeature.alwaysLocalize]);

            expect(api.node.deactivateNodeFeature).toHaveBeenCalledTimes(1);
            expect(api.node.deactivateNodeFeature.calls.argsFor(0)).toEqual([NODE_ID, NodeFeature.contentAutoOffline]);

            expect(getSpy).toHaveBeenCalledTimes(1);
            expect(getSpy).toHaveBeenCalledWith(NODE_ID);
            expect(result).toBe(activatedFeatures);

            expect(notification.show).toHaveBeenCalledWith({
                type: 'success',
                message: 'node.features_updated',
            });
        }));

        it('notifies the user about errors and rethrows them', fakeAsync(() => {
            const error = new Error('Test Error');
            api.node.activateNodeFeature.and.returnValue(createDelayedError(error));
            api.node.deactivateNodeFeature.and.returnValue(createDelayedError(error));
            errorHandler.assertNotifyAndRethrowIsCalled(nodeOps.updateNodeFeatures(NODE_ID, featuresUpdate), error);
        }));

    });

    describe('getAvailableFeatures()', () => {

        it('fetches the available node features', fakeAsync(() => {
            const mockFeatures: NodeFeatureModel[] = [
                { id: NodeFeature.newTagEditor, name: 'New Tag Editor', description: '' },
                { id: NodeFeature.alwaysLocalize, name: 'Always Localize', description: '' },
            ];
            api.node.getNodeFeatureList.and.returnValue(
                createDelayedObservable({ items: mockFeatures }),
            );

            const req: NodeFeatureListRequestOptions = {
                page: 2,
                pageSize: 10,
            };
            let result: NodeFeatureModel[];
            subscribeSafely(
                nodeOps.getAvailableFeatures(req),
                stopper,
                features => result = features,
            );

            tick();
            expect(api.node.getNodeFeatureList).toHaveBeenCalledTimes(1);
            expect(api.node.getNodeFeatureList).toHaveBeenCalledWith(req);
            expect(result).toBe(mockFeatures);
        }));

        it('notifies the user about errors and rethrows them', fakeAsync(() => {
            const error = new Error('Test Error');
            api.node.getNodeFeatureList.and.returnValue(createDelayedError(error));
            errorHandler.assertNotifyAndRethrowIsCalled(nodeOps.getAvailableFeatures(), error);
        }));

    });

    describe('getNodeFeatures()', () => {

        it('fetches the node\'s activated features', fakeAsync(() => {
            const activatedFeatures: NodeFeature[] = [
                NodeFeature.newTagEditor,
                NodeFeature.alwaysLocalize,
            ];
            api.node.getNodeFeatures.and.returnValue(
                createDelayedObservable({ items: activatedFeatures }),
            );

            let result: NodeFeature[];
            subscribeSafely(
                nodeOps.getNodeFeatures(NODE_ID),
                stopper,
                features => result = features,
            );

            tick();
            expect(api.node.getNodeFeatures).toHaveBeenCalledTimes(1);
            expect(api.node.getNodeFeatures).toHaveBeenCalledWith(NODE_ID);
            expect(result).toBe(activatedFeatures);
        }));

        it('notifies the user about errors and rethrows them', fakeAsync(() => {
            const error = new Error('Test Error');
            api.node.getNodeFeatures.and.returnValue(createDelayedError(error));
            errorHandler.assertNotifyAndRethrowIsCalled(nodeOps.getNodeFeatures(NODE_ID), error);
        }));

    });

    describe('getNodeLanguages()', () => {

        it('fetches the node\'s languages', fakeAsync(() => {
            const languages: Language[] = [
                { id: 1, code: 'en', name: 'English'},
                { id: 2, code: 'de', name: 'Deutsch'},
            ];
            api.node.getNodeLanguageList.and.returnValue(
                createDelayedObservable({ items: languages }),
            );

            let result: Language[];
            subscribeSafely(
                nodeOps.getNodeLanguages(NODE_ID),
                stopper,
                features => result = features,
            );

            tick();
            expect(api.node.getNodeLanguageList).toHaveBeenCalledTimes(1);
            expect(api.node.getNodeLanguageList).toHaveBeenCalledWith(NODE_ID);
            expect(result).toBe(languages);
        }));

        it('notifies the user about errors and rethrows them', fakeAsync(() => {
            const error = new Error('Test Error');
            api.node.getNodeLanguageList.and.returnValue(createDelayedError(error));
            errorHandler.assertNotifyAndRethrowIsCalled(nodeOps.getNodeLanguages(NODE_ID), error);
        }));

    });

    describe('updateNodeLanguages()', () => {

        it('updates the node\'s languages and returns them', fakeAsync(() => {
            const languages: Language[] = [
                { id: 1, code: 'en', name: 'English'},
                { id: 2, code: 'de', name: 'Deutsch'},
            ];
            api.node.updateNodeLanguages.and.returnValue(
                createDelayedObservable({ items: languages }),
            );
            const getSpy = spyOn(nodeOps, 'getNodeLanguages').and.returnValue(
                createDelayedObservable(languages),
            );

            let result: Language[];
            subscribeSafely(
                nodeOps.updateNodeLanguages(NODE_ID, languages),
                stopper,
                features => result = features,
            );

            tick();
            expect(api.node.updateNodeLanguages).toHaveBeenCalledTimes(1);
            expect(api.node.updateNodeLanguages).toHaveBeenCalledWith(NODE_ID, languages);

            expect(getSpy).toHaveBeenCalledTimes(1);
            expect(getSpy).toHaveBeenCalledWith(NODE_ID);
            expect(result).toBe(languages);

            expect(notification.show).toHaveBeenCalledWith({
                type: 'success',
                message: 'node.languages_updated',
            });
        }));

        it('notifies the user about errors and rethrows them', fakeAsync(() => {
            const languages: Language[] = [
                { id: 1, code: 'en', name: 'English'},
                { id: 2, code: 'de', name: 'Deutsch'},
            ];

            const error = new Error('Test Error');
            api.node.updateNodeLanguages.and.returnValue(createDelayedError(error));
            errorHandler.assertNotifyAndRethrowIsCalled(nodeOps.updateNodeLanguages(NODE_ID, languages), error);
        }));

    });

});
