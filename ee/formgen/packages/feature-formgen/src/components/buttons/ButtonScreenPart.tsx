import React from 'react';
import { View, Button } from 'react-native';

const ButtonScreenPart: React.FC<{
  buttons: { id: number; label: string }[];
}> = ({ buttons }) => {
  return (
    <View>
      {buttons.map((button) => (
        <Button
          key={button.id}
          title={button.label}
          onPress={() => console.log(`${button.label} Button geklickt!`)}
        />
      ))}
    </View>
  );
};

export default ButtonScreenPart;
