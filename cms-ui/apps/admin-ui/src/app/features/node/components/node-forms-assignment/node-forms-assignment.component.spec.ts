import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NodeFormsAssignmentComponent } from './node-forms-assignment.component';

describe('NodeFormsAssignmentComponent', () => {
    let component: NodeFormsAssignmentComponent;
    let fixture: ComponentFixture<NodeFormsAssignmentComponent>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            imports: [NodeFormsAssignmentComponent],
        }).compileComponents();

        fixture = TestBed.createComponent(NodeFormsAssignmentComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });
});
