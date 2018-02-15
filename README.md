# React Native Android Webview Image Upload (camera + file)

This is a Custom React Native Android module that enables image upload from Webview:

* by choosing an existing gallery image
* by taking a new photo using the camera

All I did was take [dahjelle's react-native-android-webview-file-image-upload][dahjelle] implementation and extract it into a separate module like [lucasferreira's react-native-webview-android][lucasferreira].

It works with React Native 0.50+, and reverts to the built-in WebView on iOS.

## Installation

```bash
npm install react-native-webview-android --save
```

## Add it to your android project

* Update `android/setting.gradle`

```gradle
......

include ':react-native-webview-android-image-upload'
project(':react-native-webview-android-image-upload').projectDir = new File(rootProject.projectDir, '../node_modules/react-native-webview-android-image-upload/android')

......
```

* Update `android/app/build.gradle`

```gradle
......

dependencies {
  ......
  compile project(':react-native-webview-android-image-upload')
}
```

* Register Module in `android/app/src/main/java/com/[your-project-package]/MainApplication.java`

```java
import com.rncustomwebview.CustomWebViewPackage;;  // <--- import package

public class MainApplication extends Application implements ReactApplication {

......

  @Override
  protected List<ReactPackage> getPackages() {
    return Arrays.<ReactPackage>asList(
      new MainReactPackage(),
      new CustomWebViewPackage()  // <------ add this line to your MainApplication class
    ); 
  }

  ......

}
```

## Example
```javascript
import React, { Component } from "react";

// import module
import CustomWebView from "react-native-webview-android-image-upload";

export default class App extends Component {
  render() {
    return (
      <CustomWebView
        source={{ uri: "https://github.com/andreipfeiffer/react-native-webview-android-image-upload" }}
        startInLoadingState={true}
        // any other attributes supported by React Native's WebView
      />
    );
  }
}
```

[dahjelle]: https://github.com/dahjelle/react-native-android-webview-file-image-upload
[lucasferreira]: https://github.com/lucasferreira/react-native-webview-android
