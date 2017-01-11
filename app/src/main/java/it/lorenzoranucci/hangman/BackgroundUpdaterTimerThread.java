package it.lorenzoranucci.hangman;

/**
 * Created by Lorenzo on 31/07/2015.
 */
public class BackgroundUpdaterTimerThread extends Thread {
    public interface TimerThreadListener {
        void onTimerOn();
    }
    public void setListener(TimerThreadListener listener) {
        this.listener = listener;
    }

    private TimerThreadListener listener;
    private boolean stop = false;

    public void stopThread() {
        this.stop = true;
        listener = null;
    }

    @Override
    public void run() {
        while (!stop) {
            try {
                Thread.sleep(40000, 0);
                listener.onTimerOn();
            } catch (InterruptedException e) {

            }
        }

    }
}