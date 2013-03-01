package com.google.code.quranquiz;

import java.io.BufferedInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;

public class QQDashboardActivity extends Activity {

	private QQProfileHandler myQQProfileHandler = null;
	
	// Function to read the result from newly created activity
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (resultCode == 12345) {
			myQQProfileHandler = (QQProfileHandler) data.getExtras().get(
					"ProfileHandler");
		}
	}

	@Override
	public void onBackPressed() {
		if (myQQProfileHandler != null) {
			Intent lastIntent = new Intent(QQDashboardActivity.this,
					QQLastScreenActivity.class);
			lastIntent.putExtra("ProfileHandler", myQQProfileHandler);
			startActivity(lastIntent);
		}
		finish();
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.dashboard_layout);

		/**
		 * Creating all buttons instances
		 * */
		Button btnQQQuestionaire = (Button) findViewById(R.id.btnQQQuestionaire);
		Button btnSettings = (Button) findViewById(R.id.btnSettings);
		Button btnScoreHistory = (Button) findViewById(R.id.btnScoreHistory);

		/**
		 * Handling all button click events
		 * */
		btnQQQuestionaire.setOnClickListener(new View.OnClickListener() {

			public void onClick(View view) {
				Intent i = new Intent(getApplicationContext(),
						QQQuestionaireActivity.class);
				startActivityForResult(i, 12345);
			}
		});

		btnSettings.setOnClickListener(new View.OnClickListener() {
			public void onClick(View view) {
				if (myQQProfileHandler == null) {
					myQQProfileHandler = new QQProfileHandler(
							getApplicationContext());
					myQQProfileHandler.getProfile();
				}
				Intent intentStudyList = new Intent(QQDashboardActivity.this,
						QQStudyListActivity.class);
				intentStudyList.putExtra("ProfileHandler", myQQProfileHandler);
				startActivity(intentStudyList);
			}
		});

		// Listening Messages button click
		btnScoreHistory.setOnClickListener(new View.OnClickListener() {

			public void onClick(View view) {
				if (myQQProfileHandler == null) {
					myQQProfileHandler = new QQProfileHandler(
							getApplicationContext());
					myQQProfileHandler.getProfile();
				}
				startActivity((new QQScoreChart(
						myQQProfileHandler.CurrentProfile))
						.execute(getApplicationContext()));
			}
		});
	}
}
