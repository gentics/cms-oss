import React from 'react';
import { View } from 'react-native';
import {
  CommonValidation,
  FormState,
  IFormComponentAttributes,
  IFormGridOptions,
} from '../../../types/interfaces';
import * as validationUtils from '../../../utils/ValidationUtils';
import { ElementRenderer } from '../../../screens/essentials/ElementRenderer';

/**
 * CmsContainerElement renders a group of child elements via `ElementRenderer`.
 *
 * In the schema, both plain groups and when-then conditionals share the type `"container"` —
 * the only difference is whether a condition is defined in the element's schema.
 * This component treats both identically: it simply renders whatever children are passed in.
 *
 * Domain/project-specific behavior:
 * - Delegates child rendering entirely to `ElementRenderer` with `isAggregate={true}` so that
 *   the inner `Row` uses the aggregate layout path (no additional `formWidth` constraint),
 *   allowing children to fill the container's full width based on their own `numberOfColumns`.
 * - Supports unlimited nesting: a child can itself be a `"container"`, creating mutual recursion
 *   between `CmsContainerElement` and `ElementRenderer`.
 * - No aggregate logic: no add/delete rows, no table view, no pagination, no Reanimated.
 * - `getSummaryValue` returns `''` — the container has no own value; each child renders its own summary.
 * - `validate` delegates to `commonValidate` (the container itself has no validation logic).
 */
function CmsContainerElement(props: IFormComponentAttributes) {
  return (
    <View>
      <ElementRenderer
        {...props}
        elements={props.elements}
        jsonpath={props.jsonpath}
        parentId={props.id}
        isAggregate={true}
      />
    </View>
  );
}

CmsContainerElement.validate = function (
  state: FormState,
  jsonpath: string,
  id: string,
  updateErrorMessages: boolean,
  _t: any,
  formGridOptions?: IFormGridOptions,
  localValue?: any,
): CommonValidation {
  return validationUtils.commonValidate(
    state,
    jsonpath,
    id,
    updateErrorMessages,
    formGridOptions,
    localValue,
  );
};

CmsContainerElement.getSummaryValue = function () {
  return '';
};

export default CmsContainerElement;
