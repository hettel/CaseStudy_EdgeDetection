package app;

import javafx.application.Application;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.fxml.FXMLLoader;

/**
 * 
 * This Java program is an example for using parallelization to enhance the performance. 
 * As show case the calculation of edges with the Sobel operator is used.
 *
 */
public class EdgeDetection extends Application
{
  @Override
  public void start(Stage primaryStage)
  {
    long time = System.nanoTime();
    try
    {
      BorderPane root = (BorderPane) FXMLLoader.load(getClass().getResource("ui/ui.fxml"));
      Scene scene = new Scene(root);
      scene.getStylesheets().add(getClass().getResource("ui/ui.css").toExternalForm());
      primaryStage.setTitle("Edge Detecting");
      primaryStage.setScene(scene);
      primaryStage.show();
    }
    catch (Exception e)
    {
      e.printStackTrace();
    }
    System.out.println("Application startup time : " + (System.nanoTime() - time)/1_000_000 + " [ms]");
  }

  public static void main(String[] args)
  {
    launch(args);
    
    System.out.println("done");
  }
}
