import { Platform, View } from "react-native";

/**
 * Props for the Resizer component.
 *
 * @property {number} idx - The index of the column to resize.
 * @property {object} styles - The styles object containing `resizer` definitions.
 * @property {(idx: number, pageX: number) => void} onStartResize -
 *   Callback invoked with the column index and initial pointer X position when resizing starts.
 */
interface ResizerProps {
  idx: number;
  styles: any;
  onStartResize: (idx: number, pageX: number) => void;
}

/**
 * Resizer
 * ----------------------------------------
 * A small interactive handle used in the ExpertSearchTable to allow
 * users to adjust column widths by dragging.
 *
 * This component renders an invisible (or styled) View that captures
 * touch/mouse events and triggers a resize callback when the user
 * begins a drag gesture. On web platforms, it shows the 'col-resize'
 * cursor to indicate resizability.
 *
 * @param {number} idx - The zero-based index of the column being resized.
 * @param {object} styles - A StyleSheet or style object from ExpertSearchTable,
 *   expected to contain a `resizer` style definition for visual layout.
 * @param {(idx: number, pageX: number) => void} onStartResize -
 *   Callback invoked when the user starts dragging the resizer handle.
 *   Receives the column index (`idx`) and the initial horizontal touch
 *   position (`pageX`). Used by ExpertSearchTable to compute and apply
 *   new column widths.
 *
 * @example
 * <Resizer
 *   idx={colIndex}
 *   styles={styles}
 *   onStartResize={handleStartResize}
 * />
 */
const Resizer: React.FC<ResizerProps> = ({ idx, styles, onStartResize }) => (
  <View
    onStartShouldSetResponder={() => true}
    onResponderGrant={(e) => onStartResize(idx, (e.nativeEvent as any).pageX)}
    style={[styles.resizer, Platform.OS === 'web' && { cursor: 'col-resize' }]}
  />
);

export default Resizer;