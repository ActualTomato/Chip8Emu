package naren.ragu.chip8emujavafx;

import javax.sound.sampled.*;

/**
 * Credit to Michael Arnauts for this file
 * @author Michaël Arnauts (https://github.com/michaelarnauts)
 */
public class Sound {
    private boolean isEnabled;
    private boolean isPlaying;
    private AudioFormat af;
    private SourceDataLine sdl;

    private Thread playThread;

    private byte[] buf = new byte[256];

    int volume;

    /** Creates a new instance of Sound */
    public Sound(boolean isEnabled, int volume) {
        this.volume = volume;
        try {
            af = new AudioFormat(44100f, 8, 1, true, true);
            sdl = AudioSystem.getSourceDataLine(af);
            sdl.open(af, 512);
            this.isEnabled = isEnabled;
            isPlaying = false;

            regenerateSoundBuffer();

        } catch (LineUnavailableException ex) {
            ex.printStackTrace();
            isEnabled = false;
        }

        playThread = new PlayThread();
        playThread.setPriority(Thread.MAX_PRIORITY);
        playThread.start();
    }

    int getVolume(){
        return this.volume;
    }

    void setVolume(int volume){
        this.volume = volume;

        regenerateSoundBuffer();
    }

    void regenerateSoundBuffer(){
        // generate the sound to play
        for (int i=0; i<buf.length; i++) {
            buf[i] = (byte) ((127 * (Math.sin(i * 0.05f))) * ((double)volume/100));
        }
    }

    public void startSound() {
        /*
        if (isPlaying || !isEnabled) return;
        isPlaying = true;

        playThread = new PlayThread();
        playThread.setPriority(Thread.MAX_PRIORITY);
        playThread.start();

        */

        if (isPlaying || !isEnabled) return;
        isPlaying = true;
    }

    public void stopSound() {
        isPlaying = false;
    }

    public void endSound(){
        playThread.stop();
    }

    public void setEnabled(boolean isEnabled) {
        this.isEnabled = isEnabled;
    }

    public boolean isEnabled() {
        return isEnabled;
    }

    class PlayThread extends Thread {

        public void run(){
            try {
                sdl.start();
                while (true){
                    if(isPlaying) {
                        sdl.start();
                        sdl.write(buf, 0, buf.length/4);
                    }
                    else{
                        sdl.stop();
                        //sdl.write(new byte[2], 0, 2);
                        //sdl.stop();
                        //sdl.drain();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

}