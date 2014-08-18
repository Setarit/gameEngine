package domain.video;

import com.xuggle.xuggler.Global;
import com.xuggle.xuggler.IContainer;
import com.xuggle.xuggler.IPacket;
import com.xuggle.xuggler.IPixelFormat;
import com.xuggle.xuggler.IStream;
import com.xuggle.xuggler.IStreamCoder;
import com.xuggle.xuggler.ICodec;
import com.xuggle.xuggler.IVideoPicture;
import com.xuggle.xuggler.IVideoResampler;
import com.xuggle.xuggler.Utils;
import java.awt.image.BufferedImage;

public class VideoPlayer {
    private boolean endOfVideo = false;
    private VideoPanel panel = new VideoPanel();
    
    /**
     * Only plays the file. You need the getVideoPanel() method to
     * get the panel on which the video plays
     * @param fileName - Name of the file to play
     */
    public void playMovie(String fileName) {
        //nagaan of we video naar pixels kunnen converteren
        if (!IVideoResampler.isSupported(
                IVideoResampler.Feature.FEATURE_COLORSPACECONVERSION)) {
            throw new RuntimeException("You must install the GPL version"
                    + " of Xuggler (with IVideoResampler support) for "
                    + " seeing the game videos \n --> Proposed solution open the game without video enabled");
        }
        IContainer videoContainer = IContainer.make(); // maken container Xuggler voor video
        // video container openen
        if (videoContainer.open(fileName, IContainer.Type.READ, null) < 0) {
            throw new IllegalArgumentException("could not open file: " + fileName);
        }

        int numStreams = videoContainer.getNumStreams(); // nagaan hoeveel video stream de opening van de container heeft gevonden

        //starten zoeken eerste video stream
        int videoStreamId = -1;
        IStreamCoder videoCoder = null; // decodeert de stroom aan bytes
        for (int i = 0; i < numStreams; i++) {
            // Vind het stream object
            IStream stream = videoContainer.getStream(i);
            IStreamCoder coder = stream.getStreamCoder();//juiste decoder vinden
            if (coder.getCodecType() == ICodec.Type.CODEC_TYPE_VIDEO) { //gevonden
                videoStreamId = i;
                videoCoder = coder;
                break;
            }
        }
        if (videoStreamId == -1) { //stop
            throw new RuntimeException("System error - <VideoPlayer> :: could not a find video stream in container: "
                    + fileName);
        }

        // Videostream gevonden in file, nu openen decoder.  
        if (videoCoder.open(null,null) < 0) {
            throw new RuntimeException("System error - <VideoPlayer> :: could not open video decoder for container: "
                    + fileName);
        }

        IVideoResampler resampler = null;
        if (videoCoder.getPixelType() != IPixelFormat.Type.BGR24) {
            //ongeldig formaat dus decoderen
            resampler = IVideoResampler.make(videoCoder.getWidth(),
                    videoCoder.getHeight(), IPixelFormat.Type.BGR24,
                    videoCoder.getWidth(), videoCoder.getHeight(), videoCoder.getPixelType());
            if (resampler == null) {
                throw new RuntimeException("System error - <VideoPlayer> :: could not create color space "
                        + "resampler for: " + fileName);
            }
        }
        /*
         * And once we have that, we draw a window on screen
         */
        //openJavaWindow();

        //doorheen de container gaan en alle pakkettjes analyseren
        IPacket packet = IPacket.make();
        long firstTimestampInStream = Global.NO_PTS;
        long systemClockStartTime = 0;
        while (videoContainer.readNextPacket(packet) >= 0) {
            //pakket gevonden, nagaan of tot stream behoort
            if (packet.getStreamIndex() == videoStreamId) {
                //afbeelding maken om videoframe te tonen
                IVideoPicture picture = IVideoPicture.make(videoCoder.getPixelType(),
                        videoCoder.getWidth(), videoCoder.getHeight());

                int offset = 0;
                while (offset < packet.getSize()) {
                    //decoderen en zoeken naar fouten
                    int bytesDecoded = videoCoder.decodeVideo(picture, packet, offset);
                    if (bytesDecoded < 0) {
                        throw new RuntimeException("System error - <VideoPlayer> :: got error decoding video in: "
                                + fileName);
                    }
                    offset += bytesDecoded;

                    //nagaan of videoafbeelding wel compleet is
                    if (picture.isComplete()) {
                        IVideoPicture newPic = picture;
                        /*
                         * If the resampler is not null, that means we didn't get the
                         * video in BGR24 format and
                         * need to convert it into BGR24 format.
                         */
                        if (resampler != null) {
                            // we moeten resamplen
                            newPic = IVideoPicture.make(resampler.getOutputPixelFormat(),
                                    picture.getWidth(), picture.getHeight());
                            if (resampler.resample(newPic, picture) < 0) {
                                throw new RuntimeException("System error - <VideoPlayer> :: could not resample video from: "
                                        + fileName);
                            }
                        }
                        if (newPic.getPixelType() != IPixelFormat.Type.BGR24) {
                            throw new RuntimeException("System error - <VideoPlayer> :: could not decode video"
                                    + " as BGR 24 bit data in: " + fileName);
                        }

                        /**
                         * We could just display the images as quickly as we decode them,
                         * but it turns out we can decode a lot faster than you think.
                         * 
                         * So instead, the following code does a poor-man's version of
                         * trying to match up the frame-rate requested for each
                         * IVideoPicture with the system clock time on your computer.
                         * 
                         * Remember that all Xuggler IAudioSamples and IVideoPicture objects
                         * always give timestamps in Microseconds, relative to the first
                         * decoded item. If instead you used the packet timestamps, they can
                         * be in different units depending on your IContainer, and IStream
                         * and things can get hairy quickly.
                         */
                        if (firstTimestampInStream == Global.NO_PTS) {
                            // This is our first time through
                            firstTimestampInStream = picture.getTimeStamp();
                            // get the starting clock time so we can hold up frames
                            // until the right time.
                            systemClockStartTime = System.currentTimeMillis();
                        } else {
                            long systemClockCurrentTime = System.currentTimeMillis();
                            long millisecondsClockTimeSinceStartofVideo =
                                    systemClockCurrentTime - systemClockStartTime;
                            // compute how long for this frame since the first frame in the
                            // stream.
                            // remember that IVideoPicture and IAudioSamples timestamps are
                            // always in MICROSECONDS,
                            // so we divide by 1000 to get milliseconds.
                            long millisecondsStreamTimeSinceStartOfVideo =
                                    (picture.getTimeStamp() - firstTimestampInStream) / 1000;
                            final long millisecondsTolerance = 50; // and we give ourselfs 50 ms of tolerance
                            final long millisecondsToSleep =
                                    (millisecondsStreamTimeSinceStartOfVideo
                                    - (millisecondsClockTimeSinceStartofVideo
                                    + millisecondsTolerance));
                            if (millisecondsToSleep > 0) {
                                try {
                                    Thread.sleep(millisecondsToSleep);
                                } catch (InterruptedException e) {
                                    // we might get this when the user closes the dialog box, so
                                    // just return from the method.
                                    return;
                                }
                            }
                        }

                        // And finally, convert the BGR24 to an Java buffered image
                        BufferedImage javaImage = Utils.videoPictureToImage(newPic);

                        // and display it on the Java Swing window
                        updatePanel(javaImage);
                    }
                }
            } else {
                /*
                 * This packet isn't part of our video stream, so we just
                 * silently drop it.
                 */
                do {
                } while (false);
            }

        }
        /*
         * Technically since we're exiting anyway, these will be cleaned up by 
         * the garbage collector... but because we're nice people and want
         * to be invited places for Christmas, we're going to show how to clean up.
         */
        if (videoCoder != null) {
            videoCoder.close();
            videoCoder = null;
        }
        if (videoContainer != null) {
            videoContainer.close();
            videoContainer = null;
        }
        videoEnded();

    }

    private void updatePanel(BufferedImage javaImage) {
        panel.setNewImage(javaImage);
    }

    private void videoEnded() {
        this.endOfVideo = true;
    }

    public boolean isEndOfVideo() {
        return endOfVideo;
    }

    public VideoPanel getVideoPanel() {
        return panel;
    }
    
    
}