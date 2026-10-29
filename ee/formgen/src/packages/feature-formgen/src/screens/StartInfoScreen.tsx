import AndpText from '@andp/core/src/components/AndpText';
import { View } from 'react-native';

const StartInfoScreen: React.FC = () => {
  return (
    <View style={{
      height: "100%",
      marginLeft: 'auto',
      marginRight: 'auto',
      marginBottom: 100,
    }}>
      <AndpText>StartInfoScreen</AndpText>
      <AndpText>Hier ist der StartScreen.</AndpText>
    </View>
  );
};

export default StartInfoScreen;
