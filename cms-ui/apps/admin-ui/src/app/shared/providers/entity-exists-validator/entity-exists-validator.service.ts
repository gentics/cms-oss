import { AppStateService } from '@admin-ui/state';
import { Injectable } from '@angular/core';
import { AbstractControl, ValidationErrors, Validator } from '@angular/forms';
import { EntityIdType, Index, NormalizableEntityType, NormalizableEntityTypesMapBO, Normalized } from '@gentics/cms-models';

@Injectable()
export class EntityExistsValidator<T extends NormalizableEntityTypesMapBO<Normalized>[NormalizableEntityType]> implements Validator {

    /** Name of the entity */
    private entityIdentifier: NormalizableEntityType;

    /** Property of entity to check for duplication */
    private entityCheckProperty: keyof T;

    /** If set, the entity with this ID will be excluded from the checks.
     *  This is needed when editing an entity.
     *  In this case using the entity's original name should not trigger an error.
     */
    selfId?: number;

    constructor(
        private state: AppStateService,
    ) { }

    configure(entity: NormalizableEntityType, property: keyof T): void {
        this.entityIdentifier = entity;
        this.entityCheckProperty = property;
    }

    /**
     * Validator to check if the login name entered by user does already exist on CMS instance
     */
    validate = (control: AbstractControl): ValidationErrors | null => {
        if (!this.entityIdentifier || !this.entityCheckProperty) {
            throw new Error('Validator properties not configured');
        }
        const identifyingProperty = control.value;
        const entities = this.state.now.entity[this.entityIdentifier] as Index<EntityIdType, T>;
        if (entities) {
            const cond = Object.keys(entities).find((entityId: EntityIdType) => {
                return entities[entityId][this.entityCheckProperty] === identifyingProperty
                    && entityId !== this.selfId;
            });
            if (cond) {
                return { entityExists: true };
            } else {
                return null;
            }
        } else {
            return null;
        }
    }
}
