package app.util.ui;

import static java.util.stream.Collectors.toList;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ForkJoinPool;

import javafx.scene.image.ImageView;

/**
 * Helper class for parallel IO
 * 
 * Reads the jpg and png files of a directory and produced a List of ImageView-Objects.
 */
public final class FileIOHelper
{ 
  private FileIOHelper()
  {
  }

  // This method should not run very often. So its okay to create each time a 
  // thread pool for loading the images. Don't forget to shutdown the executor!
  public static List<ImageView> loadPreViewImages(File imageFolder)
  {
    File[] listOfFiles = imageFolder.listFiles((File dir, String name) -> name.endsWith(".png") || name.endsWith(".jpg"));
    if( listOfFiles.length == 0 )
      return Collections.emptyList();
    
    int maxThreads = 40; // bound to 40 threads
    ForkJoinPool pool = new ForkJoinPool( Math.min(listOfFiles.length, maxThreads) );

    // Asynchrone parallel IO
    var task = pool.submit(
      () ->  Arrays.stream(listOfFiles).parallel()
                        .map( FileIOHelper::createPreviewImage )
                        .map( FileIOHelper::createImageView )
                        .collect( toList() )   
      );

         
    return task.join(); 
  }

  private static PreviewImage createPreviewImage(File imageFile)
  {
    try
    {
      byte[] bytes = Files.readAllBytes(Paths.get(imageFile.getAbsolutePath()));
      InputStream iStream = new ByteArrayInputStream(bytes);

      return new PreviewImage(iStream, imageFile);
    }
    catch (IOException exce)
    {
      exce.printStackTrace();
      throw new RuntimeException(exce);
    }
  }

  private static ImageView createImageView(PreviewImage image)
  {
    ImageView imageView = new ImageView(image);
    imageView.setFitHeight(80);
    imageView.setPreserveRatio(true);
    imageView.setSmooth(true);
    imageView.setCache(true);
    return imageView;
  }
}
