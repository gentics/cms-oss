import { animate, group, query, stagger, state, style, transition, trigger } from '@angular/animations';

export const GTX_FORM_EDITOR_ANIMATIONS = [
    trigger('fadeAnim', [
        state('in', style({
            opacity: 1,
        })),
        transition(':enter', [
            style({
                opacity: 0,
            }),
            animate(100),
        ]),
        transition(':leave',
            animate(100, style({
                opacity: 0,
            }),
        )),
    ]),
    trigger('slideAnim', [
        state('in', style({
            opacity: 1,
            height: '*',
            'padding-top': '*',
            'padding-bottom': '*',
            'margin-top': '*',
            'margin-bottom': '*',
        })),
        transition(':enter', [
            style({
                opacity: 0,
                height: '0rem',
                'padding-top': '0',
                'padding-bottom': '0',
                'margin-top': '0',
                'margin-bottom': '0',
            }),
            animate(100),
        ]),
        transition(':leave',
            animate(100, style({
                opacity: 0,
                height: '0rem',
                'padding-top': '0',
                'padding-bottom': '0',
                'margin-top': '0',
                'margin-bottom': '0',
            }),
        )),
    ]),
];
