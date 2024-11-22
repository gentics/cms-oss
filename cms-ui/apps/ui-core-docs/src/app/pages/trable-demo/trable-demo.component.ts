import { ChangeDetectionStrategy, Component, OnInit } from '@angular/core';
import {
    NotificationService,
    TableAction,
    TableActionClickEvent,
    TableColumn,
    TableRow,
    TableSortOrder,
    TrableRow,
    TrableRowExpandEvent,
} from '@gentics/ui-core';
import { IDocumentation } from '../../common/docs';
import { InjectDocumentation } from '../../common/docs-loader';

interface FolderContent {
    id: number;
    name: string;
    type: 'folder' | 'file';
    entries?: FolderContent[];
}

const FOLDERS: FolderContent[] = [
    {
        id: 1,
        name: 'Windows',
        type: 'folder',
        entries: [
            {
                id: 2,
                name: 'System32',
                type: 'folder',
                entries: [
                    {
                        id: 3,
                        name: 'at.exe',
                        type: 'file',
                    },
                    {
                        id: 4,
                        name: 'AtBroker.exe',
                        type: 'file',
                    },
                ],
            },
            {
                id: 5,
                name: 'Fonts',
                type: 'folder',
                entries: [
                    {
                        id: 6,
                        name: 'Comic Sans.ttf',
                        type: 'file',
                    },
                    {
                        id: 7,
                        name: 'Versana.ttf',
                        type: 'file',
                    },
                ],
            },
        ],
    },
    {
        id: 8,
        name: 'Users',
        type: 'folder',
        entries: [
            {
                id: 9,
                name: 'Administrator',
                type: 'folder',
            },
            {
                id: 10,
                name: 'Gentics',
                type: 'folder',
                entries: [
                    {
                        id: 11,
                        name: 'PlanForWorldDominiation.docx',
                        type: 'file',
                    },
                    {
                        id: 12,
                        name: 'grocery-list.txt',
                        type: 'file',
                    },
                ],
            },
            {
                id: 13,
                name: 'log.txt',
                type: 'file',
            },
        ],
    },
];

function mapToTrableRow(folder: FolderContent, level: number = 0, parent?: TrableRow<FolderContent>): TrableRow<FolderContent> {
    const row: TrableRow<FolderContent> = {
        expanded: false,
        hasChildren: folder.entries != null && folder.entries.length > 0,
        id: `${folder.id}`,
        item: folder,
        level,
        loaded: true,
        selectable: true,
        children: [],
        parent,
    };

    row.children = (folder.entries || []).map(entry => mapToTrableRow(entry, level + 1, row));

    return row;
}

@Component({
    templateUrl: './trable-demo.component.html',
    styleUrls: ['./trable-demo.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class TrableDemoPage implements OnInit {

    @InjectDocumentation('trable.component')
    documentation: IDocumentation;

    columns: TableColumn<FolderContent>[] = [
        {
            id: 'name',
            fieldPath: 'name',
            label: 'Name',
            sortable: false,
        },
        {
            id: 'id',
            fieldPath: 'id',
            label: 'ID',
            sortable: false,
        },
    ];

    actions: TableAction<FolderContent>[] = [
        {
            id: 'show',
            icon: 'visibility',
            label: 'Show Notification',
            type: 'secondary',
            enabled: true,
        },
        {
            id: 'delete',
            icon: 'delete',
            label: 'Delete User',
            type: 'alert',
            enabled: true,
        },
    ];

    rows: TrableRow<FolderContent>[] = [];
    selection: string[] = [];
    active: string;
    inlineSelection = false;
    inlineExpansion = false;

    constructor(
        private notification: NotificationService,
    ) {}

    ngOnInit(): void {
        this.rebuildRows();
    }

    updateSelection(selection: string[]): void {
        this.selection = selection;
    }

    rebuildRows(): void {
        this.rows = FOLDERS.map(f => mapToTrableRow(f));
    }

    setRowActive(row: TableRow<FolderContent>): void {
        this.active = row.id;
    }

    handleAction(event: TableActionClickEvent<FolderContent>): void {
        switch (event.actionId) {
            case 'show':
                this.notification.show({
                    message: `Hello from ${event.item.name}!`,
                    type: 'success',
                });
                break;

            case 'delete':
                this.notification.show({
                    message: `Deleting ${event.item.name} ...`,
                    type: 'alert',
                });
                break;
        }
    }

    handleRowExpansion(event: TrableRowExpandEvent<FolderContent>): void {
        event.row.expanded = !event.row.expanded;
    }
}
