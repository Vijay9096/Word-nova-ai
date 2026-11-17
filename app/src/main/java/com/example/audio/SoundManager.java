package com.example.audio;

import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioTrack;

import com.example.data.GamePrefs;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SoundManager {
    private final GamePrefs prefs;
    private final ExecutorService sfxExecutor = Executors.newSingleThreadExecutor();
    private volatile boolean musicRunning = false;
    private Thread musicThread;
    private AudioTrack musicTrack;

    public SoundManager(GamePrefs prefs) {
        this.prefs = prefs;
    }

    public void onResume() {
        if (prefs.isMusicEnabled()) {
            startAmbientMusic();
        }
    }

    public void onPause() {
        stopAmbientMusic();
    }

    public void refreshMusicState() {
        if (prefs.isMusicEnabled()) {
            startAmbientMusic();
        } else {
            stopAmbientMusic();
        }
    }

    private synchronized void startAmbientMusic() {
        if (musicRunning) return;
        musicRunning = true;
        musicThread = new Thread(() -> {
            final int sampleRate = 22050;
            final int bufferSamples = sampleRate / 2;
            int minBuf = AudioTrack.getMinBufferSize(sampleRate,
                    AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT);
            int bufBytes = Math.max(minBuf, bufferSamples * 2);

            try {
                AudioTrack track = new AudioTrack.Builder()
                        .setAudioAttributes(new AudioAttributes.Builder()
                                .setUsage(AudioAttributes.USAGE_GAME)
                                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                                .build())
                        .setAudioFormat(new AudioFormat.Builder()
                                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                                .setSampleRate(sampleRate)
                                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                                .build())
                        .setBufferSizeInBytes(bufBytes)
                        .setTransferMode(AudioTrack.MODE_STREAM)
                        .build();

                musicTrack = track;
                track.play();

                // Gentle calming pentatonic ambient chime loop
                double[] notes = {261.63, 329.63, 392.00, 523.25, 392.00, 329.63, 293.66, 392.00};
                short[] pcm = new short[bufferSamples];
                int noteIdx = 0;
                double phase = 0.0;

                while (musicRunning && prefs.isMusicEnabled()) {
                    double freq = notes[noteIdx % notes.length];
                    noteIdx++;
                    for (int i = 0; i < bufferSamples; i++) {
                        double t = (double) i / bufferSamples;
                        double env = Math.sin(Math.PI * t);
                        double sample = Math.sin(phase) * env * 0.035;
                        pcm[i] = (short) (sample * Short.MAX_VALUE);
                        phase += 2.0 * Math.PI * freq / sampleRate;
                    }
                    if (!musicRunning) break;
                    track.write(pcm, 0, pcm.length);
                }

                try {
                    track.stop();
                } catch (IllegalStateException ignored) {}
                track.release();
                musicTrack = null;
            } catch (Throwable ignored) {
                musicRunning = false;
            }
        }, "WordNovaMusicThread");
        musicThread.setDaemon(true);
        musicThread.start();
    }

    public synchronized void stopAmbientMusic() {
        musicRunning = false;
        if (musicTrack != null) {
            try {
                musicTrack.pause();
                musicTrack.flush();
            } catch (Throwable ignored) {}
        }
        if (musicThread != null) {
            musicThread.interrupt();
            musicThread = null;
        }
    }

    public void playClick() {
        playTone(620, 45, 0.10);
    }

    public void playWordFound() {
        if (!prefs.isSfxEnabled()) return;
        sfxExecutor.execute(() -> {
            playToneSync(523.25, 70, 0.16);
            playToneSync(659.25, 70, 0.16);
            playToneSync(783.99, 110, 0.18);
        });
    }

    public void playLevelComplete() {
        if (!prefs.isSfxEnabled()) return;
        sfxExecutor.execute(() -> {
            playToneSync(523.25, 80, 0.18);
            playToneSync(659.25, 80, 0.18);
            playToneSync(783.99, 80, 0.18);
            playToneSync(1046.50, 180, 0.22);
        });
    }

    public void playError() {
        playTone(220, 90, 0.12);
    }

    private void playTone(double freqHz, int durationMs, double amplitude) {
        if (!prefs.isSfxEnabled()) return;
        sfxExecutor.execute(() -> playToneSync(freqHz, durationMs, amplitude));
    }

    private void playToneSync(double freqHz, int durationMs, double amplitude) {
        final int sampleRate = 22050;
        final int count = (sampleRate * durationMs) / 1000;
        if (count <= 0) return;
        short[] samples = new short[count];
        for (int i = 0; i < count; i++) {
            double env = 1.0 - ((double) i / count);
            double s = Math.sin(2.0 * Math.PI * i * freqHz / sampleRate) * env * amplitude;
            samples[i] = (short) (s * Short.MAX_VALUE);
        }
        try {
            int minBuf = AudioTrack.getMinBufferSize(sampleRate,
                    AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT);
            AudioTrack track = new AudioTrack.Builder()
                    .setAudioAttributes(new AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_GAME)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build())
                    .setAudioFormat(new AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build())
                    .setBufferSizeInBytes(Math.max(minBuf, samples.length * 2))
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build();
            track.write(samples, 0, samples.length);
            track.play();
            Thread.sleep(durationMs + 20L);
            track.release();
        } catch (Throwable ignored) {}
    }

    public void release() {
        stopAmbientMusic();
        sfxExecutor.shutdownNow();
    }
}
