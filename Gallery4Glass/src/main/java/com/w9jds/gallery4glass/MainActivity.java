package com.w9jds.gallery4glass;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.animation.Animator;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.glass.media.Sounds;
import com.google.android.glass.widget.CardScrollView;
import com.w9jds.gallery4glass.Adapters.csaAdapter;
import com.w9jds.gallery4glass.Classes.AuthPreferences;
import com.w9jds.gallery4glass.Classes.ServiceHttpClient;
import com.w9jds.gallery4glass.Classes.cPaths;
import com.w9jds.gallery4glass.Widget.SliderView;

import org.apache.http.client.methods.HttpPost;
//import org.apache.http.entity.mime.HttpMultipartMode;
//import org.apache.http.entity.mime.MultipartEntityBuilder;
//import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;

public class MainActivity extends Activity
{
    private static final int AUTHORIZATION_CODE = 1993;

    private final String CAMERA_IMAGE_BUCKET_NAME = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).toString() + "/Camera";
    private final String CAMERA_IMAGE_BUCKET_ID = getBucketId(CAMERA_IMAGE_BUCKET_NAME);

    private ConnectivityManager mcmCon;
    private AudioManager maManager;

    private AuthPreferences mauthPreferences;

    //custom adapter
    private csaAdapter mcvAdapter;
    //custom object
    private cPaths mcpPaths = new cPaths();

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        mcmCon = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        maManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        mauthPreferences = new AuthPreferences(this);

        CreatePictureView();
    }

    private void CreatePictureView()
    {

        //get all the images from the camera folder (paths)
        mcpPaths.setImagePaths(getCameraImages());

        Collections.reverse(mcpPaths.getImagePaths());
        //create a new card scroll viewer for this context
        CardScrollView csvCardsView = new CardScrollView(this);
        //create a new adapter for the scroll viewer
        mcvAdapter = new csaAdapter(this, mcpPaths.getImagePaths());
        //set this adapter as the adapter for the scroll viewer
        csvCardsView.setAdapter(mcvAdapter);
        //activate this scroll viewer
        csvCardsView.activate();
        //add a listener to the scroll viewer that is fired when an item is clicked
        csvCardsView.setOnItemClickListener(new AdapterView.OnItemClickListener()
        {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id)
            {
                maManager.playSoundEffect(Sounds.TAP);
                //save the card index that was selected
                mcpPaths.setMainPosition(position);
                //open the menu
                openOptionsMenu();
            }
        });

        //set the view of this activity
        setContentView(csvCardsView);
    }

    /***
     * Register for broadcasts
     */
    @Override
    protected void onResume()
    {
        super.onResume();
    }

    /***
     * Unregister for broadcasts
     */
    @Override
    protected void onPause()
    {
        super.onPause();
    }

    public String getBucketId(String path)
    {
        return String.valueOf(path.toLowerCase().hashCode());
    }

    /***
     * Get all the image file paths on this device (from the camera folder)
     * @return an arraylist of all the file paths
     */
    public ArrayList<String> getCameraImages()
    {
        final String[] projection = { MediaStore.Images.Media.DATA };
        final String selection = MediaStore.Images.Media.BUCKET_ID + " = ?";
        final String[] selectionArgs = { CAMERA_IMAGE_BUCKET_ID };
        final Cursor cursor = this.getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, projection, selection, selectionArgs, null);
        ArrayList<String> result = new ArrayList<String>(cursor.getCount());

        if (cursor.moveToFirst())
        {
            final int dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            do
            {
                final String data = cursor.getString(dataColumn);
                result.add(data);

            } while (cursor.moveToNext());
        }

        cursor.close();
        return result;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        if (resultCode == RESULT_OK)
        {
            switch (requestCode)
            {

                case 1:
                    CreatePictureView();

                    break;

//                case AUTHORIZATION_CODE:
////                    requestToken();
//
//                    break;
            }
        }
    }

    private void requestToken(String sScope)
    {
        AccountManager accountManager = AccountManager.get(this);
        Account userAccount = null;
        String user = mauthPreferences.getUser();
        for (Account account : accountManager.getAccountsByType("com.google"))
        {
            if (account.name.equals(user))
            {
                userAccount = account;
                break;
            }
        }

        accountManager.getAuthToken(userAccount, "oauth2:" + sScope, null, this, new OnTokenAcquired(), null);
    }

    private void invalidateToken()
    {
        AccountManager accountManager = AccountManager.get(this);
        accountManager.invalidateAuthToken("com.google", mauthPreferences.getToken());

        mauthPreferences.setToken(null);
    }

    private void createDriveCall(String sPath)
    {
//        https://www.googleapis.com/upload/drive/v2/files?uploadType=media

//        Ion.with(this, "https://www.googleapis.com/upload/drive/v2/files?uploadType=media")
//                .setHeader("Content-Type", "image/jpeg")
//                .setHeader("Authorization", "Bearer " + mauthPreferences.getToken())
//                .setFileBody(new File(mcpPaths.getCurrentPositionPath()))
//                .asJsonObject()
//                .setCallback(new FutureCallback<JsonObject>() {
//                    @Override
//                    public void onCompleted(Exception e, JsonObject result)
//                    {
//                        // do stuff with the result or error
//                        e.printStackTrace();
//                    }
//                });

        (new SendImageToService(sPath)).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    @Override
    public boolean onOptionsItemSelected(android.view.MenuItem iItem)
    {
        SliderView svProgress;

        switch (iItem.getItemId())
        {
            case R.id.vignette_menu_item:

                Intent iVignette = new Intent(this, VignetteActivity.class);
                iVignette.putExtra("PathsObject", mcpPaths);
                startActivityForResult(iVignette, 1);

                return true;

            case R.id.effects_menu_item:

                Intent iEffects = new Intent(this, EffectActivity.class);
                iEffects.putExtra("PathsObject", mcpPaths);
                startActivityForResult(iEffects, 1);

                return true;

            case R.id.delete_menu_item:
                //set the text as deleting
                setContentView(R.layout.menu_layout);
                ((ImageView)findViewById(R.id.icon)).setImageResource(R.drawable.ic_delete_50);
                ((TextView)findViewById(R.id.label)).setText(getString(R.string.deleting_label));

                svProgress = (SliderView)findViewById(R.id.slider);
                svProgress.startProgress(1000, new Animator.AnimatorListener()
                {
                    @Override
                    public void onAnimationStart(Animator animation)
                    {

                    }

                    @Override
                    public void onAnimationEnd(Animator animation)
                    {
                        //pull the file from the path of the selected item
                        File fPic = new File(mcpPaths.getCurrentPositionPath());
                        //delete the image
                        fPic.delete();
                        //refresh the folder
                        sendBroadcast(new Intent(Intent.ACTION_MEDIA_MOUNTED, Uri.parse("file://" + Environment.getExternalStorageDirectory())));
                        //remove the selected item from the list of images
                        mcpPaths.removeCurrentPositionPath();
                        //let the adapter know that the list of images has changed
                        mcvAdapter.notifyDataSetChanged();
                        //handled

                        setContentView(R.layout.menu_layout);
                        ((ImageView)findViewById(R.id.icon)).setImageResource(R.drawable.ic_done_50);
                        ((TextView)findViewById(R.id.label)).setText(getString(R.string.deleted_label));

                        maManager.playSoundEffect(Sounds.SUCCESS);

                        new Handler().postDelayed(new Runnable()
                        {
                            public void run()
                            {
                                CreatePictureView();
                            }
                        }, 1000);
                    }

                    @Override
                    public void onAnimationCancel(Animator animation)
                    {
                        CreatePictureView();
                    }

                    @Override
                    public void onAnimationRepeat(Animator animation)
                    {

                    }
                });

                return true;

            case R.id.upload_menu_item:

                if (mcmCon.getActiveNetworkInfo().isConnected())
                {
                    //get google account credentials and store to member variable
                    AccountManager amManager = AccountManager.get(this);
                    //get a list of all the accounts on the device
                    Account[] myAccounts = amManager.getAccounts();
                    //for each account
                    for (int i = 0; i < myAccounts.length; i++)
                    {
                        //if the account type is google
                        if (myAccounts[i].type.equals("com.google"))
                        {
                            mauthPreferences.setUser(myAccounts[i].name);
                            requestToken("https://www.googleapis.com/auth/drive.file");
                        }
                    }
                }

//                Ion.with(getApplicationContext(), "https://accounts.google.com/o/oauth2/device/code")
//                        .addQuery("client_id", "132043361416-mtdnpsn1vmp9nb51o9hege0k2e9c3klh.apps.googleusercontent.com")
//                        .addQuery("scope", "email profile")
//                        .asJsonObject()
//                        .withResponse()
//                        .setCallback(new FutureCallback<Response<JsonObject>>()
//                        {
//                            @Override
//                            public void onCompleted(Exception e, Response<JsonObject> jsonObjectResponse)
//                            {
//                                e.getCause().printStackTrace();
//                            }
//                        });

                return true;

            default:
                return super.onOptionsItemSelected(iItem);
        }
    }

    private class OnTokenAcquired implements AccountManagerCallback<Bundle>
    {

        @Override
        public void run(AccountManagerFuture<Bundle> result)
        {
            try
            {
                Bundle bundle = result.getResult();

                Intent launch = (Intent) bundle.get(AccountManager.KEY_INTENT);

                if (launch != null)
                    startActivityForResult(launch, AUTHORIZATION_CODE);

                else
                {
                    String token = bundle.getString(AccountManager.KEY_AUTHTOKEN);

                    mauthPreferences.setToken(token);

                    createDriveCall("PosttoDrive");
                }
            }

            catch (Exception e)
            {
                throw new RuntimeException(e);
            }
        }
    }


    public class SendImageToService extends AsyncTask<Void, Void, Boolean>
    {
        String msPath;

        SendImageToService(String sPath)
        {
            msPath = sPath;
        }

        @Override
        protected Boolean doInBackground(Void... params)
        {
            try
            {
                DefaultHttpClient dhcClient = new ServiceHttpClient(getApplicationContext());

                HttpPost hpPost = new HttpPost("");
                hpPost.addHeader("Content-Type", "image/jpeg");
                hpPost.addHeader("Authorization", "Bearer " + mauthPreferences.getToken());

//                MultipartEntityBuilder meBuilder = MultipartEntityBuilder.create();
//
//                meBuilder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
//
////                MultipartEntity meEntity = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE);
//
////                ByteArrayOutputStream bosStream = new ByteArrayOutputStream();
////                BitmapFactory.decodeFile(mcpPaths.getCurrentPositionPath()).compress(Bitmap.CompressFormat.JPEG, 100, bosStream);
////
////                String[] saPath = mcpPaths.getCurrentPositionPath().split("/");
////
////                meEntity.addPart("image", new ByteArrayBody(bosStream.toByteArray(), saPath[saPath.length - 1]));
//
//                meBuilder.addPart("image", new FileBody(new File(mcpPaths.getCurrentPositionPath())));
//
//                hpPost.setEntity(meBuilder.build());
//                dhcClient.execute(hpPost);

            }
            catch(Exception ex)
            {
                ex.getCause().printStackTrace();

            }

            return true;
        }
    }
}



