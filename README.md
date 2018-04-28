# React Native Android WebView File Upload (camera + file)

This is a Custom React Native Android module that enables file uploads from a WebView `<input type="file" />` element:

* by __taking a new photo using the camera__
* by __recording a video using the camera__
* by __choosing an existing gallery image__

All I did was take [dahjelle's react-native-android-webview-file-image-upload][dahjelle] implementation, fix some problems, extract it into a separate module like [lucasferreira's react-native-webview-android][lucasferreira] and add the video recording functionality.

It works with React Native 0.50+, and reverts to the built-in WebView on iOS.

![](https://github.com/andreipfeiffer/react-native-webview-android-image-upload/blob/master/docs/preview.gif)

# Installation

```bash
npm install git+ssh://git@github.com:andreipfeiffer/react-native-webview-android-image-upload.git
```

### Auto linking

```
react-native link react-native-webview-android-image-upload
```

The above should make the changes listed below. If it doesn't work, you should try manual linking.

### Manual linking

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
import com.rncustomwebview.CustomWebViewPackage;  // <--- import package

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

### Re-build your application

Since you have changed native code, reloading the JS code alone won't work:

```bash
react-native run-android
```

# Example
```javascript
import React, { Component } from "react";

// import module
import CustomWebView from "react-native-webview-android-image-upload";

export default class App extends Component {
  render() {
    return (
      <CustomWebView
        source={{ uri: "your-web-url" }}
        startInLoadingState={true}
        // any other props supported by React Native's WebView
      />
    );
  }
}
```

### Getting the WebView `ref`
```javascript
export default class App extends Component {
  render() {
    return (
      <CustomWebView
        source={{ uri: "your-web-url" }}
        getRef={e => (this.webview = e)}
      />
    );
  }

  // then you can call any of the methods available on the built-in ref, like:
  // this.webview.reload();
  // this.webview.injectJavaScript();
}
```

### Controlling image and/or video

You can use the `accept` attribute on the `<input />` element to control what type of media your users will be allowed to upload.

* `<input type="file" />` will default to images and videos
* `<input type="file" accept="image/*" />` will allow only image capture / selection
* `<input type="file" accept="video/*" />` will allow only video recording / selection
* `<input type="file" accept="image/*, video/*" />` same as default

Check out the [example html][example].

# @todo

The default accepted mime types are `"image/*, video/*"`, which don't include `audio`. This default could be customized from the React Native Component and maybe we can default to `"*/*"`.

[dahjelle]: https://github.com/dahjelle/react-native-android-webview-file-image-upload
[lucasferreira]: https://github.com/lucasferreira/react-native-webview-android
[example]: https://andreipfeiffer.github.io/react-native-webview-android-image-upload/index.html
