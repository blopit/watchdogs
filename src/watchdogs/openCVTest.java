package watchdogs;

import java.io.IOException;

import javax.swing.JFrame;

import org.opencv.core.Core;
import org.opencv.videoio.VideoCapture;

public class openCVTest {

	public openCVTest() {
	}

	public static void main(String[] args) throws IOException {
		
		System.out.println(System.getProperty("java.library.path"));

		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
		VideoCapture camera = new VideoCapture(0);
		videoCamera cam = new videoCamera(camera);

		JFrame frame = new JFrame();
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.add(cam);
		frame.setSize(1200, 800);
		frame.setVisible(true);

		while (camera.isOpened()) {
			cam.repaint();

		}

	}

}