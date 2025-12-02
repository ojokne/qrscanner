// In App.js in a new project

import * as React from 'react';
import { createStaticNavigation } from '@react-navigation/native';
import { createNativeStackNavigator } from '@react-navigation/native-stack';
import HomeScreen from "./HomeScreen"
import DetailsScreen from "./DetailsScreen"
import ScanScreen from "./ScanScreen"

const RootStack = createNativeStackNavigator({
  initialRouteName: 'Home',
  screenOptions: {
    // headerStyle: { backgroundColor: 'tomato' },
  },
  screens: {
    Home: {
      screen: HomeScreen,
      options: {
        title: 'Home',
      },
    },
    Details: {
      screen: DetailsScreen,
      options: {
        title: 'Details',
      },
    },
    Scan: {
      screen: ScanScreen,
      options: {
        title: 'Details',
      },
    },
  },
});

const Navigation = createStaticNavigation(RootStack);

export default function App() {
  return <Navigation />;
}