import { useHoverStyle } from '@andp/core/src/AndpUtils';
import AndpText from '@andp/core/src/components/AndpText';
import React from 'react';
import { Pressable, View } from 'react-native';
import Animated from 'react-native-reanimated';
import CheckBox from '../Checkbox';
import { expertSearchTableTheme } from '@andp/core/src/styles/theme';
import { IReportResultHeaderColumn } from '../../../../types/interfaces';

/**
 * Props for the Card component.
 *
 * @property {any[]} item - The data array for this card, where each element corresponds to a cell value.
 * @property {number} index - The zero-based index of this card/row.
 * @property {any} styles - StyleSheet object containing card and cell styles.
 * @property {boolean} isMultiSelect - Whether multi-select mode is enabled.
 * @property {IReportResultHeaderColumn[]} headerColumns - Definitions for all columns to render within the card.
 * @property {(r: number) => boolean} isChecked - Callback returning whether the given row index is selected.
 * @property {(r: number) => void} toggleRow - Callback to toggle selection state for the given row index.
 * @property {(id: string, param: string) => void} triggerFormEndpoint - Handler invoked when a button cell is pressed, receiving its `buttonID` and `parameter`.
 */
interface CardProps {
  item: any[];
  index: number;
  styles: any;
  isMultiSelect: boolean;
  headerColumns: IReportResultHeaderColumn[];
  isChecked: (r: number) => boolean;
  toggleRow: (r: number) => void;
  triggerFormEndpoint: (id: string, param: string) => void;
}

// Außerhalb des Card Components, damit nicht bei jedem render ein neues Komponent erzeugt wird
const AnimatedPressable = Animated.createAnimatedComponent(Pressable);

/**
 * Card
 * ----------------------------------------
 * Renders a card-style view for a single ExpertSearchTable row, showing the primary cell value,
 * an optional multi-select checkbox, and additional cells or action buttons with hover effects.
 *
 * @component
 * @param {CardProps} props - Properties to configure the card's data and interactions.
 * @returns {JSX.Element} The rendered card element.
 */
const Card: React.FC<CardProps> = ({ item, index, headerColumns, styles, isMultiSelect, isChecked, toggleRow, triggerFormEndpoint }) => {
  const cardCols = headerColumns
    .map((col, idx) => ({ col, idx }))
    .filter(
      ({ col, idx }) =>
        idx > 0 && !(col.type === 'button' && col.isMultiSelect),
    );
  const {
    animatedStyle: buttonHoverStyle,
    onHoverIn,
    onHoverOut,
  } = useHoverStyle(
    styles.cardButton,
    styles.cardButtonOnHover,
    expertSearchTableTheme.cardView.card.button.hover.transitionDuration,
  );
  return (
    <View style={styles.card}>
      <View style={styles.cardHeader}>
        <AndpText
          numberOfLines={1}
          ellipsizeMode="tail"
          style={styles.cardTitle}
        >
          {typeof item[0] === 'object' ? JSON.stringify(item[0]) : item[0]}
        </AndpText>
        {isMultiSelect && (
          <Pressable onPress={() => toggleRow(index)}>
            {CheckBox(isChecked(index))}
          </Pressable>
        )}
      </View>
      <View style={styles.cardDivider} />
      {cardCols.map(({ col, idx }, i) => {
        const cell = item[idx];
        if (col.type === 'button' && cell?.buttonID) {
          return (
            <AnimatedPressable
              key={idx}
              onPress={() => triggerFormEndpoint(cell.buttonID, cell.parameter)}
              onHoverIn={onHoverIn}
              onHoverOut={onHoverOut}
              style={buttonHoverStyle}
            >
              <AndpText
                numberOfLines={1}
                ellipsizeMode="tail"
                style={styles.cardButtonText}
              >
                {col.name}
              </AndpText>
            </AnimatedPressable>
          );
        }
        return (
          <View key={idx} style={styles.cardRow}>
            <AndpText
              numberOfLines={1}
              ellipsizeMode="tail"
              style={styles.cardLabel}
            >
              {col.name}
            </AndpText>
            <AndpText
              numberOfLines={1}
              ellipsizeMode="tail"
              style={styles.cardValue}
            >
              {typeof cell === 'object' ? JSON.stringify(cell) : cell}
            </AndpText>
          </View>
        );
      })}
    </View>
  );
};

export default Card;