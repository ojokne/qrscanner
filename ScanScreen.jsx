import { useNavigation } from '@react-navigation/native';
import { Button, StyleSheet, View } from 'react-native';

export default function HomeScreen() {
  const navigation = useNavigation();

  const navigateToDetails = (data)=>{
    navigation.navigate('Details', {
            data,
          });
  }
  return (
    <View style={styles.container}>
      <Button onPress={() => navigateToDetails("Hello world")} title='Go to Details'/>
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
