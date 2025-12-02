import { useNavigation } from '@react-navigation/native';
import { Button, StyleSheet, View } from 'react-native';

export default function HomeScreen() {
  const navigation = useNavigation();

  return (
    <View style={styles.container}>
      <Button
        title="Scan QR Code"
        onPress={() => navigation.navigate("Scan")}
      />
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
  },
});
