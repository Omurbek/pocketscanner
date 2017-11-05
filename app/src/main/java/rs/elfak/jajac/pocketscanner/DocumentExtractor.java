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

        int maxWidth = (int) Math.max(
                Math.sqrt(Math.pow(topRight.x - topLeft.x, 2) + Math.pow(topRight.y - topLeft.y, 2)),
                Math.sqrt(Math.pow(bottomRight.x - bottomLeft.x, 2) + Math.pow(bottomRight.y - bottomLeft.y, 2))
        );

        int maxHeight = (int) Math.max(
                Math.sqrt(Math.pow(topLeft.x - bottomLeft.x, 2) + Math.pow(topLeft.y - bottomLeft.y, 2)),
                Math.sqrt(Math.pow(topRight.x - bottomRight.x, 2) + Math.pow(topRight.y - bottomRight.y, 2))
        );

        List<Point> transformedCorners = new ArrayList<>();
        transformedCorners.add(new Point(0, 0));
        transformedCorners.add(new Point(maxWidth - 1, 0));
        transformedCorners.add(new Point(maxWidth - 1, maxHeight - 1));
        transformedCorners.add(new Point(0, maxHeight - 1));

        image = new Mat(bitmap.getHeight(), bitmap.getWidth(), CvType.CV_8UC4);
        gray = new Mat(bitmap.getHeight(), bitmap.getWidth(), CvType.CV_8UC1);
        imageRect = getMatOfPoints(corners);
        transformedRect = getMatOfPoints(transformedCorners);
        Utils.bitmapToMat(bitmap, image);

        transform = Imgproc.getPerspectiveTransform(imageRect, transformedRect);
        Imgproc.warpPerspective(image, image, transform, new Size(maxWidth, maxHeight));

        Imgproc.cvtColor(image, gray, Imgproc.COLOR_RGBA2GRAY);
        Imgproc.threshold(gray, gray, 0, 255, Imgproc.THRESH_OTSU);

        bitmap = Bitmap.createBitmap(image.width(), image.height(), bitmap.getConfig());

        Utils.matToBitmap(gray, bitmap);

        imageRect.release();
        transformedRect.release();
        transform.release();
        image.release();
        gray.release();

        return bitmap;
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
