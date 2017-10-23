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

    private double mDeviation;
    private boolean mClosed;
    private int mBlur;

    public DocumentFinder(int blur, double deviation, boolean closed) {
        mDeviation = deviation;
        mClosed = closed;
        mBlur = blur;
    }

    public List<Point> findCorners(Bitmap bitmap) {
        List<Point> documentCorners = new ArrayList<>();
        documentCorners.add(new Point(0, 0));
        documentCorners.add(new Point(bitmap.getWidth(), 0));
        documentCorners.add(new Point(bitmap.getWidth(), bitmap.getHeight()));
        documentCorners.add(new Point(0, bitmap.getHeight()));
        Mat image = new Mat(bitmap.getHeight(), bitmap.getWidth(), CvType.CV_8UC4);
        Mat edges = new Mat(bitmap.getHeight(), bitmap.getWidth(), CvType.CV_8UC1);
        Mat hierarchy = new Mat();
        List<MatOfPoint> contourPoints = new ArrayList<>();
        MatOfPoint2f maxContour2f = new MatOfPoint2f();

        Utils.bitmapToMat(bitmap, image);

//        Photo.fastNlMeansDenoising(image, image);
        Imgproc.medianBlur(image, image, mBlur);
        Imgproc.Canny(image, edges, 75, 200);
        Imgproc.findContours(edges, contourPoints, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

        if (contourPoints.size() != 0) {
            // Sort in descending order of the contour area
            Collections.sort(contourPoints, (left, right) -> {
                double leftArea = Imgproc.contourArea(left);
                double rightArea = Imgproc.contourArea(right);
                return Double.compare(rightArea, leftArea);
            });

            contourPoints.get(0).convertTo(maxContour2f, CvType.CV_32F);

            double perimeter = Imgproc.arcLength(maxContour2f, mClosed);
            Imgproc.approxPolyDP(maxContour2f, maxContour2f, mDeviation * perimeter, mClosed);

            if (maxContour2f.size().height == 4) {
                documentCorners = getOrderedPoints(maxContour2f);
            }
        }

        image.release();
        edges.release();
        hierarchy.release();
        releaseContourList(contourPoints);
        maxContour2f.release();

        return documentCorners;
    }

    private ArrayList<Point> getOrderedPoints(MatOfPoint2f contourPoints) {
        List<org.opencv.core.Point> points = contourPoints.toList();
        ArrayList<Point> pts = new ArrayList<>();
        int i = 0;
        for (org.opencv.core.Point point : points) {
            pts.add(new Point((int) point.x, (int) point.y));
        }
        return pts;
    }

    private void releaseContourList(List<MatOfPoint> contours) {
        for (MatOfPoint contour : contours) {
            contour.release();
        }
        contours.clear();
    }

}
