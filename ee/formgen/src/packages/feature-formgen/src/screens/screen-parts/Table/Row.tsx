import { useHoverStyle } from '@andp/core/src/AndpUtils';
import AndpText from '@andp/core/src/components/AndpText';
import { expertSearchTableTheme } from '@andp/core/src/styles/theme';
import { Pressable, View } from 'react-native';
import Animated from 'react-native-reanimated';
import CheckBox from './Checkbox';
import React, { JSX, useMemo } from 'react';
import { IReportResultHeaderColumn } from '../../../types/interfaces';

/**
 * Props for the Row component.
 *
 * @property {any[]} item - The data array for this row, where each element corresponds to a cell value.
 * @property {number} index - The zero-based index of this row within the table.
 * @property {number[]} colWidths - Array of pixel widths for each visible column.
 * @property {IReportResultHeaderColumn[]} headerColumns - Definitions for all columns, used to render header and cell types.
 * @property {number[]} visibleColumns - Indices of columns currently visible for this row.
 * @property {(r: number) => boolean} isChecked - Callback that returns whether the row at index `r` is selected.
 * @property {(r: number) => void} toggleRow - Callback to toggle the selection state of row `r`.
 * @property {(id: string, param: string) => void} triggerFormEndpoint - Handler invoked for button-type cells with the cell's `buttonID` and `parameter`.
 * @property {boolean} isMultiSelect - Flag indicating if multi-select mode is active.
 * @property {any} styles - Style object containing row and cell styles.
 */
export interface RowProps {
  item: any[];
  index: number;
  colWidths: number[];
  headerColumns: IReportResultHeaderColumn[];
  visibleColumns: number[];
  isChecked: (r: number) => boolean;
  toggleRow: (r: number) => void;
  triggerFormEndpoint: (id: string, param: string) => void;
  isMultiSelect: boolean;
  styles: any;
}

/**
 * Row
 * ----------------------------------------
 * Renders a single data row in the ExpertSearchTable, applying hover animations,
 * optional multi-select checkbox, and custom cell rendering (including button cells).
 * Uses react-native-reanimated for hover effects and Pressable for interactions.
 *
 * @component
 * @param {RowProps} props - Properties to configure the row's behavior and appearance.
 * @returns {JSX.Element} The rendered row element.
 */
const Row: React.FC<RowProps> = React.memo(({
  item,
  index,
  colWidths,
  headerColumns,
  visibleColumns,
  isChecked,
  toggleRow,
  triggerFormEndpoint,
  isMultiSelect,
  styles,
}: RowProps): JSX.Element => {
  // Retrieving only once here in order to avoid repetitive Objekt-Lookups in the .map Funktion
  const { borderWidth } = expertSearchTableTheme.table.cell;

  const allCellStyles = useMemo(
    () =>
      visibleColumns.map((_, i) => [
        styles.cell,
        { width: colWidths[i] },
        (isMultiSelect || i > 0) && { borderLeftWidth: borderWidth },
      ]),
    [colWidths, visibleColumns, isMultiSelect],
  );

  // +-------------------------------------------------------+
  // | Animation variables for hover effects for a table Row |
  // +-------------------------------------------------------+
  const {
    animatedStyle: cellHoverStyle,
    onHoverIn: cellHoverIn,
    onHoverOut: cellHoverOut,
  } = useHoverStyle(
    styles.row,
    styles.rowOnHover,
    expertSearchTableTheme.table.row.hoverTransitionDuration,
  );
  // +-------------------------------------------------------+

  // +-------------------------------------------------------------+
  // | Animation variables for hover effects for a Button Cell Row |
  // +-------------------------------------------------------------+
  const {
    animatedStyle: cellButtonHoverStyle,
    onHoverIn,
    onHoverOut,
  } = useHoverStyle(
    styles.buttonText,
    styles.buttonOnHover,
    expertSearchTableTheme.table.buttonCell.hover.transitionDuration,
  );
  // +-------------------------------------------------------+
  return (
    <Pressable
      id={`expert-search-result-row-${index}`}
      onHoverIn={cellHoverIn}
      onHoverOut={cellHoverOut}
      // @ts-ignore
      style={{cursor: isMultiSelect ? 'pointer' : 'default'}}
      onPress={isMultiSelect ? () => toggleRow(index) : undefined}
    >
      <Animated.View style={[cellHoverStyle]}>
        {isMultiSelect && (
          <View style={styles.rowSelect}>{CheckBox(isChecked(index))}</View>
        )}
        {visibleColumns.map((colIndex, colPos) => {
          const cell = item[colIndex];
          const col = headerColumns[colIndex];
          if (col.type === 'button' && cell?.buttonID) {
            return (
              <Pressable
                id={`expert-search-result-row-${index}-btn-${cell.buttonID}`}
                key={colIndex}
                style={allCellStyles[colPos]}
                onHoverIn={onHoverIn}
                onHoverOut={onHoverOut}
                onPress={() =>
                  triggerFormEndpoint(cell.buttonID, cell.parameter)
                }
              >
                <Animated.Text
                  numberOfLines={1}
                  ellipsizeMode="tail"
                  style={cellButtonHoverStyle}
                >
                  {col.name}
                </Animated.Text>
              </Pressable>
            );
          }
          return (
            <AndpText
              key={colIndex}
              numberOfLines={1}
              ellipsizeMode="tail"
              style={allCellStyles[colPos]}
            >
              {cell}
              {/* ATENTION: Removed following line with ANDP-476 in order to imporve Performance (JSON.stringify is expensive for each Row re-render) and replaced it with the above line */}
              {/* I'm am not sure if we actually need to check for the type object - I don't think that the value of a table cell can ever be an object (Don't think AN supports that) */}
              {/* {typeof cell === 'object' ? JSON.stringify(cell) : cell} */}
            </AndpText>
          );
        })}
      </Animated.View>
    </Pressable>
  );
});

export default Row;
