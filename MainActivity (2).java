package com.example.xutils;

import java.io.File;

import com.lidroid.xutils.HttpUtils;
import com.lidroid.xutils.exception.HttpException;
import com.lidroid.xutils.http.HttpHandler;
import com.lidroid.xutils.http.ResponseInfo;
import com.lidroid.xutils.http.callback.RequestCallBack;

import android.app.Activity;
import android.os.Bundle;
import android.os.Environment;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

public class MainActivity extends Activity {

	private EditText et_path;
	private ProgressBar pb;
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		et_path =(EditText) findViewById(R.id.et_path);
		pb = (ProgressBar) findViewById(R.id.pb);
	}
	public void click(View v){
		String path = et_path.getText().toString().trim();
		HttpUtils http = new HttpUtils();
		String target = Environment.getExternalStorageDirectory().getPath().toString()+"/temp.exe";
		HttpHandler handler = http.download(path, target, new RequestCallBack<File>() {

			public void onSuccess(ResponseInfo<File> responseInfo) {
				Toast.makeText(getApplicationContext(), "œ¬‘ÿ≥…π¶", 1).show();
			}

			
			public void onFailure(HttpException error, String msg) {
				
			}
			@Override
			public void onLoading(long total, long current, boolean isUploading) {
				super.onLoading(total, current, isUploading);
				pb.setMax((int)total);
				pb.setProgress((int)current);
			} 

	       
	});
	}

}
