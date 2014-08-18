package domain.audio;

import java.io.BufferedInputStream;
import java.io.InputStream;

import javax.swing.JOptionPane;
import javazoom.jl.decoder.JavaLayerException;
import javazoom.jl.player.Player;

public class AudioPlayer{
	Player MP3player;
	private boolean looping = true;
        private Thread t_mp3;
        public boolean running;

	public void startPlaying(String url){
		try {
			InputStream is = this.getClass().getResourceAsStream(url);
			BufferedInputStream bis = new BufferedInputStream(is);
			MP3player = new Player(bis);
			start();			
		} catch (JavaLayerException e) {
                    JOptionPane.showMessageDialog(null, e.getMessage());
		}
	}
	public void start() {
		t_mp3 = new Thread(new Runnable() { public void run() {
			try {
				//while(looping){
					MP3player.play();
				//}
			} catch (JavaLayerException e) {
				System.out.println("Critical error - <AudioPlayer>");
				e.printStackTrace();
			}
		}},"loop-mp3");
                running = true;
		t_mp3.start();
                
	}
	public void stopLoop() {
        //try {
            running = false;
            //t_mp3.join();
        //} catch (InterruptedException ex) {
          //  System.err.println("Stoppen thread: "+ex.getMessage());
       // }
            looping = false;
            
	}
	public void stopPlaying(){
            running = false;
		if(MP3player!=null)MP3player.close();                
	}
        public void load(String url){
            try {
			InputStream is = this.getClass().getResourceAsStream(url);
			BufferedInputStream bis = new BufferedInputStream(is);
			MP3player = new Player(bis);			
		} catch (JavaLayerException e) {
                    JOptionPane.showMessageDialog(null, e.getMessage());
		}
        }
}
