package uk.org.redfelineninja.metronome;

import java.util.Random;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;

public class Metronome implements AudioTrack.OnPlaybackPositionUpdateListener {
	private AudioTrack mAudioTrack;

	private int delayBufferSize = 44100;
	private short[] delayBuffer;
	private int delayPointer;
	private int delayInFrames = 64;
	
	private short decay = 0x6000;
	
	private short exciteBufferSize = 68;
	private short[] exciteBuffer;
	private short excitePointer = exciteBufferSize;
	
	private short[] greaterClickBuffer;
	private short[] lesserClickBuffer;
	
	private long frameCounter;
	private long frameAlarm;
	private int sampleRateInHz = 44100;
	
	private int beatCounter = 1;
	private int beatsPerBar = 4;
	private int beatsPerMinute;
	private int framesPerBeat;
	
	public Metronome() {
		delayBuffer = new short[delayBufferSize];
		
		greaterClickBuffer = new short[exciteBufferSize];
		lesserClickBuffer = new short[exciteBufferSize];
		
		Random random = new Random();
		for (int i=0; i<exciteBufferSize; i++) {
			lesserClickBuffer[i] = getSawtooth(i, exciteBufferSize/2);
			greaterClickBuffer[i] = (short) (lesserClickBuffer[i] + (random.nextInt() >> 19));
		}
		
		setBeatsPerMinute(120);
	}
	
	public int getBeatsPerMinute() {
		return beatsPerMinute;
	}

	public void setBeatsPerMinute(int beatsPerMinute) {
		this.beatsPerMinute = beatsPerMinute;
		
		framesPerBeat = (int) (sampleRateInHz / ((double) beatsPerMinute / 60.0));
	}

    public void start() {
		if (null != mAudioTrack)
			return;
		
		int sampleRateInHz = 44100;
		int channelConfig = AudioFormat.CHANNEL_OUT_MONO;
		int audioFormat = AudioFormat.ENCODING_PCM_16BIT;

		int minBufferSize = AudioTrack.getMinBufferSize (
				sampleRateInHz, channelConfig, audioFormat);
		assert 0 != minBufferSize; // TODO
		int bufferSize = 1 * sampleRateInHz * 2; // two seconds
		if (bufferSize < minBufferSize)
			bufferSize = minBufferSize;
		
		mAudioTrack = new AudioTrack(
				AudioManager.STREAM_MUSIC,
				sampleRateInHz, channelConfig, audioFormat,
				bufferSize, AudioTrack.MODE_STREAM);
		mAudioTrack.setPositionNotificationPeriod(bufferSize / (2 * 4));
		mAudioTrack.setPlaybackPositionUpdateListener(this);
		for (int i=0; i<4; i++)
			issuePeriod();
		mAudioTrack.play();
    }
    
    public void stop() {
		if (null == mAudioTrack)
			return;
		
		mAudioTrack.pause();
		mAudioTrack.release();
		mAudioTrack = null;
    }

	@Override
	public void onMarkerReached(AudioTrack arg0) {
		assert false; // not reached
		
	}

	@Override
	public void onPeriodicNotification(AudioTrack arg0) {
		assert arg0 == mAudioTrack;
		
		issuePeriod();
	}

	
	private short getSawtooth(int step, int period) {
		int half = period/2;
		int quarter = period/4;
		int sign;
		step %= period;
		
		if (step < half) {
			sign = 1;
	
		} else {
			sign = -1;
			step = step - half;
		}
		
		if (step > quarter)
			step = half - step;
		
		return (short) (sign * (((0x3fff0000 / quarter) * step) >> 16));
	}
	
	private short getHistoricSample(int pointer, int offset) {
		return delayBuffer[(pointer + delayBufferSize - offset) % delayBufferSize];
	}
	
	private short excite() {
		if (excitePointer >= exciteBufferSize)
			return 0;
		
		return exciteBuffer[excitePointer++];
	}
	
	private short filter(int pointer) {
		int sample;
		
		// trivial FIR
		sample  = getHistoricSample(pointer, delayInFrames);
		sample += getHistoricSample(pointer, delayInFrames-1);
		sample /= 2;

		// scale the sample (Q16)
		sample *= decay;
		sample >>= 15;
		
		return (short) sample;
	}
	
	private void issuePeriod() {
		int numSamples = mAudioTrack.getPositionNotificationPeriod();
		
		// karplus-strong synth storing new samples in its own delay buffer
		int pointer = delayPointer;
		for (int i=0; i<numSamples; i++) {
			delayBuffer[pointer] = (short) (excite() + filter(pointer));
		
			if(++pointer >= delayBufferSize) {
				pointer = 0;
			}
			
			if (frameAlarm < frameCounter++) {
				// arm the frame alarm
				frameAlarm += framesPerBeat;
				
				// send in a new excitement pulse
				if (beatCounter == 1) {
					exciteBuffer = greaterClickBuffer;
				} else {
					exciteBuffer = lesserClickBuffer;
				}
				excitePointer = 0;
				
				beatCounter++;
				if (beatCounter > beatsPerBar) {
					beatCounter = 1;
				}
			}
		}
		
		if ((delayPointer + numSamples) <= delayBufferSize) {
			mAudioTrack.write(delayBuffer, delayPointer, numSamples);
		} else {
			mAudioTrack.write(delayBuffer, delayPointer, delayBufferSize - delayPointer);
			mAudioTrack.write(delayBuffer, 0, numSamples - (delayBufferSize - delayPointer));
		}
		
		delayPointer = pointer;
	}
}
