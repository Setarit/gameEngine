package domain.gameEngine;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JPanel;

/**
 *
 * @author javalsoft
 */
public abstract class GamePanel extends JPanel implements Runnable {
    private Dimension screenDimension;
    private Thread animator;
// for the animation
    private volatile boolean running = false;
// stops the animation
    private volatile boolean gameOver = false;
// for game termination
    private Image dbImage;
    private Graphics dbg;
    private static final int NO_DELAYS_PER_YIELD = 16;
    /* Number of frames with a delay of 0 ms before the
    animation thread yields to other running threads. */
    private long period;
    private volatile boolean isPauzed = false;
    private static final int MAX_FRAME_SKIPS = 4;
    private long totalElapsedTime = 0L;
    private int frameCount;
    private int timeInGame;
    private long gameStartTime;
    private long prevStatsTime;

    /**
     * Constructor. Requests focus, initialises te engine
     * @param screenDimension - the dimension of the gamepanel 
     * @param fps - the FPS preferred (normal about 80)
     */
    public GamePanel(Dimension screenDimension, int fps) {
        this.screenDimension = screenDimension;
        setBackground(Color.white);
        setPreferredSize(screenDimension);
        setFocusable(true);
        requestFocus();// JPanel now receives key events
        readyForTermination();
        //period instellen
        //int fps = 80;
        this.period = (long) (1000.0/fps);
        this.period = period*1000000L;// ms --> nanosecs
        
        initGameComponents(screenDimension.height, screenDimension.width);
        addGameMouseListeners();
        addGameKeyListeners();
    }

    public void addNotify() /* Wait for the JPanel to be added to the
    JFrame/JApplet before starting. */ {
        super.addNotify();
// creatles the peer
        startGame();
// start the thread
    }

    private void startGame() // initialise and start the thread
    {
        if (animator == null || !running) {
            animator = new Thread(this);
            animator.start();
        }
    } // end of startGame( )

    public void stopGame() // called by the user to stop execution
    {
        running = false;
    }

    public void run() /* Repeatedly update, render, sleep */ {
        long beforeTime, afterTime, timeDiff, sleepTime;
        long overSleepTime = 0L;
        int noDelays = 0;
        long excess = 0L;
        gameStartTime = System.nanoTime();
        prevStatsTime = gameStartTime;
        beforeTime = gameStartTime;

        running = true;
        while (running) {
            if (!isPauzed) {
                gameUpdate();
// game state is updated
                gameRender();
// render to a buffer
                paintScreen();
// paint with the buffer
                afterTime = System.nanoTime();
                timeDiff = afterTime - beforeTime;
                sleepTime = (period - timeDiff) - overSleepTime;
                if (sleepTime > 0) {// some time left in this cycle
                    try {
                        Thread.sleep(sleepTime / 1000000L); // nano -> ms
                    } catch (InterruptedException ex) {
                    }
                    overSleepTime = (System.nanoTime() - afterTime) - sleepTime;
                } else {// sleepTime <= 0; frame took longer than the period
                    excess -= sleepTime; // store excess time value
                    overSleepTime = 0L;
                    if (++noDelays >= NO_DELAYS_PER_YIELD) {
                        Thread.yield();// give another thread a chance to run
                        noDelays = 0;
                    }
                }
                beforeTime = System.nanoTime();
                /* If frame animation is taking too long, update the game state
                without rendering it, to get the updates/sec nearer to
                the required FPS. */
                int skips = 0;
                while ((excess > period) && (skips < MAX_FRAME_SKIPS)) {
                    excess -= period;
                    gameUpdate();// update state but don't render
                    skips++;
                }
                printGameStats();
                //framesSkipped += skips;
            } else {
                try {
                    //gepauzeerd
                    Thread.sleep(1000);
                } catch (InterruptedException ex) {
                    Logger.getLogger(GamePanel.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
        this.endGame();
    } // end of run( )
// so enclosing JFrame/JApplet exits

    private void gameUpdate() {
        if (!gameOver&&!isPauzed) {
            updateAnimatableGameComponents();
        }
    }
// more methods, explained later...

    private void gameRender() {//current frame to a buffer
        if (dbImage == null) { // create the buffer
            dbImage = createImage(screenDimension.width, screenDimension.height);
            if (dbImage == null) {
                System.out.println("dbImage is null");
                return;
            } else {
                dbg = dbImage.getGraphics();
            }
        }
// clear the background
        dbg.setColor(Color.white);
        dbg.fillRect(0, 0, screenDimension.width, screenDimension.height);
        this.drawObjectsOnScreen(dbg);
        
        if (gameOver) {
            gameOverMessage(dbg);
        }
    } // end of gameRender( )

    protected void gameOverMessage(Graphics g) // center the game-over message
    {
        int x = 20;
        int y = 20;
        g.drawString("Game over", x, y);
    } // end of gameOverMessage( )

    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (dbImage != null) {
            g.drawImage(dbImage, 0, 0, null);
        }
    }

    private void readyForTermination() {
        addKeyListener(new KeyAdapter() {// listen for esc, q, end, ctrl-c

            public void keyPressed(KeyEvent e) {
                int keyCode = e.getKeyCode();
                if ((keyCode == KeyEvent.VK_ESCAPE)
                        || (keyCode == KeyEvent.VK_Q)
                        || (keyCode == KeyEvent.VK_END)
                        || ((keyCode == KeyEvent.VK_C) && e.isControlDown())) {
                    running = false;
                }
            }
        });
    }

    protected abstract void addGameMouseListeners();

    protected abstract void addGameKeyListeners();

    private void paintScreen() /* actively render the buffer image to the screen*/ {
        Graphics g;
        try {
            g = this.getGraphics(); // get the panel's graphic context
            if ((g != null) && (dbImage != null)) {
                g.drawImage(dbImage, 0, 0, null);
            }
            Toolkit.getDefaultToolkit().sync(); // sync the display on some systems
            g.dispose();
        } catch (Exception e) {
            System.out.println("Graphics context error: " + e);
        }
    } // end of paintScreen()

    public void pauseGame() {
        this.isPauzed = true;
    }
    public void resumeGame(){
        this.isPauzed = false;
    }

    protected abstract void initGameComponents(int screenHeight, int screenWidth);

    private void printGameStats() {
        frameCount++;
        long timeNow = System.nanoTime();
        double actualFPS = 0;
        long realElapsedTime = timeNow - prevStatsTime;// time since last stats collection
        totalElapsedTime += realElapsedTime;
        if (totalElapsedTime > 0) {
            //actualFPS = (((double)frameCount / totalElapsedTime)*1000000000L);        
            actualFPS = (((double)frameCount / prevStatsTime)*1000000000L); //ik
            actualFPS = actualFPS*1000000L;//ik
        }
        frameCount=0;//ik
        //System.out.println("FPS: "+actualFPS);
        timeInGame = (int) ((timeNow - gameStartTime)/1000000000L);
    }
    /**
     * 
     * @return time in game in seconds
     */
    public int getTimeInGame() {
        return timeInGame;
    }
    
    /**
     * Objecten die zich op de achtergrond bevinden dienen eerst getekend te worden!
     * @param dbg 
     */
    public void drawObjectsOnScreen(Graphics dbg) {
        dbg.setColor(Color.RED);
        dbg.drawString("No gameElements, override drawObjectsOnScreen", 40, 40);
    }

    protected abstract void updateAnimatableGameComponents();
    public void gameOver(){
        this.gameOver = true;
    }

    public abstract void endGame();

    public boolean isGameOver() {
        return this.gameOver;
    }
}
// end of GamePanel class
