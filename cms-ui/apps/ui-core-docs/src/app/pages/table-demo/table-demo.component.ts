import { ChangeDetectionStrategy, Component, OnInit } from '@angular/core';
import { NotificationService, TableAction, TableActionClickEvent, TableColumn, TableColumnMappingFn, TableRow, TableSortOrder } from '@gentics/ui-core';
import { at } from 'lodash-es';
import { IDocumentation } from '../../common/docs';
import { InjectDocumentation } from '../../common/docs-loader';

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

        let left;
        let right;

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
            // eslint-disable-next-line @typescript-eslint/no-unsafe-call
            return left.getTime() - right.getTime();
        }

        // eslint-disable-next-line @typescript-eslint/no-unsafe-call
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
            // eslint-disable-next-line @typescript-eslint/explicit-module-boundary-types
            mapper: (hobbies: string[]) => (hobbies || []).join(', '),
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
            hobbies: ['Crafting', 'Swimming'],
        },
        {
            firstName: 'Mike',
            lastName: 'Doe',
            dateOfBirth: new Date(1984, 9, 17),
            hobbies: ['Skiing'],
        },
        {
            firstName: 'Emily',
            lastName: 'Johnson',
            dateOfBirth: new Date(1995, 2, 15),
            hobbies: ['Reading', 'Traveling'],
        },
        {
            firstName: 'Chris',
            lastName: 'Smith',
            dateOfBirth: new Date(1988, 11, 30),
            hobbies: ['Cooking', 'Hiking'],
        },
        {
            firstName: 'Jessica',
            lastName: 'Brown',
            dateOfBirth: new Date(1992, 5, 10),
            hobbies: ['Photography', 'Yoga'],
        },
        {
            firstName: 'David',
            lastName: 'Wilson',
            dateOfBirth: new Date(1980, 8, 22),
            hobbies: ['Fishing', 'Camping'],
        },
        {
            firstName: 'Sarah',
            lastName: 'Davis',
            dateOfBirth: new Date(1993, 3, 5),
            hobbies: ['Knitting', 'Gardening'],
        },
        {
            firstName: 'Daniel',
            lastName: 'Garcia',
            dateOfBirth: new Date(1985, 7, 14),
            hobbies: ['Running', 'Cycling'],
        },
        {
            firstName: 'Laura',
            lastName: 'Martinez',
            dateOfBirth: new Date(1991, 0, 28),
            hobbies: ['Dancing', 'Singing'],
        },
        {
            firstName: 'Matthew',
            lastName: 'Hernandez',
            dateOfBirth: new Date(1987, 9, 19),
            hobbies: ['Video Games', 'Comics'],
        },
        {
            firstName: 'Sophia',
            lastName: 'Lopez',
            dateOfBirth: new Date(1994, 6, 11),
            hobbies: ['Baking', 'Painting'],
        },
        {
            firstName: 'James',
            lastName: 'Gonzalez',
            dateOfBirth: new Date(1982, 12, 2),
            hobbies: ['Traveling', 'Surfing'],
        },
        {
            firstName: 'Olivia',
            lastName: 'Perez',
            dateOfBirth: new Date(1996, 4, 25),
            hobbies: ['Writing', 'Poetry'],
        },
        {
            firstName: 'Ethan',
            lastName: 'Wilson',
            dateOfBirth: new Date(1989, 3, 17),
            hobbies: ['Basketball', 'Football'],
        },
        {
            firstName: 'Ava',
            lastName: 'Anderson',
            dateOfBirth: new Date(1990, 8, 8),
            hobbies: ['Fashion', 'Shopping'],
        },
        {
            firstName: 'Liam',
            lastName: 'Thomas',
            dateOfBirth: new Date(1986, 1, 20),
            hobbies: ['Music', 'Concerts'],
        },
        {
            firstName: 'Mia',
            lastName: 'Taylor',
            dateOfBirth: new Date(1992, 5, 30),
            hobbies: ['Fitness', 'Wellness'],
        },
        {
            firstName: 'Noah',
            lastName: 'Moore',
            dateOfBirth: new Date(1984, 10, 12),
            hobbies: ['Technology', 'Gadgets'],
        },
        {
            firstName: 'Isabella',
            lastName: 'Jackson',
            dateOfBirth: new Date(1995, 7, 4),
            hobbies: ['Art', 'Crafts'],
        },
        {
            firstName: 'Lucas',
            lastName: 'Martin',
            dateOfBirth: new Date(1981, 2, 18),
            hobbies: ['Sports', 'Fitness'],
        },
    ];

    allHobbies: string[] = [];

    actions: TableAction<User>[] = [
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

    rows: TableRow<User>[] = [];
    selected: string[] = [];
    active: string;
    sortColumn: string;
    sortOrder: TableSortOrder = TableSortOrder.ASCENDING;
    filters: { hobbies?: string[] } = {};
    filteredRows: TableRow<User>[] = [];

    page = 1;
    perPage = 10;
    paginatedRows: TableRow<User>[] = [];

    constructor(
        private notification: NotificationService,
    ) {
        this.users.push({
            firstName: 'Sophie',
            lastName: 'Doe',
            hobbies: ['Skiing', 'Dancing'],
            dateOfBirth: new Date(2003, 3, 7),
            childOf: [this.users[1], this.users[2]],
        });
    }

    ngOnInit(): void {
        this.allHobbies = Array.from(new Set(this.users.flatMap(user => user.hobbies)));
        this.rebuildRows();
    }

    updateSelection(selection: string[]): void {
        this.selected = selection;
    }

    updateSortColumn(columnId: string): void {
        this.sortColumn = columnId;
        this.rows = sortRows(this.rows, this.columns, this.sortColumn, this.sortOrder);
        this.updatePaginatedRows();
        this.updateFilteredRows();
    }

    updateSortOrder(order: TableSortOrder): void {
        this.sortOrder = order;
        this.rows = sortRows(this.rows, this.columns, this.sortColumn, this.sortOrder);
        this.updatePaginatedRows();
        this.updateFilteredRows();
    }

    rebuildRows(): void {
        this.rows = sortRows(this.generateRows(this.users), this.columns, this.sortColumn, this.sortOrder);
        this.updatePaginatedRows();
        this.updateFilteredRows();
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

    applyFilterValue(key: string, value: any): void {
        this.filters[key] = value;
        this.updateFilteredRows();
    }

    updateFilteredRows(): void {
        this.filteredRows = this.rows.filter(row => !this.filters.hobbies?.length
            || row.item.hobbies.some(hobby => this.filters.hobbies.includes(hobby)),
        );
    }

    updatePaginatedRows(): void {
        const from = (this.page - 1) * this.perPage;
        this.paginatedRows = this.rows.slice(from, from + this.perPage);
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
