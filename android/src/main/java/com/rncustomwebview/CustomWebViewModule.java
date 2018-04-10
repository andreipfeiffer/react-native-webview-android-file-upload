package com.rncustomwebview;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;

import com.facebook.react.bridge.ActivityEventListener;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static android.app.Activity.RESULT_OK;

/**
 * Much of the code here derived from the sample at https://github.com/hushicai/ReactNativeAndroidWebView.
 */
public class CustomWebViewModule extends ReactContextBaseJavaModule implements ActivityEventListener {
    public static final String REACT_CLASS = "CustomWebViewModule";
    private static final int REQUEST_CAMERA = 1;
    private static final int SELECT_FILE = 2;
    private CustomWebViewPackage aPackage;
    private ValueCallback<Uri[]> filePathCallback;
    private Uri outputFileUri;

    // @todo this could be configured from JS
    final String[] DEFAULT_MIME_TYPES = {"image/*", "video/*"};

    final String TAKE_PHOTO = "Take a photo…";
    final String TAKE_VIDEO = "Record a video…";
    final String CHOOSE_FILE = "Choose an existing file…";
    final String CANCEL = "Cancel";

    public CustomWebViewModule(ReactApplicationContext context) {
        super(context);
        context.addActivityEventListener(this);
    }

    @Override
    public String getName() {
        return REACT_CLASS;
    }

    public void onActivityResult(Activity activity, int requestCode, int resultCode, Intent data) {

        if (this.filePathCallback == null) {
            return;
        }

        // based off of which button was pressed, we get an activity result and a file
        // the camera activity doesn't properly return the filename* (I think?) so we use
        // this filename instead
        switch (requestCode) {
        case REQUEST_CAMERA:
            if (resultCode == RESULT_OK) {
                filePathCallback.onReceiveValue(new Uri[] { outputFileUri });
            } else {
                filePathCallback.onReceiveValue(null);
            }
            break;
        case SELECT_FILE:
            if (resultCode == RESULT_OK && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                filePathCallback.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(resultCode, data));
            } else {
                filePathCallback.onReceiveValue(null);
            }
            break;
        }
        this.filePathCallback = null;
    }

    public void onNewIntent(Intent intent) {
    }

    public boolean startPhotoPickerIntent(
            final ValueCallback<Uri[]> filePathCallback,
            final WebChromeClient.FileChooserParams fileChooserParams
    ) {
        this.filePathCallback = filePathCallback;
        final String[] acceptTypes = getSafeAcceptedTypes(fileChooserParams);
        final CharSequence[] items = getDialogItems(acceptTypes);

        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(getCurrentActivity());
        builder.setTitle("Upload file:");

        // this gets called when the user:
        // 1. chooses "Cancel"
        // 2. presses "Back button"
        // 3. taps outside the dialog
        builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                // we need to tell the callback we cancelled
                filePathCallback.onReceiveValue(null);
            }
        });

        builder.setItems(items, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int item) {
                if (items[item].equals(TAKE_PHOTO)) {
                    startCamera(MediaStore.ACTION_IMAGE_CAPTURE, "image-", ".jpg");
                } else if (items[item].equals(TAKE_VIDEO)) {
                    startCamera(MediaStore.ACTION_VIDEO_CAPTURE, "video-", ".mp4");
                } else if (items[item].equals(CHOOSE_FILE)) {
                    startFileChooser(fileChooserParams);
                } else if (items[item].equals(CANCEL)) {
                    dialog.cancel();
                }
            }
        });
        builder.show();

        return true;
    }

    public CustomWebViewPackage getPackage() {
        return this.aPackage;
    }

    public void setPackage(CustomWebViewPackage aPackage) {
        this.aPackage = aPackage;
    }

    private void startCamera(String intentType, String prefix, String suffix) {

        // bring up a camera picker intent; we need to pass a filename for the file to be saved to
        Intent intent = new Intent(intentType);
        try {
            // need to specify a directory here
            // the download directory was the one that didn't end up giving me permissions errors
            outputFileUri = Uri.fromFile(File.createTempFile(
                    prefix,
                    suffix,
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS))
            );
        } catch (java.io.IOException e) {
        }
        intent.putExtra(MediaStore.EXTRA_OUTPUT, outputFileUri);
        getCurrentActivity().startActivityForResult(intent, REQUEST_CAMERA);
    }

    private void startFileChooser(WebChromeClient.FileChooserParams fileChooserParams) {
        final String[] acceptTypes = getSafeAcceptedTypes(fileChooserParams);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Intent intent = fileChooserParams.createIntent();
            intent.setType("*/*");
            intent.putExtra(Intent.EXTRA_MIME_TYPES, getAcceptedMimeType(acceptTypes));
            getCurrentActivity().startActivityForResult(intent, SELECT_FILE);
        }
    }

    private CharSequence[] getDialogItems(String[] types) {
        List<String> listItems = new ArrayList<String>();

        if (acceptsImages(types)) {
            listItems.add(TAKE_PHOTO);
        }
        if (acceptsVideo(types)) {
            listItems.add(TAKE_VIDEO);
        }

        listItems.add(CHOOSE_FILE);
        listItems.add(CANCEL);

        return listItems.toArray(new CharSequence[listItems.size()]);
    }

    private Boolean acceptsImages(String[] types) {
        return isArrayEmpty(types) || arrayContainsString(types, "image");
    }

    private Boolean acceptsVideo(String[] types) {
        return isArrayEmpty(types) || arrayContainsString(types, "video");
    }

    private Boolean arrayContainsString(String[] array, String pattern){
        for(String content : array){
            if(content.indexOf(pattern) > -1){
                return true;
            }
        }
        return false;
    }

    private String[] getSafeAcceptedTypes(WebChromeClient.FileChooserParams params) {
        // the getAcceptTypes() is available only in api 21+
        // for lower level, we ignore it
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            return params.getAcceptTypes();
        }

        final String[] EMPTY = {};
        return EMPTY;
    }

    private String[] getAcceptedMimeType(String[] types) {
        if (isArrayEmpty(types)) {
            return DEFAULT_MIME_TYPES;
        }
        return types;
    }

    private Boolean isArrayEmpty(String[] arr) {
        // when our array returned from getAcceptTypes() has no values set from the webview
        // i.e. <input type="file" />, without any "accept" attr
        // will be an array with one empty string element, afaik
        return arr.length == 0 ||
                (arr.length == 1 && arr[0].length() == 0);
    }
}
