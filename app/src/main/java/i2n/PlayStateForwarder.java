package i2n;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.StrictMode;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class PlayStateForwarder extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // fuck your main thread
        StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder().permitAll().build());

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initServerUrlField();
        registerChangeReceiver();
        setStatus(R.string.status_waiting);
    }

    private void registerChangeReceiver() {
        registerReceiver(
                new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        publish(new PlayState(
                                intent.getBooleanExtra("playing", true),
                                intent.getStringExtra("track"),
                                intent.getStringExtra("album"),
                                intent.getStringExtra("artist")));
                    }
                },
                new IntentFilter("com.android.music.playstatechanged"));
    }

    private void initServerUrlField() {
        EditText serverUrlField = findViewById(R.id.server_url);
        serverUrlField.setText(getServerUrl());
        serverUrlField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                getPreferences(Context.MODE_PRIVATE).edit().putString("server_url", editable.toString()).apply();
            }
        });
    }

    private void setStatus(int id, Object... formatArgs) {
        ((TextView) findViewById(R.id.status_label))
                .setText(getResources().getString(id, formatArgs));
    }

    private String getServerUrl() {
        return getPreferences(Context.MODE_PRIVATE).getString("server_url", "http://10.0.2.2:12321");
    }

    private void publish(PlayState state) {
        HttpURLConnection conn = null;
        String message = "playing=" + state.playing + "\n" +
                "track=" + state.track + "\n" +
                "album=" + state.album + "\n" +
                "artist=" + state.artist;

        try {
            URL url = new URL(getServerUrl());
            byte[] bytes = message.getBytes(StandardCharsets.UTF_8);

            conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(5000);
            conn.setDoOutput(true);
            conn.setFixedLengthStreamingMode(bytes.length);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "text/plain; utf-8");
            conn.getOutputStream().write(bytes);
            conn.getResponseCode();

            setStatus(R.string.status_success, message);
        } catch (Exception e) {
            setStatus(R.string.status_error, e.getMessage(), message);
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    private static final class PlayState {
        private final boolean playing;
        private final String track;
        private final String album;
        private final String artist;

        private PlayState(boolean playing, String track, String album, String artist) {
            this.playing = playing;
            this.track = track;
            this.album = album;
            this.artist = artist;
        }
    }
}
