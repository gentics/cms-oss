import { parseCopilotYaml } from './copilot-yaml.parser';

describe('parseCopilotYaml', () => {

    it('returns the disabled default for an empty document', () => {
        const cfg = parseCopilotYaml('');
        expect(cfg.enabled).toBe(false);
        expect(cfg.actions).toEqual([]);
    });

    it('parses a single boolean key', () => {
        const cfg = parseCopilotYaml('enabled: true');
        expect(cfg.enabled).toBe(true);
        expect(cfg.actions).toEqual([]);
    });

    it('accepts the alternative truthy spellings yes/on', () => {
        expect(parseCopilotYaml('enabled: yes').enabled).toBe(true);
        expect(parseCopilotYaml('enabled: on').enabled).toBe(true);
    });

    it('accepts the inline empty-list shorthand', () => {
        const cfg = parseCopilotYaml('enabled: true\nactions: []');
        expect(cfg.enabled).toBe(true);
        expect(cfg.actions).toEqual([]);
    });

    it('parses a list of actions with all supported fields', () => {
        const yaml = [
            'enabled: true',
            'actions:',
            '    - id: summarize',
            '      label: Zusammenfassen',
            '      icon: summarize',
            '      description: kurz halten',
            '      prompt: Fasse zusammen',
            '    - id: translate',
            '      label: \'Übersetzen\'',
            '      icon: translate',
        ].join('\n');

        const cfg = parseCopilotYaml(yaml);

        expect(cfg.enabled).toBe(true);
        expect(cfg.actions.length).toBe(2);
        expect(cfg.actions[0]).toEqual({
            id: 'summarize',
            label: 'Zusammenfassen',
            icon: 'summarize',
            description: 'kurz halten',
            prompt: 'Fasse zusammen',
        });
        expect(cfg.actions[1]).toEqual({
            id: 'translate',
            label: 'Übersetzen',
            icon: 'translate',
        });
    });

    it('strips trailing line comments', () => {
        const cfg = parseCopilotYaml('enabled: true  # toggle');
        expect(cfg.enabled).toBe(true);
    });

    it('falls back to the default when an action is missing the required id', () => {
        const yaml = [
            'enabled: true',
            'actions:',
            '    - icon: summarize',
            '      label: Zusammenfassen',
        ].join('\n');

        const cfg = parseCopilotYaml(yaml);

        expect(cfg.enabled).toBe(false);
        expect(cfg.actions).toEqual([]);
    });

    it('falls back to the default when an action is missing the required label', () => {
        const yaml = [
            'enabled: true',
            'actions:',
            '    - id: summarize',
            '      icon: summarize',
        ].join('\n');

        const cfg = parseCopilotYaml(yaml);

        expect(cfg.enabled).toBe(false);
        expect(cfg.actions).toEqual([]);
    });

    it('ignores unknown top-level keys without failing the whole load', () => {
        const yaml = [
            'enabled: true',
            'unknownKey: whatever',
            'actions: []',
        ].join('\n');

        const cfg = parseCopilotYaml(yaml);

        expect(cfg.enabled).toBe(true);
        expect(cfg.actions).toEqual([]);
    });
});
