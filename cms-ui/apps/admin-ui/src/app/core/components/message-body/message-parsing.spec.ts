import {matchPlaceholder, parseMessage, placeholderStringToRegExp} from './message-parsing';


describe('Message parsing', () => {

    describe('parseMessage()', () => {

        it('passes through a normal message', () => {
            let result = parseMessage('A normal message');
            expect(result.links).toEqual([]);
            expect(result.textAfterLinks).toEqual('A normal message');

            result = parseMessage('Eine normale Nachricht');
            expect(result.links).toEqual([]);
            expect(result.textAfterLinks).toEqual('Eine normale Nachricht');
        });

        it('parses "translation master of page has changed" messages', () => {
            let text = 'The translation master GCN5 Demo/Home/Willkommen (49) of page GCN5 Demo/Home/Welcome (50) has changed.';
            let result = parseMessage(text);
            expect(result.links).toEqual([
                {
                    textBefore: 'The translation master ',
                    type: 'page',
                    id: 49,
                    name: 'Willkommen',
                    nodeName: 'GCN5 Demo',
                    fullPath: 'GCN5 Demo/Home/Willkommen'
                }, {
                    textBefore: ' of page ',
                    type: 'page',
                    id: 50,
                    name: 'Welcome',
                    nodeName: 'GCN5 Demo',
                    fullPath: 'GCN5 Demo/Home/Welcome'
                }
            ]);
            expect(result.textAfterLinks).toBe(' has changed.');

            text = 'Die Übersetzungsvorlage GCN5 Demo/Home/Willkommen (49) der Seite GCN5 Demo/Home/Welcome (50) hat sich geändert.';
            result = parseMessage(text);
            expect(result.links).toEqual([
                {
                    textBefore: 'Die Übersetzungsvorlage ',
                    type: 'page',
                    id: 49,
                    name: 'Willkommen',
                    nodeName: 'GCN5 Demo',
                    fullPath: 'GCN5 Demo/Home/Willkommen'
                }, {
                    textBefore: ' der Seite ',
                    type: 'page',
                    id: 50,
                    name: 'Welcome',
                    nodeName: 'GCN5 Demo',
                    fullPath: 'GCN5 Demo/Home/Welcome'
                }
            ]);
            expect(result.textAfterLinks).toBe(' hat sich geändert.');
        });

        it('parses "Page has been taken into revision" messages (en)', () => {
            let text = 'The page GCN5 Demo/Home/Welcome (50) has been taken into revision.';
            let result = parseMessage(text);
            expect(result.links).toEqual([
                {
                    textBefore: 'The page ',
                    type: 'page',
                    id: 50,
                    name: 'Welcome',
                    nodeName: 'GCN5 Demo',
                    fullPath: 'GCN5 Demo/Home/Welcome'
                }
            ]);
            expect(result.textAfterLinks).toBe(' has been taken into revision.');
        });

        it('parses "Page has been taken into revision" messages (de)', () => {
            let text = 'Die Seite GCN5 Demo/Home/Willkommen (49) wurde in Arbeit zurück gestellt.';
            let result = parseMessage(text);
            expect(result.links).toEqual([
                {
                    textBefore: 'Die Seite ',
                    type: 'page',
                    id: 49,
                    name: 'Willkommen',
                    nodeName: 'GCN5 Demo',
                    fullPath: 'GCN5 Demo/Home/Willkommen'
                }
            ]);
            expect(result.textAfterLinks).toBe(' wurde in Arbeit zurück gestellt.');
        });

        it('parses "Page has been taken into revision" messages with comment (en)', () => {
            let text = 'The page GCN5 Demo/Home/Welcome (50) has been taken into revision.\n\n--\n\nThis is a message.';
            let result = parseMessage(text);
            expect(result.links).toEqual([
                {
                    textBefore: 'The page ',
                    type: 'page',
                    id: 50,
                    name: 'Welcome',
                    nodeName: 'GCN5 Demo',
                    fullPath: 'GCN5 Demo/Home/Welcome'
                }
            ]);
            expect(result.textAfterLinks).toBe(' has been taken into revision.\n\n--\n\nThis is a message.');
        });

        it('parses "Page has been taken into revision" messages with comment (de)', () => {
            let text = 'Die Seite GCN5 Demo/Home/Willkommen (49) wurde in Arbeit zurück gestellt.\n\n--\n\nThis is a message.';
            let result = parseMessage(text);
            expect(result.links).toEqual([
                {
                    textBefore: 'Die Seite ',
                    type: 'page',
                    id: 49,
                    name: 'Willkommen',
                    nodeName: 'GCN5 Demo',
                    fullPath: 'GCN5 Demo/Home/Willkommen'
                }
            ]);
            expect(result.textAfterLinks).toBe(' wurde in Arbeit zurück gestellt.\n\n--\n\nThis is a message.');
        });

        it('Can parse page links in arbitrary messages', () => {
            // Actual message from a live server
            const message = `Der Link-Tag "gtxalohapagelink1" in der Seite "Projektname Branchen/Ein Unterordner/Aktuelles/test" (12345)` +
                            ` ist nicht mehr gültig, da die Zielseite "Projektname Branchen/Ein Unterordner/Aktuelles/test2" (54321)` +
                            ` gelöscht wurde.`;
            const nodes = [
                { id:  1, name: 'Projektname' },
                { id:  2, name: 'Projektname Branchen' }
            ];

            let result = parseMessage(message, nodes);
            expect(result.links.length).toBe(2);
            expect(result.links[0]).toEqual(jasmine.objectContaining({
                textBefore: 'Der Link-Tag "gtxalohapagelink1" in der Seite ',
                type: 'page',
                id: 12345,
                name: 'test',
                nodeName: 'Projektname Branchen',
                fullPath: 'Projektname Branchen/Ein Unterordner/Aktuelles/test'
            }));
            expect(result.links[1]).toEqual(jasmine.objectContaining({
                textBefore: ' ist nicht mehr gültig, da die Zielseite ',
                type: 'page',
                id: 54321,
                name: 'test2',
                nodeName: 'Projektname Branchen',
                fullPath: 'Projektname Branchen/Ein Unterordner/Aktuelles/test2'
            }));
            expect(result.textAfterLinks).toBe(' gelöscht wurde.');
        });

        it('Can parse messages with no subfolders in the path', () => {
            // GCU-344 - "node/folder/page" worked, "node/page" did not
            const message = `Der Link-Tag "gtxalohapagelink1" in der Seite "GCN5 Demo/t1" (144)` +
                            ` ist nicht mehr gültig, da die Zielseite "GCN5 Demo/t2" (145) vom Server genommen wurde.`;
            const nodes = [
                { id:  1, name: 'GCN5 Demo' }
            ];

            let result = parseMessage(message, nodes);
            expect(result.links.length).toBe(2);
            expect(result.links[0]).toEqual(jasmine.objectContaining({
                textBefore: 'Der Link-Tag "gtxalohapagelink1" in der Seite ',
                type: 'page',
                id: 144,
                name: 't1',
                nodeName: 'GCN5 Demo',
                fullPath: 'GCN5 Demo/t1'
            }));
            expect(result.links[1]).toEqual(jasmine.objectContaining({
                textBefore: ' ist nicht mehr gültig, da die Zielseite ',
                type: 'page',
                id: 145,
                name: 't2',
                nodeName: 'GCN5 Demo',
                fullPath: 'GCN5 Demo/t2'
            }));
            expect(result.textAfterLinks).toBe(' vom Server genommen wurde.');
        });

    });

    describe('matchPlaceholder()', () => {

        function canMatchAs(type: string, ...texts: string[]): void {
            for (let text of texts) {
                let result = matchPlaceholder(type, text);
                expect(result.isMatch).toBe(true, `Parsing "${text}" as ${type} failed.`);
            }
        }

        function canNotMatchAs(type: string, ...texts: string[]): void {
            for (let text of texts) {
                let result = matchPlaceholder(type, text);
                expect(result.isMatch).toBe(false, `Parsing "${text}" as ${type} succeeded, but should not.`);
            }
        }

        it('matches dates in varous representations', () => {
            canMatchAs('date',
                '1.10.2016',
                '16:44 AM',
                '2016-12-31',
                '12/31/2016'
            );

            canNotMatchAs('date',
                '',
                ' ',
                'wtf',
                '15h20'
            );
        });

        it('matches folder names', () => {
            canMatchAs('folder',
                'Some folder name',
                '(Folder name)',
                '[]$&!%"§$/()=',
                ' ' // yes, really.
            );

            canNotMatchAs('folder', '');
        });

        it('matches node names', () => {
            canMatchAs('node',
                'Some node name',
                '(Node name)',
                '[]$&!%"§$/()=',
                ' ' // yes, really.
            );

            canNotMatchAs('node', '');
        });

        it('matches pages', () => {
            canMatchAs('page',
                'Node/Some page name (1)',
                '(1) Node/Some page name',
                'Node name with spaces/folder with spaces/page name (1)',
                '(1) Node name with spaces/folder with spaces/page name',
                'Node/[]$&!%"§$/()= (1)',
                ' /  (1)' // yes, really.
            );

            canNotMatchAs('page',
                '',
                'Node/Page name without ID',
                'Node/Page',
                'ID but no node (1)',
                '(2)'
            );
        });

        it('matches pages and returns the matched text', () => {
            let result = matchPlaceholder('page', 'Node name/Page name (1)');
            expect(result.isMatch).toBe(true);
            expect(result.matches).toEqual([
                'Node name/Page name',
                'Node name',
                'Page name',
                '1',
                undefined,
                undefined,
                undefined,
                undefined
            ]);

            result = matchPlaceholder('page', '(1) Node name/Page name');
            expect(result.isMatch).toBe(true);
            expect(result.matches).toEqual([
                undefined,
                undefined,
                undefined,
                undefined,
                '1',
                'Node name/Page name',
                'Node name',
                'Page name'
            ]);
        });

        it('matches user names', () => {
            canMatchAs('user',
                'John Doe',
                'Hans Wurst',
                '$ %',
                '   ' // yes, really.
            );

            canNotMatchAs('user',
                '',
                '  ',
                'John-Doe',
                ' - '
            );
        });

        it('matches messages', () => {
            canMatchAs('message', 'Hi! How are you doing?');
            canNotMatchAs('message', '');
        });

    });

    describe('placeholderStringToRegExp', () => {
        it('works for empty strings', () => {
            let result = placeholderStringToRegExp('');
            expect(result.placeholders).toEqual([]);
            expect(result.regExp).toEqual(/^()$/);
        });

        it('works for strings with no placeholders', () => {
            let result = placeholderStringToRegExp('No groups here!');
            expect(result.placeholders).toEqual([]);
            expect(result.regExp).toEqual(/^(No groups here!)$/);
        });

        it('works for strings with one placeholder', () => {
            let result = placeholderStringToRegExp('Hello {{user}}!');
            expect(result.placeholders).toEqual(['user']);
            expect(result.regExp).toEqual(/^(Hello )([\s\S]*?)(!)$/);
        });

        it('works for strings with two placeholders', () => {
            let result = placeholderStringToRegExp('User {{user}} wants to publish {{type}}!');
            expect(result.placeholders).toEqual(['user', 'type']);
            expect(result.regExp).toEqual(/^(User )([\s\S]*?)( wants to publish )([\s\S]*?)(!)$/);
        });

        it('works for strings with three placeholders', () => {
            let result = placeholderStringToRegExp('An {{fruit}} a {{timeunit}} keeps the {{occupation}} away!');
            expect(result.placeholders).toEqual(['fruit', 'timeunit', 'occupation']);
            expect(result.regExp).toEqual(/^(An )([\s\S]*?)( a )([\s\S]*?)( keeps the )([\s\S]*?)( away!)$/);
        });

        it('replaces RegExp special characters', () => {
            let result = placeholderStringToRegExp('Maybe (maybe!) // you want to escape this \\n ?$');
            expect(result.placeholders).toEqual([]);
            expect(result.regExp).toEqual(/^(Maybe \(maybe!\) \/\/ you want to escape this \n \?\$)$/);
        });

        it('ignores whitespace around placeholders', () => {
            let result = placeholderStringToRegExp('User {{ user }} wants to publish {{ type }}!');
            expect(result.placeholders).toEqual(['user', 'type']);
            expect(result.regExp).toEqual(/^(User )([\s\S]*?)( wants to publish )([\s\S]*?)(!)$/);
        });

        it('works for strings with newlines', () => {
            let result = placeholderStringToRegExp('{{user}}:\n\n--\n\n{{message}}');
            expect(result.placeholders).toEqual(['user', 'message']);
            expect(result.regExp).toEqual(/^()([\s\S]*?)(:\n\n--\n\n)([\s\S]*?)()$/);
        });

    });

});
