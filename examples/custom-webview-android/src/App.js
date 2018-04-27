import React, { Component } from "react";
import { StyleSheet, Text, View, WebView } from "react-native";

export default class App extends Component {
  render() {
    return (
      <View style={styles.container}>
        <WebView
          style={styles.container}
          source={{
            uri:
              "https://andreipfeiffer.github.io/react-native-webview-android-image-upload/index.html"
          }}
        />
      </View>
    );
  }
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
  },
});
