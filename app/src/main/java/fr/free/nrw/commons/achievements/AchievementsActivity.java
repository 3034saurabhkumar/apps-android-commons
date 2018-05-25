package fr.free.nrw.commons.achievements;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import com.dinuscxj.progressbar.CircleProgressBar;


import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.Utils;
import fr.free.nrw.commons.auth.SessionManager;
import fr.free.nrw.commons.mwapi.MediaWikiApi;
import fr.free.nrw.commons.theme.NavigationBaseActivity;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;

/**
 * activity for sharing feedback on uploaded activity
 */
public class AchievementsActivity extends NavigationBaseActivity {

    private static final double BADGE_IMAGE_WIDTH_RATIO = 0.4;
    private static final double BADGE_IMAGE_HEIGHT_RATIO = 0.3;

    @BindView(R.id.achievement_badge)
    ImageView imageView;
    @BindView(R.id.achievement_level)
    TextView textView;
    @BindView(R.id.toolbar)
    Toolbar toolbar;
    @BindView(R.id.thanks_received)
    TextView thanksReceived;
    @BindView(R.id.images_uploaded_progressbar)
    CircleProgressBar imagesUploadedProgressbar;
    @BindView(R.id.images_used_by_wiki_progressbar)
    CircleProgressBar imagesUsedByWikiProgessbar;
    @Inject
    SessionManager sessionManager;
    @Inject
    MediaWikiApi mediaWikiApi;

    private CompositeDisposable compositeDisposable = new CompositeDisposable();

    /**
     * This method helps in the creation Achievement screen and
     * dynamically set the size of imageView
     *
     * @param savedInstanceState Data bundle
     */
    @Override
    @SuppressLint("StringFormatInvalid")
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_achievements);
        ButterKnife.bind(this);
        /**
         * DisplayMetrics used to fetch the size of the screen
         */
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int height = displayMetrics.heightPixels;
        int width = displayMetrics.widthPixels;

        /**
         * Used for the setting the size of imageView at runtime
         */
        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams)
                imageView.getLayoutParams();
        params.height = (int) (height * BADGE_IMAGE_HEIGHT_RATIO);
        params.width = (int) (width * BADGE_IMAGE_WIDTH_RATIO);
        imageView.setImageResource(R.drawable.featured);
        imageView.requestLayout();

        setSupportActionBar(toolbar);
        setAchievements();
        initDrawer();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_about, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.share_app_icon) {
            View rootView = getWindow().getDecorView().findViewById(android.R.id.content);
            Bitmap screenShot = Utils.getScreenShot(rootView);
            shareScreen(screenShot);
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * To take bitmap and store it temporary storage and share it
     *
     * @param bitmap
     */
    void shareScreen(Bitmap bitmap) {
        try {
            File file = new File(this.getExternalCacheDir(), "screen.png");
            FileOutputStream fOut = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fOut);
            fOut.flush();
            fOut.close();
            file.setReadable(true, false);
            final Intent intent = new Intent(android.content.Intent.ACTION_SEND);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(file));
            intent.setType("image/png");
            startActivity(Intent.createChooser(intent, "Share image via"));
        } catch (IOException e) {
            //Do Nothing
        }
    }

    /**
     * To call the API to get results in form Single<JSONObject>
     * which then calls parseJson when results are fetched
     */
    private void setAchievements() {
        compositeDisposable.add(mediaWikiApi
                .getAchievements(sessionManager.getCurrentAccount().name)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        jsonObject -> parseJson(jsonObject)
                ));
    }

    /**
     * used to parse the JSONObject containing results
     *
     * @param object
     */
    private void parseJson(JSONObject object) {
        Achievements achievements = new Achievements();
        try {
            achievements.setUniqueUsedImages(object.getInt("uniqueUsedImages"));
            achievements.setArticlesUsingImages(object.getInt("articlesUsingImages"));
            achievements.setThanksReceived(object.getInt("thanksReceived"));
            achievements.setImagesEditedBySomeoneElse(object.getInt("imagesEditedBySomeoneElse"));

        } catch (JSONException e) {
            e.printStackTrace();
        }
        inflateAchievements(achievements);
    }

    private void inflateAchievements( Achievements achievements){
        thanksReceived.setText(Integer.toString(achievements.getThanksReceived()));
        imagesUsedByWikiProgessbar.setProgress(100*achievements.getUniqueUsedImages()/25);
        imagesUsedByWikiProgessbar.setProgressTextFormatPattern(achievements.getUniqueUsedImages() + "/25");
    }

    /**
     * Creates a way to change current activity to AchievementActivity
     *
     * @param context
     */
    public static void startYourself(Context context) {
        Intent intent = new Intent(context, AchievementsActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        context.startActivity(intent);
    }

}
