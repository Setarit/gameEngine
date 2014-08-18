/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package launcher;

import domain.video.VideoPlayer;
import javax.swing.JFrame;

public class Test extends JFrame{
    public Test(){
        this.setBounds(0, 0, 800, 600);
        VideoPlayer player = new VideoPlayer();
        this.getContentPane().add(player.getVideoPanel());
        this.setVisible(true);
        player.playMovie("test.avi");
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }
}
