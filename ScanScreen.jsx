import React, { useState, useEffect } from 'react';
import {
  SafeAreaView,
  View,
  Text,
  StyleSheet,
  Platform,
  PermissionsAndroid,
} from 'react-native';
import { useNavigation } from '@react-navigation/native';
import QRScanner from './QRScanner'; // 👈 updated import

export default function ScanScreen() {
  const navigation = useNavigation();
  const [scanning, setScanning] = useState(true);

  useEffect(() => {
    // requestPermissions();
  }, []);

  //   async function requestPermissions() {
  //     if (Platform.OS !== "android") return;

  //     try {
  //       const res = await PermissionsAndroid.request(
  //         PermissionsAndroid.PERMISSIONS.CAMERA
  //       );

  //       if (res !== PermissionsAndroid.RESULTS.GRANTED) {
  //         navigation.navigate("Details", {
  //           data: null,
  //           error: "Camera permission denied",
  //         });
  //       }
  //     } catch (err) {
  //       navigation.navigate("Details", {
  //         data: null,
  //         error: "Permission error",
  //       });
  //     }
  //   }

  const handleQRCode = (data) => {
    if (!scanning) return;
    setScanning(false);
    navigation.navigate('Details', {
      data,
      error: null,
    });
  };

  const handleDecodeFail = e => {
    if (!scanning) return;
    setScanning(false);
    console.log(e.message);

    navigation.navigate('Details', {
      data: null,
      error: 'QR code decode failed',
    });
  };

  return (
    <SafeAreaView style={styles.container}>
      <QRScanner
        style={styles.camera}
        onQRCodeDetected={result => {            
          handleQRCode(result);
        }}
        cameraFacing="back"
        torch="off"
        zoom={0}
        onError={handleDecodeFail}
      />

      <View style={styles.overlay}>
        <Text style={styles.text}>Scanning...</Text>
      </View>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#000' },
  camera: { flex: 1 },
  overlay: {
    position: 'absolute',
    top: 50,
    alignSelf: 'center',
    backgroundColor: 'rgba(0,0,0,0.4)',
    padding: 10,
    borderRadius: 5,
  },
  text: { color: '#fff', fontSize: 16 },
});
