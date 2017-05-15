package streamdata.vkontacteapi;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.vk.sdk.api.VKApi;
import com.vk.sdk.api.VKApiConst;
import com.vk.sdk.api.VKBatchRequest;
import com.vk.sdk.api.VKBatchRequest.VKBatchRequestListener;
import com.vk.sdk.api.VKError;
import com.vk.sdk.api.VKParameters;
import com.vk.sdk.api.VKRequest;
import com.vk.sdk.api.VKRequest.VKRequestListener;
import com.vk.sdk.api.VKResponse;
import com.vk.sdk.api.methods.VKApiCaptcha;
import com.vk.sdk.api.model.VKApiPhoto;
import com.vk.sdk.api.model.VKApiUser;
import com.vk.sdk.api.model.VKAttachments;
import com.vk.sdk.api.model.VKPhotoArray;
import com.vk.sdk.api.model.VKWallPostResult;
import com.vk.sdk.api.photo.VKImageParameters;
import com.vk.sdk.api.photo.VKUploadImage;
import com.vk.sdk.dialogs.VKShareDialog;
import com.vk.sdk.dialogs.VKShareDialogBuilder;

import org.json.JSONArray;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class TestActivity extends ActionBarActivity {

    private static final int[] IDS = {R.id.friends_get};

    public static final int TARGET_GROUP = 60479154;
    public static final int TARGET_ALBUM = 181808365;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction().add(R.id.container, new PlaceholderFragment()).commit();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment implements View.OnClickListener {
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View view = inflater.inflate(R.layout.fragment_test, container, false);
            for (int id : IDS) {
                view.findViewById(id).setOnClickListener(this);
            }
            return view;
        }

        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.friends_get:
                    startApiCall(VKApi.friends().get(VKParameters.from(VKApiConst.FIELDS, "id,first_name,last_name,contacts,crop_photo")));
                    break;
            }
        }

        private void startApiCall(VKRequest request) {
            Intent i = new Intent(getActivity(), ApiCallActivity.class);
            i.putExtra("request", request.registerObject());
            startActivity(i);
        }

        private void showError(VKError error) {
            new AlertDialog.Builder(getActivity())
                    .setMessage(error.toString())
                    .setPositiveButton("OK", null)
                    .show();
            if (error.httpError != null) {
                Log.w("Test", "Error in request or upload", error.httpError);
            }
        }

        private Bitmap getPhoto() {
            try {
                return BitmapFactory.decodeStream(getActivity().getAssets().open("android.jpg"));
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }

        private static void recycleBitmap(@Nullable final Bitmap bitmap) {
            if (bitmap != null) {
                bitmap.recycle();
            }
        }

        private File getFile() {
            try {
                InputStream inputStream = getActivity().getAssets().open("android.jpg");
                File file = new File(getActivity().getCacheDir(), "android.jpg");
                OutputStream output = new FileOutputStream(file);
                byte[] buffer = new byte[4 * 1024]; // or other buffer size
                int read;

                while ((read = inputStream.read(buffer)) != -1) {
                    output.write(buffer, 0, read);
                }
                output.flush();
                output.close();
                return file;
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        private void makePost(VKAttachments attachments) {
            makePost(attachments, null);
        }

        private void makeRequest() {
            VKRequest request = new VKRequest("apps.getFriendsList", VKParameters.from("extended", 1, "type", "request"));
            request.executeWithListener(new VKRequestListener() {
                @Override
                public void onComplete(VKResponse response) {
                    final Context context = getContext();
                    if (context == null || !isAdded()) {
                        return;
                    }
                    try {
                        JSONArray jsonArray = response.json.getJSONObject("response").getJSONArray("items");
                        int length = jsonArray.length();
                        final VKApiUser[] vkApiUsers = new VKApiUser[length];
                        CharSequence[] vkApiUsersNames = new CharSequence[length];
                        for (int i = 0; i < length; i++) {
                            VKApiUser user = new VKApiUser(jsonArray.getJSONObject(i));
                            vkApiUsers[i] = user;
                            vkApiUsersNames[i] = user.first_name + " " + user.last_name;
                        }
                        new AlertDialog.Builder(context)
                                .setTitle(R.string.send_request_title)
                                .setItems(vkApiUsersNames, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        startApiCall(new VKRequest("apps.sendRequest",
                                                VKParameters.from("user_id", vkApiUsers[which].id, "type", "request")));
                                    }
                                }).create().show();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        }

        private void makePost(VKAttachments attachments, String message) {
            VKRequest post = VKApi.wall().post(VKParameters.from(VKApiConst.OWNER_ID, "-" + TARGET_GROUP, VKApiConst.ATTACHMENTS, attachments, VKApiConst.MESSAGE, message));
            post.setModelClass(VKWallPostResult.class);
            post.executeWithListener(new VKRequestListener() {
                @Override
                public void onComplete(VKResponse response) {
                    if (isAdded()) {
                        VKWallPostResult result = (VKWallPostResult) response.parsedModel;
                        Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(String.format("https://vk.com/wall-%d_%s", TARGET_GROUP, result.post_id)));
                        startActivity(i);
                    }
                }

                @Override
                public void onError(VKError error) {
                    showError(error.apiError != null ? error.apiError : error);
                }
            });
        }
    }
}
