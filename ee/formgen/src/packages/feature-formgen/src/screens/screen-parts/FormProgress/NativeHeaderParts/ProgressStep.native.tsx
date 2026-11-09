import React, { useMemo } from 'react';
import { View, Pressable } from 'react-native';
import AndpText from '@andp/core/src/components/AndpText';
import theme, { paginationTheme } from '@andp/core/src/styles/theme';
import Animated, {
  FadeIn,
  FadeOut,
  useAnimatedStyle,
  useSharedValue,
  withTiming,
  interpolate,
  SharedValue,
  Extrapolation,
  LinearTransition,
} from 'react-native-reanimated';
import { mergeStyles } from '@andp/core/src/AndpUtils';
import { pagingStyles as customPagingStyles } from '../../../../styles/custom/custom-style';
import { pagingStyles } from '../../../../styles/common/common-style';

const AnimatedPressable = Animated.createAnimatedComponent(Pressable);

type ProgressItem = {
  kind: 'form' | 'flow';
  index: number;
  label: string;
  pageNumber?: number;
};

const ProgressStep: React.FC<{
  item: ProgressItem;
  index: number;
  isCurrent: boolean;
  isLast: boolean;
  isClickable: boolean;
  hasError: boolean;
  handlePageChange: (pageNumber: number) => void;
  progressSV: SharedValue<number>;
  isIntro?: boolean;
}> = ({
  item,
  index,
  isCurrent,
  isLast,
  isClickable,
  hasError,
  handlePageChange,
  progressSV,
  isIntro = false,
}) => {
    const styles = useMemo(() => mergeStyles(pagingStyles, customPagingStyles), []);
    const activeAnim = useSharedValue(isCurrent ? 1 : 0);

    React.useEffect(() => {
      activeAnim.value = withTiming(isCurrent ? 1 : 0, { duration: paginationTheme.animation!.duration.scale });
    }, [isCurrent]);

    const animatedBubbleStyle = useAnimatedStyle(() => {
      return {
        transform: [
          {
            scale: interpolate(activeAnim.value, [0, 1], [0.98, 1], Extrapolation.CLAMP),
          },
        ],
      };
    });

    const introDelay = isIntro ? index * paginationTheme.animation!.delay.stagger : 0;

    return (
      <>
        <AnimatedPressable
          entering={FadeIn.duration(paginationTheme.animation!.duration.entrance).delay(introDelay)}
          layout={LinearTransition.springify().mass(paginationTheme.animation!.steps.mass)
            .damping(paginationTheme.animation!.steps.damping)
            .stiffness(paginationTheme.animation!.steps.stiffness)}
          style={[
            styles.pageItem,
            isCurrent && styles.currentPageItem,
            !isClickable && styles.disabledPageItem,
            hasError && styles.hasErrorPageItem,
            animatedBubbleStyle,
          ]}
          onPress={() => (isClickable ? handlePageChange(index) : null)}
          disabled={!isClickable}
        >
          {isCurrent ? (
            <Animated.View entering={FadeIn.duration(paginationTheme.animation!.duration.textEntrance).delay(introDelay + paginationTheme.animation!.delay.text)} exiting={FadeOut.duration(paginationTheme.animation!.duration.exit)}>
              <AndpText numberOfLines={1} ellipsizeMode="tail"
                style={[styles.number, styles.currentNumber, hasError && styles.hasErrorNumber]}>
                {index + 1 + ' - ' + item.label}
              </AndpText>
            </Animated.View>
          ) : (
            <Animated.View entering={FadeIn.duration(paginationTheme.animation!.duration.exit).delay(introDelay + paginationTheme.animation!.delay.text)} exiting={FadeOut.duration(paginationTheme.animation!.duration.exit)}>
              <AndpText
                style={[
                  styles.number,
                  styles.disabledNumber,
                  hasError && styles.hasErrorNumber,
                ]}
              >
                {index + 1}
              </AndpText>
            </Animated.View>
          )}
        </AnimatedPressable>
        {!isLast && (
          <View
            style={[
              styles.dashBetweenSteps
            ]}
            pointerEvents="none"
          >
            <Animated.View
              style={[
                {
                  position: 'absolute',
                  left: 0,
                  height: '100%',
                },
                hasError ? styles.hasErrorDashBetweenSteps : styles.doneDashBetweenSteps,
                useAnimatedStyle(() => {
                  return {
                    width: `${interpolate(
                      progressSV.value,
                      [index, index + 1],
                      [0, 100],
                      Extrapolation.CLAMP
                    )}%`,
                  };
                }),
              ]}
            />
          </View>
        )}
      </>
    );
  };

export default ProgressStep;