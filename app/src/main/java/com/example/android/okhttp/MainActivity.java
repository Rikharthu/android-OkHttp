package com.example.android.okhttp;

import android.os.Handler;
import android.os.Looper;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Cache;
import okhttp3.CacheControl;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Headers;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {
    public static final String TAG=MainActivity.class.getSimpleName();

    public static final String URL="https://guides.codepath.com/android/Using-OkHttp";
    public static final String JSON_URL="https://guides.codepath.com/android/Using-OkHttp";
    // should be a singleton
    OkHttpClient client;
    private Button mDownloadButton;
    private Button mDownloadJSONButton;
    private TextView mOutputTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mDownloadButton= (Button) findViewById(R.id.downloadButton);
        mDownloadJSONButton= (Button) findViewById(R.id.downloadJSONButton);
        mOutputTextView= (TextView) findViewById(R.id.output);

        // create cache (not required)
        int cacheSize = 10 * 1024 * 1024; // 10 MiB
        Cache cache = new Cache(getApplication().getCacheDir(), cacheSize);

        // Create OkHttpClient (it should be Singleton)
        client = new OkHttpClient.Builder().cache(cache).build();

        // If you need to add some query parameters. (can be skipped)
        HttpUrl.Builder urlBuilder = HttpUrl.parse(URL).newBuilder();
        urlBuilder.addQueryParameter("v", "1.0");
        urlBuilder.addQueryParameter("q", "android");
        urlBuilder.addQueryParameter("rsz", "8");
        String url = urlBuilder.build().toString();

        final Request request = new Request.Builder()
                .header("Authorization", "token abcd") // headers can be added (can be skipped)
                .url(url)
                .build();

        // get Handler to the main thread (can be skipped if runOnUi() is used)
        final Handler handler = new Handler(Looper.getMainLooper());

        mDownloadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Create and Asynchronous call
                // Get a handler that can be used to post to the main thread
                Log.d(TAG,"downloading from url: "+request.url());
                client.newCall(request).enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        e.printStackTrace();
                    }

                    @Override
                    public void onResponse(Call call, final Response response) throws IOException {
                        if (!response.isSuccessful()) {
                            throw new IOException("Unexpected code " + response);
                        }else{
                            // get response body data
                            final String responseData=response.body().string();
                            // update UI (we are on a different thread)
                            Runnable r = new Runnable() {
                                @Override
                                public void run() {
                                        mOutputTextView.setText(responseData);
                                }
                            };
                            // post to the main thread to edit UI
                            handler.post(r);
                            // or
                            // From within an Activity,
                            // usually executed within a worker thread to update UI
//                            runOnUiThread(r);

                            // process response headers
                            Headers responseHeaders = response.headers();
                            for (int i = 0; i < responseHeaders.size(); i++) {
                                Log.d(TAG, responseHeaders.name(i) + ": " + responseHeaders.value(i));
                            }
                            // headers can also be accesses directly
                            String header = response.header("Date");
                            Log.d(TAG,"date header - "+header);
                        }
                    }
                });
            }
        });

        // Downloading JSON data
        mDownloadJSONButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final Request request = new Request.Builder()
                        .url("https://api.github.com/users/codepath")
                        // accept the response only if it is cached
//                        .cacheControl(new CacheControl.Builder().onlyIfCached().build())
                        .build();
                /*  .noCache() - force a network response
                    .maxStale(365, TimeUnit.DAYS) - specify maximum staleness age for cached response
                 */

                // Create new gson object
                final Gson gson = new Gson();
                Log.d(TAG,"downloading from url: "+request.url());
                client.newCall(request).enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        e.printStackTrace();
                    }

                    @Override
                    public void onResponse(Call call, final Response response) throws IOException {
                        try {
//                            // retreive cached response
                            final Response cachedResponse = response.cacheResponse();
                            // if no cached object, result will be null
                            if (cachedResponse != null) {
//                                Log.d("here",cachedResponse.toString());
                                Log.d(TAG,"response\t\t\t\t"+response);
                                Log.d(TAG,"response date\t\t\t"+response.header("Date"));
                                Log.d(TAG,"cached response\t\t"+cachedResponse);
                                Log.d(TAG,"cached response date\t"+cachedResponse.header("Date"));
                                Log.d(TAG,"network response\t\t"+response.networkResponse());
                                Log.d(TAG,"");
                                Log.d(TAG,"");
                                /*  response				Response{protocol=http/1.1, code=200, message=OK, url=https://api.github.com/users/codepath}
                                    response date			Mon, 24 Oct 2016 13:09:09 GMT
                                    cached response		    Response{protocol=http/1.1, code=200, message=OK, url=https://api.github.com/users/codepath}
                                    cached response date	Thu, 20 Oct 2016 16:47:11 GMT
                                    network response		Response{protocol=http/1.1, code=304, message=Not Modified, url=https://api.github.com/users/codepath} */
                            }

                            // retrieve data from current response
                            String responseData = response.body().string();
                            // create json object from string
                            JSONObject json = new JSONObject(responseData);
                            final String owner = json.getString("name");
                            // use Gson to convert json into an object
                            final GitUser user = gson.fromJson(responseData, GitUser.class);
                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(MainActivity.this, user.toString(), Toast.LENGTH_SHORT).show();
                                }
                            });
                    } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
        });

    }
}
