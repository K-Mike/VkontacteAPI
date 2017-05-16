package streamdata.vkontacteapi;

import android.app.IntentService;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
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
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.net.URL;

import java.util.ArrayList;
import java.util.Arrays;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.RawContacts;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentValues;
import android.content.Intent;

public class ContactDownloadService extends IntentService {
    private VKRequest myRequest;
    public ContactDownloadService() {
        super(ContactDownloadService.class.getName());
    }

    @Override
    protected void onHandleIntent(Intent workIntent) {
        Log.i("ContactDownloadService", "Service started");

        // Gets data from the incoming Intent
        String dataString = workIntent.getDataString();

        VKRequest request = VKApi.friends().get(VKParameters.from(VKApiConst.FIELDS,
                "id,first_name,last_name,contacts,photo_50"));
        myRequest = request;
        request.unregisterObject();
//        request.executeWithListener(mRequestListener);
        request.executeSyncWithListener(mRequestListener);


//        Log.i("ContactDownloadService", request.toString());


        Log.i("ContactDownloadService", "Service stopping");
        this.stopSelf();
    }

    VKRequest.VKRequestListener mRequestListener = new VKRequest.VKRequestListener() {
        @Override
        public void onComplete(VKResponse response) {

            Intent broadcastIntent = new Intent();
            broadcastIntent.setAction(getString(R.string.ACTION_NAME));
            sendBroadcast(broadcastIntent);
//            response.json.get()
            try {
//                Log.i("ContactDownloadService", response.json.get("response").toString());
//                Log.i("ContactDownloadService", response.json.getJSONArray("response").toString());
//                JSONObject obj = new JSONObject();
//                Log.i("ContactDownloadService", response.json.toString());
                JSONObject o = new JSONObject(response.json.get("response").toString());
//                Log.i("ContactDownloadService", o.);
                JSONArray rawData = o.getJSONArray("items");

                Log.i("ContactDownloadService", "here");
                for(int n = 0; n < rawData.length(); n++) {

                    sendBroadcast(broadcastIntent);
                    String first_name = rawData.getJSONObject(n).getString("first_name");
                    String last_name = rawData.getJSONObject(n).getString("last_name");
                    String mobile_phone = rawData.getJSONObject(n).optString("mobile_phone");
                    String crop_photo = rawData.getJSONObject(n).optString("photo_50");



                    if (n == 3) {
                        addContact(rawData.getJSONObject(n));
                    } else {
                        continue;
                    }

                    Log.i("ContactDownloadService", new Integer(n).toString());
                    if (first_name != null) {
                        Log.i("ContactDownloadService", first_name);}
                    if (last_name != null) {
                        Log.i("ContactDownloadService", last_name); }
                    if (mobile_phone != null) {
                        Log.i("ContactDownloadService", mobile_phone); }
                    if (crop_photo != null) {
                        Log.i("ContactDownloadService", crop_photo); }

//                    Log.i("ContactDownloadService", crop_photo);
                }

//                Log.i("ContactDownloadService", response.json.get("response").toString());
            } catch (Exception e){
                Log.i("ContactDownloadService", "Exception");
                Log.i("ContactDownloadService", e.getMessage());
            }

//            Log.i("ContactDownloadService", response.json.toString());
//            setResponseText(response.json.toString());
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

            ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();
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
            try {
                ContentProviderResult[] res = getContentResolver().applyBatch(ContactsContract.AUTHORITY, ops);
            } catch (Exception e){
                Log.i("ContactDownloadService", e.getMessage());
            }


        } catch (Exception e) {
            Log.i("ContactDownloadService", "addContact");
        }

    }
}