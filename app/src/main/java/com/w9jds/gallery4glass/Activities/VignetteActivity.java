package com.w9jds.gallery4glass.Activities;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.media.AudioManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.speech.RecognizerIntent;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.glass.app.Card;
import com.google.android.glass.media.Sounds;
import com.google.android.glass.widget.CardScrollView;
import com.w9jds.gallery4glass.Adapters.GalleryAdapter;
import com.w9jds.gallery4glass.Classes.Paths;
import com.w9jds.gallery4glass.Classes.SingleMediaScanner;
import com.w9jds.gallery4glass.Classes.Size;
import com.w9jds.gallery4glass.R;
import com.w9jds.gdk_progress_widget.SliderView;

import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.List;

public class VignetteActivity extends Activity
{
    private static final int SPEECH_REQUEST = 0;

    static final Size FULL_COMPOSITE_SIZE = new Size(1920, 1080);
    private static final Paint SCALE_PAINT;
    private static final Paint SCREEN_PAINT;
    private static final RectF SCREEN_POSITION = new RectF(0.645833F, 0.037037F, 0.979167F, 0.37037F);

    private String mSpoken;

    private GalleryAdapter mGalleryAdapter;
    private Paths mPaths = new Paths();
    private int mVignettePosition;
    private AudioManager mAudioManager;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        Intent iThis = getIntent();
        iThis.getExtras();
        mPaths = iThis.getParcelableExtra("PathsObject");

        mPaths.insertString( getString(R.string.vignette_activity_label), 0);
        mPaths.insertString( getString(R.string.vignette_text_card) , 1);

        CreatePictureView();
    }

//    @Override
//    public void onStart()
//    {
//        super.onStart();
//        // The rest of your onStart() code.
//        EasyTracker.getInstance(this).activityStart(this);  // Add this method.
//    }
//
//    @Override
//    public void onStop()
//    {
//        super.onStop();
//        // The rest of your onStop() code.
//        EasyTracker.getInstance(this).activityStop(this);  // Add this method.
//    }

    private void displaySpeechRecognizer()
    {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        startActivityForResult(intent, SPEECH_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        if (resultCode == RESULT_OK)
        {
            switch (requestCode)
            {
                case SPEECH_REQUEST:
                    List<String> results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                    mSpoken = results.get(0);

                    //set the view to a new menu layout
                    setContentView(R.layout.menu_layout);
                    //set the icon to the vignette icon
                    ((ImageView) findViewById(R.id.icon)).setImageResource(R.drawable.ic_vignette_medium);
                    //and set the label
                    ((TextView) findViewById(R.id.label)).setText(getString(R.string.making_vignette_label));

                    //make sure it has the slider view in it
                    SliderView svProgress = (SliderView) findViewById(R.id.slider);
                    //and start the progressbar as indeterminate
                    svProgress.startIndeterminate();

                    //create composite
                    startCompositeCreation();

                    break;
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void startCompositeCreation()
    {
        (new CreateComposite(mPaths, mVignettePosition, this)).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private void CreatePictureView()
    {
        CardScrollView csvCardsView = new CardScrollView(this);
        mGalleryAdapter = new GalleryAdapter(this, mPaths.getImagePaths());
        csvCardsView.setAdapter(mGalleryAdapter);
        csvCardsView.activate();

        csvCardsView.setOnItemClickListener(new AdapterView.OnItemClickListener()
        {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id)
            {
                switch(position)
                {
                    case 0:
                        mAudioManager.playSoundEffect(Sounds.DISALLOWED);
                        break;
                    case 1:
                        mAudioManager.playSoundEffect(Sounds.TAP);
                        //save the card index that was selected
                        mVignettePosition = position;

                        //display speech recognition screen
                        displaySpeechRecognizer();
                        break;
                    default:
                        mAudioManager.playSoundEffect(Sounds.TAP);
                        //save the card index that was selected
                        mVignettePosition = position;

                        //set the view to a new menu layout
                        setContentView(R.layout.menu_layout);
                        //set the icon to the vignette icon
                        ((ImageView)findViewById(R.id.icon)).setImageResource(R.drawable.ic_vignette_medium);
                        //and set the label
                        ((TextView)findViewById(R.id.label)).setText(getString(R.string.making_vignette_label));

                        //make sure it has the slider view in it
                        SliderView svProgress = (SliderView)findViewById(R.id.slider);
                        //and start the progressbar as indeterminate
                        svProgress.startIndeterminate();

                        //create the vignette and save it
                        startCompositeCreation();
                        break;
                }
            }
        });

        //set the view of this activity
        setContentView(csvCardsView);
    }

    static
    {
        SCALE_PAINT = new Paint();
        SCALE_PAINT.setFilterBitmap(true);
        SCALE_PAINT.setDither(true);

        SCREEN_PAINT = new Paint(SCALE_PAINT);
        SCREEN_PAINT.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SCREEN));
    }

    public class CreateComposite extends AsyncTask<Void, Void, Boolean>
    {
        private Paths mcpPaths;
        private int miVignettePosition;
        private Context mcContext;

        //Constructor
        public CreateComposite(Paths cpPaths, int iVignettePosition, Context cContext)
        {
            mcpPaths = cpPaths;
            miVignettePosition = iVignettePosition;
            mcContext = cContext;
        }

        @Override
        protected Boolean doInBackground(Void... params)
        {
//            EasyTracker.getInstance(getApplicationContext()).send(MapBuilder.createEvent(
//                    "Creation",     // Event category (required)
//                    "Made",  // Event action (required)
//                    "Vignette_Made",   // Event label
//                    null).build());

            //pull in just the info for the main image of the vignette
            Bitmap bitMain = BitmapFactory.decodeFile(mcpPaths.getImagePathsIndex(mcpPaths.getMainPosition() + 2));
            //set the size on the image
            Size sWhole = FULL_COMPOSITE_SIZE;

            Bitmap bitWhole = Bitmap.createBitmap(sWhole.Width, sWhole.Height, Bitmap.Config.ARGB_8888);
            Canvas cBuild = new Canvas(bitWhole);

            //calculate the position to start into the top of the image so that the aspect ratio is preserved
            int i = (int)((sWhole.Height - bitMain.getHeight() * sWhole.Width / bitMain.getWidth()) / 2.0F);
            cBuild.drawBitmap(bitMain, null, new Rect(0, i, sWhole.Width, sWhole.Height - i), SCALE_PAINT);

            //draw the vignette overlay on top of that main image
            cBuild.drawBitmap(BitmapFactory.decodeResource(mcContext.getResources(), R.drawable.vignette_overlay), null, new Rect(0, 0, sWhole.Width, sWhole.Height), SCALE_PAINT);

            //if user selected the text option
            if (miVignettePosition == 1)
            {
                //resize the bitmap to 640 x 360
                Bitmap bView = resizeBitmap(loadBitmapFromView());

                //turn the view into a bitmap and draw it in the top right hand corner
                cBuild.drawBitmap(bView, null, new Rect(Math.round(SCREEN_POSITION.left * sWhole.Width), Math.round(SCREEN_POSITION.top * sWhole.Height), Math.round(SCREEN_POSITION.right * sWhole.Width), Math.round(SCREEN_POSITION.bottom * sWhole.Height)), SCREEN_PAINT);
            }
            //otherwise
            else
            {
                //resize the bitmap to 640 x 360
                Bitmap bMini = resizeBitmap(BitmapFactory.decodeFile(mcpPaths.getImagePathsIndex(miVignettePosition)));

                //take the second image and draw it in the top right hand corner
                cBuild.drawBitmap(bMini, null, new Rect(Math.round(SCREEN_POSITION.left * sWhole.Width), Math.round(SCREEN_POSITION.top * sWhole.Height), Math.round(SCREEN_POSITION.right * sWhole.Width), Math.round(SCREEN_POSITION.bottom * sWhole.Height)), SCREEN_PAINT);
            }

            try
            {



                //get the path to the camera directory
                String path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).toString() + "/Camera";
                //create a new output stream
                OutputStream fOut;

                String[] saPath = mcpPaths.getImagePathsIndex(mcpPaths.getMainPosition() + 2).split("/|\\.");

                //create a new file with the added _x for the vignette to be stored in
                java.io.File fImage = new java.io.File(path, saPath[saPath.length - 2] + "_x.jpg");
                //create an output stream with the new file
                fOut = new FileOutputStream(fImage);

                //compress the image we just made
                bitWhole.compress(Bitmap.CompressFormat.JPEG, 100, fOut);
                fOut.flush();
                fOut.close();

                //add to media scanner
                new SingleMediaScanner(mcContext, fImage);
            }
            catch (Exception e)
            {
                Log.d("VignetteMaker", e.getCause().toString());
            }

            return false;
        }

        public Bitmap resizeBitmap(Bitmap bVig)
        {
            //make a new bitmap for the aspect ratio resize
            Bitmap bMini = Bitmap.createBitmap(640, 360, Bitmap.Config.ARGB_8888);
            Canvas cMini = new Canvas(bMini);

            //calculate the position to start into the top of the image so that the aspect ratio is preserved
            int i = (int)((360 - bVig.getHeight() * 640 / bVig.getWidth()) / 2.0F);
            //draw the new version of this image
            cMini.drawBitmap(bVig, null, new Rect(0, i, 640, 360 - i), SCALE_PAINT);

            return bMini;
        }

        public Bitmap loadBitmapFromView()
        {
            //create a new card and place the input text in it
            Card txtCard = new Card(mcContext);
            txtCard.setText(mSpoken);

            //change the card into a view
            View vCard = txtCard.getView();

            //get the measurements for the view (since it hasn't been displayed)
            vCard.measure(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            //create a new bitmap matching the size of the view
            Bitmap bView = Bitmap.createBitmap(vCard.getMeasuredWidth(), vCard.getMeasuredHeight(), Bitmap.Config.ARGB_8888);
            //create a new canvas for the bitmap
            Canvas cView = new Canvas(bView);
            //set up the layout for the view
            vCard.layout(0, 0, vCard.getMeasuredWidth(), vCard.getMeasuredHeight());
            //and draw it
            vCard.draw(cView);

            return bView;
        }

        @Override
        protected void onPostExecute(Boolean uploaded)
        {
            setContentView(R.layout.menu_layout);
            ((ImageView)findViewById(R.id.icon)).setImageResource(R.drawable.ic_done_50);
            ((TextView)findViewById(R.id.label)).setText(getString(R.string.made_vignette_label));

            mAudioManager.playSoundEffect(Sounds.SUCCESS);

            new Handler().postDelayed(new Runnable()
            {
                public void run()
                {
                    Intent returnIntent = new Intent();
                    setResult(RESULT_OK, returnIntent);
                    finish();
                }
            }, 1000);
        }

    }

}
