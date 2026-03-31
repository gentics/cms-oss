import { Component, OnInit, Input } from '@angular/core';

@Component({
  selector: 'app-dropdown-type',
  template: `
  <ng-container *ngIf="shouldDisplay()">
    <div style="margin-top: 15px;">
      <div *ngIf="options.text">
         <span [innerHTML]="options.text"></span>
          <br>
      </div>
      <span>{{ options.name }}</span>
      <select label="Dropdown" [(ngModel)]="element.formGridOptions[options.key]" class="fg-select float-left">
         <option *ngFor="let opt of options.selectOptions" [value]="opt.value">{{ opt.key }}</option>
      </select>
    </div>
  </ng-container>

  `,
})
export class DropdownTypeComponent implements OnInit {
  @Input() element: any;
  @Input() options: any;
  @Input() schema: any;
  @Input() mesh: any;

  ngOnInit() {
    console.log('DropdownTypeComponent initialized with options:', this.options);
    console.log('Element:', this.element);
  }

shouldDisplay(): boolean {
    return this.evaluateConstraint(this.options.constraint);
  }

 evaluateConstraint(constraint: string): boolean {
     if (!constraint) return true;
     try {
         const constraintFunction = new Function('schema', 'element', 'mesh', `with (this) { return ${constraint}; }`);
         return constraintFunction.call(this, this.schema, this.element, this.mesh);
     } catch (error) {
         console.error('Error evaluating constraint:', error);
         return false;
     }
 }
}
