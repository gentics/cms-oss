import { ChangeDetectionStrategy, Component, OnInit } from '@angular/core';
import { NotificationService, TableAction, TableActionClickEvent, TableColumn, TableColumnMappingFn, TableRow, TableSortOrder } from '@gentics/ui-core';
import { IDocumentation } from '../../common/docs';
import { InjectDocumentation } from '../../common/docs-loader';
import { at } from 'lodash-es';

interface User {
    firstName: string;
    lastName: string;
    dateOfBirth: Date;
    hobbies?: string[];
    childOf?: [mother: User, father: User];
}

const THIS_YEAR = new Date().getFullYear();
const AGE_MAPPER: TableColumnMappingFn<User> = (user: User) => THIS_YEAR - user.dateOfBirth.getFullYear();

function sortRows(rows: TableRow<User>[], columns: TableColumn<User>[], key: string, order: TableSortOrder): TableRow<User>[] {
    if (!key) {
        return rows;
    }

    const col = columns.find(col => col.id === key);

    const arr =  rows.slice().sort((a, b) => {
        let aVal: any = a.item;
        let bVal: any = b.item;

        if (col?.fieldPath) {
            aVal = at(aVal, col.fieldPath)?.[0];
            bVal = at(bVal, col.fieldPath)?.[0];
        }

        if (col?.mapper) {
            aVal = col.mapper(aVal, col);
            bVal = col.mapper(bVal, col);
        }

        let left, right;
        if (order === TableSortOrder.ASCENDING) {
            left = bVal;
            right = aVal;
        } else {
            left = aVal;
            right = bVal;
        }

        if (typeof left === 'number') {
            return left - right;
        }

        if (left instanceof Date) {
            return left.getTime() - right.getTime();
        }

        return ((left ?? '').toString()).localeCompare((right ?? '').toString());
    });
    return arr;
}

@Component({
    templateUrl: './table-demo.component.html',
    styleUrls: ['./table-demo.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class TableDemoPage implements OnInit {

    i18nDate = new Intl.DateTimeFormat('en-GB');

    @InjectDocumentation('table.component')
    documentation: IDocumentation;

    columns: TableColumn<User>[] = [
        {
            id: 'firstName',
            fieldPath: 'firstName',
            label: 'First Name',
            sortable: true,
        },
        {
            id: 'lastName',
            fieldPath: 'lastName',
            label: 'Last Name',
            sortable: true,
        },
        {
            id: 'dateOfBirth',
            fieldPath: 'dateOfBirth',
            align: 'right',
            label: 'Date of Birth',
            sortable: true,
        },
        {
            id: 'age',
            mapper: AGE_MAPPER,
            align: 'right',
            label: 'Age',
            sortable: true,
        },
        {
            id: 'hobbies',
            fieldPath: 'hobbies',
            label: 'Hobbies',
            sortable: false,
        },
        {
            id: 'childOf',
            fieldPath: 'childOf',
            label: 'Child Of',
            sortable: false,
        },
    ];

    users: User[] = [
        {
            firstName: 'Max',
            lastName: 'Mustermann',
            dateOfBirth: new Date(1990, 1, 1),
            hobbies: ['Soccer', 'Swimming'],
        },
        {
            firstName: 'Jane',
            lastName: 'Doe',
            dateOfBirth: new Date(1983, 6, 28),
            hobbies: ['Crafting'],
        },
        {
            firstName: 'Mike',
            lastName: 'Doe',
            dateOfBirth: new Date(1984, 9, 17),
            hobbies: ['Skiing'],
        },
    ];

    actions: TableAction<User>[] = [
        {
            id: 'show',
            icon: 'visibility',
            label: 'Show Notification',
            type: 'secondary',
        },
        {
            id: 'delete',
            icon: 'delete',
            label: 'Delete User',
            type: 'alert',
        },
    ];

    rows: TableRow<User>[] = [];
    selection: string[] = [];
    active: string;
    sortColumn: string;
    sortOrder: TableSortOrder = TableSortOrder.ASCENDING;

    constructor(
        private notification: NotificationService,
    ) {
        this.users.push({
            firstName: 'Sophie',
            lastName: 'Doe',
            hobbies: ['Drawing', 'Dancing'],
            dateOfBirth: new Date(2003, 3, 7),
            childOf: [this.users[1], this.users[2]],
        });
    }

    ngOnInit(): void {
        this.rebuildRows();
    }

    updateSelection(selection: string[]): void {
        this.selection = selection;
    }

    updateSortColumn(columnId: string): void {
        this.sortColumn = columnId;
        this.rows = sortRows(this.rows, this.columns, this.sortColumn, this.sortOrder);
    }

    updateSortOrder(order: TableSortOrder): void {
        this.sortOrder = order;
        this.rows = sortRows(this.rows, this.columns, this.sortColumn, this.sortOrder);
    }

    rebuildRows(): void {
        this.rows = sortRows(this.generateRows(this.users), this.columns, this.sortColumn, this.sortOrder);
    }

    setRowActive(row: TableRow<User>): void {
        this.active = row.id;
    }

    handleAction(event: TableActionClickEvent<User>): void {
        switch (event.actionId) {
            case 'show':
                this.notification.show({
                    message: `Hello from ${event.item.firstName}!`,
                    type: 'success',
                });
                break;

            case 'delete':
                this.notification.show({
                    message: `Deleting ${event.item.firstName} ${event.item.lastName} ...`,
                    type: 'alert',
                });
                break;
        }
    }

    generateRows(users: User[]): TableRow<User>[] {
        return users.map(user => {
            const id =`${user.firstName}_${user.lastName}`.toLowerCase();

            return {
                id: id,
                item: user,
            };
        });
    }
}
