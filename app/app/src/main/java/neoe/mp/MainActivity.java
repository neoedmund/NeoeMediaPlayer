package neoe.mp;

import android.content.Context;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.OptIn;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.analytics.AnalyticsListener;
import androidx.media3.session.MediaSession;
import androidx.navigation.ui.AppBarConfiguration;

import java.io.File;

import neoe.mp.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "me";
    private AppBarConfiguration appBarConfiguration;
    ActivityMainBinding binding;
    Lyrics lyrics;
    private static Thread lyricsThread;

    void confirmPermission() {
        if (ContextCompat.checkSelfPermission(getApplicationContext()
                , android.Manifest.permission.WRITE_EXTERNAL_STORAGE ) == PackageManager.PERMISSION_GRANTED) {
        } else {
            requestPermissions(
                    new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    123);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                           int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults.length > 0 &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED) {

        }
    }

    @OptIn(markerClass = UnstableApi.class)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);


        MainActivity acti = this;
        Context context = getApplicationContext();

        player = new ExoPlayer.Builder(context).build();
        player.setRepeatMode(Player.REPEAT_MODE_ALL);
        lyrics = Lyrics.getInst(this);
        confirmPermission();
        if (lyricsThread == null) {
            lyricsThread = new Thread(() -> {
                while (true) {
                    Log.d("lyricsThread", "lyricsThread: loop ");
                    try {
                        new Handler(Looper.getMainLooper()).post(() -> {
                            if (lyrics != null && acti != null && acti.player != null && acti.player.isPlaying()) {
                                long x=acti.player.getCurrentPosition();
                                acti.runOnUiThread(()->{lyrics.process(x);});
                            }
                        });
                        asleep(500);
                    } catch (Throwable ex) {
                        Log.w("me", "Lyrics: process loop", ex);
                        asleep(100);
                    }
                }
            });
            lyricsThread.start();
        }
        player.addListener(new Player.Listener() {
            @Override
            public void onPositionDiscontinuity(Player.PositionInfo oldPosition, Player.PositionInfo newPosition, int reason) {
                lyrics.seeked();
            }

            @Override
            public void onMediaItemTransition(@Nullable MediaItem mediaItem, int reason) {
                Log.i(TAG, "onMediaItemTransition: " + mediaItem);
                if (mediaItem != null && mediaItem.localConfiguration != null)
                    lyrics.open(new File(mediaItem.localConfiguration.uri.getPath()).getAbsolutePath(), player.getDuration());
            }

            public void onPlaybackStateChanged(int state) {
                if (state == ExoPlayer.STATE_ENDED) {
                    lyrics.finished(); //not called?
                }
            }

        });
        player.setPlayWhenReady(true);
        mediaSession = new MediaSession.Builder(context, player).build();
        binding.player.setPlayer(player);
        mGetContent = registerForActivityResult(new ActivityResultContracts.GetContent(),
                new ActivityResultCallback<Uri>() {
                    @Override
                    public void onActivityResult(Uri uri) {
                        MediaItem mediaItem = MediaItem.fromUri(uri);
                        player.addMediaItem(mediaItem);
                        player.prepare();
                        player.seekTo(player.getMediaItemCount()-1,0);
                    }
                });


    }

    ActivityResultLauncher<String> mGetContent;
    ExoPlayer player;
    MediaSession mediaSession;

    private static void asleep(int i) {
        try {
            Thread.sleep(i);
        } catch (InterruptedException e) {
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.selectFile) {
            mGetContent.launch("*/*");
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


}