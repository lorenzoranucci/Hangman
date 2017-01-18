package it.lorenzoranucci.hangman.workers;

import org.opencv.android.CameraBridgeViewBase;
import org.opencv.core.Mat;
import org.opencv.core.Rect;

import java.util.Map;

/**
 * Created by lorenzo on 18/01/17.
 */
public abstract class FaceMaskGenerator {

    /**
     * Detect the face's pixel and return a mat of them.
     * @param faceROI Face region of interest
     * @return Mask of point that belong to the foreground(face)
     */
    public abstract Mat getFaceMask(Mat currentFrame, Rect faceROI);
    public abstract Mat getNewBackground();
    public abstract void initBackground(CameraBridgeViewBase.CvCameraViewFrame currentFrame);
    public abstract void setParameters(Map<String, String> parameters);

}
