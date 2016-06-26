package application;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

public class FXController {
	@FXML
	private Button start_btn;
	@FXML
	private Button record_start_btn;
	@FXML
	private ImageView currentFrame;
	// a timer for acquiring the video stream
	private ScheduledExecutorService timer;
	// the OpenCV object that realizes the video capture
	private VideoCapture capture = new VideoCapture();
	// a flag to change the button behavior
	private boolean cameraActive = false;
	protected int flag = 3;
	protected Scalar colour;

	@FXML
	protected void startCamera(ActionEvent event) {
		if (!this.cameraActive) {
			// start the video capture
			this.capture.open(0);
			// Check if the video stream is available
			if (this.capture.isOpened()) {
				this.cameraActive = true;

				// grab a frame every 33 ms(30 frames/sec)
				Runnable frameGrabber = new Runnable() {

					@Override
					public void run() {
						Image imageToShow = grabFrame();
						currentFrame.setImage(imageToShow);
					}

				};

				this.timer = Executors.newSingleThreadScheduledExecutor();
				this.timer.scheduleAtFixedRate(frameGrabber, 0, 33, TimeUnit.MILLISECONDS);

				// update the button content
				this.start_btn.setText("Stop Camera");

			} else {
				// log the error
				System.err.println("Impossible to open the camera connection...");
			}
		} else {
			// the camera is not active at this point
			this.cameraActive = false;
			// update again the button content
			this.start_btn.setText("Start Camera");
			// stop the timer
			try {
				this.timer.shutdown();
				this.timer.awaitTermination(33, TimeUnit.MILLISECONDS);
			} catch (InterruptedException e) {
				// log the exception
				System.err.println("Exception in stopping the frame capture, trying to release the camera now... " + e);
			}

			// release the camera
			this.capture.release();
			// clean the frame
			this.currentFrame.setImage(null);
		}
	}

	private void setFlag(String message) {
		switch (message) {
		case "Triangle":
			flag = 3;
		case "Square":
			flag = 4;
		case "Pentagon":
			flag = 5;
		case "Hexagon":
			flag = 6;
		default:
			flag = 0;
		}

	}

	public void parseReturnedJSONString(String json) {
		String shape = "";
		if (json.contains("TRIANGLE")) {
			shape = "Triangle";
			flag = 3;
		} else if (json.contains("SQUARE")) {
			shape = "Square";
			flag = 4;
		} else if (json.contains("PENTAGON")) {
			shape = "Pentagon";
			flag = 5;
		} else if (json.contains("HEXAGON")) {
			shape = "Hexagon";
			flag = 6;
		} else {
			flag = 0;
		}
		System.out.println("flag is " + flag);

		if (json.contains("BLUE")) {
			colour = new Scalar(255, 0, 0, 133);
		} else if (json.contains("GREEN")) {
			colour = new Scalar(0, 255, 0, 133);
		} else if (json.contains("RED") || json.contains("RIDE")) {
			colour = new Scalar(0, 0, 255, 133);
		} else {
			colour = new Scalar(255, 0, 255, 133);
		}

	}

	/**
	 * Get a frame from the opened video stream (if any)
	 * 
	 * @return the {@link Image} to show
	 */
	private Image grabFrame() {
		// init everything
		Image imageToShow = null;
		Mat frame = new Mat();
		// check if the capture is open
		if (this.capture.isOpened()) {
			try {
				// read the current frame
				this.capture.read(frame);

				// if the frame is not empty, process it
				if (!frame.empty()) {
					// convert the image to gray scale
					// Imgproc.cvtColor(frame, frame, Imgproc.COLOR_BGR2GRAY);
					// convert the Mat object (OpenCV) to Image (JavaFX)

					frame = ProcessImage(frame);
					imageToShow = mat2Image(frame);
				}

			} catch (Exception e) {
				// log the error
				System.err.println("Exception during the image elaboration: " + e);
			}
		}

		return imageToShow;
	}

	private Mat ProcessImage(Mat frame) {
		// this is where the magic stuff happens
		// all the 3d projections and detections happen in this function
		// init
		List<MatOfPoint> contours = new ArrayList<>();
		Point a = new Point(100, 50);
		Point b = new Point(700, 574);
		Scalar c = new Scalar(0, 60, 255);

		Mat manipulatedframe = frame.clone();
		Imgproc.cvtColor(frame, manipulatedframe, Imgproc.COLOR_BGR2GRAY);
		Imgproc.Canny(manipulatedframe, manipulatedframe, (double) 64, (double) 150);

		// approximate contours to polygons
		Imgproc.findContours(manipulatedframe, contours, new Mat(), Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_NONE);
		// iterate through each contour
		MatOfPoint2f approxCurve = new MatOfPoint2f();
		for (int i = 0; i < contours.size(); i++) {
			// Convert contours from MatOfPoint to MatOfPoint2f
			MatOfPoint2f contour2f = new MatOfPoint2f(contours.get(i).toArray());
			double approxDistance = Imgproc.arcLength(contour2f, true) * 0.02;
			if (approxDistance > 5) {
				// Find Polygons
				Imgproc.approxPolyDP(contour2f, approxCurve, approxDistance, true);

				// Convert back to MatOfPoint
				MatOfPoint points = new MatOfPoint(approxCurve.toArray());

				// Rectangle Checks - Points, area, convexity
				if (points.total() == flag && Math.abs(Imgproc.contourArea(points)) > 150
						&& Imgproc.isContourConvex(points)) {
					double cos = 0;
					double mcos = 0;

					for (int sc = 2; sc < 5; sc++) {

						// TODO Figure a way to check angle
						if (cos > mcos) {
							mcos = cos;
						}
					}
					if (mcos < 0.3) {

						// Get bounding rect of contour
						Rect rect = Imgproc.boundingRect(points);

						if (Math.abs(rect.height - rect.width) < 100) {
							// draw enclosing rectangle
							// TODO Change back to picori
							// Imgproc.rectangle(frame, rect.tl(), rect.br(),
							// new Scalar(255, 0, 0), 1, 8, 0);
							Imgproc.drawContours(frame, contours, i, colour, -1);

						}

					}
				}

			}
		}
		// Imgproc.drawContours(frame, contours, largestCounterindex, new
		// Scalar(173, 23, 32), 25);
		// System.out.println(largestContour.size());
		return frame;
	}

	/**
	 * Convert a Mat object (OpenCV) in the corresponding Image for JavaFX
	 * 
	 * @param frame
	 *            the {@link Mat} representing the current frame
	 * @return the {@link Image} to show
	 */
	private Image mat2Image(Mat frame) {
		// create a temporary buffer
		MatOfByte buffer = new MatOfByte();
		// encode the frame in the buffer
		Imgcodecs.imencode(".png", frame, buffer);
		// build and return an Image created from the image encoded in the
		// buffer
		return new Image(new ByteArrayInputStream(buffer.toArray()));
	}

}
