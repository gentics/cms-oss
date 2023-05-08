import { ChangeDetectionStrategy, Component } from '@angular/core';
import { IDocumentation } from '../../common/docs';
import { InjectDocumentation } from '../../common/docs-loader';

@Component({
    templateUrl: './split-button-demo.component.html',
    styleUrls: ['./split-button-demo.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SplitButtonDemoPage {

    @InjectDocumentation('split-button.component')
    documentation: IDocumentation;

    saveResult: string;

    secondaryActions: string[] = [];
    actionClicked: number;

    buttonIsDisabled = false;
    secondaryActionIsDisabled = false;
    clickCount = 0;

    save(): void {
        this.saveResult = 'Save clicked!';
    }

    saveAndPublish(): void {
        this.saveResult = 'Save and Publish clicked!';
    }

    saveAndEmail(): void {
        this.saveResult = 'Save and Send via E-Mail';
    }

    addSecondaryAction(): void {
        this.secondaryActions.push('Secondary Action');
    }

    removeSecondaryAction(): void {
        this.secondaryActions.pop();
    }

    onActionClick(actionId: number): void {
        this.actionClicked = actionId;
    }

}
