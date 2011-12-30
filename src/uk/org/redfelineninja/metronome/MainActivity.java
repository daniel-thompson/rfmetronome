package uk.org.redfelineninja.metronome;

//import uk.org.redfelineninja.metronome.R;

import android.app.Activity;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.ToggleButton;
import android.widget.AdapterView.OnItemSelectedListener;

public class MainActivity extends Activity {
	private static final int MIN_TEMPO = 12;
	
	private boolean mEnabled = false;
	private int mBpm = 100;
	private int mAccent = 0;
	
	private TextView mCachedTextViewBpm;
	private TextView mCachedTextViewTempo;
	private SeekBar mCachedSeekBarTempo;
	
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
			// do nothing
			
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

	
	private OnItemSelectedListener mAccentListener = new OnItemSelectedListener() {
		@Override
	    public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
			mAccent = pos;
			
			//Toast.makeText(parent.getContext(),
			//		"Accent pattern is " + parent.getItemAtPosition(pos).toString(),
			//		Toast.LENGTH_LONG).show();
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
        setContentView(R.layout.main);
        
        ToggleButton toggle = (ToggleButton) findViewById(R.id.toggleButton1);
        toggle.setOnCheckedChangeListener(mEnableListener);
        
        mCachedTextViewBpm = (TextView) findViewById(R.id.textViewBpm);
        mCachedTextViewTempo = (TextView) findViewById(R.id.textViewTempo);
        
        mCachedSeekBarTempo = (SeekBar) findViewById(R.id.seekBarTempo);
        mCachedSeekBarTempo.setOnSeekBarChangeListener(mSeekBarListener);
        
        Button minusButton = (Button) findViewById(R.id.button1);
        minusButton.setTag(R.id.button_tag, new Integer(-1));
        minusButton.setOnClickListener(mPlusMinusListener);
        
        Button plusButton = (Button) findViewById(R.id.button2);
        plusButton.setTag(R.id.button_tag, new Integer(1));
        plusButton.setOnClickListener(mPlusMinusListener);
        
        Spinner spinner = (Spinner) findViewById(R.id.spinner1);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this, R.array.accent_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(mAccentListener);
        
        mMetronome = new Metronome();
        setBpm(mBpm);
    }

    @Override
    protected void onStart() {
        super.onStart();
        // The activity is visible (but make not have focus)
        
        if (mEnabled)
        	mMetronome.stop();
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
        
        mMetronome.stop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // The activity is about to be destroyed.
        // Do nothing - the three lifetime states are nested (so onStop() should have happened
        //              already
    }

    public int getBpm() {
    	return mBpm;
    }
    
    public void setBpm(int bpm) {
    	if (bpm < MIN_TEMPO)
    		bpm = MIN_TEMPO;
    	
    	int max_tempo = MIN_TEMPO + mCachedSeekBarTempo.getMax();
    	if (bpm > max_tempo)
    		bpm = max_tempo;

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