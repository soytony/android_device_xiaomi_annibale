// SPDX-License-Identifier: Apache-2.0
package android.media;

import android.content.Context;

/** Linkage for disabled MIUI/OZO recording extensions, never reports support. */
public final class AudioParaManger {
    public interface EventListener {
        void onAudioLevel(int left, int right);
        void onMicBlocking(int left, int right);
    }
    public interface TuneListener {}
    public AudioParaManger(AudioRecord recorder, Context context) {}
    public AudioParaManger(MediaRecorder recorder, Context context) {}
    public boolean creatOzoEffect(int session) { return false; }
    public boolean createAudioEffects(int session) { return false; }
    public boolean init(int a, int b, int c, double d, double e, double f,
            double g, double h, boolean i) { return false; }
    public int[] getAudioConfig() { return null; }
    public int setFocusRegion(int a, int b, int c, int d, int e) { return -1; }
    public int setViewRegion(int a, int b, int c, int d, int e) { return -1; }
    public void createAudioObject(EventListener event, TuneListener tune) {}
    public void createOzo(EventListener event) {}
    public void createOzo(EventListener event, TuneListener tune) {}
    public void prepare() {}
    public void start() {}
    public void releaseAudioEffect() {}
    public void releaseOzoEffect() {}
    public void setAudioFocusAzimuth(double value) {}
    public void setAudioFocusAzimuth(double value, int channel) {}
    public void setAudioFocusElevation(double value) {}
    public void setAudioFocusElevation(double value, int channel) {}
    public void setAudioFocusHeight(double value) {}
    public void setAudioFocusWidth(double value) {}
    public void setAudioWindNoise(boolean value) {}
    public void setAudioZoomLevel(double value) {}
    public void setMaxSupportLevel(int value) {}
    public void setRecordType(int value) {}
    public void setUserGain(double value) {}
    public void setUserMode(boolean value) {}
}
