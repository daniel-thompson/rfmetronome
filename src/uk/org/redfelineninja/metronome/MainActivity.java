package uk.org.redfelineninja.metronome;

//import uk.org.redfelineninja.metronome.R;

import java.util.List;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;
import android.widget.AdapterView.OnItemSelectedListener;

public class MainActivity extends Activity {
	private static final int MIN_TEMPO = 12;
	
	private boolean mEnabled = false;
	private int mBpm = 100;
	private int mAccent = 0;
	
	private ToggleButton mCachedToggleButton;
	private TextView mCachedTextViewBpm;
	private TextView mCachedTextViewTempo;
	private SeekBar mCachedSeekBarTempo;
	private Spinner mCachedSpinnerAccent;
	
	private Metronome mMetronome;
	
	private CompoundButton.OnCheckedChangeListener mEnableListener =
			new CompoundButton.OnCheckedChangeListener() {
		@Override
		public void onCheckedChanged (CompoundButton buttonView, boolean isChecked) {
			setEnabled(isChecked);
		}
	};
	
	private SeekBar.OnSeekBarChangeListener mSeekBarListener = new SeekBar.OnSeekBarChangeListener() {
		@Override
		public void onStopTrackingTouch(SeekBar seekBar) {
			mMetronome.flush();
		}
		
		@Override
		public void onStartTrackingTouch(SeekBar seekBar) {
			// do nothing
		}
		
		@Override
		public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
			if (fromUser) {
				setBpm(MIN_TEMPO + progress);
			}
			
		}
	};
	
	private View.OnClickListener mPlusMinusListener = new View.OnClickListener() {
		@Override
        public void onClick(View v) {
			Integer i = (Integer) v.getTag(R.id.button_tag);
			setBpm(getBpm() + i.intValue());
		}
    };

	private View.OnClickListener mTapTempoListener = new View.OnClickListener() {
		long lastTap = 0;

		@Override
        public void onClick(View v) {
			final int MINUTE_MILLIS = 60000;

			long thisTap = SystemClock.uptimeMillis();
			long tapDuration = thisTap - lastTap; // milliseconds
			int bpm = (int) (MINUTE_MILLIS / tapDuration);

			// bpm might be nonsense but setBpm() ignores out-of-range values
			setBpm(bpm);
			mMetronome.flush();

			lastTap = thisTap;
		}
    };

	private OnItemSelectedListener mAccentListener = new OnItemSelectedListener() {
		@Override
	    public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
			setAccent(pos);
		}

		@Override
	    public void onNothingSelected(AdapterView<?> parent) {
	      // Do nothing.
	    }
	};
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // this is a neat trick to get the volume buttons controlling
        // the metronome volume even when the metronome is stopped
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        
        setContentView(R.layout.main);
        
        mCachedToggleButton = (ToggleButton) findViewById(R.id.toggleButton1);
        mCachedToggleButton.setOnCheckedChangeListener(mEnableListener);
        
        mCachedTextViewBpm = (TextView) findViewById(R.id.textViewBpm);
        mCachedTextViewTempo = (TextView) findViewById(R.id.textViewTempo);
        
        mCachedSeekBarTempo = (SeekBar) findViewById(R.id.seekBarTempo);
        mCachedSeekBarTempo.setOnSeekBarChangeListener(mSeekBarListener);
        
        Button buttonMinus = (Button) findViewById(R.id.buttonMinus);
        buttonMinus.setTag(R.id.button_tag, new Integer(-1));
        buttonMinus.setOnClickListener(mPlusMinusListener);
    
        Button buttonTapTempo = (Button) findViewById(R.id.buttonTapTempo);
        buttonTapTempo.setOnClickListener(mTapTempoListener);
        
        Button buttonPlus = (Button) findViewById(R.id.buttonPlus);
        buttonPlus.setTag(R.id.button_tag, new Integer(1));
        buttonPlus.setOnClickListener(mPlusMinusListener);
        
        mCachedSpinnerAccent = (Spinner) findViewById(R.id.spinner1);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this, R.array.accent_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mCachedSpinnerAccent.setAdapter(adapter);
        mCachedSpinnerAccent.setOnItemSelectedListener(mAccentListener);
        
        mMetronome = new Metronome();
        
        // Restore preferences
        SharedPreferences settings = getPreferences(MODE_PRIVATE);
        mBpm = settings.getInt("beatsPerMinute", mBpm);
        mAccent = settings.getInt("accentPattern", mAccent);
       
        // Apply preferences
        setBpm(mBpm);
        setAccent(mAccent);
    }

    @Override
    protected void onStart() {
        super.onStart();
        // The activity is visible (but make not have focus)
        // Do nothing.
    }

    @Override
    protected void onResume() {
        super.onResume();
        // The activity has focus (it is now "resumed").
        // Do nothing.
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Another activity is taking focus (this activity is about to be "paused").
        // Do nothing.
    }

    @Override
    protected void onStop() {
        super.onStop();
        // The activity is no longer visible (it is now "stopped")
        
        // Halt the clicking (and update the UI)
        mMetronome.stop();
        mCachedToggleButton.setChecked(false);
        mEnabled = false;
        
        // Store preferences
        SharedPreferences settings = getPreferences(MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        editor.putInt("beatsPerMinute", mBpm);
        editor.putInt("accentPattern", mAccent);
        editor.commit();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // The activity is about to be destroyed.
        // Do nothing - the three lifetime states are nested (so onStop() should have happened
        //              already
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
        case R.id.main_item_about:
        	
        	Intent intent = new Intent("org.openintents.action.SHOW_ABOUT_DIALOG");
        	
        	// See http://android-developers.blogspot.com/2009/01/can-i-use-this-intent.html
    	    final PackageManager packageManager = this.getPackageManager();
    	    List<ResolveInfo> list =
    	            packageManager.queryIntentActivities(intent,
    	                    PackageManager.MATCH_DEFAULT_ONLY);
    	    boolean intentAvailable = list.size() > 0;
    	    
    		if (intentAvailable) {
    			startActivityForResult(intent, 0);
    		} else {
    			Toast.makeText(this, "OI About is not installed", Toast.LENGTH_SHORT).show();
    		}
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }
    public int getBpm() {
    	return mBpm;
    }
    
    public void setBpm(int bpm) {
    	if (bpm < MIN_TEMPO)
    		return;
    	
    	int max_tempo = MIN_TEMPO + mCachedSeekBarTempo.getMax();
    	if (bpm > max_tempo)
    		return;

    	mCachedTextViewBpm.setText(Integer.toString(bpm));
    	mCachedSeekBarTempo.setProgress(bpm - MIN_TEMPO);
    
    	Resources res = getResources();
    	String[] tempos = res.getStringArray(R.array.tempo_array);
    	int tempoIndex = bpm / 20;
    	if (tempoIndex >= tempos.length)
    		tempoIndex = tempos.length - 1;
    	mCachedTextViewTempo.setText(tempos[tempoIndex]);
    
    	mMetronome.setBeatsPerMinute(bpm);
    	
    	mBpm = bpm;
    }
    
    public int getAccent() {
    	return getAccent();
    }
    
    public void setAccent(int accent) {
		int beatsPerBar = 0;
		
		// see accent_array
		switch (accent) {
		case 0: beatsPerBar = 0; break; // no accent
		case 1: beatsPerBar = 4; break; // 4/4
		case 2: beatsPerBar = 2; break; // 2/4
		case 3: beatsPerBar = 3; break; // 3/4
		case 4: beatsPerBar = 3; break; // 6/8 (double bpm?)
		default: assert false;
		}
		
		int uiAccent = mCachedSpinnerAccent.getSelectedItemPosition();
		if (uiAccent != accent)
			mCachedSpinnerAccent.setSelection(accent);
		
		mMetronome.setBeatsPerBar(beatsPerBar);
		mMetronome.flush();
		
		mAccent = accent;
    }
    
    public boolean getEnabled() {
    	return mEnabled;
    }
    
    public void setEnabled(boolean enabled) {
    	if (enabled)
    		mMetronome.start();
    	else
    		mMetronome.stop();
    	
    	mEnabled = enabled;
    }
}