import {
    ChangeDetectionStrategy,
    Component,
    Input,
} from '@angular/core';
import { FormControl, FormGroup } from '@angular/forms';
import { BasePropertiesComponent } from '@gentics/cms-components';
import { NodeFeature, NodeFeatureModel } from '@gentics/cms-models';
import { FormProperties, generateFormProvider, generateValidatorProvider } from '@gentics/ui-core';

export type NodeFeaturesFormData = Partial<Record<NodeFeature, boolean>>;

/**
 * Creates a dynamic form based on `availableFeatures` and shows a checkbox for
 * each avaiable node feature.
 */
@Component({
    selector: 'gtx-node-features',
    templateUrl: './node-features.component.html',
    styleUrls: ['./node-features.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    providers: [
        generateFormProvider(NodeFeaturesComponent),
        generateValidatorProvider(NodeFeaturesComponent),
    ],
})
export class NodeFeaturesComponent extends BasePropertiesComponent<NodeFeaturesFormData> {

    public readonly NodeFeature = NodeFeature;

    /** An array of all node features that are available. */
    @Input()
    public availableFeatures: NodeFeatureModel[];

    protected createForm(): FormGroup<FormProperties<NodeFeaturesFormData>> {
        const controls: FormProperties<NodeFeaturesFormData> = {};
        this.availableFeatures.forEach(feat => {
            controls[feat.id] = new FormControl(this.value?.[feat.id]);
        });

        return new FormGroup(controls);
    }

    protected configureForm(value: NodeFeaturesFormData, loud?: boolean): void {
        // no-op
    }

    protected assembleValue(value: NodeFeaturesFormData): NodeFeaturesFormData {
        return value;
    }
}
