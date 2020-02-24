package app.util.alg;



public class SobelOperator
{
  public static int[] getEdgeDetectedImage(int[] grayPixelBuffer, int width, int height, double threashold)
  {
    int[] edgePixelBuffer = new int[grayPixelBuffer.length];
    
    // Insert the implementation of the Sobel operator here
    // --------------------------------------------------------
    // Here we only copy the picture (buffer values)
    //
    //
    System.arraycopy(grayPixelBuffer, 0, edgePixelBuffer, 0, grayPixelBuffer.length);
    //
    //
    
    return edgePixelBuffer;
  }
}
