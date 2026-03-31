import { Component, OnInit, Input } from '@angular/core';

@Component({
    selector: 'app-boolean-type',
    template: `
    <ng-container *ngIf="shouldDisplay()">
      <div style="margin-top: 15px;">
        <div *ngIf="options.text" >
          <span [innerHTML]="options.text"></span>
          <br>
        </div>
        <gtx-checkbox *ngIf="element && options" [(ngModel)]="element.formGridOptions[options.key]" [label]="options.name"></gtx-checkbox>
      </div>
    </ng-container>
  `,
})
export class BooleanTypeComponent implements OnInit {
    @Input() element: any;
    @Input() options: any;
    @Input() schema: any;
    @Input() mesh: any;

    ngOnInit() {
        console.log('BooleanTypeComponent initialized with options:', this.options);
        console.log('Element:', this.element);
    }

    shouldDisplay(): boolean {
        return this.evaluateConstraint(this.options.constraint);
    }

    evaluateConstraint(constraint: string): boolean {
        if (!constraint) return true;
        try {
            // Create a function that uses the current context
            const constraintFunction = new Function('schema', 'element', 'mesh', `with (this) { return ${constraint}; }`);
            // Bind the function to the current context (this)
            return constraintFunction.call(this, this.schema, this.element, this.mesh);
        } catch (error) {
            console.error('Error evaluating constraint:', error);
            return false;
        }
    }

}
