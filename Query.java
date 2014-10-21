/*
 * this is privacy project.
 * not applied any copyrights yet.
 * - author: admin@devflow.kr
 */

package **INSERT_YOUR_PACKAGE**;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CookieStore;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.params.HttpParams;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;


public class Query {

    /**
     * !IMPORTANT! should be turn off debug mode when released app
     */
    public static boolean DEBUG_MODE = true;

    /**
     * version of query class. not your app!
     */
    public static String VERSION = "1.2.0.0";

    /**
     * Logging tag name
     */
    public static String TAG = "devflow.XEQuery/" + VERSION;

    /**
     * target for Queries. should be set only host (exclude http://)
     */
    public static String HOST_NAME = "*";

    /**
     * this setting used to identify app kind
     */
    public static String APP_NAME = "*";

    public static Exception lastException = new Exception();

    /**
     * base instance for query; this class's concept is only one single instance in your app. if you wanna more instance?, fix it like your tastes
     */
    public static Query instance;

    public static String GET = "get";
    public static String POST = "post";
    public static JSONObject EMPTY_JSON;

    // error codes
    public static final int ERR_INVALID_JSON = 311;
    public static final int ERR_CLIENT_PROTOCOL = 312;
    public static final int ERR_UNSUPPORTED_ENCODING = 314;
    public static final int ERR_IO = 315;
    public static final int ERR_EXCEPTION = 9999;

    /**
     * app's context.
     */
    private Context context;

    /**
     * cookie manager for control cookies. inc. communication between web view and http client
     */
    public CookieManager cookieManager;

    public static Queue<RequestQueryData> requestQueue = new LinkedList<RequestQueryData>();

    private DefaultHttpClient httpclient;
    private OnQueryResponseCallback defaultCallback;
    private OnPreQueryCallback defaultPreCallback;


    /**
     * constructor
     *
     * @param _ctx context your app
     */
    public Query(Context _ctx) {
        context = _ctx;
        CookieSyncManager.createInstance(_ctx);
        cookieManager = CookieManager.getInstance();
        CookieSyncManager.getInstance().startSync();

        httpclient = getThreadSafeClient();
        instance = this;

        try {
            EMPTY_JSON = new JSONObject("{}");
        } catch (Exception e) {
            e.printStackTrace();
        }

        //print all request and response for debugging.
        if (DEBUG_MODE) {
            java.util.logging.Logger.getLogger("org.apache.http.wire").setLevel(java.util.logging.Level.FINEST);
            java.util.logging.Logger.getLogger("org.apache.http.headers").setLevel(java.util.logging.Level.FINEST);
            System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.SimpleLog");
            System.setProperty("org.apache.commons.logging.simplelog.showdatetime", "true");
            System.setProperty("org.apache.commons.logging.simplelog.log.httpclient.wire", "debug");
            System.setProperty("org.apache.commons.logging.simplelog.log.org.apache.http", "debug");
            System.setProperty("org.apache.commons.logging.simplelog.log.org.apache.http.headers", "debug");
        }

    }

    /**
     * set default response callback
     *
     * @param oq
     */
    public void setOnQueryResponseCallback(OnQueryResponseCallback oq) {
        defaultCallback = oq;
    }

    /**
     * set default pre query callback
     */
    public void setOnPreQueryCallback(OnPreQueryCallback opc) {
        defaultPreCallback = opc;
    }

    /**
     * generate http client
     *
     * @return httpclient
     */
    public static DefaultHttpClient getThreadSafeClient() {
        DefaultHttpClient client = new DefaultHttpClient();
        ClientConnectionManager mgr = client.getConnectionManager();
        HttpParams params = client.getParams();
        client = new DefaultHttpClient(new ThreadSafeClientConnManager(params, mgr.getSchemeRegistry()), params);
        client.getParams().setParameter(CoreProtocolPNames.USER_AGENT, APP_NAME);
        return client;
    }

    @Deprecated
    private static void SyncPHPSession(DefaultHttpClient httpClient) {
        try {
            String cookie = CookieManager.getInstance().getCookie("http://" + HOST_NAME);

            String[] cookies = cookie.split(";");
            for (String keyValue : cookies) {
                keyValue = keyValue.trim();
                String[] cookieSet = keyValue.split("=");
                String key = cookieSet[0];
                String value = cookieSet[1];
                if (!"PHPSESSID".equals(key)) {
                    continue;
                }
                BasicClientCookie bCookie = new BasicClientCookie(key, value);
                bCookie.setDomain("." + HOST_NAME);
                bCookie.setPath("/");
                CookieStore store = httpClient.getCookieStore();
                store.addCookie(bCookie);
            }
        } catch (Exception e) {
            if (DEBUG_MODE)
                Log.w(TAG, "not found cookies.");
        }
    }


    /**
     * Send Query
     *
     * @param queryMethod query method get or post
     * @param queryId     query id (ex. foo.bar)
     * @param queryParams query params
     */
    public void doQuery(String queryMethod, String queryId, QueryParam... queryParams) {
        if (DEBUG_MODE) {
            Log.d(TAG, "doQuery(" + queryMethod + "," + queryId + ")");
            for (QueryParam qp : queryParams) {
                Log.d(TAG, "[Param] " + qp.key + "=" + qp.val);
            }
        }

        boolean isAccept = true;

        if (defaultPreCallback != null) {
            isAccept = defaultPreCallback.onPreQuery(queryMethod, queryId, queryParams);
        }

        if (isAccept)
            requestQueue.add(new RequestQueryData(queryMethod, queryId, queryParams));

        if (DEBUG_MODE) {
            Log.d(TAG, "Now QueueStack Size : " + requestQueue.size());
        }

        if (requestQueue.size() == 1)
            scheduling();
    }

    /**
     * send query with callback. not default callback
     *
     * @param queryMethod query method. Query.GET or Query.POST
     * @param queryId     query id (ex. foo.bar)
     * @param callback    query on Response Callback
     * @param queryParams query params
     */
    public void doQueryEx(String queryMethod, String queryId, OnQueryResponseCallback callback, QueryParam... queryParams) {
        if (DEBUG_MODE) {
            Log.d(TAG, "doQueryEx(" + queryMethod + "," + queryId + ")");
            for (QueryParam qp : queryParams) {
                Log.d(TAG, "[Param] " + qp.key + "=" + qp.val);
            }
        }

        requestQueue.add(new RequestQueryData(queryMethod, queryId, queryParams, callback));

        if (DEBUG_MODE) {
            Log.d(TAG, "Now QueueStack Size : " + requestQueue.size());
        }

        if (requestQueue.size() == 1)
            scheduling();

    }

    /**
     * do scheduling
     */
    private void scheduling() {
        if (requestQueue.size() > 0) {
            new procQuery().execute(requestQueue.peek());
        }
        if (DEBUG_MODE) {
            Log.d(TAG, ">> Scheduling...");
        }
    }


    public class procQuery extends AsyncTask<RequestQueryData, Void, ResponseQueryData> {

        @Override
        protected void onPreExecute() {
            Log.d(TAG, ">> Query Request");
            super.onPreExecute();
        }

        @Override
        protected ResponseQueryData doInBackground(RequestQueryData... params) {
            ResponseQueryData rqd;
            try {
                SyncPHPSession(httpclient);
                HttpResponse response = null;

                if (params[0].QueryMethod.equals(GET)) {
                    String QueryString = "?act=" + params[0].QueryID;

                    for (int i = 0; i < params[0].QueryParams.length; i++) {
                        QueryString += "&" + params[0].QueryParams[i].key + "=" + params[0].QueryParams[i].val;
                    }

                    HttpGet thGet = new HttpGet("http://" + HOST_NAME + "/index.php" + QueryString);
                    thGet.setHeader("Content-Type", "application/json");

                    response = httpclient.execute(thGet);

                    // boundary send test.
                /*} else if(params[0].QueryMethod.equalsIgnoreCase("image")){
                    if(isDebugMode)
                        Log.d(TAG,"on image send");

                    HttpPost thPost = new HttpPost("http://" + HOST_NAME + "/index.php");
                    MultipartEntityBuilder builder = MultipartEntityBuilder.create();
                    builder.setCharset(MIME.UTF8_CHARSET);
                    String boundary = "-------------" + System.currentTimeMillis();

                    builder.setBoundary(boundary);
                    builder.addTextBody("act", params[0].QueryID.split("\\.")[1]);
                    builder.addTextBody("module", params[0].QueryID.split("\\.")[0]);

                    for(int index=0; index <  params[0].QueryParams.length; index++) {
                        if(params[0].QueryParams[index].key.equalsIgnoreCase("imgfile")) {
                            if(isDebugMode)
                                Log.d(TAG,"add image" + params[0].QueryParams[index].val);
                            builder.addBinaryBody("imgfile", new File(params[0].QueryParams[index].val));
                        } else {
                            builder.addTextBody(params[0].QueryParams[index].key, params[0].QueryParams[index].val);
                        }
                    }

                    thPost.setHeader("Content-Type", "multipart/form-data; boundary=" + boundary);


                    thPost.setEntity(builder.build());

                    response = httpclient.execute(thPost);
                */
                } else if (params[0].QueryMethod.equals(POST)) {
                    HttpPost thPost = new HttpPost("http://" + HOST_NAME + "/index.php");

                    List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(params[0].QueryParams.length + 2);

                    String modDotAct[] = params[0].QueryID.split("\\.");

                    nameValuePairs.add(new BasicNameValuePair("act", modDotAct[1]));
                    nameValuePairs.add(new BasicNameValuePair("module", modDotAct[0]));

                    for (int i = 0; i < params[0].QueryParams.length; i++) {
                        nameValuePairs.add(new BasicNameValuePair(params[0].QueryParams[i].key, params[0].QueryParams[i].val));
                    }

                    thPost.setHeader("Content-Type", "application/json");

                    thPost.setEntity(new UrlEncodedFormEntity(nameValuePairs, "UTF-8"));

                    response = httpclient.execute(thPost);
                }

                BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent(), "UTF-8"), 8);

                StringBuilder sb = new StringBuilder();
                sb.append(reader.readLine() + "\n");
                String line = "0";
                while ((line = reader.readLine()) != null) {
                    sb.append(line + "\n");
                }
                reader.close();

                JSONObject resJson;
                try {
                    resJson = new JSONObject(sb.toString());
                    rqd = new ResponseQueryData(params[0].QueryID, resJson.getInt("error"), resJson.getString("message"), resJson, params[0].callback);
                } catch (JSONException ex) {
                    rqd = new ResponseQueryData(params[0].QueryID, ERR_INVALID_JSON, "", EMPTY_JSON, params[0].callback);
                }
            } catch (ClientProtocolException e) {
                rqd = new ResponseQueryData(params[0].QueryID, ERR_CLIENT_PROTOCOL, "", EMPTY_JSON, e, params[0].callback);
            } catch (UnsupportedEncodingException e) {
                rqd = new ResponseQueryData(params[0].QueryID, ERR_UNSUPPORTED_ENCODING, "", EMPTY_JSON, e, params[0].callback);
            } catch (IOException e) {
                rqd = new ResponseQueryData(params[0].QueryID, ERR_IO, "", EMPTY_JSON, e, params[0].callback);
            } catch (Exception e) {
                rqd = new ResponseQueryData(params[0].QueryID, ERR_EXCEPTION, e.getMessage(), EMPTY_JSON, e, params[0].callback);
                if (DEBUG_MODE) {
                    Log.d(TAG, "{ Exception Error (" + params[0].QueryMethod + "," + params[0].QueryID + ")");
                    e.printStackTrace();
                    Log.d(TAG, "};");
                }
            }
            return rqd;
        }

        @Override
        protected void onPostExecute(ResponseQueryData result) {
            requestQueue.poll();

            scheduling();

            if (result.callback == null)
                defaultCallback.onResponse(result);
            else
                result.callback.onResponse(result);

            if (DEBUG_MODE) {
                Log.d(TAG, "<< OnResponse.");
            }

        }

    }

}

//eof
