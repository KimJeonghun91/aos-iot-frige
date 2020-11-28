package com.studio0610.iotfrige;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.label.ImageLabel;
import com.google.mlkit.vision.label.ImageLabeler;
import com.google.mlkit.vision.label.ImageLabeling;
import com.google.mlkit.vision.label.automl.AutoMLImageLabelerLocalModel;
import com.google.mlkit.vision.label.automl.AutoMLImageLabelerOptions;

import java.io.IOException;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private Context context;
    private WebView mWebView;
    private Button btnPicFrige,btnRealTime,btnNoti;
    private ImageView imageView01;
    private TextView text1;
    private AutoMLImageLabelerLocalModel localModel;
    private AutoMLImageLabelerOptions autoMLImageLabelerOptions;
    private ImageLabeler labeler;
    private int count = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            WebView.enableSlowWholeDocumentDraw();
        }
        setContentView(R.layout.activity_main);
        context = this;

        // https://firebase.google.com/docs/ml/android/label-images-with-automl?authuser=0
        // 파이어베이스에서 학습완료 누르면 테스트 가능.
        localModel =
                new AutoMLImageLabelerLocalModel.Builder()
                        .setAssetFilePath("manifest.json")
                        .build();

        autoMLImageLabelerOptions =
                new AutoMLImageLabelerOptions.Builder(localModel)
                        .setConfidenceThreshold(0.0f)  // Evaluate your model in the Firebase console
                        // to determine an appropriate value.
                        .build();
        labeler = ImageLabeling.getClient(autoMLImageLabelerOptions);


        mWebView = (WebView) findViewById(R.id.webview);
        btnPicFrige = findViewById(R.id.btnPicFrige);
        btnRealTime = findViewById(R.id.btnRealTime);
        btnNoti = findViewById(R.id.btnNoti);
        imageView01 = findViewById(R.id.imageView01);
        text1 = findViewById(R.id.text1);



        mWebView.getSettings().setJavaScriptEnabled(true);//자바스크립트 허용
        mWebView.loadUrl("http://192.168.0.7/stream");//웹뷰 실행
//        mWebView.loadUrl("https://www.naver.com/");//웹뷰 실행
        mWebView.setWebChromeClient(new WebChromeClient());//웹뷰에 크롬 사용 허용//이 부분이 없으면 크롬에서 alert가 뜨지 않음
        mWebView.setWebViewClient(new WebViewClientClass());//새창열기 없이 웹뷰 내에서 다시 열기//페이지 이동 원활히 하기위해 사용



        btnRealTime.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mWebView.setVisibility(View.VISIBLE);
            }
        });

        btnPicFrige.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
               Bitmap wvImg = screenshot(mWebView);
               mlStart(wvImg);
            }
        });

        btnNoti.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                count++;
                NotificationSomethings();
            }
        });


    }

    public static Bitmap screenshot(WebView webView) {
        try {
            Bitmap bitmap = Bitmap.createBitmap(webView.getWidth(), webView.getHeight(), Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            webView.draw(canvas);
            return bitmap;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public void mlStart(Bitmap bitmap) {
        // ** ML 대상 이미지
//        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.flower1);
        InputImage image = InputImage.fromBitmap(bitmap, 0);

        // ** 캡쳐 이미지 미리보기
        imageView01.setImageBitmap(bitmap);

        // ** ML 시작
        labeler.process(image)
                .addOnSuccessListener(new OnSuccessListener<List<ImageLabel>>() {
                    @Override
                    public void onSuccess(List<ImageLabel> labels) {
                        int idx = 0;
                        for (ImageLabel label : labels) {
                            String text = label.getText();
                            float confidence = label.getConfidence();
                            int index = label.getIndex();

                            Log.d("label","라벨 텍스트 : "+text + " / idx : "+idx);
                            if(idx == 0){
                                if("lv0".equals(text)){
                                    text1.setText("사과가 싱싱해요!");
                                }else if("lv1".equals(text)){
                                    text1.setText("사과가 아직까지 싱싱하네요");
                                }else if("lv2".equals(text)){
                                    text1.setText("사과의 유통기한 5일 남았어요 :)");
                                }else if("lv3".equals(text)){
                                    text1.setText("사과의 유통기한 2일 남았어요 :)");
                                }else if("lv4".equals(text)){
                                    text1.setText("사과를 전부 먹지는 못하겠네요, 상한 부분은 잘라내고 드세요");
                                }else if("lv5".equals(text)){
                                    text1.setText("사과를 버릴지 말지 결정해야 될거에요 :(");
                                }else if("lv6".equals(text)){
                                    text1.setText("냉장고안에 사과를 버려주세요 :(");
                                }

                            }

                            idx = idx+1;
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.d("ERROR","labeler process ERROR : "+e);
                        Toast.makeText(context,"labeler process ERROR : "+e, Toast.LENGTH_LONG);
                    }
                });
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {//뒤로가기 버튼 이벤트
        if ((keyCode == KeyEvent.KEYCODE_BACK) && mWebView.canGoBack()) {//웹뷰에서 뒤로가기 버튼을 누르면 뒤로가짐
            mWebView.goBack();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    private class WebViewClientClass extends WebViewClient {//페이지 이동
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            Log.d("check URL",url);
            view.loadUrl(url);
            return true;
        }
    }



    public void NotificationSomethings() {


        NotificationManager notificationManager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);

        Intent notificationIntent = new Intent(this, MainActivity.class);
        notificationIntent.putExtra("notificationId", count); //전달할 값
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK) ;
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent,  PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "1000101")
                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher_foreground)) //BitMap 이미지 요구
                .setContentTitle("(스마트 신선집사)사과의 상태를 확인해주세요!")
                .setContentText("사과의 유통기한이 2일 밖에 안남았어요 :(")
                // 더 많은 내용이라서 일부만 보여줘야 하는 경우 아래 주석을 제거하면 setContentText에 있는 문자열 대신 아래 문자열을 보여줌
                //.setStyle(new NotificationCompat.BigTextStyle().bigText("더 많은 내용을 보여줘야 하는 경우..."))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent) // 사용자가 노티피케이션을 탭시 ResultActivity로 이동하도록 설정
                .setAutoCancel(true);

        //OREO API 26 이상에서는 채널 필요
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            builder.setSmallIcon(R.drawable.ic_launcher_foreground); //mipmap 사용시 Oreo 이상에서 시스템 UI 에러남
            CharSequence channelName  = "노티페케이션 채널";
            String description = "오레오 이상을 위한 것임";
            int importance = NotificationManager.IMPORTANCE_HIGH;

            NotificationChannel channel = new NotificationChannel("1000101", channelName , importance);
            channel.setDescription(description);

            // 노티피케이션 채널을 시스템에 등록
            assert notificationManager != null;
            notificationManager.createNotificationChannel(channel);

        }else builder.setSmallIcon(R.mipmap.ic_launcher); // Oreo 이하에서 mipmap 사용하지 않으면 Couldn't create icon: StatusBarIcon 에러남

        assert notificationManager != null;
        notificationManager.notify(1234, builder.build()); // 고유숫자로 노티피케이션 동작시킴

    }

}
