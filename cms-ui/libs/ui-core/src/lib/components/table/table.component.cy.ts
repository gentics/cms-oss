import { createOutputSpy, mount, MountResponse } from 'cypress/angular';
import { GenticsUICoreModule } from '@gentics/ui-core';
import { CHECKBOX_STATE_INDETERMINATE, TableColumn, TableRow } from '../../common';
import { randomId } from '../../utils';
import { TableComponent } from './table.component';

interface TestUser {
    id: string;
    name: string;
    birthDate: Date;
    postCount: number;
}

const NAMES = [
    'Alexander Walker',
    'Olivia Bennett',
    'Ethan Carter',
    'Ava Collins',
    'Liam Foster',
    'Sophia Evans',
    'Benjamin Taylor',
    'Mia Brooks',
    'Noah Morgan',
    'Charlotte Ross',
    'Lucas Hughes',
    'Amelia Parker',
    'Mason Bryant',
    'Harper Mitchell',
    'Logan Adams',
    'Evelyn Rivera',
    'James Cooper',
    'Abigail Peterson',
    'Henry Bailey',
    'Scarlett Kelly',
    'Jacob Hayes',
    'Lily Moore',
    'Michael Reed',
    'Emily Jenkins',
    'Daniel Howard',
    'Zoey Fisher',
    'Matthew Sanders',
    'Chloe Price',
    'Samuel Morris',
    'Grace Foster',
    'Gabriel Butler',
    'Aria Murphy',
    'Owen Simmons',
    'Riley West',
    'Carter Powell',
    'Ella Campbell',
    'Wyatt Brooks',
    'Nora Gray',
    'David Barnes',
    'Luna Green',
];

const DOB_MIN = new Date(1950, 0, 1).getTime();
const CURRENT_YEAR = new Date().getFullYear();
const DOB_MAX = new Date(CURRENT_YEAR - 18, 0, 1).getTime();

const DATE_FORMATTER = new Intl.DateTimeFormat('en', { hourCycle: 'h24' });
const DEFAULT_COLUMNS: TableColumn<TestUser>[] = [
    {
        id: 'name',
        label: 'Name',
        fieldPath: 'name',
    },
    {
        id: 'dob',
        label: 'Date of Birth',
        fieldPath: 'birthDate',
        mapper: (val) => DATE_FORMATTER.format(val),
    },
    {
        id: 'age',
        label: 'Age',
        fieldPath: 'birthDate',
        mapper: (date: Date) => CURRENT_YEAR - date.getFullYear(),
    },
    {
        id: 'posts',
        label: '# of posts',
        fieldPath: 'postCount',
    },
];

function createRandomTestUser(): TestUser {
    const id = randomId();
    const name = NAMES[Math.floor(Math.random() * NAMES.length)];

    return {
        id,
        name,
        birthDate: new Date(DOB_MIN + (Math.floor((Math.random() * DOB_MAX) - DOB_MIN))),
        postCount: Math.floor(Math.random() * 50),
    };
}

function createRandomRow(): TableRow<TestUser> {
    const user = createRandomTestUser();

    return {
        id: user.id,
        item: user,
    };
}

function generateRows(length: number): TableRow<TestUser>[] {
    const arr: TableRow<TestUser>[] = [];

    for (let i = 0; i < length; i++) {
        arr.push(createRandomRow());
    }

    return arr;
}

function detectChangesAndWait(ref: MountResponse<any>) {
    return cy.wrap(null, { log: false }).then(() => {
        ref.fixture.detectChanges();

        return ref.fixture.whenRenderingDone();
    });
}

function resetSpy(spyName: string) {
    cy.get<Cypress.Agent<sinon.SinonSpy>>(`@${spyName}`)
        .then(spy => {
            (spy as unknown as Cypress.Agent<sinon.SinonSpy>).resetHistory();
        });
}

function calledOnceWithReset(spyName: string, value: any) {
    cy.get<Cypress.Agent<sinon.SinonSpy>>(`@${spyName}`)
        .then(spy => {
            expect(spy).to.have.been.calledOnceWith(value);
            spy.resetHistory();
        });
}

describe('TableComponent', () => {
    const ALIAS_PAGE_CHANGE = 'pageChangeSpy';

    it('should display the all rows without pagination', () => {
        const ROWS = generateRows(15);

        mount(TableComponent, {
            componentProperties: {
                paginated: false,
                columns: DEFAULT_COLUMNS,
                rows: ROWS,
            },
            imports: [
                GenticsUICoreModule.forRoot(),
            ],
        }).then(ref => {
            return ref.fixture.whenRenderingDone()
                .then(() => ref)
        }).then(ref => {
            cy.get('.grid-row.data-row')
                .should('have.length', ROWS.length);
            cy.get('.table-pagination')
                .should('not.exist');
        });
    });

    it('should display the rows with pagination', () => {
        const ROWS = generateRows(15);
        const PER_PAGE = 10;

        mount(TableComponent, {
            componentProperties: {
                columns: DEFAULT_COLUMNS,
                rows: ROWS,
                perPage: PER_PAGE,
                pageChange: createOutputSpy(ALIAS_PAGE_CHANGE),
            },
            imports: [
                GenticsUICoreModule.forRoot(),
            ],
        }).then(ref => {
            return ref.fixture.whenRenderingDone()
                .then(() => ref)
        }).then(ref => {
            cy.get('.grid-row.data-row')
                .should('have.length', PER_PAGE);
            cy.get('.table-pagination')
                .should('exist')
                .find('.pages>.page')
                .should('have.length', 2);

            cy.get('.table-pagination .pages .page.link')
                .click();

            cy.get(`@${ALIAS_PAGE_CHANGE}`)
                .should('have.been.calledOnceWith', 2);
        });
    });

    it('should show the correct selected rows and change the selection correctly', () => {
        const ROWS = generateRows(15);
        const INITIAL_IDX = 3;
        const CHANGE_IDX = 7;
        const ALIAS_SELECTED_CHANGE = 'selectedChangeSpy';

        mount(`
            <gtx-table
                [paginated]="false"
                [selectable]="true"
                [multiple]="false"
                [columns]="columns"
                [rows]="rows"
                [selected]="selected"
            />
        `, {
            componentProperties: {
                columns: DEFAULT_COLUMNS,
                rows: ROWS,
                selected: [ROWS[INITIAL_IDX].id],
            },
            imports: [
                GenticsUICoreModule.forRoot(),
            ],
        }).then(ref => {
            return ref.fixture.whenRenderingDone()
                .then(() => ref)
        }).then(ref => {
            const table: TableComponent<TestUser> = ref.fixture.debugElement.childNodes[0].componentInstance;
            table.selectedChange = createOutputSpy(ALIAS_SELECTED_CHANGE);

            cy.get(`.grid-row.data-row[data-id="${ROWS[INITIAL_IDX].id}"]`)
                .should('have.class', 'selected');
            cy.get(`.grid-row.data-row[data-id="${ROWS[CHANGE_IDX].id}"]`)
                .should('not.have.class', 'selected');

            cy.get(`@${ALIAS_SELECTED_CHANGE}`)
                .should('not.have.been.called');

            cy.get(`.grid-row.data-row[data-id="${ROWS[CHANGE_IDX].id}"] .selection-checkbox label`)
                .click();
            calledOnceWithReset(ALIAS_SELECTED_CHANGE, [ROWS[CHANGE_IDX].id]);
            cy.get(`.grid-row.data-row[data-id="${ROWS[CHANGE_IDX].id}"]`)
                .should('not.have.class', 'selected');

            cy.wrap(null, { log: false }).then(() => {
                ref.component.selected = [ROWS[CHANGE_IDX].id];
            });
            detectChangesAndWait(ref);

            cy.get(`.grid-row.data-row[data-id="${ROWS[INITIAL_IDX].id}"]`)
                .should('not.have.class', 'selected');
            cy.get(`.grid-row.data-row[data-id="${ROWS[CHANGE_IDX].id}"]`)
                .should('have.class', 'selected');
        });
    });

    it('should show the correct selected rows and change the selection correctly (useSelectionMap)', () => {
        const ROWS = generateRows(15);
        const ALIAS_SELECTED_CHANGE = 'selectedChangeSpy';
        const OTHER_IDX = 0;
        const SELECTION_MAP = {
            [ROWS[4].id]: true,
            [ROWS[6].id]: CHECKBOX_STATE_INDETERMINATE,
            [ROWS[8].id]: CHECKBOX_STATE_INDETERMINATE,
            [ROWS[2].id]: false,
        };
        const NEW_SELECTION_MAP = {
            ...SELECTION_MAP,
            [ROWS[OTHER_IDX].id]: true,
        };

        mount(`
            <gtx-table
                [paginated]="false"
                [selectable]="true"
                [multiple]="true"
                [useSelectionMap]="true"
                [columns]="columns"
                [rows]="rows"
                [selected]="selected"
            />
        `, {
            componentProperties: {
                columns: DEFAULT_COLUMNS,
                rows: ROWS,
                selected: SELECTION_MAP,
            },
            imports: [
                GenticsUICoreModule.forRoot(),
            ],
        }).then(ref => {
            return ref.fixture.whenRenderingDone()
                .then(() => ref)
        }).then(ref => {
            const table: TableComponent<TestUser> = ref.fixture.debugElement.childNodes[0].componentInstance;
            table.selectedChange = createOutputSpy(ALIAS_SELECTED_CHANGE);

            // Validate intial state
            Object.entries(SELECTION_MAP).forEach(([id, value]) => {
                cy.get(`.grid-row.data-row[data-id="${id}"] .selection-checkbox input`)
                    // eslint-disable-next-line @typescript-eslint/restrict-template-expressions
                    .should('have.attr', 'data-state', `${value}`)
            });
            cy.get(`.grid-row.data-row[data-id="${ROWS[OTHER_IDX].id}"] .selection-checkbox input`)
                .should('have.attr', 'data-state', 'false');
            cy.get(`@${ALIAS_SELECTED_CHANGE}`)
                .should('not.have.been.called');

            cy.get(`.grid-row.data-row[data-id="${ROWS[OTHER_IDX].id}"] .selection-checkbox label`)
                .click();

            calledOnceWithReset(ALIAS_SELECTED_CHANGE, NEW_SELECTION_MAP);
            cy.wrap(null, { log: false }).then(() => {
                ref.component.selected = NEW_SELECTION_MAP;
            });
            detectChangesAndWait(ref);

            Object.entries(SELECTION_MAP).forEach(([id, value]) => {
                cy.get(`.grid-row.data-row[data-id="${id}"] .selection-checkbox input`)
                    // eslint-disable-next-line @typescript-eslint/restrict-template-expressions
                    .should('have.attr', 'data-state', `${value}`)
            });
            cy.get(`.grid-row.data-row[data-id="${ROWS[OTHER_IDX].id}"] .selection-checkbox input`)
                .should('have.attr', 'data-state', 'true');
        });
    });
});
