package com.unity3d.nostatusbar;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Build.VERSION;
import android.os.Bundle;
import android.support.multidex.MultiDex;
import android.util.Log;
import android.view.View;
import android.view.View.OnSystemUiVisibilityChangeListener;
import android.view.Window;
import android.widget.Toast;
import com.lzy.imagepicker.ImagePicker;
import com.lzy.imagepicker.bean.ImageItem;
import com.lzy.imagepicker.ui.ImageGridActivity;
import com.unity3d.player.UnityPlayer;
import com.unity3d.player.UnityPlayerActivity;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;

public class UnityPlayerActivityStatusBar extends UnityPlayerActivity
{
	public static int Image_Picker = 1000;
	public static final String LogTag = "UKidsClient";
	private String mImageSavePath;

	protected void attachBaseContext(Context base)
	{
		super.attachBaseContext(base);
		MultiDex.install(this);
	}

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		getWindow().clearFlags(1024);

		showSystemUi();
		addUiVisibilityChangeListener();

		ImagePicker imagePicker = ImagePicker.getInstance();
		imagePicker.setImageLoader(new PicassoImageLoader());
		imagePicker.setShowCamera(true);

		imagePicker.setSelectLimit(9);
	}

	private static int getLowProfileFlag()
    {
		return Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT
		?
			View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY |
			View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
			View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
			View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
			View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
			View.SYSTEM_UI_FLAG_FULLSCREEN
		:
			View.SYSTEM_UI_FLAG_LOW_PROFILE;
	}

	private void showSystemUi()
	{
		// Works from API level 11
		if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.HONEYCOMB)
			return;

		mUnityPlayer.setSystemUiVisibility(mUnityPlayer.getSystemUiVisibility() & ~getLowProfileFlag());
	}
	
	private void addUiVisibilityChangeListener()
	{
		// Works from API level 11
		if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.HONEYCOMB)
			return;

		mUnityPlayer.setOnSystemUiVisibilityChangeListener(new View.OnSystemUiVisibilityChangeListener()
		{
			@Override
			public void onSystemUiVisibilityChange(final int visibility)
			{
				// Whatever changes - force status/nav bar to be visible
				showSystemUi();
			}
		});
	}

	public boolean isAliPayInstalled()
	{
		PackageManager manager = getPackageManager();
		Intent action = new Intent("android.intent.action.VIEW");
		action.setData(Uri.parse("alipays://"));
		List<ResolveInfo> list = manager.queryIntentActivities(action,  PackageManager.GET_RESOLVED_FILTER);
		return (list != null) && (list.size() > 0);
	}

	public void gotoWifiSettings()
	{
		Intent intent = new Intent("android.settings.WIFI_SETTINGS");
		startActivity(intent);
	}

	public void selectMultiplePictures(int maxSelectCount, String imageFileSavePath)
	{
		ImagePicker imagePicker = ImagePicker.getInstance();
		imagePicker.setSelectLimit(maxSelectCount);
		this.mImageSavePath = imageFileSavePath;

		Intent intent = new Intent(this, ImageGridActivity.class);
		startActivityForResult(intent, Image_Picker);
	}

	protected void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		super.onActivityResult(requestCode, resultCode, data);
		if (resultCode == 1004)
		{
			if ((data != null) && (requestCode == Image_Picker))
			{
				ArrayList<ImageItem> images = (ArrayList)data.getSerializableExtra("extra_result_items");
				Log.d("ImagePicker", "get " + images.size() + " pictures");

				String resultJson = "{\"FileList\":[";
				for (int i = 0; i < images.size(); i++)
				{
					ImageItem imageItem = (ImageItem)images.get(i);
					if (imageItem != null)
					{
						Log.d("UKidsClient", "Image FileName = " + imageItem.name + " FileType = " + imageItem.mimeType + " FilePath = " + imageItem.path);
						try
						{
							String md5 = ScaleBitmap(imageItem.path);
							if (i == images.size() - 1) {
								resultJson = resultJson + "\"" + md5 + "\"]}";
							} else {
								resultJson = resultJson + "\"" + md5 + "\",";
							}
						}
						catch (IOException e)
						{
							Log.d("UKidsClient", "Failed to copy file from " + imageItem.path);
						}
					}
				}
				UnityPlayer.UnitySendMessage("MultipleImagePicker", "onImageSelectFinished", resultJson);
			}
			else
			{
				Toast.makeText(this, "No Image Selected", Toast.LENGTH_SHORT).show();
				UnityPlayer.UnitySendMessage("MultipleImagePicker", "onImageSelectFinished", "");
			}
		}
		else {
			UnityPlayer.UnitySendMessage("MultipleImagePicker", "onImageSelectFinished", "");
		}
	}

	public static boolean fileCopy(String oldFilePath, String newFilePath)
			throws IOException
	{
		Log.d("UKidsClient", "Copying file from " + oldFilePath + " to " + newFilePath);
		FileInputStream inputStream = new FileInputStream(new File(oldFilePath));
		byte[] data = new byte[2048];

		FileOutputStream outputStream = new FileOutputStream(new File(newFilePath));
		int length;
		while ((length = inputStream.read(data)) > 0) {
			outputStream.write(data, 0, length);
		}
		inputStream.close();
		outputStream.close();
		return true;
	}

	public static String getMd5OfFile(String filePath)
	{
		String returnVal = "";
		try
		{
			InputStream input = new FileInputStream(filePath);
			byte[] buffer = new byte[2048];
			MessageDigest md5Hash = MessageDigest.getInstance("MD5");
			int numRead = 0;
			while (numRead != -1)
			{
				numRead = input.read(buffer);
				if (numRead > 0) {
					md5Hash.update(buffer, 0, numRead);
				}
			}
			input.close();

			byte[] md5Bytes = md5Hash.digest();
			for (int i = 0; i < md5Bytes.length; i++) {
				returnVal = returnVal + Integer.toString((md5Bytes[i] & 0xFF) + 256, 16).substring(1);
			}
		}
		catch (Throwable t)
		{
			t.printStackTrace();
		}
		return returnVal.toLowerCase();
	}

	public static String getMd5OfData(byte[] data)
	{
		String returnVal = "";
		try
		{
			MessageDigest md5Hash = MessageDigest.getInstance("MD5");
			md5Hash.update(data);
			byte[] md5Bytes = md5Hash.digest();
			for (int i = 0; i < md5Bytes.length; i++) {
				returnVal = returnVal + Integer.toString((md5Bytes[i] & 0xFF) + 256, 16).substring(1);
			}
		}
		catch (Throwable t)
		{
			t.printStackTrace();
		}
		return returnVal.toLowerCase();
	}

	public String ScaleBitmap(String filePath)
			throws IOException
	{
		File imgFileOrig = new File(filePath);
		Bitmap b = BitmapFactory.decodeFile(imgFileOrig.getAbsolutePath());

		int origWidth = b.getWidth();
		int origHeight = b.getHeight();

		float destPixel = 1000.0f;
		float scale = 1.0F;
		if (origWidth > origHeight)
		{
			if (origWidth > destPixel) {
				scale = destPixel / origWidth;
			}
		}
		else if (origHeight > destPixel) {
			scale = destPixel / origHeight;
		}
		Bitmap b2 = Bitmap.createScaledBitmap(b, (int)(origWidth * scale), (int)(origHeight * scale), false);
		ByteArrayOutputStream outStream = new ByteArrayOutputStream();
		b2.compress(CompressFormat.PNG, 100, outStream);

		byte[] bitmapBytes = outStream.toByteArray();
		String md5 = getMd5OfData(bitmapBytes);
		Log.d("UKidsClient", "Image FileName = " + filePath + " FileMd5 = " + md5);
		String newFilePath = this.mImageSavePath + File.separator + md5;

		File f = new File(newFilePath);
		f.createNewFile();

		FileOutputStream fo = new FileOutputStream(f);
		fo.write(outStream.toByteArray());
		fo.close();

		return md5;
	}
}
