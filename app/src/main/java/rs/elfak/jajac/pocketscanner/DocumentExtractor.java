package rs.elfak.jajac.pocketscanner;

import android.graphics.Bitmap;
import android.graphics.Point;

import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;

public class DocumentExtractor {

    private Bitmap bitmap;
    private List<Point> corners;

    private Mat image;
    private Mat gray;
    private Mat imageRect;
    private Mat transformedRect;
    private Mat transform;

    public DocumentExtractor(Bitmap bitmap, List<Point> corners) {
        this.bitmap = bitmap;
        this.corners = corners;
    }

    public Bitmap extract() {
        Point topLeft = corners.get(0);
        Point topRight = corners.get(1);
        Point bottomRight = corners.get(2);
        Point bottomLeft = corners.get(3);

        int maxWidth = getBiggerDistance(topRight, topLeft, bottomRight, bottomLeft);
        int maxHeight = getBiggerDistance(topLeft, bottomLeft, topRight, bottomRight);

        List<Point> transformedCorners = getTransformedCorners(maxWidth, maxHeight);
        initMatrices(bitmap.getHeight(), bitmap.getWidth(), transformedCorners);
        transform = Imgproc.getPerspectiveTransform(imageRect, transformedRect);

        Utils.bitmapToMat(bitmap, image);
        Imgproc.warpPerspective(image, image, transform, new Size(maxWidth, maxHeight));
        Imgproc.cvtColor(image, gray, Imgproc.COLOR_RGBA2GRAY);
        Imgproc.threshold(gray, gray, 0, 255, Imgproc.THRESH_OTSU);

        bitmap = Bitmap.createBitmap(image.width(), image.height(), bitmap.getConfig());
        Utils.matToBitmap(gray, bitmap);

        releaseMatrices();
        transform.release();
        return bitmap;
    }

    private int getBiggerDistance(Point p11, Point p12, Point p21, Point p22) {
        return (int) Math.max(
                Math.sqrt(Math.pow(p11.x - p12.x, 2) + Math.pow(p11.y - p12.y, 2)),
                Math.sqrt(Math.pow(p21.x - p22.x, 2) + Math.pow(p21.y - p22.y, 2))
        );
    }

    private List<Point> getTransformedCorners(int maxWidth, int maxHeight) {
        List<Point> transformedCorners = new ArrayList<>();
        transformedCorners.add(new Point(0, 0));
        transformedCorners.add(new Point(maxWidth - 1, 0));
        transformedCorners.add(new Point(maxWidth - 1, maxHeight - 1));
        transformedCorners.add(new Point(0, maxHeight - 1));
        return transformedCorners;
    }

    private void initMatrices(int height, int width, List<Point> transformedCorners) {
        image = new Mat(height, width, CvType.CV_8UC4);
        gray = new Mat(height, width, CvType.CV_8UC1);
        imageRect = getMatOfPoints(corners);
        transformedRect = getMatOfPoints(transformedCorners);
    }

    private void releaseMatrices() {
        imageRect.release();
        transformedRect.release();
        image.release();
        gray.release();
    }

    private MatOfPoint2f getMatOfPoints(List<Point> points) {
        List<org.opencv.core.Point> opencvPoints = new ArrayList<>();
        for (Point point : points) {
            opencvPoints.add(new org.opencv.core.Point(point.x, point.y));
        }
        MatOfPoint2f mat = new MatOfPoint2f();
        mat.fromList(opencvPoints);
        return mat;
    }

}
