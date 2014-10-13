/* 
 * Copyright (C) 2014 Peter Cai
 *
 * This file is part of BlackLight
 *
 * BlackLight is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * BlackLight is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with BlackLight.  If not, see <http://www.gnu.org/licenses/>.
 */

package us.shandian.blacklight.support;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.Environment;

import com.google.gson.Gson;

import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.net.HttpURLConnection;
import java.net.URL;

import us.shandian.blacklight.R;
import us.shandian.blacklight.api.statuses.PostApi;
/*
 * Support class for loading emoticons
 */
public class Emoticons {
	// Sina's emoticon format
	public static class SinaEmotion {
		public String category;
		public String url;
		public String value;
	}

	public static class Emoticon {
		public String name;
		public String file;
	}

	public static class EmoticonCategory {
		public String name;
		public ArrayList<Emoticon> emoticons = new ArrayList<Emoticon>();
	}

	private static final String DIR = Environment.getExternalStorageDirectory().getPath() + "/BlackLight/emoticons/";
	private static final String CONFIG = DIR + "config.json";

	public static boolean downloaded() {
		File f = new File(CONFIG);
		return f.exists();
	}

	public static void startDownload(Context context) {
		new Downloader(context).execute();
	}

	private static void processEmoticon(SinaEmotion emo, ArrayList<EmoticonCategory> categories) {
		EmoticonCategory ca = getEmoticonCategory(categories, emo.category);
		if (ca == null) {
			ca = new EmoticonCategory();
			ca.name = emo.category;
			categories.add(ca);
		}

		Emoticon emoticon = new Emoticon();
		emoticon.name = emo.value;
		emoticon.file = DIR + emo.url.replaceAll(":", ".").replaceAll("/", ".");

		try {
			downloadEmoticon(emoticon, emo.url);
		} catch (IOException e) {
			return;
		}
		ca.emoticons.add(emoticon);
	}

	private static void downloadEmoticon(Emoticon emoticon, String url) throws IOException {
		HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
		conn.setRequestMethod("GET");
		conn.setConnectTimeout(5000);

		InputStream ipt = conn.getInputStream();
		File f = new File(emoticon.file);
		if (!f.exists()) {
			f.createNewFile();
		}

		FileOutputStream opt = new FileOutputStream(f);
		byte[] buf = new byte[1024];
		int len;

		while ((len = ipt.read(buf)) != -1) {
			opt.write(buf, 0, len);
		}

		opt.close();
		ipt.close();
	}

	private static EmoticonCategory getEmoticonCategory(ArrayList<EmoticonCategory> categories, String name) {
		for (EmoticonCategory emo : categories) {
			if (emo.name.equals(name)) {
				return emo;
			}
		}

		return null;
	}

	private static class Downloader extends AsyncTask<Void, Object, Void> {
		private ProgressDialog prog;
		private Context context;

		public Downloader(Context context) {
			this.context = context;
		}

		@Override
		protected void onPreExecute() {
			prog = new ProgressDialog(context);
			prog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
			prog.setMax(100);
			prog.setMessage(context.getString(R.string.download_emoticons));
			prog.setCancelable(false);
			prog.show();
		}

		@Override
		protected Void doInBackground(Void... params) {
			new File(DIR).mkdirs();

			ArrayList<SinaEmotion> face = PostApi.getEmoticons("face");
			ArrayList<SinaEmotion> cartoon = PostApi.getEmoticons("cartoon");
			publishProgress(0, face.size() + cartoon.size());

			ArrayList<EmoticonCategory> categories = new ArrayList<EmoticonCategory>();

			for (int i = 0; i < face.size(); i++) {
				SinaEmotion emo = face.get(i);
				processEmoticon(emo, categories);
				publishProgress(1, i + 1);
			}

			for (int i = 0; i < cartoon.size(); i++) {
				SinaEmotion emo = cartoon.get(i);
				processEmoticon(emo, categories);
				publishProgress(1, face.size() + i + 1);
			}

			String json = new Gson().toJson(categories);

			File f = new File(CONFIG);
			try {
				if (f.createNewFile()) {
					FileOutputStream opt = new FileOutputStream(f);
					opt.write(json.getBytes());
					opt.close();
				}
			} catch (IOException e) {
			}

			return null;
		}

		@Override
		protected void onProgressUpdate(Object... progress) {
			switch (Integer.parseInt(String.valueOf(progress[0]))) {
				case 0:
					prog.setMax(Integer.parseInt(String.valueOf(progress[1])));
					prog.setProgress(0);
					break;
				case 1:
					prog.setProgress(Integer.parseInt(String.valueOf(progress[1])));
					break;
			}
		}

		@Override
		protected void onPostExecute(Void result) {
			prog.dismiss();
		}
	}

}
