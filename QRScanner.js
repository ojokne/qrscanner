// Polyfill for React Native Hermes engines
import { TextDecoder, TextEncoder } from 'text-encoding';

if (typeof global.TextDecoder === 'undefined') {
  global.TextDecoder = TextDecoder;
}
if (typeof global.TextEncoder === 'undefined') {
  global.TextEncoder = TextEncoder;
}

import React, { forwardRef, useImperativeHandle, useRef } from 'react';
import {
  requireNativeComponent,
  findNodeHandle,
  NativeModules,
  UIManager,
} from 'react-native';
import decodeQR from 'qr/decode.js';
import { Buffer } from 'buffer';

const { QRScannerModule } = NativeModules;

const NativeQRScannerView = requireNativeComponent('QRScannerView');

const QRScanner = forwardRef((props, ref) => {
  const { style, onQRCodeDetected, cameraFacing, torch, zoom, onError } = props;
  const nativeRef = useRef(null);

  // Allow JS to call native capture()
  useImperativeHandle(ref, () => ({
    async capture() {
      const tag = findNodeHandle(nativeRef.current);
      if (!tag) throw new Error('Camera view not found');
      return await QRScannerModule.capture(tag);
    },
  }));

  // Tell native we finished processing a frame
  //   const acknowledgeFrame = () => {

  //     const tag = findNodeHandle(nativeRef.current);
  //     if (tag) {
  //         console.log("Acknowledge");
  //       QRScannerModule.frameComplete(tag);
  //     }
  //   };
  const acknowledgeFrame = () => {
    const tag = findNodeHandle(nativeRef.current);
    if (!tag) return;

    try {
      const commandId =
        UIManager.getViewManagerConfig('QRScannerView').Commands.frameComplete;

      UIManager.dispatchViewManagerCommand(tag, commandId, []);
      console.log('Acknowledged');
    } catch (e) {
      console.warn('Failed to send frameComplete command', e);
    }
  };

  const MAX_TRIES = 5;
  let retries = 0;

  // Handle camera frames
  const handleFrame = event => {
    if (!onQRCodeDetected) {
      acknowledgeFrame();
      return;
    }
    console.log('Frame recieved');


    if (retries >= MAX_TRIES)
      onError(`Could not decode QR Code after ${retries} retries`);

    try {
      const { pixels, width, height } = event.nativeEvent;

      const byteArray = Uint8Array.from(Buffer.from(pixels, 'base64'));

      const image = { data: byteArray, width, height };

      const start = global.performance?.now?.() ?? Date.now();
      const result = decodeQR(image);
      const end = global.performance?.now?.() ?? Date.now();
      const duration = end - start;

      console.log(`QR decode time: ${duration.toFixed(2)} ms`);

      if (result)
        retries = 0;
        onQRCodeDetected({
          data: result,
          time: `QR decode time: ${duration.toFixed(2)} ms`,
        });
    } catch (e) {
      console.log(`QR decode failed after ${retries}`);
      retries += 1;
      //   onError(e);
    } finally {
      acknowledgeFrame();
    }
  };

  return (
    <NativeQRScannerView
      ref={nativeRef}
      style={style}
      onFrame={handleFrame}
      cameraFacing={cameraFacing}
      torch={torch}
      zoom={zoom}
    />
  );
});

export default QRScanner;
