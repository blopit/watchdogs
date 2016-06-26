package watchdogs;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.rmi.server.UID;
import java.time.Duration;
import java.util.ArrayList;
import java.util.UUID;

import javax.swing.JPanel;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.videoio.VideoCapture;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.objdetect.Objdetect;

public class videoCamera extends JPanel {

	VideoCapture camera;
	int idx = 0;
	static final int hbins = 30;
	Point mx = new Point(0, 0);
	int minTime = 5;
	boolean inited = false;
	int fire = 0;
	boolean debug = false;
	CascadeClassifier faceCascade = new CascadeClassifier("res/haarcascades_cuda/haarcascade_frontalface_alt.xml");
	CascadeClassifier faceCascadeProfile = new CascadeClassifier("res/haarcascades_cuda/haarcascade_profileface.xml");
	ArrayList<Face> faces = new ArrayList<Face>();

	public videoCamera(VideoCapture cam) {

		camera = cam;
	}
	
	public class Face {
		Rect _bounds;
		double _destBoundsX, _destBoundsY, _destBoundsW, _destBoundsH, 
		_drawBoundsX, _drawBoundsY, _drawBoundsW, _drawBoundsH;
		UID _id;
		int _duration;
		int _life;
		boolean _sent;
		Color color;
		
		public Face(Rect bounds) {
			_id = new UID();
			_bounds = bounds;
			_destBoundsX= _destBoundsY= _destBoundsW= _destBoundsH= 
			_drawBoundsX= _drawBoundsY= _drawBoundsW= _drawBoundsH=0;
			_duration = 0;
			_sent = false;
			color = new Color((int)(Math.random() * 0x1000000));
			_life = 20;
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
			
			if (_duration > 10){
				_drawBoundsX += -(_drawBoundsX - _destBoundsX) * 0.3;
				_drawBoundsY += -(_drawBoundsY - _destBoundsY) * 0.3;
				_drawBoundsW += -(_drawBoundsW - _destBoundsW) * 0.1;
				_drawBoundsH += -(_drawBoundsH - _destBoundsH) * 0.1;
			}
		}
		
	}
	
	public double compareFace(Face f1, Rect r2) {
		Rect r1 = f1._bounds;
		//Rect r2 = f2._bounds;
		double x1 = r1.x + r1.width/2;
		double y1 = r1.y + r1.height/2;
		double x2 = r2.x + r2.width/2;
		double y2 = r2.y + r2.height/2;
		
		return Math.sqrt((x1-x2)*(x1-x2) + (y1-y2)*(y1-y2));
	}
	
	public Rect boundFace(Face f1, Rect r2) {
		Rect r1 = f1._bounds;
		//Rect r2 = f2._bounds;
		double x1 = r1.x + r1.width/2;
		double y1 = r1.y + r1.height/2;
		
		int nW =  (r1.width + r2.width) / 2;
		int nH =  (r1.height + r2.height) / 2;
		
		double x2 = r2.x + r2.width/2;
		double y2 = r2.y + r2.height/2;
		
		int xx = (int)(x2 - nW/2);
		int yy = (int)(y2 - nH/2);
		
		return new Rect(xx, yy, nW, nH);
	}
	
	public Face createFace(Rect bounds) {
		
		double min = -1;
		Face choose = null;
		for (Face f : faces) {
			double c = compareFace(f, bounds);
			if ( c < 256 && (min == -1 || c < min) ) {
				
				f._bounds =  bounds; //boundFace(f, bounds);//bounds;
				
				min = c;
				choose = f;
			}
		}
		
		if (choose != null) {
			choose._bounds = bounds;
			choose.keepAlive();
			return choose;
		} else {
			Face nFace =  new Face(bounds);
			faces.add(nFace);
			return nFace;
		}
	}

	public double polygonArea(ArrayList<Point> pnts) {
		double area = 0;
		int j = pnts.size() - 1;

		for (int i = 0; i < pnts.size(); i++) {
			area = area + (pnts.get(j).x + pnts.get(i).x)
					* (pnts.get(j).y - pnts.get(i).y);
			j = i;
		}
		return area / 2;
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

	public double pDistance(double x, double y, double x1, double y1,
			double x2, double y2) {

		double A = x - x1;
		double B = y - y1;
		double C = x2 - x1;
		double D = y2 - y1;

		double dot = A * C + B * D;
		double len_sq = C * C + D * D;
		double param = -1;
		if (len_sq != 0)
			param = dot / len_sq;

		double xx, yy;

		if (param < 0) {
			xx = x1;
			yy = y1;
		} else if (param > 1) {
			xx = x2;
			yy = y2;
		} else {
			xx = x1 + param * C;
			yy = y1 + param * D;
		}

		double dx = x - xx;
		double dy = y - yy;
		return Math.sqrt(dx * dx + dy * dy);
	}

	public double pointToLineDistance(Point v, Point w, Point p) {
		return pDistance(p.x, p.y, v.x, v.y, w.x, w.y);
	}

	public void paintComponent(Graphics g) {
		fire--;

		super.paintComponent(g);
		Graphics2D g2 = (Graphics2D) g;

		Mat mat = new Mat();
		camera.read(mat);
		/*Core.flip(mat, mat, 1);

		Mat source = mat.clone();

		Mat hist = source.clone();

		Mat l_src = source.clone();
		Mat img = mat.clone();*/

		//CascadeClassifier faceCascade = new CascadeClassifier();
		
		/*
		 * CascadeClassifier faceDetector = new
		 * CascadeClassifier(videoCamera.class
		 * .getResource("haarcascade_frontalface_alt.xml").getPath());
		 * 
		 * MatOfRect faceDetections = new MatOfRect();
		 * faceDetector.detectMultiScale(img, faceDetections);
		 * 
		 * System.out.println(String.format("Detected %s faces",
		 * faceDetections.toArray().length));
		 * 
		 * for (Rect rect : faceDetections.toArray()) { Core.rectangle(img, new
		 * Point(rect.x, rect.y), new Point(rect.x + rect.width, rect.y +
		 * rect.height), new Scalar(0, 255, 0)); }
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
		
		System.out.println(facesArray.length);
		
		for (int i = 0; i < facesArray.length; i++) {
			this.createFace(facesArray[i]);
		}
		
		for (Face f : faces){
			g2.setColor(f.color);
			g2.drawRect((int) (f._drawBoundsX),
					(int) (f._drawBoundsY),
					(int) (f._drawBoundsW),
					(int) (f._drawBoundsH));
			g2.setColor(Color.WHITE);
			g2.drawString(new Integer(f._duration).toString(), (int)(f._drawBoundsX), (int)(f._drawBoundsY + f._drawBoundsH));
			f.update();
			
			if (f._duration > 40 && !f._sent){
				f._sent = true;
				Rect r = f._bounds;
				r.x -= r.width * 0.25;
				r.y -= r.height * 0.25;
				r.width *= 1.5;
				r.height *= 1.5;
				
				Mat m = mat.submat(r);
				saveImage(m);
			}
			
		}
		g2.setColor(Color.BLACK);
		
		ArrayList<Face> copyP = new ArrayList<Face>(faces);
		for (Face f : copyP){
			if (f._life <= 0){
				faces.remove(f);
			}
		}

	}

	public void saveImage(Mat subimg){
		Imgcodecs.imwrite("filename" + idx + ".png",subimg);
		idx++;
	}
	
	public Rect[] concat(Rect[] a, Rect[] b) {
		   int aLen = a.length;
		   int bLen = b.length;
		   Rect[] c= new Rect[aLen+bLen];
		   System.arraycopy(a, 0, c, 0, aLen);
		   System.arraycopy(b, 0, c, aLen, bLen);
		   return c;
		}
	
	public boolean compareMatOfPoint(MatOfPoint2f point, MatOfPoint2f approx) {
		MatOfPoint2f c1f = new MatOfPoint2f(point.toArray());
		MatOfPoint2f c2f = new MatOfPoint2f(approx.toArray());

		Point p1A = new Point(c1f.get(0, 0)[0], c1f.get(0, 0)[1]);
		Point p1B = new Point(c1f.get(1, 0)[0], c1f.get(1, 0)[1]);
		Point p2A = new Point(c2f.get(0, 0)[0], c2f.get(0, 0)[1]);
		Point p2B = new Point(c2f.get(1, 0)[0], c2f.get(1, 0)[1]);

		if (distance(p1A, p2A) < 4 && distance(p1B, p2B) < 4) {
			return true;
		} else if (distance(p1A, p2B) < 4 && distance(p1B, p2A) < 4) {
			return true;
		}
		return false;
	}

	public double distance(Point p1, Point p2) {
		return Math.sqrt(Math.pow(p1.x - p2.x, 2) + Math.pow(p1.y - p2.y, 2));
	}

	public double angleBetween(Point center, Point current, Point previous) {

		return Math.toDegrees(Math.atan2(current.x - center.x, current.y
				- center.y)
				- Math.atan2(previous.x - center.x, previous.y - center.y));
	}

	public static boolean contains(ArrayList<Point> points, Point test) {
		int i;
		int j;
		boolean result = false;
		for (i = 0, j = points.size() - 1; i < points.size(); j = i++) {
			if ((points.get(i).y > test.y) != (points.get(j).y > test.y)
					&& (test.x < (points.get(j).x - points.get(i).x)
							* (test.y - points.get(i).y)
							/ (points.get(j).y - points.get(i).y)
							+ points.get(i).x)) {
				result = !result;
			}
		}
		return result;
	}

	public double percentageOfProjection(Point startPoint, Point endPoint,
			Point testPoint) {

		double scalar = ((endPoint.x - startPoint.x)
				* (testPoint.x - startPoint.x) + (endPoint.y - startPoint.y)
				* (testPoint.y - startPoint.y))
				/ (Math.pow((endPoint.x - startPoint.x), 2) + Math.pow(
						(endPoint.y - startPoint.y), 2));
		int ProjectionX = (int) (startPoint.x + ((endPoint.x - startPoint.x) * scalar));
		int ProjectionY = (int) (startPoint.y + ((endPoint.y - startPoint.y) * scalar));
		Point newProjectionPoint = new Point(ProjectionX, ProjectionY);
		double linefromStarttoEnd = distance(startPoint, endPoint);
		double linefromProjectiontoEnd = distance(startPoint,
				newProjectionPoint);
		return (double) (linefromProjectiontoEnd / linefromStarttoEnd);
	}

	public Mat turnGray(Mat img)

	{
		Mat mat1 = new Mat();
		Imgproc.cvtColor(img, mat1, Imgproc.COLOR_RGB2GRAY);
		return mat1;
	}

	public Mat threash(Mat img) {
		Mat threshed = new Mat();
		int SENSITIVITY_VALUE = 100;
		Imgproc.threshold(img, threshed, SENSITIVITY_VALUE, 255,
				Imgproc.THRESH_BINARY);
		return threshed;
	}
}
