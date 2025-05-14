import { ChangeDetectionStrategy, Component } from '@angular/core';
import { IDocumentation } from '../../common/docs';
import { InjectDocumentation } from '../../common/docs-loader';

interface User {
    firstName: string;
    lastName: string;
    dateOfBirth: Date;
    hobbies?: string[];
    childOf?: [mother: User, father: User];
}

@Component({
    templateUrl: './paging-demo.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false
})
export class PagingDemoPage {

    @InjectDocumentation('paging.component')
    documentation: IDocumentation;

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

    id = 'custom-id-123';

    page = 1;
}
