package com.rncustomwebview;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.support.v4.content.FileProvider;
import android.util.Log;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;

import com.facebook.react.bridge.ActivityEventListener;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
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
    // Seek Asia Customized. Disable photo & video, only support doc and text resume
    final String[] DEFAULT_MIME_TYPES = {"text/*", "application/rtf", "application/msword", "application/vnd.openxmlformats-officedocument.wordprocessingml.document", "application/pdf", };


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
                Log.i("RESULT_OK", outputFileUri.toString());
                filePathCallback.onReceiveValue(new Uri[] { outputFileUri });
            } else {
                filePathCallback.onReceiveValue(null);
            }
            break;
        case SELECT_FILE:
            if (resultCode == RESULT_OK && data != null) {
                Uri result[] = this.getSelectedFiles(data, resultCode);
                filePathCallback.onReceiveValue(result);
            } else {
                filePathCallback.onReceiveValue(null);
            }
            break;
        }
        this.filePathCallback = null;
    }

    public void onNewIntent(Intent intent) {
    }

    private Uri[] getSelectedFiles(Intent data, int resultCode) {
        // we have one files selected
        if (data.getData() != null) {
            if (resultCode == RESULT_OK && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                Uri[] result = WebChromeClient.FileChooserParams.parseResult(resultCode, data);
                return result;
            } else {
                return null;
            }
        }
        // we have multiple files selected
        if (data.getClipData() != null) {
            final int numSelectedFiles = data.getClipData().getItemCount();
            Uri[] result = new Uri[numSelectedFiles];
            for (int i = 0; i < numSelectedFiles; i++) {
                result[i] = data.getClipData().getItemAt(i).getUri();
            }
            return result;
        }
        return null;
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

        // bring up a camera picker intent
        // we need to pass a filename for the file to be saved to
        Intent intent = new Intent(intentType);

        // Create the File where the photo should go
        try {
            String packageName = getReactApplicationContext().getPackageName();
            File capturedFile = createCapturedFile(prefix, suffix);
            outputFileUri = FileProvider.getUriForFile(getReactApplicationContext(), packageName+".fileprovider", capturedFile);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, outputFileUri);
            getCurrentActivity().startActivityForResult(intent, REQUEST_CAMERA);
        } catch (IOException ex) {
            Log.e("CREATE FILE", "Error occurred while creating the File", ex);
        }
    }

    private void startFileChooser(WebChromeClient.FileChooserParams fileChooserParams) {
        final String[] acceptTypes = getSafeAcceptedTypes(fileChooserParams);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            final boolean allowMultiple = fileChooserParams.getMode() == WebChromeClient.FileChooserParams.MODE_OPEN_MULTIPLE;

            Intent intent = fileChooserParams.createIntent();
            intent.setType("*/*");
            intent.putExtra(Intent.EXTRA_MIME_TYPES, getAcceptedMimeType(acceptTypes));
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, allowMultiple);
            getCurrentActivity().startActivityForResult(intent, SELECT_FILE);
        }
    }

    private File createCapturedFile(String prefix, String suffix) throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = prefix + "_" + timeStamp;
        File storageDir = getReactApplicationContext().getExternalFilesDir(null);
        return File.createTempFile(imageFileName, suffix, storageDir);
    }
    
    private CharSequence[] getDialogItems(String[] types) {
        List<String> listItems = new ArrayList<String>();

        if (acceptsImages(types)) {
            // Seek Asia Customized. Disable Photo Resume
            // listItems.add(TAKE_PHOTO);
        }
        if (acceptsVideo(types)) {
            // Seek Asia Customized. Disable Video Resume
            // listItems.add(TAKE_VIDEO);
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
            if(content.contains(pattern)){
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
