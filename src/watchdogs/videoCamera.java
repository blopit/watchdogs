package watchdogs;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.rmi.server.UID;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Future;

import javax.swing.JPanel;

import org.json.JSONException;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.objdetect.Objdetect;
import org.opencv.videoio.VideoCapture;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.async.Callback;
import com.mashape.unirest.http.exceptions.UnirestException;

public class videoCamera extends JPanel {

	VideoCapture camera;
	int idx = 0;
	static final int hbins = 30;
	Point mx = new Point(0, 0);
	int minTime = 5;
	boolean inited = false;
	int fire = 0;
	boolean debug = false;

	CascadeClassifier faceCascade = new CascadeClassifier(
			"res/haarcascades_cuda/haarcascade_frontalface_alt.xml");
	CascadeClassifier faceCascadeProfile = new CascadeClassifier(
			"res/haarcascades_cuda/haarcascade_profileface.xml");
	ArrayList<Face> faces = new ArrayList<Face>();
	char[] alphabet = "_ABCDEFGHIJKLMNOPQRSTUV1234567890".toCharArray();

	public videoCamera(VideoCapture cam) {

		camera = cam;
	}

	public static Color blend(Color c0, Color c1, double weight) {
		double weight0 = 1 - weight;
		double r = weight0 * c0.getRed() + weight * c1.getRed();
		double g = weight0 * c0.getGreen() + weight * c1.getGreen();
		double b = weight0 * c0.getBlue() + weight * c1.getBlue();
		double a = Math.min(c0.getAlpha(), c1.getAlpha());

		return new Color((int) r, (int) g, (int) b, (int) a);
	}

	public class Face {
		Rect _bounds;
		double _destBoundsX, _destBoundsY, _destBoundsW, _destBoundsH,
				_drawBoundsX, _drawBoundsY, _drawBoundsW, _drawBoundsH, li;
		UID _id;
		int _duration;
		int _life;
		boolean _sent;
		Color color;

		String name, drawName, occ, drawOcc;

		double rotate;
		boolean loading;

		public Face(Rect bounds) {
			_id = new UID();
			_bounds = bounds;
			_destBoundsW = _destBoundsH = _drawBoundsW = _drawBoundsH = 0;
			_destBoundsX = _destBoundsY = _drawBoundsX = _drawBoundsY = -100;
			_duration = 0;
			_sent = false;
			color = new Color((int) (Math.random() * 0x1000000));
			_life = 20;
			li = 0;

			name = null;
			drawName = "BcdZaXbabYe";
			occ = null;
			drawOcc = "17545792740";

			rotate = 0;
			loading = false;
		}

		public void keepAlive() {
			_life = 20;
			this._duration++;

			_destBoundsX = _bounds.x;
			_destBoundsY = _bounds.y;
			_destBoundsW = _bounds.width;
			_destBoundsH = _bounds.height;
		}

		public void update() {
			_life--;

			if (_duration > 10) {
				_drawBoundsX += -(_drawBoundsX - _destBoundsX) * 0.3;
				_drawBoundsY += -(_drawBoundsY - _destBoundsY) * 0.3;
				_drawBoundsW += -(_drawBoundsW - _destBoundsW) * 0.1;
				_drawBoundsH += -(_drawBoundsH - _destBoundsH) * 0.1;
			}

			if (loading) {
				rotate += 15;
				rotate = rotate % 360;
			} else {
				if (rotate > 0 && rotate < 360)
					rotate += 15;
				else
					rotate = 0;

			}

			li -= 0.05;
			if (li < 0) {
				li = 0;
			}

			if (name == null) {
				drawName = randofy(drawName, 6);
			} else {
				drawName = randoSet(drawName, name, 12, 3);
			}

			if (occ == null) {
				drawOcc = randofy(drawOcc, 6);
			} else {
				drawOcc = randoSet(drawOcc, occ, 12, 3);
			}

		}

	}

	public String randofy(String str, int entropy) {
		String s = str;
		for (int i = 0; i < entropy; i++) {
			Random random = new Random();
			int index = random.nextInt(s.length());
			StringBuilder myName = new StringBuilder(s);
			myName.setCharAt(index,
					alphabet[new Random().nextInt(alphabet.length)]);
			s = myName.toString();
		}
		return s;
	}

	public String randoSet(String str, String act, int entropy, int speed) {

		String s = str;
		if (s.length() != act.length()) {
			for (int i = 0; i < entropy; i++) {
				Random random = new Random();
				int index = random.nextInt(s.length());
				StringBuilder myName = new StringBuilder(s);
				myName.setCharAt(index,
						alphabet[new Random().nextInt(alphabet.length)]);
				s = myName.toString();
			}

			if (s.length() > act.length()) {
				if (s != null && s.length() > 0) {
					s = s.substring(0, s.length() - 1);
				}
				return s;
			} else {
				return s + alphabet[new Random().nextInt(alphabet.length)];
			}

		} else {
			// Random random = new Random();
			// int index = random.nextInt(s.length());
			for (int i = 0; i < speed; i++) {
				int index = 0;
				for (; index < Math.max(act.length(), s.length()) - 1; index++) {
					if (s.charAt(index) != act.charAt(index))
						break;
				}

				StringBuilder myName = new StringBuilder(s);
				myName.setCharAt(index, act.charAt(index));
				s = myName.toString();
			}
		}

		return s;
	}

	public double compareFace(Face f1, Rect r2) {
		Rect r1 = f1._bounds;
		// Rect r2 = f2._bounds;
		double x1 = r1.x + r1.width / 2;
		double y1 = r1.y + r1.height / 2;
		double x2 = r2.x + r2.width / 2;
		double y2 = r2.y + r2.height / 2;

		return Math.sqrt((x1 - x2) * (x1 - x2) + (y1 - y2) * (y1 - y2));
	}

	public Rect boundFace(Face f1, Rect r2) {
		Rect r1 = f1._bounds;
		// Rect r2 = f2._bounds;
		double x1 = r1.x + r1.width / 2;
		double y1 = r1.y + r1.height / 2;

		int nW = (r1.width + r2.width) / 2;
		int nH = (r1.height + r2.height) / 2;

		double x2 = r2.x + r2.width / 2;
		double y2 = r2.y + r2.height / 2;

		int xx = (int) (x2 - nW / 2);
		int yy = (int) (y2 - nH / 2);

		return new Rect(xx, yy, nW, nH);
	}

	public Face createFace(Rect bounds) {

		double min = -1;
		Face choose = null;
		for (Face f : faces) {
			double c = compareFace(f, bounds);
			if (c < 256 && (min == -1 || c < min)) {

				f._bounds = bounds; // boundFace(f, bounds);//bounds;

				min = c;
				choose = f;
			}
		}

		if (choose != null) {
			choose._bounds = bounds;
			choose.keepAlive();
			return choose;
		} else {
			Face nFace = new Face(bounds);
			faces.add(nFace);
			return nFace;
		}
	}

	public BufferedImage Mat2BufferedImage(Mat m) {

		int type = BufferedImage.TYPE_BYTE_GRAY;
		if (m.channels() > 1) {
			type = BufferedImage.TYPE_3BYTE_BGR;
		}
		int bufferSize = m.channels() * m.cols() * m.rows();
		byte[] b = new byte[bufferSize];
		m.get(0, 0, b);
		BufferedImage img = new BufferedImage(m.cols(), m.rows(), type);
		final byte[] targetPixels = ((DataBufferByte) img.getRaster()
				.getDataBuffer()).getData();
		System.arraycopy(b, 0, targetPixels, 0, b.length);
		return img;
	}

	public void drawDiamond(Graphics g, int _drawBoundsX, int _drawBoundsY,
			int d, int e) {
		if ((_drawBoundsX < d) && (_drawBoundsY > e)) {
			int x = _drawBoundsX + ((d - _drawBoundsX) / 2);
			int y = e + ((_drawBoundsY - e) / 2);
			g.drawLine(_drawBoundsX, _drawBoundsY, x, y);
			g.drawLine(x, y, _drawBoundsX, e);
			x = _drawBoundsX - ((d - _drawBoundsX) / 2);
			g.drawLine(_drawBoundsX, e, x, y);
			g.drawLine(x, y, _drawBoundsX, _drawBoundsY);
		}// end of if
		else if ((_drawBoundsX > d) && (_drawBoundsY < e)) {
			int x = d + ((_drawBoundsX - d) / 2);
			int y = _drawBoundsY + ((e - _drawBoundsY) / 2);
			g.drawLine(_drawBoundsX, _drawBoundsY, x, y);
			g.drawLine(x, y, _drawBoundsX, e);
			x = _drawBoundsX + ((_drawBoundsX - d) / 2);
			y = _drawBoundsY + ((e - _drawBoundsY) / 2);
			g.drawLine(_drawBoundsX, e, x, y);
			g.drawLine(x, y, _drawBoundsX, _drawBoundsY);
		}// end of else if
		else if ((_drawBoundsX > d) && (_drawBoundsY > e)) {
			int x = d + ((_drawBoundsX - d) / 2);
			int y = e + ((_drawBoundsY - e) / 2);
			g.drawLine(_drawBoundsX, _drawBoundsY, x, y);
			g.drawLine(x, y, _drawBoundsX, e);
			x = _drawBoundsX + ((_drawBoundsX - d) / 2);
			y = e + ((_drawBoundsY - e) / 2);
			g.drawLine(_drawBoundsX, e, x, y);
			g.drawLine(x, y, _drawBoundsX, _drawBoundsY);
		}// end of else if
		else if ((_drawBoundsX < d) && (_drawBoundsY < e)) {
			int x = _drawBoundsX + ((d - _drawBoundsX) / 2);
			int y = _drawBoundsY + ((e - _drawBoundsY) / 2);
			g.drawLine(_drawBoundsX, _drawBoundsY, x, y);
			g.drawLine(x, y, _drawBoundsX, e);
			x = _drawBoundsX - ((d - _drawBoundsX) / 2);
			y = _drawBoundsY + ((e - _drawBoundsY) / 2);
			g.drawLine(_drawBoundsX, e, x, y);
			g.drawLine(x, y, _drawBoundsX, _drawBoundsY);
		}// end of else if
	}// end of method drawDiamond()

	public void paintComponent(Graphics g) {
		fire--;

		super.paintComponent(g);
		Graphics2D g2 = (Graphics2D) g;

		Mat mat = new Mat();
		camera.read(mat);
		/*
		 * Core.flip(mat, mat, 1);
		 * 
		 * Mat source = mat.clone();
		 * 
		 * Mat hist = source.clone();
		 * 
		 * Mat l_src = source.clone(); Mat img = mat.clone();
		 */

		MatOfRect facs = new MatOfRect();
		MatOfRect facsProf = new MatOfRect();

		Mat grayFrame = new Mat();

		// convert the frame in gray scale
		Imgproc.cvtColor(mat, grayFrame, Imgproc.COLOR_BGR2GRAY);
		// equalize the frame histogram to improve the result
		Imgproc.equalizeHist(grayFrame, grayFrame);

		// compute minimum face size (20% of the frame height, in our case)
		double absoluteFaceSize = 0;
		if (absoluteFaceSize == 0) {
			int height = grayFrame.rows();
			if (Math.round(height * 0.2f) > 0) {
				absoluteFaceSize = Math.round(height * 0.2f);
			}
		}

		// detect faces
		faceCascade.detectMultiScale(grayFrame, facs, 1.1, 2,
				0 | Objdetect.CASCADE_SCALE_IMAGE, new Size(absoluteFaceSize,
						absoluteFaceSize), new Size());

		faceCascadeProfile.detectMultiScale(grayFrame, facsProf, 1.1, 2,
				0 | Objdetect.CASCADE_SCALE_IMAGE, new Size(absoluteFaceSize,
						absoluteFaceSize), new Size());

		BufferedImage image = Mat2BufferedImage(mat);
		g.drawImage(image, 10, 10, image.getWidth(), image.getHeight(), null);

		Rect[] facArray = facs.toArray();
		Rect[] facProfsArray = facsProf.toArray();

		Rect[] facesArray = concat(facProfsArray, facArray);

		for (int i = 0; i < facesArray.length; i++) {
			this.createFace(facesArray[i]);
		}

		for (Face f : faces) {
			g2.setColor(blend(f.color, Color.WHITE, f.li));
			g2.setStroke(new BasicStroke(4 + (int) (f.li * 4)));

			/*
			 * drawDiamond( g2, (int) (f._drawBoundsX + f._drawBoundsW / 2),
			 * (int) (f._drawBoundsY + f._drawBoundsH / 2 - f._drawBoundsH *
			 * 0.75), (int) (f._drawBoundsX + f._drawBoundsW * 2), (int)
			 * (f._drawBoundsY + f._drawBoundsH * 2 - f._drawBoundsH * 0.75));
			 */

			double cx = f._drawBoundsX + f._drawBoundsW / 2;
			double cy = f._drawBoundsY + f._drawBoundsH / 2;
			double ro = f._drawBoundsW * 0.75;

			for (int i = 0; i < 4; i++) {
				double rot = Math.toRadians(i * 90);
				double rot2 = Math.toRadians(i * 90 + 45);
				double rot3 = Math.toRadians(i * 90 - 45);
				int dd = -(int) (ro * 0.3 + 0.3 * f.li);

				g2.drawLine((int) (cx + ro * Math.cos(rot)), (int) (cy + ro
						* Math.sin(rot)), (int) (cx + ro * Math.cos(rot) + dd
						* Math.cos(rot2)), (int) (cy + ro * Math.sin(rot) + dd
						* Math.sin(rot2)));

				g2.drawLine((int) (cx + ro * Math.cos(rot)), (int) (cy + ro
						* Math.sin(rot)), (int) (cx + ro * Math.cos(rot) + dd
						* Math.cos(rot3)), (int) (cy + ro * Math.sin(rot) + dd
						* Math.sin(rot3)));

				rot = Math.toRadians(f.rotate + i * 90);
				rot2 = Math.toRadians(f.rotate + i * 90 + 45 + 90);
				rot3 = Math.toRadians(f.rotate + i * 90 - 45);
				double rot4 = Math.toRadians(f.rotate + i * 90 + 90);
				ro = f._drawBoundsW
						* (0.5 + 0.25 * Math.cos(Math.toRadians(f.rotate)));

				g2.drawLine(
						(int) (cx + ro * Math.cos(rot) + dd * Math.cos(rot3)),
						(int) (cy + ro * Math.sin(rot) + dd * Math.sin(rot3)),
						(int) (cx + ro * Math.cos(rot4) + dd * Math.cos(rot2)),
						(int) (cy + ro * Math.sin(rot4) + dd * Math.sin(rot2)));

				ro = f._drawBoundsW * 0.75;
			}

			g2.setColor(Color.WHITE);

			g2.drawString("Name: " + f.drawName, (int) (f._drawBoundsX),
					(int) (f._drawBoundsY + f._drawBoundsH));
			g2.drawString("Occupation: " + f.drawOcc, (int) (f._drawBoundsX),
					(int) (f._drawBoundsY + f._drawBoundsH) + 16);

			f.update();

			if (f._duration > 40 && !f._sent) {
				f._sent = true;
				f.li = 1.0;
				Rect r = f._bounds;
				r.x -= r.width * 0.25;
				r.y -= r.height * 0.25;
				r.width *= 1.5;
				r.height *= 1.5;

				if (r.x < 0)
					r.x = 0;
				if (r.y < 0)
					r.y = 0;
				if (r.width > mat.cols())
					r.width = mat.cols();
				if (r.height > mat.rows())
					r.height = mat.rows();

				Mat m = mat.submat(r);
				saveImage(m, f);
				// f.name = "Shrenil";

			}

		}
		g2.setColor(Color.BLACK);

		ArrayList<Face> copyP = new ArrayList<Face>(faces);
		for (Face f : copyP) {
			if (f._life <= 0) {
				faces.remove(f);
			}
		}

	}

	public void saveImage(Mat subimg, Face f) {
		idx++;
		f.loading = true;

		Imgcodecs.imwrite("filename" + idx + ".jpg", subimg);

		String key = "c2281afa671f40c1a58dd1a39aada04a";

		try {

			final InputStream stream = new FileInputStream(new File("filename"
					+ idx + ".jpg"));
			final byte[] bytes = new byte[stream.available()];
			stream.read(bytes);
			stream.close();

			Future<HttpResponse<JsonNode>> response = Unirest
					.post("https://api.projectoxford.ai/face/v1.0/detect?returnFaceId=true&returnFaceLandmarks=false")
					.header("Ocp-Apim-Subscription-Key", key)
					.header("Content-Type", "application/octet-stream")
					.body(bytes).asJsonAsync(new Callback<JsonNode>() {

						public void failed(UnirestException e) {
							System.out.println("The request has failed");
							f._duration = 30;
						}

						public void completed(HttpResponse<JsonNode> response) {
							int code = response.getStatus();
							Map<String, List<String>> headers = response
									.getHeaders();
							JsonNode body = response.getBody();
							InputStream rawBody = response.getRawBody();
							System.out.println(body.toString());
							String faceID = "";
							try {
								faceID = (body.getArray().getJSONObject(0)
										.getString("faceId"));
								System.out.println(faceID);
							} catch (JSONException e1) {
								// TODO Auto-generated catch block
								e1.printStackTrace();
								f._duration = 30;
							}

							Future<HttpResponse<JsonNode>> response2 = Unirest
									.post("https://api.projectoxford.ai/face/v1.0/identify")
									.header("Ocp-Apim-Subscription-Key", key)
									.header("Content-Type", "application/json")
									.body("{\"personGroupId\":\"hackgroup\", \"maxNumOfCandidatesReturned\":1, \"faceIds\":[\""
											+ faceID + "\"]}")
									.asJsonAsync(new Callback<JsonNode>() {

										public void failed(UnirestException e) {
											System.out
													.println("The request has failed");
											f._duration = 30;
										}

										public void completed(
												HttpResponse<JsonNode> response) {
											int code = response.getStatus();
											Map<String, List<String>> headers = response
													.getHeaders();
											JsonNode body = response.getBody();
											InputStream rawBody = response
													.getRawBody();
											System.out.println(body.toString());
											try {
												String personId = (body
														.getArray()
														.getJSONObject(0)
														.getJSONArray(
																"candidates")
														.getJSONObject(0)
														.getString("personId"));

												System.out.println(personId);

												Future<HttpResponse<JsonNode>> response3 = Unirest
														.get("https://api.projectoxford.ai/face/v1.0/persongroups/hackgroup/persons/"
																+ personId)
														.header("Ocp-Apim-Subscription-Key",
																key)
														.header("Content-Type",
																"application/json")
														.asJsonAsync(
																new Callback<JsonNode>() {

																	public void failed(
																			UnirestException e) {
																		System.out
																				.println("The request has failed");
																		f._duration = 30;
																	}

																	public void completed(
																			HttpResponse<JsonNode> response) {
																		int code = response
																				.getStatus();
																		Map<String, List<String>> headers = response
																				.getHeaders();
																		JsonNode body = response
																				.getBody();
																		InputStream rawBody = response
																				.getRawBody();
																		System.out
																				.println(body
																						.toString());

																		try {
																			f.name = body
																					.getObject()
																					.getString(
																							"name")
																					.toUpperCase();
																			f.occ = body
																					.getObject()
																					.getString(
																							"userData");
																			f.loading = false;
																		} catch (JSONException e) {
																			// TODO
																			// Auto-generated
																			// catch
																			// block
																			e.printStackTrace();
																			f._duration = 30;
																		}

																	}

																	public void cancelled() {
																		System.out
																				.println("The request has been cancelled");
																		f._duration = 30;
																	}
																});

											} catch (JSONException e) {
												// TODO Auto-generated catch
												// block
												e.printStackTrace();
												f._duration = 30;
											}

										}

										public void cancelled() {
											System.out
													.println("The request has been cancelled");
											f._duration = 30;
										}
									});

						}

						public void cancelled() {
							System.out
									.println("The request has been cancelled");
							f._duration = 30;
						}
					});
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			f._duration = 30;
		}

	}

	public Rect[] concat(Rect[] a, Rect[] b) {
		int aLen = a.length;
		int bLen = b.length;
		Rect[] c = new Rect[aLen + bLen];
		System.arraycopy(a, 0, c, 0, aLen);
		System.arraycopy(b, 0, c, aLen, bLen);
		return c;
	}

}
