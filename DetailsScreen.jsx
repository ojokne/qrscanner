import { useNavigation, useRoute } from '@react-navigation/native';
import { Button, StyleSheet, Text, View } from 'react-native';

export default function DetailsScreen() {
  const navigation = useNavigation();
  const route = useRoute();
  const { data, error } = route.params || {};

  return (
    <View style={styles.container}>
      <Text style={styles.title}>Scan Result</Text>

      {data ? (
        <Text style={styles.success}>{data?.data}</Text>
      ) : (
        <Text style={styles.error}>{error}</Text>
      )}

      {data ? <Text>{data?.time}</Text> : null}
      <Button title="Go Home" onPress={() => navigation.navigate('Home')} />

      <View style={{ marginTop: 10 }}>
        <Button
          title="Scan Again"
          onPress={() => navigation.navigate('Scan')}
        />
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
    padding: 20,
  },
  title: { fontSize: 22, fontWeight: 'bold', marginBottom: 20 },
  success: { fontSize: 18, color: 'green', textAlign: 'center' },
  error: { fontSize: 18, color: 'red', textAlign: 'center' },
});
