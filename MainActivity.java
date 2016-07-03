package com.example.multidownload;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import android.R.integer;
import android.app.Activity;
import android.os.Bundle;
import android.os.Environment;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

public class MainActivity extends Activity {

	private EditText et_path;
	private EditText et_threadCount;
	private LinearLayout ll_pb;
	private int runningThread;
	private String path;
	private int threadCount;
	private List<ProgressBar> lists;
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		et_path = (EditText) findViewById(R.id.et_path);
		et_threadCount = (EditText) findViewById(R.id.et_threadCount);
		ll_pb = (LinearLayout) findViewById(R.id.ll_pb);
		lists = new ArrayList<ProgressBar>();
	}
	public void click(View v){
		path = et_path.getText().toString().trim();
		String sthreadCount = et_threadCount.getText().toString().trim();
		threadCount = Integer.parseInt(sthreadCount);
		ll_pb.removeAllViews();
		lists.clear();
		for (int i = 0; i < threadCount; i++) {
			ProgressBar pb = (ProgressBar) View.inflate(getApplicationContext(), R.layout.item, null);
			ll_pb.addView(pb);
			lists.add(pb);
		}
		
		new Thread(){
			public void run() {
				try {
					URL url = new URL(path);
					HttpURLConnection conn = (HttpURLConnection) url.openConnection();
					conn.setRequestMethod("GET");
					conn.setConnectTimeout(5000);
					int code = conn.getResponseCode();
					if (code == 200) {

						runningThread = threadCount;
						int len = conn.getContentLength();
						System.out.println("文件大小：" + len);

						// 3创建一个和服务器文件大小一样的文件
						
						RandomAccessFile randomAccessFile = new RandomAccessFile(getFileName(path), "rw");
						randomAccessFile.setLength(len);

						// 4计算每个线程下载的开始位置和结束位置

						// 算出每个线程下载的大小
						int blogSize = len / threadCount;

						for (int i = 0; i < threadCount; i++) {
							int startIndex = i * blogSize;
							int endIndex = (i + 1) * blogSize - 1;
							if (i == threadCount - 1) {
								endIndex = len - 1;
							}

							System.out.println("线程" + i + "理论下载位置:" + startIndex + "--------" + endIndex);

							// 5开启线程去服务器下载文件

							DownLoadThread downLoadThread = new DownLoadThread(startIndex, endIndex, i);
							downLoadThread.start();
						}

					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			};
		}.start();

	}
	public static String getFileName(String path){
		String position = Environment.getExternalStorageDirectory().getPath();
		int start = path.lastIndexOf("/")+1;
		return position+"/"+path.substring(start);
	}
	private class DownLoadThread extends Thread {
		private int startIndex;
		private int endIndex;
		private int threadId;
		
		private int pbMaxSize;
		private int pbLastPosition;
		private String filePath;

		public DownLoadThread(int startIndex, int endIndex, int threadId) {
			this.startIndex = startIndex;
			this.endIndex = endIndex;
			this.threadId = threadId;
		}

		@Override
		public void run() {
			super.run();
			try {
				pbMaxSize = endIndex-startIndex;
				filePath = Environment.getExternalStorageDirectory().getPath()+"/";
				System.out.println(filePath);
				URL url = new URL(path);
				HttpURLConnection conn = (HttpURLConnection) url.openConnection();
				conn.setRequestMethod("GET");
				conn.setConnectTimeout(5000);

				
				File file = new File(filePath+threadId + ".txt");
				if (file.exists() && file.length() > 0) {
					FileInputStream in = new FileInputStream(file);
					BufferedReader br = new BufferedReader(new InputStreamReader(in));
					int lastposition = Integer.parseInt(br.readLine());
					pbLastPosition = lastposition-startIndex;
					startIndex = lastposition;
				}
				System.out.println("线程" + threadId + "实际下载位置:" + startIndex + "--------" + endIndex);

				conn.setRequestProperty("Range", "bytes=" + startIndex + "-" + endIndex);

				int code = conn.getResponseCode();
				// 206代表请求部分资源
				if (code == 206) {
					
					RandomAccessFile randomAccessFile = new RandomAccessFile(getFileName(path), "rw");

					randomAccessFile.seek(startIndex);
					InputStream in = conn.getInputStream();
					byte[] buffer = new byte[1024 * 1024];
					int len = -1;
					int total = 0;

					while ((len = in.read(buffer)) != -1) {
						randomAccessFile.write(buffer, 0, len);
						// 6实现断点续传
						total += len;
						System.out.println(threadId+"total"+total);
						
						RandomAccessFile raf = new RandomAccessFile(filePath+threadId + ".txt", "rwd");
						System.out.println(threadId+"pbMaxSize"+pbMaxSize);
						System.out.println(threadId+"pbCurrSize"+(pbLastPosition + total));
						raf.write(String.valueOf(startIndex + total).getBytes());
						
						raf.close();

						
						
						ProgressBar pb = lists.get(threadId);
						pb.setMax(pbMaxSize);
						
						pb.setProgress(pbLastPosition + total);
					}

					randomAccessFile.close();
					System.out.println("线程" + threadId + ":下载完成");
					//7下载完成后，删除断点续传生成的txt文件
					synchronized (this) {
						runningThread--;
						if (runningThread == 0) {
							for (int i = 0; i < threadCount; i++) {
								File deletefile = new File(filePath+i + ".txt");
								deletefile.delete();

							}
						}
					}

				}
			} catch (Exception e) {
				e.printStackTrace();
			}

		}
	}
}
