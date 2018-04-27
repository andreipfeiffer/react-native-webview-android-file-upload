import React, { Component } from "react";
import { StyleSheet, View } from "react-native";
import CustomWebView from "react-native-webview-android-image-upload";

export default class App extends Component {
  render() {
    return (
      <View style={styles.container}>
        <CustomWebView
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
    flex: 1
  }
});
