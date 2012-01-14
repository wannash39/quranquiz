package com.google.code.quranquiz;


import java.io.IOException;

import android.app.Activity;
import android.database.SQLException;
import android.graphics.Typeface;
import android.os.Bundle;
import com.google.code.quranquiz.R;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

public class QuranQuizActivity extends Activity implements RadioGroup.OnCheckedChangeListener {

	/**
	 * @uml.property  name="tv"
	 * @uml.associationEnd  
	 */
	private TextView tv;
	/**
	 * @uml.property  name="rgQQOptions"
	 * @uml.associationEnd  
	 */
	private RadioGroup rgQQOptions;
	/**
	 * @uml.property  name="q"
	 * @uml.associationEnd  
	 */
	private QQDataBaseHelper q;
	/**
	 * @uml.property  name="quest"
	 * @uml.associationEnd  
	 */
	private QQQuestion Quest;
	/**
	 * @uml.property  name="qOptIdx"
	 */
	private int QOptIdx=-1;
	//TODO: Grab the last seed from the loaded profile! (replace -1, level 1)
	private int level = 1;
	private int lastSeed = -1;
	private int correct_choice=0;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		
        q = new QQDataBaseHelper(this);
        try {
        	q.createDataBase();
 
        } catch(IOException ioe) {
        	throw new Error("Unable to create database");
        }
 
	 	try {
	 		q.openDataBase();
	 	} catch(SQLException sqle) {
	 		throw sqle;
	 	}
	 	
		Typeface othmanyFont = Typeface.createFromAsset(getAssets(), "fonts/KacstQurn.ttf");
		tv = (TextView) findViewById(R.id.textView1);
		tv.setTypeface(othmanyFont);
		tv = (TextView) findViewById(R.id.radioOp1);
		tv.setTypeface(othmanyFont);
		tv = (TextView) findViewById(R.id.radioOp2);
		tv.setTypeface(othmanyFont);
		tv = (TextView) findViewById(R.id.radioOp3);
		tv.setTypeface(othmanyFont);
		tv = (TextView) findViewById(R.id.radioOp4);
		tv.setTypeface(othmanyFont);
		tv = (TextView) findViewById(R.id.radioOp5);
		tv.setTypeface(othmanyFont);
		
		tv = (TextView) findViewById(R.id.textView1);
		
		rgQQOptions = (RadioGroup) findViewById(R.id.radioQQOptions);
		
		// Make the first Question
		userAction(-1);
		// Set action Listener
		rgQQOptions.setOnCheckedChangeListener(this);

	}

	@Override
	protected void onDestroy() {
		if(q != null)q.closeDatabase();
		super.onDestroy();
	}
	
	public void onCheckedChanged(RadioGroup rg, int CheckedID) {
		int SelID=-2;

		switch(CheckedID){
		case R.id.radioOp1:
			SelID=0;
			break;
		case R.id.radioOp2:
			SelID=1;
			break;
		case R.id.radioOp3:
			SelID=2;
			break;
		case R.id.radioOp4:
			SelID=3;
			break;
		case R.id.radioOp5:
			SelID=4;
			break;
		}
		if( SelID<0)
			return;
		
		userAction(SelID);
		//rgQQOptions.clearCheck();
		((RadioButton)rgQQOptions.getChildAt(SelID)).setChecked(false);
	}

private void userAction(int selID) {
	
    // Check if wrong choice
    if(QOptIdx > 0 && correct_choice != selID){
        //Display Correct answer
		Toast.makeText(this, "["+QQUtils.getSuraName(Quest.startIdx)+"] "+q.txt(Quest.startIdx,10), Toast.LENGTH_LONG).show();
        QOptIdx = -1;
    }
    else{
    	QOptIdx = (QOptIdx==-1)?-1:QOptIdx +1;
    }
	
	if(QOptIdx == -1 || QOptIdx == 10){
		Quest = new QQQuestion(lastSeed,level,q); 
		lastSeed = Quest.getSeed();
		
		// Show the Question!
		tv.setText(q.txt(Quest.startIdx,Quest.qLen));
		QOptIdx = 0;
	}
	
	// Concat correct options to the Question!
	if(QOptIdx>0)
		tv.setText(tv.getText().toString().concat(' ' +q.txt(Quest.startIdx+Quest.qLen+QOptIdx-1)+' '));
	
    //Scramble options
    int[] scrambled = new int[5];
    scrambled  = QQUtils.randperm(5);
    correct_choice = QQUtils.findIdx(scrambled,0); //idx=1
    
    //Display Options:
	String strTemp = new String();
	for(int j=0;j<5;j++){
		strTemp = q.txt(Quest.op[QOptIdx][scrambled[j]]) ;
		((RadioButton)rgQQOptions.getChildAt(j)).setText(strTemp);
	}
	
    if(level==2){
        //display(['    -- ',num2str(validCount),' correct options left!']); % TODO: Subtract done options
    }
    else if(level==3 && QOptIdx==1){
        //display('  [-] No more valid Motashabehat!');
    }

}
}