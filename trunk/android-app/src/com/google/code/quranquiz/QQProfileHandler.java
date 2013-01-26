package com.google.code.quranquiz;

import java.io.Serializable;
import java.util.Random;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class QQProfileHandler implements Serializable{

	private static final long serialVersionUID = 1L;
	public static final String MY_PROFILE = "MyQQProfile";
    private transient static Context myContext;
    public static final String DEFAULT_STUDY_PARTS  = 
    		"1,"+String.valueOf(QQUtils.sura_idx[0])+",0,0;"
    	   +String.valueOf(QQUtils.sura_idx[0]+1)+","+String.valueOf(QQUtils.sura_idx[1]-QQUtils.sura_idx[0])+",0,0;";
    
    public QQProfile CurrentProfile;
    
    public QQProfileHandler(Context context) {
    	 myContext = context;
    }	
    
    public void saveProfile(QQProfile prof){

    	SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(myContext);
        SharedPreferences.Editor editor = settings.edit();
        editor.putInt("lastSeed", prof.getLastSeed());
        editor.putString("pref_userLevel", Integer.toString(prof.getLevel()));
        //editor.putInt("score", prof.getCorrect());     
        //editor.putInt("quesCount", prof.getQuesCount());
        editor.putString("studyParts", prof.getStudyParts());
        
        editor.commit();
        
        CurrentProfile = prof;
    }
    
	public QQProfile getProfile(){
		QQProfile myQQProfile;
		
		if (checkLastProfile()){ // Found a previously saved profile
			myQQProfile = getLastProfile();
		}else{ // Create a new profile with a random start
			//Toast.makeText(myContext, "Created a new profile!", Toast.LENGTH_LONG).show();
			myQQProfile = new QQProfile(new Random().nextInt(QQUtils.QuranWords), 1, QQProfileHandler.DEFAULT_STUDY_PARTS );
			reLoadParts(myQQProfile);
			saveProfile(myQQProfile);
		}
        CurrentProfile = myQQProfile;
		return myQQProfile;
	}
	
	private QQProfile getLastProfile() {
	    SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(myContext);
	    
	    // Note: Pref entries from xml are strings!
	    // manually inserted via editor are integers 
	    
		return new QQProfile(settings.getInt("lastSeed", 0),
							 Integer.parseInt(settings.getString("pref_userLevel", "")),
							 //settings.getInt("score", 0),
							 //settings.getInt("quesCount", 0),
							 settings.getString("studyParts", ""));
	}

	private boolean checkLastProfile(){
		// Check if a profile exists
	    SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(myContext);
	    return settings.contains("lastSeed");
	}

	public void reLoadParts(QQProfile profile) {
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(myContext);
		int checked,start, end;
		String newParts = new String("");

		// Al-Fatiha is always enabled
    	newParts = "1," +
	    		  String.valueOf(QQUtils.sura_idx[0]-1) + "," +
	    		  String.valueOf(profile.getCorrect(0)) + "," +
	    		  String.valueOf(profile.getQuesCount(0)) + ";";
    	
	    for(int i=1;i<45;i++){
	    	checked = (settings.getBoolean("QPart_s"+String.valueOf(i+1), false))?1:-1;
	    	start = QQUtils.sura_idx[i-1];
	    	end   = QQUtils.sura_idx[i]-1;
	    	newParts += String.valueOf(start*checked) + "," +
		    		  String.valueOf(end-start) + "," +
		    		  String.valueOf(profile.getCorrect(i)) + "," +
		    		  String.valueOf(profile.getQuesCount(i)) + ";";
        }
        for(int i=0;i<4;i++){
	    	checked = (settings.getBoolean("QPart_j"+String.valueOf(i+26), false))?1:-1;
	    	start = QQUtils.last5_juz_idx[i];
	    	end   = QQUtils.last5_juz_idx[i+1]-1;
	    	newParts += String.valueOf(start*checked) + "," +
		    		  String.valueOf(end-start) + "," +
		    		  String.valueOf(profile.getCorrect(i+45)) + "," +
		    		  String.valueOf(profile.getQuesCount(i+45)) + ";";
        }
    	start = QQUtils.last5_juz_idx[4];
    	end   = QQUtils.last5_juz_idx[5]-1;
		// Juz' 3amma is always enabled
    	newParts += String.valueOf(start) + "," +
	    		  String.valueOf(end-start) + "," +
	    		  String.valueOf(profile.getCorrect(49)) + "," +
	    		  String.valueOf(profile.getQuesCount(49));
    	
    	profile.setStudyParts(newParts);
    	saveProfile(profile);
	}

	public void reLoadCurrentProfile() {
		CurrentProfile = getLastProfile();
	}

}