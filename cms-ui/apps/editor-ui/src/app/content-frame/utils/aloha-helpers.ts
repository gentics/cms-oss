import { TAG_ALIASES } from '@editor-ui/app/common/models/aloha-integration';
import { AlohaDOM, AlohaEditable, AlohaRangeObject } from '@gentics/aloha-models';

export interface ToggleOptions {
    allowAdd: boolean;
    allowRemove: boolean;
    skipAliases?: boolean;
}

export interface FormatApplyResult {
    added: boolean;
    format: string;
}

export function findAndToggleFormats(
    currentRange: AlohaRangeObject,
    dom: AlohaDOM,
    activeEditable: AlohaEditable,
    jQueryInstance: JQueryStatic,
    nodeNames: string[],
    options: ToggleOptions,
): FormatApplyResult[] {
    const appliedFormats: FormatApplyResult[] = [];

    for (const nameToCheck of nodeNames) {
        const alias = options.skipAliases ? null : TAG_ALIASES[nameToCheck];
        const nodeName = alias ?? nameToCheck;

        const foundMarkup = currentRange.findMarkup(function() {
            return this.nodeName === nameToCheck || (alias != null && this.nodeName === alias);
        }, activeEditable.obj);

        // Push/Splice can't be used, as the change detection isn't triggered, because it's still
        // the same array. Even with `markForCheck`, it simply doesn't re-run the includes pipe.
        if (!foundMarkup) {
            if (options.allowAdd) {
                dom.addMarkup(currentRange, jQueryInstance(`<${nodeName}>`));
                if (activeEditable) {
                    activeEditable.smartContentChange({type: 'block-change'});
                }
                appliedFormats.push({ added: true, format: nodeName });
            }
        } else if (options.allowRemove) {
            // remove the markup
            if (currentRange.isCollapsed()) {
                // when the range is collapsed, we remove exactly the one DOM element
                dom.removeFromDOM(foundMarkup, currentRange, true);
            } else {
                // the range is not collapsed, so we remove the markup from the range
                dom.removeMarkup(currentRange, jQueryInstance(foundMarkup), activeEditable.obj);
            }
            appliedFormats.push({ added: false, format: nodeName });
        }
    }

    return appliedFormats;
}
