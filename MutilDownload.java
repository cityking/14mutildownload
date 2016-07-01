package com.city.king;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class MutilDownload {

	// 1定义下载路径
	private static String path = "http://192.168.1.103:8080/SQLiteExpertSetup.exe";
	private final static int threadCount = 3;
	private static int runningThread;

	public static void main(String[] args) {
		// 2获取服务器文件大小
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
	}

	private static class DownLoadThread extends Thread {
		private int startIndex;
		private int endIndex;
		private int threadId;

		public DownLoadThread(int startIndex, int endIndex, int threadId) {
			this.startIndex = startIndex;
			this.endIndex = endIndex;
			this.threadId = threadId;
		}

		@Override
		public void run() {
			super.run();
			try {
				URL url = new URL(path);
				HttpURLConnection conn = (HttpURLConnection) url.openConnection();
				conn.setRequestMethod("GET");
				conn.setConnectTimeout(5000);

				File file = new File(threadId + ".txt");
				if (file.exists() && file.length() > 0) {
					FileInputStream in = new FileInputStream(file);
					BufferedReader br = new BufferedReader(new InputStreamReader(in));
					String lastposition = br.readLine();
					startIndex = Integer.parseInt(lastposition);
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
						RandomAccessFile raf = new RandomAccessFile(threadId + ".txt", "rwd");
						raf.write(String.valueOf(startIndex + total).getBytes());
						raf.close();
					}

					randomAccessFile.close();
					System.out.println("线程" + threadId + ":下载完成");
					//7下载完成后，删除断点续传生成的txt文件
					synchronized (MutilDownload.class) {
						runningThread--;
						if (runningThread == 0) {
							for (int i = 0; i < threadCount; i++) {
								File deletefile = new File(i + ".txt");
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
	//8获取文件名
	public static String getFileName(String path){
		int start = path.lastIndexOf("/")+1;
		return path.substring(start);
	}
}
