package kr.foryou.scflower;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Message;
import android.os.Parcelable;
import android.os.StrictMode;
import android.provider.MediaStore;
import android.util.Log;
import android.view.WindowManager;
import android.webkit.CookieManager;
import android.webkit.GeolocationPermissions;
import android.webkit.JavascriptInterface;
import android.webkit.JsResult;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.databinding.DataBindingUtil;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;


import com.canhub.cropper.CropImage;
import com.canhub.cropper.CropImageView;

import java.io.File;
import java.lang.reflect.Method;


import kr.foryou.scflower.databinding.ActivityMainBinding;
import util.BackPressCloseHandler;
import util.Common;

public class MainActivity extends AppCompatActivity {
    ActivityMainBinding binding;
    private BackPressCloseHandler backPressCloseHandler;
    boolean isIndex = true;
    final int FILECHOOSER_NORMAL_REQ_CODE = 1200,FILECHOOSER_LOLLIPOP_REQ_CODE=1300;
    ValueCallback<Uri> filePathCallbackNormal;
    ValueCallback<Uri[]> filePathCallbackLollipop;
    Uri mCapturedImageURI;
    String firstUrl = "";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this,R.layout.activity_main);
        binding.setMain(MainActivity.this);
        //화면을 계속 켜짐
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        // 웹뷰를 실행할 때 메모리 누수가 심하지 않게 설정하는 것
        if(Build.VERSION.SDK_INT>=24){
            try{
                Method m = StrictMode.class.getMethod("disableDeathOnFileUriExposure");
                m.invoke(null);
            }catch(Exception e){
                e.printStackTrace();
            }
        }
        firstUrl=getString(R.string.url);//웹뷰에 처음 실행할 것을 지정하는 url 주소입니다.
        Intent intent = getIntent();
        //자동로그인을 할 때 필요함
        if(0 < Common.getPref(MainActivity.this,"ss_mb_id","").length()){
            firstUrl=getString(R.string.url)+"/bbs/login_check.php?mb_id="+
                    Common.getPref(MainActivity.this,"ss_mb_id","")+
                    "&mb_password="+Common.getPref(MainActivity.this,"ss_mb_password","");
        }
        //푸시가 있을 때는 아래에 있는 소스가 실행이 됩니다.
        try{
            if(!intent.getExtras().getString("goUrl").equals("")){
                firstUrl = intent.getExtras().getString("goUrl");
            }
        }catch (Exception e){

        }

        /* cookie */
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {

        //    CookieSyncManager.createInstance(this);
        }


        //스플래시 실행
        Intent startIntent = new Intent(MainActivity.this, SplashActivity.class);
        startActivity(startIntent);
        webViewSetting();
    }
    @SuppressLint("ResourceAsColor")
    @RequiresApi(api = Build.VERSION_CODES.ECLAIR_MR1)
    public void webViewSetting() {
        //Common.setTOKEN(this);
        WebSettings setting = binding.webView.getSettings();//웹뷰 세팅용
        if(Build.VERSION.SDK_INT >= 21) {
            binding.webView.getSettings().setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
            //쿠키 생성
            CookieManager cookieManager = CookieManager.getInstance();
            cookieManager.setAcceptCookie(true);
            cookieManager.setAcceptThirdPartyCookies(binding.webView,true);
        }
        setting.setAllowFileAccess(true);//웹에서 파일 접근 여부
        //setting.setAppCacheEnabled(true);//캐쉬 사용여부
        setting.setGeolocationEnabled(true);//위치 정보 사용여부
        setting.setDatabaseEnabled(true);//HTML5에서 db 사용여부
        setting.setDomStorageEnabled(true);//HTML5에서 DOM 사용여부
        setting.setCacheMode(WebSettings.LOAD_DEFAULT);//캐시 사용모드 LOAD_NO_CACHE는 캐시를 사용않는다는 뜻
        setting.setJavaScriptEnabled(true);//자바스크립트 사용여부
        setting.setSupportMultipleWindows(false);//윈도우 창 여러개를 사용할 것인지의 여부 무조건 false로 하는 게 좋음
        setting.setUseWideViewPort(true);//웹에서 view port 사용여부
        setting.setTextZoom(100);
        setting.setSupportZoom(true);
        binding.webView.setWebChromeClient(chrome);//웹에서 경고창이나 또는 컴펌창을 띄우기 위한 메서드
        binding.webView.setWebViewClient(client);//웹페이지 관련된 메서드 페이지 이동할 때 또는 페이지가 로딩이 끝날 때 주로 쓰임
        setting.setUserAgentString("Mozilla/5.0 (Linux; Android 6.0; Nexus 5 Build/MRA58N) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/63.0.3239.84 Mobile Safari/537.36"+"/ADemetory");
        binding.webView.addJavascriptInterface(new WebJavascriptEvent(), "Android");
        //뒤로가기 버튼을 눌렀을 때 클래스로 제어함
        backPressCloseHandler = new BackPressCloseHandler(this);

        binding.webView.loadUrl(firstUrl);
    }

    WebChromeClient chrome;
    {
        chrome = new WebChromeClient() {
            //새창 띄우기 여부
            @Override
            public boolean onCreateWindow(WebView view, boolean isDialog, boolean isUserGesture, Message resultMsg) {
                return false;
            }

            //경고창 띄우기
            @Override
            public boolean onJsAlert(WebView view, String url, String message, final JsResult result) {
                new AlertDialog.Builder(MainActivity.this)
                        .setMessage("\n" + message + "\n")
                        .setCancelable(false)
                        .setPositiveButton("확인",
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog,
                                                        int which) {
                                        result.confirm();
                                    }
                                }).create().show();
                return true;
            }

            //컴펌 띄우기
            @Override
            public boolean onJsConfirm(WebView view, String url, String message,
                                       final JsResult result) {
                new AlertDialog.Builder(MainActivity.this)
                        .setMessage("\n" + message + "\n")
                        .setCancelable(false)
                        .setPositiveButton("확인",
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog,
                                                        int which) {
                                        result.confirm();
                                    }
                                })
                        .setNegativeButton("취소",
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog,
                                                        int which) {
                                        result.cancel();
                                    }
                                }).create().show();
                return true;
            }

            //현재 위치 정보 사용여부 묻기
            @Override
            public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissions.Callback callback) {
                // Should implement this function.
                final String myOrigin = origin;
                final GeolocationPermissions.Callback myCallback = callback;
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setTitle("Request message");
                builder.setMessage("Allow current location?");
                builder.setPositiveButton("Allow", new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int id) {
                        myCallback.invoke(myOrigin, true, false);
                    }

                });
                builder.setNegativeButton("Decline", new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int id) {
                        myCallback.invoke(myOrigin, false, false);
                    }

                });
                AlertDialog alert = builder.create();
                alert.show();
            }
            // For Android < 3.0
            public void openFileChooser(ValueCallback<Uri> uploadMsg) {
                openFileChooser(uploadMsg, "");
            }

            // For Android 3.0+
            public void openFileChooser(ValueCallback<Uri> uploadMsg, String acceptType) {
                filePathCallbackNormal = uploadMsg;
                Intent i = new Intent(Intent.ACTION_GET_CONTENT);
                i.addCategory(Intent.CATEGORY_OPENABLE);
                i.setType("image/*");
                startActivityForResult(Intent.createChooser(i, "File Chooser"), FILECHOOSER_NORMAL_REQ_CODE);
            }

            // For Android 4.1+
            public void openFileChooser(ValueCallback<Uri> uploadMsg, String acceptType, String capture) {
                openFileChooser(uploadMsg, acceptType);
            }


            // For Android 5.0+\
            // SDK 21 이상부터 웹뷰에서 파일 첨부를 해주는 기능입니다.
            public boolean onShowFileChooser(
                    WebView webView, ValueCallback<Uri[]> filePathCallback,
                    FileChooserParams fileChooserParams) {
                //카메라 프로바이더로 이용해서 파일을 가져오는 방식입니다.
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {// API 24 이상 일경우..
                    File imageStorageDir = new File(MainActivity.this.getFilesDir() + "/Pictures", "main_image");
                    if (!imageStorageDir.exists()) {
                        // Create AndroidExampleFolder at sdcard
                        imageStorageDir.mkdirs();
                    }
                    // Create camera captured image file path and name

                    //Toast.makeText(mainActivity.getApplicationContext(),imageStorageDir.toString(),Toast.LENGTH_LONG).show();
                    File file = new File(imageStorageDir, "IMG_" + String.valueOf(System.currentTimeMillis()) + ".jpg");
                    Uri providerURI = FileProvider.getUriForFile(MainActivity.this, MainActivity.this.getPackageName() + ".provider", file);
                    mCapturedImageURI = providerURI;

                } else {// API 24 미만 일경우..

                    File imageStorageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "main_image");
                    if (!imageStorageDir.exists()) {
                        // Create AndroidExampleFolder at sdcard
                        imageStorageDir.mkdirs();
                    }
                    // Create camera captured image file path and name
                    File file = new File(imageStorageDir + File.separator + "IMG_" + String.valueOf(System.currentTimeMillis()) + ".jpg");
                    mCapturedImageURI = Uri.fromFile(file);
                }
                if (filePathCallbackLollipop != null) {
//                    filePathCallbackLollipop.onReceiveValue(null);
                    filePathCallbackLollipop = null;
                }
                filePathCallbackLollipop = filePathCallback;
                Intent captureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                captureIntent.putExtra(MediaStore.EXTRA_OUTPUT, mCapturedImageURI);

                Intent i = new Intent(Intent.ACTION_GET_CONTENT);
                i.addCategory(Intent.CATEGORY_OPENABLE);
                i.setType("image/*");

                // Create file chooser intent
                Intent chooserIntent = Intent.createChooser(i, "Image Chooser");
                // Set camera intent to file chooser
                chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Parcelable[]{captureIntent});

                // On select image call onActivityResult method of activity
                startActivityForResult(chooserIntent, FILECHOOSER_LOLLIPOP_REQ_CODE);
                return true;

            }

        };
    }

    WebViewClient client;
    {
        client = new WebViewClient() {
            //페이지 로딩중일 때 (마시멜로) 6.0 이후에는 쓰지 않음
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {

                Log.d("url",url);
                if (url.equals(getString(R.string.url)) || url.equals(getString(R.string.domain))) {
                    isIndex=true;
                } else {
                    isIndex=false;
                }
                if(url.startsWith("tel:")){
                    Intent tel = new Intent(Intent.ACTION_DIAL, Uri.parse(url));
                    startActivity(tel);
                    return true;
                }
                /*if (url.startsWith("intent:")) {

                    try {
                        Intent intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME);
                        Intent existPackage = getPackageManager().getLaunchIntentForPackage(intent.getPackage());
                        if (existPackage != null) {
                            startActivity(intent);
                        } else {
                            Intent marketIntent = new Intent(Intent.ACTION_VIEW);
                            marketIntent.setData(Uri.parse("market://details?id=" + intent.getPackage()));
                            startActivity(marketIntent);
                        }
                        return true;
                    } catch (Exception e) {
                        Log.d("error1",e.toString());
                        e.printStackTrace();
                    }
                }*/
                return false;
            }
            //페이지 로딩이 다 끝났을 때
            @RequiresApi(api = Build.VERSION_CODES.M)
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                binding.webLayout.setRefreshing(false);

                Log.d("url",url);
                if(Build.VERSION.SDK_INT >= 21) {
                    binding.webView.getSettings().setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
                    //쿠키 생성
                    CookieManager cookieManager = CookieManager.getInstance();
                    cookieManager.setAcceptCookie(true);
                    cookieManager.setAcceptThirdPartyCookies(binding.webView,true);
                }
                Log.d("mb_id", Common.getPref(getApplicationContext(),"ss_mb_id",""));
                //로그인할 때
                if(url.startsWith(getString(R.string.domain)+"bbs/login.php")||url.startsWith(getString(R.string.domain)+"bbs/register_form.php")){
                    //view.loadUrl("javascript:fcmKey('"+ Common.TOKEN+"')");
                }

                if (url.equals(getString(R.string.url)) || url.equals(getString(R.string.domain))) {
                    isIndex=true;
                } else {
                    isIndex=false;
                }
                Log.d("isIndex",isIndex+"");
                binding.webLayout.setRefreshing(false);
                //메인 화면이 아닌 페이지는 새로고침을 할 수 있고
                if(isIndex==false) {
                    binding.webLayout.setEnabled(true);
                    binding.webLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
                        @Override
                        public void onRefresh() {
                            binding.webView.clearCache(true);
                            binding.webView.reload();
                            binding.webLayout.setRefreshing(false);
                        }
                    });
                    //메인화면이면은 새로고침을 할 수 없습니다.
                }else{
                    binding.webLayout.setEnabled(false);
                }
                try{
                    if(!Common.TOKEN.equals("")){
                        view.loadUrl("javascript:fcmKey('"+Common.TOKEN+"','"+Common.getMyDeviceId(MainActivity.this)+"')");
                    }
                }catch (Exception e){
                    //토큰 생성

                }
            }
            //페이지 오류가 났을 때 6.0 이후에는 쓰이지 않음
            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {

            }
        };
    }
    //다시 들어왔을 때
    @Override
    protected void onResume() {
        super.onResume();
        /* cookie */
        if(Build.VERSION.SDK_INT >= 21) {
            binding.webView.getSettings().setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
            //쿠키 생성
            CookieManager cookieManager = CookieManager.getInstance();
            cookieManager.setAcceptCookie(true);
            cookieManager.setAcceptThirdPartyCookies(binding.webView,true);
        }




        Log.d("newtork","onResume");


        //netCheck.networkCheck();
    }
    //홈버튼 눌러서 바탕화면 나갔을 때
    @Override
    protected void onPause() {
        super.onPause();
    }
    //뒤로가기를 눌렀을 때
    public void onBackPressed() {
        //super.onBackPressed();
        //웹뷰에서 히스토리가 남아있으면 뒤로가기 함
        Log.d("isIndex",isIndex+"");
        if(!isIndex) {
            if (binding.webView.canGoBack()) {
                binding.webView.goBack();
            } else if (binding.webView.canGoBack() == false) {
                backPressCloseHandler.onBackPressed();
            }
        }else{
            backPressCloseHandler.onBackPressed();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        //여기서부터 카메라와 갤러리에서 가져오는 값이 조금씩 달라집니다.
        Uri[] results = null;
        if(resultCode==RESULT_OK) {
            //롤리팝 이전 버전 소스는 일단 뺐습니다.
            if(requestCode == FILECHOOSER_LOLLIPOP_REQ_CODE){
                Uri[] result = new Uri[0];
                Log.d("filePath1", mCapturedImageURI.toString());
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    filePathCallbackLollipop.onReceiveValue(result);
                }


            }else if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {


            }

        }else{
            try {
                if (filePathCallbackLollipop != null) {
                    filePathCallbackLollipop.onReceiveValue(null);
                    filePathCallbackLollipop = null;
                }
            }catch (Exception e){

            }
        }
    }

    //로그인 로그아웃
    class WebJavascriptEvent{
        @JavascriptInterface
        public void setLogin(String mb_id,String mb_password){
            Log.d("login","로그인");
            Common.savePref(getApplicationContext(),"ss_mb_id",mb_id);
            Common.savePref(getApplicationContext(),"ss_mb_password",mb_password);
        }
        @JavascriptInterface
        public void setLogout(){
            Log.d("logout","로그아웃");
            Common.savePref(getApplicationContext(),"ss_mb_id","");
            Common.savePref(getApplicationContext(),"ss_mb_password","");
        }
        @JavascriptInterface
        public void doShare(String url){
            Intent sharedIntent = new Intent();
            sharedIntent.setAction(Intent.ACTION_SEND);
            sharedIntent.setType("text/plain");
            sharedIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.app_name));
            sharedIntent.putExtra(Intent.EXTRA_TEXT, url);
            Intent chooser = Intent.createChooser(sharedIntent, "공유");
            startActivity(chooser);
        }
        @JavascriptInterface
        public void getMessage(String message){
            Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
        }
    }


}