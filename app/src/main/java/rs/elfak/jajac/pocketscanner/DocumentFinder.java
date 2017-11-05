package rs.elfak.jajac.pocketscanner;

import android.graphics.Bitmap;
import android.graphics.Point;

import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DocumentFinder {

    private static final String TAG = "DocumentFinder";

    private double deviation;
    private boolean closed;
    private int blurSize;
    private double cannyLow;
    private double cannyHigh;
    private int sobelSize;

    private Mat image;
    private Mat blurred;
    private Mat gray;
    private Mat edges;
    private Mat hierarchy;
    private List<MatOfPoint> contourPoints = new ArrayList<>();
    private MatOfPoint2f maxContour2f = new MatOfPoint2f();

    public DocumentFinder(int blurSize, double deviation, boolean closed,
                          double cannyLow, double cannyHigh, int sobelSize) {
        this.blurSize = blurSize;
        this.deviation = deviation;
        this.closed = closed;
        this.cannyLow = cannyLow;
        this.cannyHigh = cannyHigh;
        this.sobelSize = sobelSize;
    }

    public List<Point> findCorners(Bitmap bitmap) {
        // Initially set corners to image corners themselves
        List<Point> documentCorners = getDefaultCorners(bitmap.getHeight(), bitmap.getWidth());

        initMatrices(bitmap.getHeight(), bitmap.getWidth());
        Utils.bitmapToMat(bitmap, image);

        Imgproc.medianBlur(image, blurred, blurSize);
        Imgproc.cvtColor(blurred, gray, Imgproc.COLOR_RGBA2GRAY);
        Imgproc.Canny(gray, edges, cannyLow, cannyHigh, sobelSize, false);
        Imgproc.findContours(edges, contourPoints, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

        if (contourPoints.size() != 0) {
            // Sort in descending order of the contour area
            Collections.sort(contourPoints, (left, right) -> {
                double leftArea = Imgproc.contourArea(left);
                double rightArea = Imgproc.contourArea(right);
                return Double.compare(rightArea, leftArea);
            });

            contourPoints.get(0).convertTo(maxContour2f, CvType.CV_32F);
            double perimeter = Imgproc.arcLength(maxContour2f, closed);
            Imgproc.approxPolyDP(maxContour2f, maxContour2f, deviation * perimeter, closed);

            if (maxContour2f.size().height == 4) {
                documentCorners = getListOfPoints(maxContour2f);
            }
        }

        releaseMatrices();
        return documentCorners;
    }

    private List<Point> getDefaultCorners(int height, int width) {
        List<Point> defaultCorners = new ArrayList<>();
        defaultCorners.add(new Point(0, 0));
        defaultCorners.add(new Point(width, 0));
        defaultCorners.add(new Point(width, height));
        defaultCorners.add(new Point(0, height));
        return defaultCorners;
    }

    private void initMatrices(int height, int width) {
        image = new Mat(height, width, CvType.CV_8UC4);
        blurred = new Mat(height, width, CvType.CV_8UC4);
        gray = new Mat(height, width, CvType.CV_8UC1);
        edges = new Mat(height, width, CvType.CV_8UC1);
        hierarchy = new Mat();
    }

    private void releaseMatrices() {
        image.release();
        blurred.release();
        gray.release();
        edges.release();
        hierarchy.release();
        releaseContourList(contourPoints);
        maxContour2f.release();
    }

    private void releaseContourList(List<MatOfPoint> contours) {
        contours.forEach(MatOfPoint::release);
        contours.clear();
    }

    private ArrayList<Point> getListOfPoints(MatOfPoint2f contourPoints) {
        List<org.opencv.core.Point> points = contourPoints.toList();
        ArrayList<Point> pts = new ArrayList<>();
        for (org.opencv.core.Point point : points) {
            pts.add(new Point((int) point.x, (int) point.y));
        }
        return pts;
    }

}
