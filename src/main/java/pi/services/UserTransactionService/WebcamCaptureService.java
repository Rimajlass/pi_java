package pi.services.UserTransactionService;

import com.github.sarxos.webcam.Webcam;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.imageio.ImageIO;

public class WebcamCaptureService {

    private final AtomicBoolean running = new AtomicBoolean(false);
    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "webcam-preview");
        t.setDaemon(true);
        return t;
    });

    private Webcam webcam;

    public synchronized void startPreview(ImageView targetView) {
        if (running.get()) {
            return;
        }
        webcam = Webcam.getDefault();
        if (webcam == null) {
            throw new IllegalStateException("No webcam detected.");
        }
        webcam.setViewSize(new Dimension(640, 480));
        webcam.open();
        running.set(true);

        executor.submit(() -> {
            while (running.get()) {
                try {
                    BufferedImage frame = webcam.getImage();
                    if (frame != null) {
                        WritableImage fxImage = SwingFXUtils.toFXImage(frame, null);
                        Platform.runLater(() -> targetView.setImage(fxImage));
                    }
                    Thread.sleep(90);
                } catch (Exception ignored) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
    }

    public synchronized Path captureFrameToTempJpg() {
        if (!running.get() || webcam == null || !webcam.isOpen()) {
            throw new IllegalStateException("Webcam is not running.");
        }
        BufferedImage frame = webcam.getImage();
        if (frame == null) {
            throw new IllegalStateException("Unable to capture frame from webcam.");
        }
        try {
            Path temp = Files.createTempFile("facepp_capture_", ".jpg");
            ImageIO.write(frame, "jpg", temp.toFile());
            return temp;
        } catch (IOException e) {
            throw new IllegalStateException("Failed to write captured image.", e);
        }
    }

    public synchronized void stop() {
        running.set(false);
        if (webcam != null) {
            try {
                webcam.close();
            } catch (Exception ignored) {
            } finally {
                webcam = null;
            }
        }
    }
}
