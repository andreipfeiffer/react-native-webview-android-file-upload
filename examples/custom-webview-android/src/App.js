import React, { Component } from "react";
import { StyleSheet, View, Button } from "react-native";
import CustomWebView from "react-native-webview-android-image-upload";

export default class App extends Component {
  inject = () => {
    this.webview.injectJavaScript("alert('JavaScript is injected')");
  };

  reload = () => {
    this.webview.reload();
  };

  render() {
    return (
      <View style={styles.container}>
        <CustomWebView
          style={styles.container}
          getRef={e => (this.webview = e)}
          injectedJavaScript={"alert('Custom webview loaded')"}
          source={{
            uri:
              "https://andreipfeiffer.github.io/react-native-webview-android-image-upload/index.html"
          }}
        />

        <View style={styles.containerHorizontal}>
          <Button title="Reload" onPress={this.reload} />
          <Button title="Inject JS" onPress={this.inject} />
        </View>
      </View>
    );
  }
}

const styles = StyleSheet.create({
  container: {
    flex: 1
  },
  containerHorizontal: {
    flexDirection: "row",
    justifyContent: "space-around",
    paddingVertical: 10,
    backgroundColor: "#eee"
  }
});
