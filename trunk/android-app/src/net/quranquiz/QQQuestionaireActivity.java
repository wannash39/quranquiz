/****
* Copyright (C) 2011-2013 Quran Quiz Net 
* Tarek Eldeeb <tarekeldeeb@gmail.com>
* License: see LICENSE.txt
****/
package net.quranquiz;

import java.util.Calendar;

import net.quranquiz.QQQuestionaire.QType;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.SQLException;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Vibrator;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.text.method.ScrollingMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;
import android.widget.ViewAnimator;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.ActionBar.OnNavigationListener;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.MenuItem;
import com.google.analytics.tracking.android.EasyTracker;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.code.microlog4android.Level;
import com.google.code.microlog4android.Logger;
import com.google.code.microlog4android.LoggerFactory;
import com.google.code.microlog4android.appender.FileAppender;
import com.jeremyfeinstein.slidingmenu.lib.SlidingMenu;
import com.tekle.oss.android.animation.AnimationFactory;
import com.tekle.oss.android.animation.AnimationFactory.FlipDirection;

public class QQQuestionaireActivity extends SherlockFragmentActivity implements
		android.view.View.OnClickListener, OnNavigationListener {
	
    private ViewAnimator viewAnimator;
	private TextView tvQ;
	private TextView tvScore;
	private TextView tvScoreUp;
	private TextView tvScoreDown;
	private TextView tvBack;
	private TextView tvInstructions;
	private Button btnBack;
	private Button btnBackReview;
	private ProgressBar bar;
	public VerticalProgressBar leftBar;
	private CountDownTimer cdt;
	private Button[] btnArray;
	private ActionBar actionbar;
	private QQDataBaseHelper q;
	private QQQuestionaire Quest;
	private int QOptIdx = -1;
	private int QQinit = 1;
	// TODO: Grab the last seed from the loaded profile! (replace -1, level 1)
	private int level = 1;
	private int lastSeed = -1;
	private int correct_choice = 0;
	private int CurrentPart = 0;
	private String quranReviewUri = "1/1"; // Sura/Aya
	private QQProfileHandler myQQProfileHandler;
	private QQProfile myQQProfile;
	private QQSession myQQSession;
	private AlertDialog.Builder builder;
	private final static Logger qqLogger = LoggerFactory.getLogger(QQQuestionaireActivity.class);

	public void onClick(View v) {
		int SelID = -2;
		switch (v.getId()) {
			case R.id.bOp1:			SelID = 0;	break;
			case R.id.bOp2:			SelID = 1;	break;
			case R.id.bOp3:			SelID = 2;	break;
			case R.id.bOp4:			SelID = 3;	break;
			case R.id.bOp5:			SelID = 4;	break;
		}
		if (SelID < 0)
			return;
		userAction(SelID);
	}

	private void showUsage(){
		Thread splashTread = new Thread() {
            public void run() {
                try {
            		Intent instructionIntent = new Intent(QQQuestionaireActivity.this,
            				QQInstructionsActivity.class);
            		startActivity(instructionIntent);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        splashTread.start();
	}
	
	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		// some work that needs to be done on orientation change
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);
	
		initDBHandle();
		initLogger();
		setContentView(R.layout.questionaire_layout);
		

		// configure the SlidingMenu
		if(QQUtils.QQDebug == 2){
			SlidingMenu menu = new SlidingMenu(this);
	
	        menu.setMode(SlidingMenu.LEFT_RIGHT);
			menu.setTouchModeAbove(SlidingMenu.TOUCHMODE_FULLSCREEN);
			menu.setShadowWidthRes(R.dimen.shadow_width);
			menu.setShadowDrawable(R.drawable.shadow);
			menu.setBehindOffsetRes(R.dimen.slidingmenu_offset);
			menu.setFadeDegree(0.35f);
			menu.setSecondaryShadowDrawable(R.drawable.shadowright);
            
			menu.attachToActivity(this, SlidingMenu.SLIDING_WINDOW);
			menu.setMenu(R.layout.lastscreen_layout);
			menu.setSecondaryMenu(R.layout.lastscreen_layout);

			FragmentManager fmanager = getSupportFragmentManager();
            Fragment fragment = fmanager.findFragmentById(R.id.map);
            SupportMapFragment supportmapfragment = (SupportMapFragment) fragment;

            //http://stackoverflow.com/questions/14047257/how-do-i-know-the-map-is-ready-to-get-used-when-using-the-supportmapfragment
            // Returns NULL!
            GoogleMap mMap = supportmapfragment.getMap();
            mMap.getUiSettings().setZoomControlsEnabled(false);
            //mMap.setOnMapClickListener(this);
            //mMap.setOnInfoWindowClickListener(this);
            //mMap.setOnMarkerClickListener(this);
            mMap.setMyLocationEnabled(true);
            //mMap.setOnMyLocationChangeListener(this);
            //mMap.setOnMyLocationButtonClickListener(this);
			
			//getFragmentManager().beginTransaction()
	        //.replace(R.layout.lastscreen_layout, new QQStudyListSideFragment().getTargetFragment())
	        //.commit();
		}
		
		initProfile();
		initSession();
		initUI();
		
				
		if(android.os.Build.VERSION.SDK_INT 
				>= android.os.Build.VERSION_CODES.HONEYCOMB)
			QQUtils.disableFixQ();	
				
		// Make the first Question
		userAction(-1);
	}
	
	/**
	 * Initialize user profile. Resume existing or create a default one
	 */
	private void initSession() {
		myQQSession = new QQSession(myQQProfile, this);		
	}

	/**
	 * Initialize user profile. Load existing or create a default one
	 */
	private void initProfile() {
		Intent intentStudyList;
		myQQProfileHandler = new QQProfileHandler(this);
		myQQProfile = myQQProfileHandler.getProfile();
		//Load List Selector first time
		if(myQQProfile.getTotalQuesCount()==0){
			if(android.os.Build.VERSION.SDK_INT 
					>= android.os.Build.VERSION_CODES.HONEYCOMB)
				intentStudyList = new Intent(QQQuestionaireActivity.this,
					QQStudyListActivity.class);
			else
				intentStudyList = new Intent(QQQuestionaireActivity.this,
						QQStudyListCompatActivity.class);
			
			intentStudyList.putExtra("ProfileHandler", myQQProfileHandler);
			startActivity(intentStudyList);
		}	
	}

	/**
	 * Initialize User-Interface components, inflate all needed resources
	 * and register all needed listeners. This includes the action bar
	 * and the navigation list.
	 */
	private void initUI() {

		actionbar = getSupportActionBar();
	    viewAnimator = (ViewAnimator)this.findViewById(R.id.view_flipper);
		bar = (ProgressBar) findViewById(R.id.progressBar1);
		leftBar = (VerticalProgressBar) findViewById(R.id.verticalBarLeft);
		leftBar.setProgress(0);
		
		btnArray = new Button[5];
		btnArray[0] = (Button) findViewById(R.id.bOp1);
		btnArray[1] = (Button) findViewById(R.id.bOp2);
		btnArray[2] = (Button) findViewById(R.id.bOp3);
		btnArray[3] = (Button) findViewById(R.id.bOp4);
		btnArray[4] = (Button) findViewById(R.id.bOp5);

		tvInstructions 	= (TextView) findViewById(R.id.tvInstruction);
		tvScore 		= (TextView) findViewById(R.id.Score);
		tvScoreUp 		= (TextView) findViewById(R.id.tvScoreUp);
		tvScoreDown 	= (TextView) findViewById(R.id.tvScoreDown);
		tvBack  		= (TextView) findViewById(R.id.tvBack);
		btnBack 		= (Button) findViewById(R.id.btnBack); 
		btnBackReview 	= (Button) findViewById(R.id.btnBackReview); 
		
		Typeface tfQQFont = Typeface.createFromAsset(getAssets(),
				"fonts/me_quran.ttf"); //amiri-quran | roboto-regular
		tvBack.setTypeface(tfQQFont);
		btnBack.setBackgroundResource(R.drawable.qqoptionbutton_correct);
		btnBackReview.setBackgroundResource(R.drawable.qqoptionbutton_correct);
		tvQ = (TextView) findViewById(R.id.textView1);
		tvQ.setTypeface(tfQQFont);
		if(android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.HONEYCOMB){
			tvQ.setMovementMethod(new ScrollingMovementMethod()); 
			tvQ.setSelected(true);	
		}

		//Inflate the view containing the SpecialQuestion Toggler
        View vwToggler = LayoutInflater.from(this).inflate(R.layout.special_toggle_view, null);
        final ToggleButton tbSpecialQ = (ToggleButton)vwToggler.findViewById(R.id.SpecialQToggler);
        tbSpecialQ.setChecked(true); // Default true, not from profile!
        tbSpecialQ.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
            	Boolean isEnabled = tbSpecialQ.isChecked();
            	myQQProfile.setSpecialEnabled(isEnabled);
            	if(isEnabled)
            		Toast.makeText(getApplicationContext(), "تم تشغيل الأسئلة الخاصة", Toast.LENGTH_SHORT).show();
            	else
            		Toast.makeText(getApplicationContext(), "تم إيقاف الاسئلة الخاصة", Toast.LENGTH_SHORT).show();
            }
        });

        //Attach to the action bar
        getSupportActionBar().setCustomView(vwToggler);
        getSupportActionBar().setDisplayShowCustomEnabled(true);
		
		for(int i=0;i<5;i++){
			btnArray[i].setTypeface(tfQQFont);
			btnArray[i].setOnClickListener(this);
		}
				
		btnBack.setOnClickListener(
				new OnClickListener(){
					public void onClick(View arg0) {
			            AnimationFactory.flipTransition(viewAnimator, FlipDirection.LEFT_RIGHT);						
					}
		});
		
		btnBackReview.setOnClickListener(
			new OnClickListener(){
				public void onClick(View arg0) {
		    		Intent quranViewer = new Intent(Intent.ACTION_VIEW, Uri.parse("quran://"+quranReviewUri)); 

		    		 if (getPackageManager().queryIntentActivities(quranViewer, 0).size() > 0){
		    	    		startActivity(quranViewer); 
		    		 } else {
		    			 //Prompt user to install a "quran://" app handler
		    			 AlertDialog.Builder installQViewerDialogBuilder = new AlertDialog.Builder(QQQuestionaireActivity.this);
		    			 installQViewerDialogBuilder.setTitle(QQApp.getContext().getResources().getString(R.string.installQViewer_title))
	    						.setMessage(QQApp.getContext().getResources().getString(R.string.installQViewer_msg))
	    						.setCancelable(true)
	    						.setPositiveButton(QQApp.getContext().getResources().getString(R.string.installQViewer_install),
	    							new DialogInterface.OnClickListener() {
	    							public void onClick(DialogInterface dialog,int id) {
	    								// Redirect to install QuranAndroid
	    								Intent intent = new Intent(Intent.ACTION_VIEW); 
	    								intent.setData(Uri.parse("market://details?id=com.quran.labs.androidquran")); 
	    								startActivity(intent);
	    							} })
	    						.setNegativeButton(QQApp.getContext().getResources().getString(R.string.txt_no),
	    							new DialogInterface.OnClickListener() {
	    							public void onClick(DialogInterface dialog,int id) {
	    								dialog.cancel();
	    							} });
		    		 
						// create alert dialog and show it
						AlertDialog installQViewerDialog = installQViewerDialogBuilder.create();
						installQViewerDialog.show();
		    		 }
				}
		});		
		
        ArrayAdapter<CharSequence> list = ArrayAdapter.createFromResource
				(actionbar.getThemedContext(),
				 R.array.userLevels, 
				 R.layout.sherlock_spinner_item);
		list.setDropDownViewResource(R.layout.sherlock_spinner_dropdown_item);
		actionbar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
		actionbar.setListNavigationCallbacks(list, this);
		actionbar.setSelectedNavigationItem(myQQProfileHandler.CurrentProfile.getLevel()-1);
	}

	/**
	 * Initializes a user-side logger. All debug values are pushed to a 
	 * file at the user storage area. Logs are included according to their
	 * level and the required Levels.
	 */
	private void initLogger() {
		if(QQUtils.QQDebug>0){
			FileAppender appender = new FileAppender();
			/*
			String strQQLogFile = getFilesDir()+"/qq-logger.txt";
			File fhQQLogFile = new File(strQQLogFile);
			if(!fhQQLogFile.exists()){
				try {
					fhQQLogFile.createNewFile();
				} catch (IOException e) {}
			}
			appender.setFileName(strQQLogFile);
			*/
			appender.setAppend(true);
	        qqLogger.addAppender(appender);
	        qqLogger.setLevel(Level.DEBUG);
	        qqLogger.warn("Logger session started!");
		}
	}

	/**
	 * Initialize a handle for the needed SQLite3 database 
	 * for QuranQuiz to operate. Initially, the DB needs to get 
	 * uncompressed then its index built. A splash screen is displayed
	 * to demonstrate a demo screen while the initial DB preparation. 
	 */
	private void initDBHandle() {
		q = new QQDataBaseHelper(this);
		if (!q.checkDataBase()){
			showUsage();
			try {
				q.createDataBase(); //slow!
			} catch (Exception sqle) {
			}
		}
		try {
			q.openDataBase();
			} 
		catch (SQLException sqle) {	}
		catch (Exception ioe) {
				finish(); //destroy Questionnaire.
				return;
			}
	}

	@Override
	protected void onDestroy() {
		if (q != null)
			q.closeDatabase();
		super.onDestroy();
	}

	/**
	 * When a user presses the back button, his profile is saved
	 * and a handle is passed back.
	 */
	@Override
	public void onBackPressed() {
			myQQProfileHandler.saveProfile(myQQProfileHandler.CurrentProfile);
			Intent i = new Intent();
			i.putExtra("ProfileHandler", myQQProfileHandler);
			setResult(12345, i);
			
			/**
			 * Close Session*/
			myQQSession.close();
			
			finish();
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) { //TODO: Clean, unused
		switch (item.getItemId()) {
		case R.id.Profile:
			Intent intentStudyList = new Intent(QQQuestionaireActivity.this,
					QQStudyListActivity.class);
			intentStudyList.putExtra("ProfileHandler", myQQProfileHandler);
			startActivity(intentStudyList);
			break;
		case R.id.Settings:
			Intent intentPreferences = new Intent(QQQuestionaireActivity.this,
					QQPreferences.class);
			startActivity(intentPreferences);
			break;
		}
		return true;
	}

	@Override
	protected void onResume() {
		myQQProfileHandler.reLoadCurrentProfile();
		super.onResume();
	}

	@Override
	public void onStart() {
	    super.onStart();
	    EasyTracker.getInstance().activityStart(this); // Add this method.
	}

	@Override
	protected void onStop() {
		super.onStop();
		myQQProfileHandler.saveProfile(myQQProfileHandler.CurrentProfile);
	    EasyTracker.getInstance().activityStop(this); // Add this method.
	}

	private void updateOptionButtonsColor(int CorrectIdx){
		for(int i=0;i<5;i++){
			if(i==CorrectIdx)
				((Button)btnArray[i]).setBackgroundResource(R.drawable.qqoptionbutton_correct);
			else
				((Button)btnArray[i]).setBackgroundResource(R.drawable.qqoptionbutton_wrong);
		}
	}
	private void startTimer(int fire) {
		bar.setProgress(100);
		bar.setVisibility(View.VISIBLE);

		final int millis = fire * 1000; // milli seconds

		/** CountDownTimer starts with fire seconds and every onTick is 1 second */
		if (cdt != null)
			cdt.cancel();
		cdt = new CountDownTimer(millis, 1000) {
			int cc = 1;

			@Override
			public void onFinish() {
				// DO something when time is up
				bar.setVisibility(View.INVISIBLE);
			}

			@Override
			public void onTick(long millisUntilFinished) {
				bar.setProgress((1 - cc * 1000 / millis) * 100);
				cc++;
			}
		}.start();

	}

	private void userAction(int selID) {
		if (QOptIdx >= 0 && correct_choice != selID) {// Wrong choice!!

			//btnArrayR[selID].startAnimation(animFadeOut);
			//btnArrayR[selID].set
			// Vibrate for 300 milliseconds
			Vibrator mVibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
			mVibrator.vibrate(300);

			setBackCard();
			QOptIdx = -1; // trigger a new question
		} else {
			QOptIdx = (QOptIdx == -1) ? -1 : QOptIdx + 1; // Keep -1, or Proceed
															// with options ..
		}

		if (QOptIdx == -1 || QOptIdx == Quest.rounds) {
			myQQProfile = myQQProfileHandler.CurrentProfile;
			
			if (QQinit == 0 && QOptIdx == -1) { // A wrong answer
				if(Quest.qType == QType.NOTSPECIAL)
					myQQProfile.addIncorrect(CurrentPart);

			} else { // A correct answer
				if(QQinit == 0 && QOptIdx == Quest.rounds){
					if(Quest.qType == QType.NOTSPECIAL)
						myQQProfile.addCorrect(CurrentPart);
					else
						myQQProfile.addSpecial(Quest.qType.getScore());
					
					// Display Correct answer
					setBackCard();
				}
			}

			if(QQinit==0){ // Need Card Flip if game not initialized
				AnimationFactory.flipTransition(viewAnimator, FlipDirection.LEFT_RIGHT);				
			}
			
			myQQProfileHandler.reLoadCurrentProfile(); // For first Question
			Quest = new QQQuestionaire(myQQProfile, q, myQQSession);
			CurrentPart = Quest.CurrentPart;
			
			if(QQUtils.QQDebug>0){
				qqLogger.debug("------" +Calendar.getInstance().getTimeInMillis()+"------");
				qqLogger.debug("@"+Quest.startIdx+" v="+Quest.validCount);
				for(int dd=0;dd<10;dd++){
					qqLogger.debug(Quest.op[dd][0]+"-"+Quest.op[dd][1]+"-"+Quest.op[dd][2]+"-"+Quest.op[dd][3]+"-"+Quest.op[dd][4]);					
				}
			}
			// Update profile after a new Question!
			lastSeed = Quest.getSeed();
			myQQProfile.setLastSeed(lastSeed);

			// Update the Score
			tvScore.setText(String.valueOf(myQQProfile.getScore()));

			myQQProfileHandler.saveProfile(myQQProfile); // TODO: Do I need to
															// save after each
															// question? On exit
															// only?

			// Show the Question!
			tvQ.setText(QQUtils.fixQ(q.txt(Quest.startIdx, Quest.qLen,QQUtils.QQTextFormat.AYAMARKS_BRACKETS_ONLY)));
			QOptIdx = 0;
			
			//Show Score Up/Down
			if(Quest.qType == QType.NOTSPECIAL){
				tvScoreUp.setText(myQQProfile.getUpScore(Quest.CurrentPart));
				tvScoreDown.setText(myQQProfile.getDownScore(Quest.CurrentPart));
			} else {
				tvScoreUp.setText(String.valueOf(Quest.qType.getScore()));
				tvScoreDown.setText("-");				
			}
			
		}

		// Concat correct options to the Question!
		if (QOptIdx > 0)
			// I use 3 spaces with quran_me font, or a single space elsewhere
			tvQ.setText(QQUtils.fixQ(tvQ
					.getText()
					.toString()
					.concat(q.txt(Quest.startIdx + Quest.qLen + (QOptIdx - 1)
									* Quest.oLen, Quest.oLen, QQUtils.QQTextFormat.AYAMARKS_BRACKETS_ONLY) + "  "
							)));

		// Scramble options
		int[] scrambled = new int[5];
		scrambled = QQUtils.randperm(5);
		correct_choice = QQUtils.findIdx(scrambled, 0); // idx=1

		//Display Instructions
		tvInstructions.setText(Quest.qType.getInstructions());
		if(Quest.qType == QType.NOTSPECIAL)
			QQUtils.tvSetBackgroundFromDrawable(tvInstructions, R.drawable.tv_instruction_background);
		else
			QQUtils.tvSetBackgroundFromDrawable(tvInstructions, R.drawable.tv_instruction_special_background);

		
		// Display Options:
		String strTemp = new String();
		for (int j = 0; j < 5; j++) {
			if(Quest.qType==QType.NOTSPECIAL)
				strTemp = q.txt(Quest.op[QOptIdx][scrambled[j]], Quest.oLen, QQUtils.QQTextFormat.AYAMARKS_NONE);
			else{
				switch(Quest.qType){
				case SURANAME:
					strTemp = "  سورة  " + QQUtils.getSuraNameFromIdx(Quest.op[QOptIdx][scrambled[j]]);
					break;
				case SURAAYACOUNT:
					strTemp = " آيات السورة " + Quest.op[QOptIdx][scrambled[j]];
					break;
				case AYANUMBER:
					strTemp = " رقم الآية " + Quest.op[QOptIdx][scrambled[j]];
					break;
				default: 
					strTemp = "-";
					break;
				}
			}
			btnArray[j].setText(QQUtils.fixQ(strTemp));
		}
		updateOptionButtonsColor(correct_choice); //Update background Color
		
		if (level == 3) {
			// Start the timer
			startTimer(5);
			if (QOptIdx == 1) {
				// display(" [-] No more valid Motashabehat!");
			} else {
				// display([' -- ',num2str(validCount),' correct options
				// left!']); // TODO: Subtract done options
			}
		}else{ // Not level 3, Remove the timer
			bar.setVisibility(View.INVISIBLE);
		}

		QQinit = 0;

	}

	private void setBackCard() {
		tvBack.setText(getCorrectAnswer());
		quranReviewUri = String.valueOf(QQUtils.getSuraIdx(Quest.startIdx)+1) +"/" +
						 q.ayaNumberOf(Quest.startIdx);		
	}

	private String getCorrectAnswer() {
		// Display Correct answer
		return "[" + "  سورة  "+ QQUtils.getSuraName(Quest.startIdx) + " - آياتها " + q.ayaCountOfSuraAt(Quest.startIdx)+ "] "+ "\n"
				+ QQUtils.fixQ(q.txt(Quest.startIdx, 12 * Quest.oLen + Quest.qLen,QQUtils.QQTextFormat.AYAMARKS_FULL))
				+ " ...";
		}

	public boolean onNavigationItemSelected(int itemPosition, long itemId) {
		myQQProfile.setLevel(itemPosition+1);
		myQQProfileHandler.saveProfile(myQQProfile);
		return true;
	}
	
	public QQProfileHandler getProfileHandler(){
		return myQQProfileHandler;
	}
	
	DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
	    @Override
	    public void onClick(DialogInterface dialog, int which) {
	        switch (which){
	        case DialogInterface.BUTTON_POSITIVE:
	            //Yes button clicked
	        	myQQSession.reportDialogDisplayed();
	        	myQQSession.isDailyQuizRunning = true;
	        	//TODO: Start The Daily Quiz!
	            break;

	        case DialogInterface.BUTTON_NEGATIVE:
	            //No button clicked
	        	myQQSession.reportDialogDisplayed();
	            break;
	        }
	    }
	};

	public void askDailyQuiz() {
		builder = new AlertDialog.Builder(this);
		builder.setMessage("Are you sure?")
			.setPositiveButton("Yes", dialogClickListener)
		    .setNegativeButton("No", dialogClickListener)
		    .show();		
	}

}