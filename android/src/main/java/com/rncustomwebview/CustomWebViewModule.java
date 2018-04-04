package com.rncustomwebview;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;

import com.facebook.react.bridge.ActivityEventListener;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;

import java.io.File;

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
        //
        // * I believe I ran across this as a possible problem and didn't do a lot of testing; this code seems to work fine
        switch (requestCode) {
        case REQUEST_CAMERA:
            if (resultCode == RESULT_OK) {
                filePathCallback.onReceiveValue(new Uri[] { outputFileUri });
            } else {
                filePathCallback.onReceiveValue(null);
            }
            break;
        case SELECT_FILE:
            filePathCallback.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(resultCode, data));
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
        final String TAKE_PHOTO = "Take a photo…";
        final String TAKE_VIDEO = "Record a video…";
        final String CHOOSE_FILE = "Choose an existing file…";
        final String CANCEL = "Cancel";
        this.filePathCallback = filePathCallback;

        // from https://stackoverflow.com/a/36306345/185651
        final CharSequence[] items = { TAKE_PHOTO, TAKE_VIDEO, CHOOSE_FILE, CANCEL };
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
                    // display a file chooser;
                    // the webview actually gives us this `createIntent` thing that brings up a reasonable image picker
                    getCurrentActivity().startActivityForResult(
                            fileChooserParams.createIntent().setType("image/*"),
                            SELECT_FILE
                    );
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
}
