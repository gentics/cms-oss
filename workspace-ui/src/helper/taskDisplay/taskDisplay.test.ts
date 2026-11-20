import { describe, expect, it } from 'vitest';

import i18n from '@/i18n';
import { taskFixture } from '@/test/cmsTasks';

import { taskMeta, taskStartParts, taskTitle } from './taskDisplay';

const t = i18n.getFixedT('en');

const brokenLinks = taskFixture({
    id: 'linkChecker:page:7710',
    source: 'linkChecker',
    type: 'fixBrokenLinks',
    title: undefined,
    objects: [{ type: 'page', id: 7710, nodeId: 3, name: 'Directions' }],
    details: { brokenCount: 2, links: [{ id: 1, url: 'https://example.com/a.pdf' }, { id: 2, url: 'https://example.org/park' }] },
});

describe('taskDisplay', () => {
    it('names a task by its title, else by its type and page', () => {
        expect(taskTitle(taskFixture({ id: '1', title: 'Proofread' }), t)).toBe('Proofread');
        expect(taskTitle(brokenLinks, t)).toBe('Fix the links on Directions');
    });

    it('says who asked and when it is due for a publish queue task', () => {
        const task = taskFixture({
            id: 'publishQueue:page:9142',
            source: 'publishQueue',
            type: 'approvePublish',
            // noon UTC this year: Nov 20 from UTC-11 to UTC+11, and shown without the year
            dueAt: Date.UTC(new Date().getFullYear(), 10, 20, 12) / 1000,
            details: { requestedBy: { id: 42, name: 'Example Erika' } },
        });

        expect(taskMeta(task, t, 'en')).toBe('Asked by Example Erika · Due Nov 20');
    });

    it('counts the broken links and says who took a group task', () => {
        expect(taskMeta(brokenLinks, t, 'en')).toBe('2 broken links');

        const claimed = taskFixture({
            id: '3',
            assignee: { type: 'user', id: 5, name: 'Chief One' },
            assigneeGroup: { type: 'group', id: 9, name: 'Chief CC' },
        });

        expect(taskMeta(claimed, t, 'en')).toBe('Taken by Chief One');
        expect(taskMeta(taskFixture({ id: '4', assignee: { type: 'group', id: 9, name: 'Chief CC' } }), t, 'en')).toBe('For Chief CC');
    });

    it('starts a session with the objects as references and the broken URLs in the instruction', () => {
        expect(taskStartParts(brokenLinks, t)).toEqual([
            { type: 'reference', ref: { type: 'page', id: '7710', node_id: 3, label: 'Directions' } },
            { type: 'text', text: 'Fix the broken links on this page:\nhttps://example.com/a.pdf\nhttps://example.org/park' },
        ]);
    });

    it('takes the instruction a task brings for the agent over its title', () => {
        const task = taskFixture({ id: '5', title: 'New tag type', genaix: { instruction: 'Create a tag type for a price table.' } });

        expect(taskStartParts(task, t)).toEqual([{ type: 'text', text: 'Create a tag type for a price table.' }]);
    });
});
