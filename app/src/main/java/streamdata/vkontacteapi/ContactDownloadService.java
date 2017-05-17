package streamdata.vkontacteapi;

import android.app.IntentService;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.provider.ContactsContract;
import android.telephony.PhoneNumberUtils;
import android.util.Log;

import com.vk.sdk.api.VKApi;
import com.vk.sdk.api.VKApiConst;
import com.vk.sdk.api.VKError;
import com.vk.sdk.api.VKParameters;
import com.vk.sdk.api.VKRequest;
import com.vk.sdk.api.VKResponse;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.net.URL;

import java.util.ArrayList;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.RawContacts;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.content.ContentProviderOperation;


public class ContactDownloadService extends IntentService {

    public ContactDownloadService() {
        super(ContactDownloadService.class.getName());
    }

    @Override
    protected void onHandleIntent(Intent workIntent) {
        Log.i("ContactDownloadService", "Service started");

        VKRequest request = VKApi.friends().get(VKParameters.from(VKApiConst.FIELDS,
                "id,first_name,last_name,contacts,photo_50"));

        request.unregisterObject();
        request.executeSyncWithListener(mRequestListener);

        Log.i("ContactDownloadService", "Service stopping");
        this.stopSelf();
    }

    VKRequest.VKRequestListener mRequestListener = new VKRequest.VKRequestListener() {
        @Override
        public void onComplete(VKResponse response) {

            Intent broadcastIntent = new Intent();
            broadcastIntent.setAction(getString(R.string.ACTION_NAME));
            sendBroadcast(broadcastIntent);

            try {
//                Load and add contacts to phone book
                Log.i("ContactDownloadService", response.json.toString());
                JSONObject jsonObject = new JSONObject(response.json.get("response").toString());
                JSONArray rawData = jsonObject.getJSONArray("items");

                int progressBarStep = (int)(rawData.length() / 10.0);
                for(int n = 0; n < rawData.length(); n++) {

                    if (n % progressBarStep == 0) {
                        sendBroadcast(broadcastIntent); }

                    addContact(rawData.getJSONObject(n));
                }

                sendBroadcast(broadcastIntent);
            } catch (JSONException e){
                Log.e("ContactDownloadService", e.getMessage());
            }
        }

        @Override
        public void onError(VKError error) {
            Log.i("ContactDownloadService", "onError");
        }

        @Override
        public void onProgress(VKRequest.VKProgressType progressType, long bytesLoaded,
                               long bytesTotal) {
            Log.i("ContactDownloadService", "onProgress");
        }

        @Override
        public void attemptFailed(VKRequest request, int attemptNumber, int totalAttempts) {
            Log.i("ContactDownloadService", "attemptFailed");
        }
    };

    private void addContact(JSONObject json) {
        try {
            String first_name = json.getString("first_name");
            String last_name = json.getString("last_name");
            String mobile_phone = json.optString("mobile_phone");
            String crop_photo = json.optString("photo_50");

            if (first_name == null) { return; }
            if (last_name == null) { return; }
            if (mobile_phone == null || !PhoneNumberUtils.isGlobalPhoneNumber(mobile_phone)) {
                return; }
            if (crop_photo == null) {
                return;
            }

            String displayName = first_name.concat(" ").concat(last_name);

            URL url = new URL(crop_photo);
            Bitmap bmp = BitmapFactory.decodeStream(url.openConnection().getInputStream());
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            bmp.compress(Bitmap.CompressFormat.PNG, 100, stream);
            byte[] photoByteArray = stream.toByteArray();

            ArrayList<ContentProviderOperation> ops = new ArrayList<>();
            int rawContactInsertIndex = ops.size();

            ops.add(ContentProviderOperation.newInsert(RawContacts.CONTENT_URI)
                    .withValue(RawContacts.ACCOUNT_TYPE, null)
                    .withValue(RawContacts.ACCOUNT_NAME,null )
                    .build());
            ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                    .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawContactInsertIndex)
                    .withValue(Data.MIMETYPE, Phone.CONTENT_ITEM_TYPE)
                    .withValue(Phone.NUMBER, mobile_phone)
                    .build());
            ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                    .withValueBackReference(Data.RAW_CONTACT_ID, rawContactInsertIndex)
                    .withValue(Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
                    .withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, displayName)
                    .build());
            ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                    .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                    .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE)
                    .withValue(ContactsContract.CommonDataKinds.Photo.PHOTO, photoByteArray)
                    .build());

            getContentResolver().applyBatch(ContactsContract.AUTHORITY, ops);
        } catch (Exception e){
//             Think that exception is not bad and we can continue.
            Log.e("ContactDownloadService", e.getMessage());
        }

    }
}