import { ChangeDetectionStrategy, ChangeDetectorRef, Component, EventEmitter, HostListener, OnInit, Output } from '@angular/core';
import { I18nService, MessageLink } from '@gentics/cms-components';
import { Message, Node, Normalized } from '@gentics/cms-models';
import { ModalService } from '@gentics/ui-core';
import { isEqual } from 'lodash-es';
import { BehaviorSubject, Observable } from 'rxjs';
import { debounceTime, distinctUntilChanged, map, startWith, tap, withLatestFrom } from 'rxjs/operators';
import { SendMessageModalComponent } from '../../../shared/components';
import { MessageStateModel } from '../../../state/messages/message.state';
import { AppStateService } from '../../../state/providers/app-state/app-state.service';
import { MessageService } from '../../providers';
import { MessageModalComponent } from '../message-modal/message-modal.component';

@Component({
    selector: 'gtx-message-inbox',
    templateUrl: './message-inbox.tpl.html',
    styleUrls: ['./message-inbox.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false,
})
export class MessageInboxComponent implements OnInit {

    /** Emits when a link in a message is clicked */
    @Output() navigate = new EventEmitter<MessageLink>();

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
        private appState: AppStateService,
        // private navigationService: NavigationService,
        // private entityResolver: EntityResolver,
        // private folderActions: FolderActions,
        private messageService: MessageService,
        private changeDetector: ChangeDetectorRef,
        private modalService: ModalService,
        private i18n: I18nService,
    ) {}

    @HostListener('document:mouseup', ['$event'])
    onGlobalClick(event: Event): void {
        if (this.waitsForDeleteConfirmation && (event.target as HTMLElement).dataset['msgId'] !== String(this.waitsForDeleteConfirmation.id)) {
            this.waitsForDeleteConfirmation = null;
        }
    }

    ngOnInit(): void {
        // MessageState Observable that keeps the currently opened message in the "unread" messages.
        const messages$ = this.appState.select((state) => state.messages).pipe(
            distinctUntilChanged((a: MessageStateModel, b: MessageStateModel) => a.read === b.read && a.unread === b.unread),
            debounceTime(5),
            withLatestFrom(this.messageToKeepInUnread$),
            map(([state, keep]) => {
                const read = (!keep || state.read.indexOf(keep) < 0)
                    ? state.read
                    : state.read.filter((id) => id !== keep);
                const unread = (!keep || state.unread.indexOf(keep) >= 0)
                    ? state.unread
                    : state.unread.concat(keep);

                return {
                    all: this.sortMapMessages(state.all, keep),
                    read: this.sortMapMessages(read, keep),
                    unread: this.sortMapMessages(unread, keep),
                };
            }),
        );

        this.hasMessages$ = messages$.pipe(
            map((state) => state.all.length > 0),
            startWith(false),
        );
        this.readMessages$ = messages$.pipe(
            map((state) => state.read),
            tap((messages) => {
                this.allReadIds = messages.map((msg) => msg.id);
                this.selectedRead = this.selectedRead.filter((id) =>
                    !!messages.find((msg) => msg.id === id),
                ).slice();
            }),
        );

        this.readMessagesCount$ = this.readMessages$.pipe(
            map((messages) => messages.length),
            distinctUntilChanged(isEqual),
        );
        this.unreadMessages$ = messages$.pipe(
            map((state) => state.unread),
            tap((messages) => {
                this.allUnreadIds = messages.map((msg) => msg.id);
                this.selectedUnread = this.selectedUnread.filter((id) =>
                    !!messages.find((msg) => msg.id === id),
                ).slice();
            }),
        );
        this.unreadMessagesCount$ = this.unreadMessages$.pipe(
            map((messages) => messages.length),
            distinctUntilChanged(isEqual),
        );

        /* this.nodes$ = this.appState.select(state => state.folder.nodes.list)
            .map(nodeIds => nodeIds.map(id => this.entityResolver.getNode(id))); */
    }

    toggleReadMessages(): void {
        this.showReadMessages = !this.showReadMessages;
    }

    showMessage(message: Message<Normalized>, keepInUnread: boolean): void {
        if (keepInUnread) {
            this.messageToKeepInUnread$.next(message.id);
        }

        this.modalService.fromComponent(MessageModalComponent, {}, { message })
            .then((modal) => modal.open())
            .then((link: MessageLink) => {
                this.messageToKeepInUnread$.next(undefined);
                this.changeDetector.markForCheck();

                if (link) {
                    // this.navigate.emit(link);
                    // this.openPage(link);
                }
            });

        if (message.unread) {
            this.messageService.markMessagesAsRead([message.id]);
        }
    }

    archiveMessagesById(messageIds: number[]): void {
        this.messageService.markMessagesAsRead(messageIds);
    }

    archiveMessages(messages: Message<Normalized>[] | Message<Normalized>): void {
        if (!Array.isArray(messages)) {
            messages = [messages];
        }

        this.archiveMessagesById(messages.map((message) => message.id));
    }

    deleteMessagesById(messageIds: number[]): void {
        this.messageService.deleteMessages(messageIds);
    }

    deleteMessages(messages: Message<Normalized>[] | Message<Normalized>): void {
        if (!Array.isArray(messages)) {
            messages = [messages];
        }

        this.deleteMessagesById(messages.map((message) => message.id));
    }

    deleteMessage(message: Message<Normalized>): void {
        if (this.waitsForDeleteConfirmation !== message) {
            this.waitsForDeleteConfirmation = message;
            return;
        }

        this.messageService.deleteMessages([message.id]);
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

    confirmBeforeDelete(callback: () => any, type: 'read' | 'unread', count: number): void {
        this.modalService.dialog({
            title: this.i18n.instant('modal.confirmation_message_delete_title'),
            body: this.i18n.instant('modal.delete_message_confirm_' + type, { count: count }),
            buttons: [
                { label: this.i18n.instant('common.cancel_button'), type: 'secondary' as const, flat: true, returnValue: false, shouldReject: true },
                { label: this.i18n.instant('common.delete_button'), type: 'alert' as const, returnValue: true },
            ],
        })
            .then((dialog) => dialog.open())
            .then(() => callback());
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

    /* private openPage(link: MessageLink): void {
        let nodeId: number;

        this.fetchPage(link.id)
            .then(page => this.fetchFolder(page.folderId))
            .then(folder => {
                nodeId = this.nodeIdByName(link.nodeName) || folder.nodeId;
                return this.navigateToFolder(folder.id, nodeId);
            })
            .then(succeeded => succeeded &&
                this.navigationService.detailOrModal(nodeId, 'page', link.id, 'preview').navigate()
            );
    } */

    /* private nodeIdByName(nodeName: string): number {
        const nodes = this.appState.now.entities.node;
        if (!nodes) {
            return undefined;
        }

        for (let nodeId of Object.keys(nodes)) {
            if ((<any> nodes)[nodeId].name === nodeName) {
                return Number(nodeId);
            }
        }

        return undefined;
    } */

    /* private fetchPage(pageId: number): Promise<Page> {
        let page = this.entityResolver.getPage(pageId);
        if (page) {
            return Promise.resolve(page);
        } else {
            return this.folderActions.getPage(pageId);
        }
    } */

    /* private fetchFolder(folderId: number): Promise<Folder> {
        let folder = this.entityResolver.getFolder(folderId);
        if (folder) {
            return Promise.resolve(folder);
        } else {
            return this.folderActions.getFolder(folderId);
        }
    } */

    /* private navigateToFolder(folderId: number, nodeId: number): Promise<boolean> {
        let newRoute = ['/editor', { outlets: { list: ['node', nodeId, 'folder', folderId] } }];
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
                return this.appState
                    .select(state =>
                        state.folder.folders.fetching ||
                        state.folder.pages.fetching ||
                        state.folder.files.fetching ||
                        state.folder.images.fetching)
                    .filter(fetching => fetching === false)
                    .take(1)
                    .mapTo(true)
                    .toPromise();
            });
    } */

    /** Maps message IDs to their entities and sorts them by unread first, newest first. */
    private sortMapMessages(ids: number[], keep: number | undefined): Message<Normalized>[] {
        const entities = this.appState.now.entity.message;
        return ids
            .map((id) => entities[id])
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
        this.modalService.fromComponent(SendMessageModalComponent)
            .then((modal) => modal.open());
    }
}
