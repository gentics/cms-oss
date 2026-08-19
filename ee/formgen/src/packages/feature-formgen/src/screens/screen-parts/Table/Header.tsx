import AndpIcon from '@andp/core/src/components/AndpIcon';
import AndpText from '@andp/core/src/components/AndpText';
import { Pressable, View } from 'react-native';
import Animated from 'react-native-reanimated';
import Resizer from './Resizer';
import { useHoverStyle } from '@andp/core/src/AndpUtils';
import { expertSearchTableTheme } from '@andp/core/src/styles/theme';
import CheckBox from './Checkbox';
import { IReportResultHeaderColumn } from '../../../types/interfaces';

/**
 * Props for the Header component.
 *
 * @property {number[]} visibleColumns - The zero-based indices of columns currently visible in the table.
 * @property {IReportResultHeaderColumn[]} headerColumns - Definitions for all columns (names, types) used to render headers.
 * @property {number[]} colWidths - Widths for each visible column.
 * @property {string[]} sortDirection - Current sort directions ('asc' or 'desc') per visible column.
 * @property {boolean} isMultiSelect - Whether multi-select mode is enabled.
 * @property {boolean} isMultiSelectChecked - Whether the multi-select checkbox is ticked or not
 * @property {any} styles - StyleSheet object containing header and cell styles.
 * @property {() => void} toggleAll - Callback to toggle all row selections on or off.
 * @property {(colIdx: number) => void} sortTable - Callback invoked to sort by a given column index.
 * @property {(idx: number, pageX: number) => void} startResize - Callback invoked when a column resize starts, with column index and initial pointer X position.
 */
interface HeaderProps {
  visibleColumns: number[];
  headerColumns: IReportResultHeaderColumn[];
  colWidths: number[];
  isMultiSelectChecked: boolean;
  sortDirection: string[];
  isMultiSelect: boolean;
  styles: any;
  toggleAll: () => void;
  sortTable: (colIdx: number) => void;
  startResize: (idx: number, pageX: number) => void;
}

/**
 * Props for a single header cell.
 *
 * @property {IReportResultHeaderColumn} col - Definition of the column this cell renders.
 * @property {number} idx - Position of the column within `visibleColumns` (not the raw column index).
 * @property {any} cellStyle - Precalculated width/border style for this cell.
 * @property {boolean} showResizer - Whether a resize handle is rendered after this cell.
 * @property {string | undefined} sortDirection - Current sort direction ('asc' or 'desc') for this cell.
 * @property {any} styles - StyleSheet object containing header and cell styles.
 * @property {(colIdx: number) => void} sortTable - Callback invoked to sort by this column.
 * @property {(idx: number, pageX: number) => void} startResize - Callback invoked when a column resize starts.
 */
interface HeaderCellProps {
  col: IReportResultHeaderColumn;
  idx: number;
  cellStyle: any;
  showResizer: boolean;
  sortDirection?: string;
  styles: any;
  sortTable: (colIdx: number) => void;
  startResize: (idx: number, pageX: number) => void;
}

/**
 * HeaderCell
 * ----------------------------------------
 * Renders a single header cell including its hover animation, sort control
 * and (optionally) the resize handle.
 *
 * @component
 * @param {HeaderCellProps} props - Properties to configure the cell.
 * @returns {JSX.Element} The rendered header cell.
 */
const HeaderCell: React.FC<HeaderCellProps> = ({
  col,
  idx,
  cellStyle,
  showResizer,
  sortDirection,
  styles,
  sortTable,
  startResize,
}: HeaderCellProps) => {
  // +------------------------------------------------------------+
  // | Animation variables for hover effects for the header cells |
  // +------------------------------------------------------------+
  const { animatedStyle, onHoverIn, onHoverOut } = useHoverStyle(
    styles.headerCell,
    styles.headerCellOnHover,
    expertSearchTableTheme.table.headerCell.hoverTransitionDuration,
  );
  // +-------------------------------------------------------+
  return (
    <Animated.View style={[cellStyle, animatedStyle]}>
      <Pressable
        onHoverIn={onHoverIn}
        onHoverOut={onHoverOut}
        onPress={col.type !== 'button' ? () => sortTable(idx) : undefined}
        style={styles.headerPressable}
      >
        <AndpText
          numberOfLines={1}
          ellipsizeMode="tail"
          style={styles.headerText}
        >
          {col.name}
        </AndpText>
        {sortDirection && col.type !== 'button' && (
          <AndpIcon
            name={sortDirection === 'asc' ? 'arrowUp' : 'arrowDown'}
            size={expertSearchTableTheme.table.headerCell.iconSize}
            color={expertSearchTableTheme.table.headerCell.iconColor}
          />
        )}
      </Pressable>
      {showResizer && (
        <Resizer idx={idx} styles={styles} onStartResize={startResize} />
      )}
    </Animated.View>
  );
};

/**
 * Header
 * ----------------------------------------
 * Renders the header row for the ExpertSearchTable, showing column titles,
 * optional multi-select checkbox, sort controls, and resize handles.
 * Uses Pressable for sorting actions and the Resizer component to adjust column widths.
 *
 * @component
 * @param {HeaderProps} props - Properties to configure header behavior and appearance.
 * @returns {JSX.Element} The rendered header row element.
 */
const Header: React.FC<HeaderProps> = ({
  visibleColumns,
  headerColumns,
  colWidths,
  isMultiSelectChecked,
  sortDirection,
  styles,
  toggleAll,
  sortTable,
  startResize,
  isMultiSelect,
}: HeaderProps): JSX.Element => {
  return (
    <View style={styles.headerRow}>
      {isMultiSelect && (
        <Pressable style={styles.headerSelectBoxCell} onPress={toggleAll}>
          {CheckBox(isMultiSelectChecked)}
        </Pressable>
      )}
      {visibleColumns.map((colIndex, idx) => (
        <HeaderCell
          key={colIndex}
          col={headerColumns[colIndex]}
          idx={idx}
          // colWidths is indexed by the position within visibleColumns,
          // not by the raw column index of headerColumns.
          cellStyle={[
            { width: colWidths[idx] },
            (idx > 0 || isMultiSelect) && {
              borderLeftWidth:
                expertSearchTableTheme.table.headerCell.borderWidth,
            },
          ]}
          showResizer={idx < visibleColumns.length - 1}
          sortDirection={sortDirection[idx]}
          styles={styles}
          sortTable={sortTable}
          startResize={startResize}
        />
      ))}
    </View>
  );
};

export default Header;
