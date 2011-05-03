package coldstream.android.nuclearlite;

import com.google.ads.*;

import java.util.Random;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.LightingColorFilter;
import android.graphics.Typeface;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.TabHost.TabSpec;

public class nuclear extends Activity implements LocationListener{
    /* this class implements LocationListener, which listens for both
	 * changes in the location of the device and changes in the status
	 * of the GPS system. */
	
	static final String tag = "Main"; // for Log
	protected static final int TIK = 0x8006;
	protected static final int HIK = 0x8007;
	protected static final int FIX = 0x8008;
	protected static final int SUCCESS = 0x1337;
	protected static final int PULSE = 0x9911;
	protected static final int START = 0x1000;
	protected static final int INSTR = 0x1001;
	protected static final int HIGH = 0x1002;
	protected static final int CRED = 0x1003;
	protected static final int QUIT = 0x1004;
	protected static final int BACK = 0x1005;
	protected static final int CONTINUE = 0x1006;
	protected static final int MENU = 0x1007;
	
	protected static final float ENABLE_RADIUS = 10.0f;
	protected static final float STEPPING = 25.0f;
	
	protected static final String MY_AD_UNIT_ID = "a14db94cbe1675d";
	
	int debug = 1; // 0 = sharp version, 1 = half debug, 2 = full or terminal debug mode
	boolean menu_flag = false;
		
	Thread thread1;

	long cdt = -10;
	double targetDistance = 0.1;
	
	private SoundManager mSoundManager;
	
	SharedPreferences app_preferences;
	SharedPreferences prefsPrivate;
	SharedPreferences.Editor editor;
	SharedPreferences.Editor prefsEditor;
	int programCounter;
	
	//Menu elements
	Button startButton;
	Button instrButton;
	Button highButton;
	Button credButton;
	Button quitButton;
	
	Button backButton1;
	Button backButton2;
	Button backButton3;
	
	//Main elements
	TextView mainTab;
	TabSpec ts1;
	TextView timerTxt;
	Button disarmButton;
	Thermometer therm;
	
	ImageView imgQuest;
	TextView txtQuest;
	TextView txtInfo;
	TextView txtInfo2;
	
	TextView highScoreTxt;
	TextView successTxt;
	Button continueButton;
	Button menuButton;
	
	StringBuilder sb;
	StringBuilder sb2;
	StringBuilder sb3;
	int noOfFixes = 0;
	double startLongitude;
	double startLatitude;
	double operatingLongitude;
	double operatingLatitude;
	double targetLongitude;
	double targetLatitude;
	float[] startDelta = new float[3];
	float[] operatingDelta = new float[3];
	float[] targetDelta = new float[3];
	float accDist = 0.0f;
	float totalDist = 0.0f;
				
	long score = 0;
	long totalScore = 0;
	long[] highScore = new long[5];
	quests myQuests;
		
	LocationManager lm;
	PowerManager pm;
	PowerManager.WakeLock wl;
	
	Vibrator vib; 

	
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		mSoundManager = new SoundManager();
		//mSoundManager.initSounds(getBaseContext());
		mSoundManager.initSounds(this);
		mSoundManager.addSound(1, R.raw.pulse3);
		//mSoundManager.addSound(2, R.raw.ambiences9);
		mSoundManager.addSound(3, R.raw.chimes3);
		mSoundManager.addSound(4, R.raw.boom4);
		//mSoundManager.playSound(2,1f);
		
		pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wl = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "My Tag");
        wl.acquire();
        //Log.v(tag, "Before Main");
		//setContentView(R.layout.main);
     // Get the app's shared preferences
        SharedPreferences app_preferences = PreferenceManager.getDefaultSharedPreferences(this);
        programCounter = app_preferences.getInt("pcounter", 0);
        
        //A program counter
        SharedPreferences.Editor editor = app_preferences.edit();
        editor.putInt("pcounter", ++programCounter);
        editor.commit(); // Very important
		
        // App's private prefs
        SharedPreferences prefsPrivate = getSharedPreferences("preferences", Context.MODE_PRIVATE);
        totalDist = prefsPrivate.getFloat("totalDist", 0.0f);
        totalScore = prefsPrivate.getLong("totalScore", 0);
        highScore[0] = prefsPrivate.getLong("hs0", 0);
        highScore[1] = prefsPrivate.getLong("hs1", 0);
        highScore[2] = prefsPrivate.getLong("hs2", 0);
        highScore[3] = prefsPrivate.getLong("hs3", 0);
        highScore[4] = prefsPrivate.getLong("hs4", 0);
        /*accBlood = prefsPrivate.getLong("accBlood", 0);
        accSweat = prefsPrivate.getLong("accSweat", 0);
        accTears = prefsPrivate.getLong("accTears", 0);*/
        myQuests = new quests(this, prefsPrivate.getInt("currentQuest", 1));
		
	    /* the location manager is the most vital part it allows access
		 * to location and GPS status services */
		lm = (LocationManager) getSystemService(LOCATION_SERVICE);
		
		vib = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
		
        setupMenu();
        //mSoundManager.playSound(3,1f);
        //mSoundManager.playSound(2,1.0f);
        MediaPlayer mp = MediaPlayer.create(this, R.raw.ambiences9);
        mp.setLooping(false);
        mp.start();
	}
	
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		Intent mainIntent = new Intent(nuclear.this,nuclear.class);
		nuclear.this.startActivity(mainIntent);
		nuclear.this.finish();
	}
	
    public void setupMenu(){
        setContentView(R.layout.menu);
		//Log.v(tag, "After Main");
                
        AdView adView1 = new AdView(this, AdSize.BANNER, MY_AD_UNIT_ID);
        LinearLayout layout = (LinearLayout)findViewById(R.id.menu_layout);
		layout.addView(adView1);
		AdRequest request1 = new AdRequest();
		request1.setTesting(false);
		try{
			Criteria crit = new Criteria();  
			//crit.setAccuracy(Criteria.ACCURACY_FINE);  
			crit.setAccuracy(Criteria.NO_REQUIREMENT);  
			String provider = lm.getBestProvider(crit, true);  
			request1.setLocation(lm.getLastKnownLocation(provider));
		}
		catch(Exception e){
			;
		}
		adView1.loadAd(request1);
        
		startButton = (Button) findViewById(R.id.startbutton);
		startButton.setOnClickListener(new OnClickListener() {
			//@Override
			public void onClick(View v) {  // onClick Method
				Message m1 = new Message();
            	m1.what = START;                            
            	messageHandler.sendMessage(m1);

			}
		});
		startButton.getBackground().setColorFilter(new LightingColorFilter(0xAAAAAAAA, 0xFFFFFF00));
		
		instrButton = (Button) findViewById(R.id.instrbutton);
		instrButton.setOnClickListener(new OnClickListener() {
			//@Override
			public void onClick(View v) {  // onClick Method
				Message m1 = new Message();
            	m1.what = INSTR;                            
            	messageHandler.sendMessage(m1);

			}
		});
		instrButton.getBackground().setColorFilter(new LightingColorFilter(0xAAAAAAAA, 0xFFFFFF00));
		
		highButton = (Button) findViewById(R.id.highbutton);
		highButton.setOnClickListener(new OnClickListener() {
			//@Override
			public void onClick(View v) {  // onClick Method
				Message m1 = new Message();
            	m1.what = HIGH;                            
            	messageHandler.sendMessage(m1);

			}
		});
		highButton.getBackground().setColorFilter(new LightingColorFilter(0xAAAAAAAA, 0xFFFFFF00));
		
		credButton = (Button) findViewById(R.id.credbutton);
		credButton.setOnClickListener(new OnClickListener() {
			//@Override
			public void onClick(View v) {  // onClick Method
				Message m1 = new Message();
            	m1.what = CRED;                            
            	messageHandler.sendMessage(m1);

			}
		});
		credButton.getBackground().setColorFilter(new LightingColorFilter(0xAAAAAAAA, 0xFFFFFF00));
		
		quitButton = (Button) findViewById(R.id.quitbutton);
		quitButton.setOnClickListener(new OnClickListener() {
			//@Override
			public void onClick(View v) {  // onClick Method
				Message m1 = new Message();
            	m1.what = QUIT;                            
            	messageHandler.sendMessage(m1);

			}
		});
		quitButton.getBackground().setColorFilter(new LightingColorFilter(0xAAAAAAAA, 0xFFFFFF00));
		
		menu_flag = true;
		Log.v(tag, "Menu setup complete");
	}
	
	public void setupMain(){
		setContentView(R.layout.main);
		mainTab = new TextView(this);
		mainTab.setText("Main");
		
		noOfFixes = 0;
		targetDelta[0] = 0.0f;
		
		TabHost tab_host = (TabHost) findViewById(R.id.edit_item_tab_host);
		tab_host.setup();
		
		ts1 = tab_host.newTabSpec("TAB_QUEST");
		ts1.setIndicator("Mission");
		//ts1.setIndicator(mainTab);
		ts1.setContent(R.id.quest_tab);
		tab_host.addTab(ts1);

		TabSpec ts2 = tab_host.newTabSpec("TAB_MAIN");
		ts2.setIndicator("Gadgets");
		ts2.setContent(R.id.main_tab);
		tab_host.addTab(ts2);

		TabSpec ts3 = tab_host.newTabSpec("TAB_STATUS");
		ts3.setIndicator("Stats");
		ts3.setContent(R.id.status_tab);
		tab_host.addTab(ts3);

		tab_host.setCurrentTab(2);
		TextView tv = (TextView) tab_host.getCurrentTabView().findViewById(android.R.id.title); //for Selected Tab
        tv.setTextColor(Color.parseColor("#FFFFFFFF"));
        
        tab_host.setCurrentTab(1);
		tv = (TextView) tab_host.getCurrentTabView().findViewById(android.R.id.title); //for Selected Tab
        tv.setTextColor(Color.parseColor("#FFFFFFFF"));
        
        tab_host.setCurrentTab(0);
		tv = (TextView) tab_host.getCurrentTabView().findViewById(android.R.id.title); //for Selected Tab
        tv.setTextColor(Color.parseColor("#FFFFFFFF"));
		
		txtInfo = (TextView) findViewById(R.id.textInfo);
		txtInfo2 = (TextView) findViewById(R.id.textInfo2);
		
		disarmButton = (Button) findViewById(R.id.disarmbutton);
		disarmButton.setOnClickListener(new OnClickListener() {
			//@Override
			public void onClick(View v) {  // onClick Method
				Message m1 = new Message();
            	m1.what = SUCCESS;                            
            	messageHandler.sendMessage(m1);
			}
		});

		//disarmButton.getBackground().setColorFilter(new LightingColorFilter(0xAAAAAAAA, 0xFFFFFF00));
		
		//disarmButton.setVisibility(View.INVISIBLE);
		disarmButton.setBackgroundResource(R.drawable.missile2);
		disarmButton.setText("    EMP    ");
		disarmButton.setClickable(false);
				
		txtQuest = (TextView) findViewById(R.id.textQuest);
		imgQuest = (ImageView) findViewById(R.id.imageQuest);
		
		//Display current quest
        txtQuest.setText(myQuests.getCurrentQuestText()); //Ok, make an update quest-tab function!!!
		imgQuest.setImageResource(myQuests.getCurrentQuestPicId());
		
		timerTxt = (TextView) findViewById(R.id.timer_text);  
	    Typeface font = Typeface.createFromAsset(getAssets(), "digit.ttf");  
	    timerTxt.setTypeface(font);
	    
	    timerTxt.setTextColor(Color.GREEN);
		timerTxt.setText(android.text.format.DateFormat.format("00:mm:ss ", myQuests.getCurrentQuestCond()[2]*1000));
	   
	    therm = (Thermometer) findViewById(R.id.thermometer);
	    therm.setHandTarget(0.0f);
	    
	    cdt = myQuests.getCurrentQuestCond()[2];
	    
	    myTimer();
	}
	
	public void showInstructions(){
		setContentView(R.layout.instructions);
		
		backButton1 = (Button) findViewById(R.id.backbutton1);
		backButton1.setOnClickListener(new OnClickListener() {
			//@Override
			public void onClick(View v) {  // onClick Method
				Message m1 = new Message();
            	m1.what = BACK;                            
            	messageHandler.sendMessage(m1);
			}
		});		
	}
	
	public void showHighScore(){
		setContentView(R.layout.highscore);
		
		backButton2 = (Button) findViewById(R.id.backbutton2);
		backButton2.setOnClickListener(new OnClickListener() {
			//@Override
			public void onClick(View v) {  // onClick Method
				Message m1 = new Message();
            	m1.what = BACK;                            
            	messageHandler.sendMessage(m1);
			}
		});		
		
		highScoreTxt = (TextView) findViewById(R.id.hightext);
		sb3 = new StringBuilder(512);
		sb3.setLength(0);
		sb3.append('\n');
		for(int i = 0; i < 5; i++){
			sb3.append((i + 1) + ".          " + highScore[i]);
			sb3.append('\n');
			sb3.append('\n');
		}
		
		highScoreTxt.setText(sb3.toString());
		
	}
	
	public void showCredits(){
		setContentView(R.layout.credits);
		
		backButton3 = (Button) findViewById(R.id.backbutton3);
		backButton3.setOnClickListener(new OnClickListener() {
			//@Override
			public void onClick(View v) {  // onClick Method
				Message m1 = new Message();
            	m1.what = BACK;                            
            	messageHandler.sendMessage(m1);
			}
		});		
	}
	
	public void showSuccess(){
		setContentView(R.layout.success);
		
		successTxt = (TextView) findViewById(R.id.successtxt);  
		
		continueButton = (Button) findViewById(R.id.continuebutton);
		continueButton.setOnClickListener(new OnClickListener() {
			//@Override
			public void onClick(View v) {  // onClick Method
				Message m1 = new Message();
            	m1.what = CONTINUE;                            
            	messageHandler.sendMessage(m1);
			}
		});
		
		menuButton = (Button) findViewById(R.id.menubutton);
		menuButton.setOnClickListener(new OnClickListener() {
			//@Override
			public void onClick(View v) {  // onClick Method
				Message m2 = new Message();
            	m2.what = MENU;                            
            	messageHandler.sendMessage(m2);
			}
		});	
		
		//update score
		score = 0;
		score += (myQuests.getCurrentQuest() * 1000);
		score += (cdt * 10);
		
		totalScore += score;
		
		//Show new Score-count
		sb2 = new StringBuilder(512);
		sb2.setLength(0);
		sb2.append("MISSION ACCOMPLISHED!");
		sb2.append('\n');
		sb2.append('\n');
		
		sb2.append("Seconds Left: " + cdt + " x10");
		sb2.append('\n');
		sb2.append("Level Bonus: " + myQuests.getCurrentQuest() + " x1000");
		sb2.append('\n');
		
		sb2.append('\n');
		sb2.append("SCORE: " + score);
		sb2.append('\n');
		
		sb2.append('\n');
		sb2.append("TOTAL SCORE: " + totalScore);
		sb2.append('\n');
		
		successTxt.setText(sb2.toString());
		
		//Goto next quest
		if(myQuests.getTotalQuests() > myQuests.getCurrentQuest())
			myQuests.setCurrentQuest(nuclear.this, myQuests.getCurrentQuest() + 1);
		
		
		txtQuest.setText(myQuests.getCurrentQuestText()); //Ok, make an update quest-tab function!!!
		imgQuest.setImageResource(myQuests.getCurrentQuestPicId());
		
		//Save private data to preferences
		SharedPreferences prefsPrivate = getSharedPreferences("preferences", Context.MODE_PRIVATE);
        SharedPreferences.Editor prefsEditor = prefsPrivate.edit();
        prefsEditor.putFloat("totalDist", totalDist);
        prefsEditor.putLong("totalScore", totalScore);
        prefsEditor.putInt("currentQuest", myQuests.getCurrentQuest());
        prefsEditor.commit(); // Very important*/
        //Continue? Menu? Show a continue button
        mSoundManager.playSound(3,1.0f);
	}
	
	public void myTimer(){
		
		thread1 = new Thread()
	    {
	        
	        public void run() {
	            try {
	            	
	                while(true) {
	                	//Location location = lm.getLastKnownLocation("gps");
	                	for(int i = 0; i < 6;i++){
	                		sleep(100);
	        				Random generator = new Random(System.currentTimeMillis());
	        				
        					if( (Math.abs(generator.nextInt()) % 150) > (Math.round((double)targetDelta[0] / targetDistance) * 100) ){
        				
		                		Message m = new Message();
		                    	m.what = PULSE;                            
		                    	messageHandler.sendMessage(m);
        					}
	                		
	                	}
	                	Message m1 = new Message();
                    	m1.what = HIK;                            
                    	messageHandler.sendMessage(m1);
	                	
                    	for(int i = 0; i < 4;i++){
	                		sleep(100);
	                		//Location location = lm.getLastKnownLocation("gps"); 
	        				Random generator = new Random(System.currentTimeMillis());
	        				
        					if( (Math.abs(generator.nextInt()) % 150) > (Math.round((double)targetDelta[0] / targetDistance) * 100) ){
        				
		                		Message m = new Message();
		                    	m.what = PULSE;                            
		                    	messageHandler.sendMessage(m);
        					}
	                		
	                	}
	                	
                    	//Log.v(tag, "Tik");
                    	
                    	Message m2 = new Message();                    	
                    	m2.what = TIK;                            
                    	messageHandler.sendMessage(m2);
	                    
	                }
	            } catch (InterruptedException e) {
	                e.printStackTrace();
	            }
	        }
	    };

	    thread1.start();
	}
	
	private Handler messageHandler = new Handler() {
		@Override
		public void handleMessage(Message msg){
			switch(msg.what){
				//handle message
			case HIK:
				if(cdt <= 30){
					timerTxt.setTextColor(Color.BLACK);
					timerTxt.setText("88:88:88 ");
					
				}
				break;
			case TIK:
				if(noOfFixes > 0){
					cdt--;
					therm.setHandTarget((float)(100 - ((double)targetDelta[0] / targetDistance) * 100));
		
				}
				if(cdt >= 0 ){
					//Log.v(tag, "Tok");
					timerTxt.setTextColor(Color.GREEN);
					timerTxt.setText(android.text.format.DateFormat.format("00:mm:ss ", cdt*1000));
					
				}
				else if(cdt < 0 && cdt >= -5){
					Log.v(tag, "BOOM");
					if(cdt == -1)
						mSoundManager.playSound(4,1.0f);
					
					setContentView(R.layout.boom);
					vib.vibrate(200 * Math.abs(cdt));

				}
				else{
					thread1.interrupt();
					//cdt = 100;
					
					//In the best of worlds...
					Intent mainIntent = new Intent(nuclear.this,nuclear.class);
					nuclear.this.startActivity(mainIntent);
					nuclear.this.finish();
				
					//reset all data - erase totalscore...??
					//HighScore?
					//Save private data to preferences
					SharedPreferences prefsPrivate = getSharedPreferences("preferences", Context.MODE_PRIVATE);
			        SharedPreferences.Editor prefsEditor = prefsPrivate.edit();
			        prefsEditor.putLong("totalScore", 0);
			        sb3 = new StringBuilder(12);
			        
			        for(int i = 0; i < 5; i++){
			        	sb3.append("hs" + i);
			        	if(totalScore > highScore[i]){
			        		prefsEditor.putLong(sb3.toString(), totalScore);
			        		totalScore = highScore[i];
			        	}
			        		
			        }			        
			        prefsEditor.putInt("currentQuest", 1);
			        prefsEditor.commit(); // Very important*/
									
					score = 0;
					totalScore = 0;
				}
				
				
				break;
			case FIX:
				if(targetDelta[0] <= ENABLE_RADIUS && noOfFixes > 0){
					//Show EMP Disarm Button
					//disarmButton.setVisibility(View.VISIBLE);
					//credButton.getBackground().setColorFilter(new LightingColorFilter(0x88888888, 0xFFFF0000));
					disarmButton.setBackgroundResource(R.drawable.missile);
					disarmButton.setText("           ");
					
					disarmButton.setClickable(true);
				}
				else{
					//disarmButton.setVisibility(View.INVISIBLE);
					//credButton.getBackground().setColorFilter(new LightingColorFilter(0xAAAAAAAA, 0xFFFFFF00));
					disarmButton.setBackgroundResource(R.drawable.missile2);
					disarmButton.setText("    EMP    ");
					disarmButton.setClickable(false);
				}
				break;
			case SUCCESS:
				thread1.interrupt();
				//Well, show statics animation
				//Show reboot
				showSuccess();
		
		        break;
			case CONTINUE:     
				/*Intent mainIntent2 = new Intent(nuclear.this,nuclear.class);
				nuclear.this.startActivity(mainIntent2);
				nuclear.this.finish();*/
				
				
				setupMain();
				
				break;
			case MENU:
				Intent mainIntent3 = new Intent(nuclear.this,nuclear.class);
				nuclear.this.startActivity(mainIntent3);
				nuclear.this.finish();
				break;
			case PULSE:
				if(cdt >= 0 && noOfFixes > 0)
					mSoundManager.playSound(1,0.5f);
				break;
			case START:
				setupMain();
				menu_flag = false;
				break;
			case INSTR:
				showInstructions();
				menu_flag = false;
				break;
			case HIGH:
				showHighScore();
				menu_flag = false;
				break;
			case CRED:
				showCredits();
				menu_flag = false;
				break;
			case QUIT:
				nuclear.this.finish();
				break;
			case BACK:
				setupMenu();
				break;
			default:
			}
		}
	};
	

	@Override
	protected void onResume() {
		/*
		 * onResume is is always called after onStart, even if the app hasn't been
		 * paused
		 *
		 * add location listener and request updates every 1000ms or 10m
		 */
		
		lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 2000, 5f, this);
		
		super.onResume();
	}

	@Override
	protected void onPause() {
		/* GPS, as it turns out, consumes battery like crazy */
		lm.removeUpdates(this);
		super.onResume();
	}

	//@Override
	public void onLocationChanged(Location location) {
		Log.v(tag, "Location Changed");

		Random generator = new Random(System.currentTimeMillis());
		sb = new StringBuilder(512);
		double rad = 180.0/Math.PI;
		double irad = 1.0/rad;
		double brng = Math.abs(generator.nextDouble()) * (2 * Math.PI);
		
		if(cdt >= 0) //Basically if we are in main...
			noOfFixes++;
		
		if(noOfFixes == 1){
			//Randomize bearing and targetDistance
			brng = Math.abs(generator.nextDouble()) * (2 * Math.PI);
			targetDistance = (double)myQuests.getCurrentQuestCond()[0] + (double)(Math.abs(generator.nextInt()) % Math.abs(myQuests.getCurrentQuestCond()[1] - myQuests.getCurrentQuestCond()[0]));
			double td = targetDistance;
			double dr = (td/6378137.0);
			
			startLatitude = location.getLatitude();
			startLongitude = location.getLongitude();
			operatingLatitude = startLatitude;
			operatingLongitude = startLongitude;
			targetLatitude = rad*(Math.asin( (Math.sin(startLatitude*irad)*Math.cos(dr)) + (Math.cos(startLatitude*irad)*Math.sin(dr)*Math.cos(brng)) ));
			targetLongitude = startLongitude + rad*(Math.atan2(Math.sin(brng)*Math.sin(dr)*Math.cos(startLatitude*irad), 
                    Math.cos(dr)-(Math.sin(startLatitude*irad)*Math.sin(targetLatitude*irad))));
		}
		
		/*Log.v(tag, String.valueOf(startLatitude));
		Log.v(tag, String.valueOf(startLongitude));
		Log.v(tag, String.valueOf(targetLatitude));
		Log.v(tag, String.valueOf(targetLongitude));*/
				
		Location.distanceBetween(startLatitude, startLongitude, location.getLatitude(), location.getLongitude(), startDelta);
		Location.distanceBetween(operatingLatitude, operatingLongitude, location.getLatitude(), location.getLongitude(), operatingDelta);
		Location.distanceBetween(targetLatitude, targetLongitude,  location.getLatitude(), location.getLongitude(),targetDelta);
						
		if(operatingDelta[0] >= STEPPING ){ 
			if(operatingDelta[0] <= 500){
				accDist += operatingDelta[0];
				totalDist += operatingDelta[0];
			}			
			operatingLatitude = location.getLatitude();
			operatingLongitude = location.getLongitude();
		}
		
		//send message and do this
		
    		
		/* display some of the data in the TxtInfo(s) */
    	if(cdt >= 0){
    		Message m1 = new Message();
        	m1.what = FIX;                            
        	messageHandler.sendMessage(m1);
        	
			sb.append("No. of GPS Fixes:  ");
			sb.append(noOfFixes);
			sb.append('\n');
			sb.append('\n');
			
			sb.append("Distance to target: " + Math.round(targetDelta[0]) + " m");
			sb.append('\n');
					
			sb.append("Distance moved: " + Math.round(accDist) + " m");
			sb.append('\n');
			
			//sb.append("Current quest: " + myQuests.getCurrentQuest() + "/" + myQuests.getTotalQuests());
			//sb.append('\n');
			
			//sb.append("Conditions: " + myQuests.getCurrentQuestCond()[0] + "R " + myQuests.getCurrentQuestCond()[1] + "G " + myQuests.getCurrentQuestCond()[2] + "B");
			//sb.append('\n');
			
			//sb.append('\n');
			//sb.append("Angle: " + brng);
			//sb.append('\n');
			
					
			txtInfo.setText(sb.toString());
			
			sb.setLength(0);
			sb.append("Sessions started: ");
			sb.append(programCounter);
			sb.append('\n');
			
			sb.append("Total distance moved: " + Math.round(totalDist) + " m");
			sb.append('\n');
			/*sb.append("Target dist: " + Math.round(targetDistance) + " m");
			sb.append('\n');
			sb.append("Distance to target: " + Math.round(targetDelta[0]) + " m");
			sb.append('\n');*/
			
			
			sb.append('\n');
			sb.append("TOTAL SCORE: " + totalScore);
			sb.append('\n');
			
			//txtInfo2.setTextColor(Color.GREEN);
			txtInfo2.setText(sb.toString());
			
			Log.v(tag, txtInfo.toString());
			Log.v(tag, sb.toString());
    	}
	}
	
	//@Override
	public void onProviderDisabled(String provider) {
		/* this is called if/when the GPS is disabled in settings */
		Log.v(tag, "Disabled");

		/* bring up the GPS settings */
		Intent intent = new Intent(
				android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
		startActivity(intent);
	}

	//@Override
	public void onProviderEnabled(String provider) {
		Log.v(tag, "Enabled");
		Toast.makeText(this, "GPS Enabled", Toast.LENGTH_SHORT).show();

	}

	//@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
		/* This is called when the GPS status alters */
		switch (status) {
		case LocationProvider.OUT_OF_SERVICE:
			Log.v(tag, "Status Changed: Out of Service");
			//Toast.makeText(this, "Status Changed: Out of Service",
			//		Toast.LENGTH_SHORT).show();
			break;
		case LocationProvider.TEMPORARILY_UNAVAILABLE:
			Log.v(tag, "Status Changed: Temporarily Unavailable");
			//Toast.makeText(this, "Status Changed: Temporarily Unavailable",
			//		Toast.LENGTH_SHORT).show();
			break;
		case LocationProvider.AVAILABLE:
			Log.v(tag, "Status Changed: Available");
			//Toast.makeText(this, "Status Changed: Available",
			//		Toast.LENGTH_SHORT).show();
			break;
		}
	}
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event)  {
	    if (  Integer.valueOf(android.os.Build.VERSION.SDK) < 7 //Instead use android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.ECLAIR
	            && keyCode == KeyEvent.KEYCODE_BACK
	            && event.getRepeatCount() == 0) {
	        // Take care of calling this method on earlier versions of
	        // the platform where it doesn't exist.
	        onBackPressed();
	    }

	    return super.onKeyDown(keyCode, event);
	}

	//@Override
	public void onBackPressed() {
	    // This will be called either automatically for you on 2.0
	    // or later, or by the code above on earlier versions of the
	    // platform.
		
		//In menu or elsewhere?
		Message m1 = new Message();    	
		if(menu_flag)
			m1.what = QUIT;   
		else
			m1.what = MENU;   
			
		messageHandler.sendMessage(m1);
	    return;
	}


	@Override
	protected void onStop() {
		/* may as well just finish since saving the state is not important for this toy app */
		wl.release();
		//Log.v(tag, "Released wlock??");
		
		try{		
			if (thread1.isAlive())
				thread1.interrupt();
		}
		catch (NullPointerException e) {
            e.printStackTrace();
        }
		
		finish();
		super.onStop();
	}
	
		

}

	
	