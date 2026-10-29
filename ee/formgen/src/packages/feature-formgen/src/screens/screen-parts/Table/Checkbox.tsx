import AndpIcon from '@andp/core/src/components/AndpIcon';
import { expertSearchTableTheme } from '@andp/core/src/styles/theme';

/**
 * CheckBox
 * ----------------------------------------
 * Renders a checkbox icon based on the checked state.
 * Uses AndpIcon to display either a check mark or an unchecked square.
 *
 * @param {boolean} isCheckedParam - Whether the checkbox is checked.
 * @returns {JSX.Element} The appropriate checkbox icon.
 */
const CheckBox = (isCheckedParam: boolean) =>
  isCheckedParam ? (
    <AndpIcon
      name="checkMark"
      color={expertSearchTableTheme.table.headerCell.checkboxCell.checkedColor}
    />
  ) : (
    <AndpIcon
      name="uncheckedSquare"
      color={
        expertSearchTableTheme.table.headerCell.checkboxCell.uncheckedColor
      }
    />
  );

export default CheckBox;
