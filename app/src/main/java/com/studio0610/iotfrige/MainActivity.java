package com.studio0610.iotfrige;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
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
    private Button btnPicFrige;
    private ImageView imageView01;
    private TextView text1;
    private AutoMLImageLabelerLocalModel localModel;
    private AutoMLImageLabelerOptions autoMLImageLabelerOptions;
    private ImageLabeler labeler;

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
        imageView01 = findViewById(R.id.imageView01);
        text1 = findViewById(R.id.text1);


        mWebView.getSettings().setJavaScriptEnabled(true);//자바스크립트 허용
        mWebView.loadUrl("https://www.naver.com/");//웹뷰 실행
        mWebView.setWebChromeClient(new WebChromeClient());//웹뷰에 크롬 사용 허용//이 부분이 없으면 크롬에서 alert가 뜨지 않음
        mWebView.setWebViewClient(new WebViewClientClass());//새창열기 없이 웹뷰 내에서 다시 열기//페이지 이동 원활히 하기위해 사용

        btnPicFrige.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
               Bitmap wvImg = screenshot(mWebView);
               mlStart(wvImg);
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
                                text1.setText("ML 결과 : "+text);
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

}
