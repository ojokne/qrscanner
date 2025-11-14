import { useNavigation, useRoute } from '@react-navigation/native';
import { Button, StyleSheet, Text, View } from 'react-native';

export default function DetailsScreen() {
  const navigation = useNavigation();
  const route = useRoute()
  const { data } = route.params;
  return (
    <View style={styles.container}>
        <Text>{data}</Text>
      <Button onPress={() => navigation.navigate('Home')} title="Go to Home" />
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
