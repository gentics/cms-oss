import { ChangeDetectionStrategy, ChangeDetectorRef, Component, EventEmitter, HostListener, OnInit, Output } from '@angular/core';
import { Router } from '@angular/router';
import { areItemsLoading } from '@editor-ui/app/common/utils/are-items-loading';
import { EditMode } from '@gentics/cms-integration-api-models';
import { Folder, IndexById, Message, Node, Normalized, Page } from '@gentics/cms-models';
import { ModalService } from '@gentics/ui-core';
import { isEqual } from 'lodash-es';
import { BehaviorSubject, Observable, combineLatest } from 'rxjs';
import { distinctUntilChanged, filter, map, publishReplay, refCount, take, tap } from 'rxjs/operators';
import { SendMessageModal } from '../../../shared/components/send-message-modal/send-message-modal.component';
import { ApplicationStateService, FolderActionsService, MessageActionsService } from '../../../state';
import { EntityResolver } from '../../providers/entity-resolver/entity-resolver';
import { I18nService } from '../../providers/i18n/i18n.service';
import { NavigationService } from '../../providers/navigation/navigation.service';
import { MessageLink } from '../message-body/message-parsing';
import { MessageModal } from '../message-modal/message-modal.component';

@Component({
    selector: 'message-inbox',
    templateUrl: './message-inbox.component.html',
    styleUrls: ['./message-inbox.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false
})
export class MessageInboxComponent implements OnInit {

    /** Emits when a link in a message is clicked */
    @Output()
    navigate = new EventEmitter<MessageLink>();

    selectedRead: number[] = [];
    selectedUnread: number[] = [];

    allReadIds: number[] = [];
    allUnreadIds: number[] = [];

    waitsForDeleteConfirmation: Message = null;

    hasMessages$: Observable<boolean>;
    unreadMessagesCount$: Observable<number>;
    unreadMessages$: Observable<Message<Normalized>[]>;
    readMessagesCount$: Observable<number>;
    readMessages$: Observable<Message<Normalized>[]>;
    nodes$: Observable<Node[]>;

    showReadMessages = false;

    private messageToKeepInUnread$ = new BehaviorSubject<number | undefined>(undefined);

    constructor(
        private appState: ApplicationStateService,
        private navigationService: NavigationService,
        private entityResolver: EntityResolver,
        private folderActions: FolderActionsService,
        private messageActions: MessageActionsService,
        private changeDetector: ChangeDetectorRef,
        private router: Router,
        private modalService: ModalService,
        private i18n: I18nService,
    ) { }

    @HostListener('document:mouseup', ['$event'])
    onGlobalClick(event: Event): void {
        if (this.waitsForDeleteConfirmation && (event.target as HTMLElement).dataset['msgId'] !== String(this.waitsForDeleteConfirmation.id)) {
            this.waitsForDeleteConfirmation = null;
        }
    }

    ngOnInit(): void {
        // MessageState Observable that keeps the currently opened message in the "unread" messages.
        const messages$ = combineLatest([
            this.appState.select(state => state.messages).pipe(
                distinctUntilChanged((a, b) => a.read === b.read && a.unread === b.unread),
            ),
            this.messageToKeepInUnread$,
            this.appState.select(state => state.entities.message),
        ]).pipe(
            map(([state, keep, messages]) => {
                const read = (!keep || state.read.indexOf(keep) < 0)
                    ? state.read
                    : state.read.filter(id => id !== keep);
                const unread = (!keep || state.unread.indexOf(keep) >= 0)
                    ? state.unread
                    : state.unread.concat(keep);

                return {
                    all: this.sortMapMessages(state.all, keep, messages),
                    read: this.sortMapMessages(read, keep, messages),
                    unread: this.sortMapMessages(unread, keep, messages),
                };
            }),
            publishReplay(1),
            refCount(),
        );

        this.hasMessages$ = messages$.pipe(
            map(state => (state.all || []).length > 0),
        );

        this.readMessages$ = messages$.pipe(
            map(state => state.read),
            tap(messages => {
                this.allReadIds = messages.map(msg => msg.id);
                this.selectedRead = this.selectedRead.filter(id => {
                    return !!messages.find(msg => msg.id === id)
                });
            }),
        );

        this.readMessagesCount$ = this.readMessages$.pipe(
            map(messages => messages.length),
            distinctUntilChanged(isEqual),
        );

        this.unreadMessages$ = messages$.pipe(
            map(state => state.unread || []),
            tap(messages => {
                this.allUnreadIds = messages.map(msg => msg.id);
                this.selectedUnread = this.selectedUnread.filter(id =>
                    !!messages.find(msg => msg.id === id),
                ).slice();
            }),
        );

        this.unreadMessagesCount$ = this.unreadMessages$.pipe(
            map(messages => messages?.length),
            distinctUntilChanged(isEqual),
        );

        this.nodes$ = this.appState.select(state => state.folder.nodes.list).pipe(
            map(nodeIds => nodeIds
                .map(id => this.entityResolver.getNode(id))
                .filter(node => node != null),
            ),
        );
    }

    toggleReadMessages(): void {
        this.showReadMessages = !this.showReadMessages;
    }

    showMessage(message: Message<Normalized>, keepInUnread: boolean): void {
        if (keepInUnread) {
            this.messageToKeepInUnread$.next(message.id);
        }

        this.modalService.fromComponent(MessageModal, {}, { message })
            .then(modal => modal.open())
            .then((link: MessageLink) => {
                this.messageToKeepInUnread$.next(undefined);
                this.changeDetector.markForCheck();

                if (link) {
                    this.navigate.emit(link);
                    this.openPage(link);
                }
            });

        if (message.unread) {
            this.messageActions.markMessagesAsRead([message.id]);
        }
    }

    archiveMessagesById(messageIds: number[]): void {
        this.messageActions.markMessagesAsRead(messageIds);
    }

    archiveMessages(messages: Message<Normalized>[] | Message<Normalized>): void {
        if (!Array.isArray(messages)) {
            messages = [messages];
        }

        this.archiveMessagesById(messages.map(message => message.id));
    }

    deleteMessagesById(messageIds: number[]): void {
        this.messageActions.deleteMessages(messageIds);
    }

    deleteMessages(messages: Message<Normalized>[] | Message<Normalized>): void {
        if (!Array.isArray(messages)) {
            messages = [messages];
        }

        this.deleteMessagesById(messages.map(message => message.id));
    }

    deleteMessage(message: Message<Normalized>): void {
        if (this.waitsForDeleteConfirmation !== message) {
            this.waitsForDeleteConfirmation = message;
            return;
        }

        this.messageActions.deleteMessages([message.id]);
        this.waitsForDeleteConfirmation = null;
    }

    selectionUnreadChange(messageIds: number[]): void {
        this.selectedUnread = messageIds;
    }

    selectionReadChange(messageIds: number[]): void {
        this.selectedRead = messageIds;
    }

    isSelectedAllRead(): boolean {
        if (this.selectedRead.length === this.allReadIds.length) {
            return true;
        }
        return false;
    }

    isSelectedAllUnread(): boolean {
        if (this.selectedUnread.length === this.allUnreadIds.length) {
            return true;
        }
        return false;
    }

    toggleAllRead(): void {
        if (this.selectedRead.length === this.allReadIds.length) {
            this.selectedRead = [];
        } else {
            this.selectedRead = this.allReadIds.slice();
        }
    }

    toggleAllUnread(): void {
        if (this.selectedUnread.length === this.allUnreadIds.length) {
            this.selectedUnread = [];
        } else {
            this.selectedUnread = this.allUnreadIds.slice();
        }
    }

    archiveAllUnread(): void {
        this.archiveMessagesById(this.selectedUnread);
    }

    confirmBeforeDelete(callback: Function, type: 'read' | 'unread', count: number): void {
        this.modalService.dialog({
            title: this.i18n.translate('modal.confirmation_message_delete_title'),
            body: this.i18n.translate('modal.delete_message_confirm_' + type, { count: count }),
            buttons: [
                {
                    label: this.i18n.translate('common.cancel_button'),
                    type: 'secondary',
                    flat: true,
                    returnValue: false,
                    shouldReject: true,
                },
                {
                    label: this.i18n.translate('common.delete_button'),
                    type: 'alert',
                    returnValue: true,
                },
            ],
        })
            .then(dialog => dialog.open())
            .then(result => callback());
    }

    deleteAllUnread(): void {
        this.confirmBeforeDelete(() => {
            this.deleteMessagesById(this.selectedUnread);
        }, 'unread', this.selectedUnread.length);
    }

    deleteAllRead(): void {
        this.confirmBeforeDelete(() => {
            this.deleteMessagesById(this.selectedRead);
        }, 'read', this.selectedRead.length);
    }

    private openPage(link: MessageLink): void {
        let nodeId: number;

        this.fetchPage(link.id)
            .then(page => this.fetchFolder(page.folderId))
            .then(folder => {
                nodeId = this.nodeIdByName(link.nodeName) || folder.nodeId;
                return this.navigateToFolder(folder.id, nodeId);
            })
            .then(succeeded => succeeded &&
                this.navigationService.detailOrModal(nodeId, 'page', link.id, EditMode.PREVIEW).navigate(),
            );
    }

    private nodeIdByName(nodeName: string): number {
        const nodes = this.appState.now.entities.node;
        if (!nodes) {
            return undefined;
        }

        for (const nodeId of Object.keys(nodes)) {
            if ((<any> nodes)[nodeId].name === nodeName) {
                return Number(nodeId);
            }
        }

        return undefined;
    }

    private fetchPage(pageId: number): Promise<Page> {
        const page = this.entityResolver.getPage(pageId);
        if (page) {
            return Promise.resolve(page);
        } else {
            return this.folderActions.getPage(pageId);
        }
    }

    private fetchFolder(folderId: number): Promise<Folder> {
        const folder = this.entityResolver.getFolder(folderId);
        if (folder) {
            return Promise.resolve(folder);
        } else {
            return this.folderActions.getFolder(folderId);
        }
    }

    private navigateToFolder(folderId: number, nodeId: number): Promise<boolean> {
        const newRoute = ['/editor', { outlets: { list: ['node', nodeId, 'folder', folderId] } }];
        if (this.router.url === newRoute.join('')) {
            return Promise.resolve(true);
        }

        return this.router.navigate(newRoute)
            .then(success => {
                // When "navigating" to current folder, Router.navigate() currently resolves to null.
                // This is a possible bug, see https://github.com/angular/angular/issues/13745
                if (success === false) {
                    return false;
                }

                // Wait until loaded
                return this.appState.select(state => state.folder).pipe(
                    map(state => areItemsLoading(state)),
                    filter(fetching => fetching === false),
                    take(1),
                    map(() => true),
                ).toPromise();
            });
    }

    /** Maps message IDs to their entities and sorts them by unread first, newest first. */
    private sortMapMessages(
        ids: number[],
        keep: number | undefined,
        messages: IndexById<Message<Normalized>>,
    ): Message<Normalized>[] {
        return ids
            .map(id => messages[id])
            .filter(msg => msg != null)
            .sort((a, b) => {
                const aUnread = a.unread || a.id === keep;
                const bUnread = b.unread || b.id === keep;

                if (aUnread !== bUnread) {
                    return aUnread ? -1 : 1;
                } else {
                    return b.timestamp - a.timestamp;
                }
            });
    }

    composeMessage(): void {
        this.modalService.fromComponent(SendMessageModal)
            .then(modal => modal.open());
    }
}
