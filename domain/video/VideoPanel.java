package domain.video;

import java.awt.Graphics;
import java.awt.Image;
import java.awt.image.BufferedImage;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

public class VideoPanel extends JPanel {

    private Image imageOnScreen;

    public void setNewImage(BufferedImage javaImage) {
        SwingUtilities.invokeLater(new RefreshableImage(javaImage));
    }

    @Override
    public synchronized void paint(Graphics g) {
        super.paintComponent(g);
        if (imageOnScreen != null) {
            g.drawImage(imageOnScreen, 0, 0, null);
        }
    }

    private class RefreshableImage implements Runnable {

        private final BufferedImage newImage;

        public RefreshableImage(BufferedImage javaImage) {
            this.newImage = javaImage;
        }

        @Override
        public void run() {
            imageOnScreen = newImage;
            repaint();
        }
    }
}
