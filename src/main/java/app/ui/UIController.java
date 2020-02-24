package app.ui;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.IntBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.CompletableFuture;
import java.util.stream.IntStream;

import app.util.alg.SobelOperator;
import app.util.ui.FileIOHelper;
import app.util.ui.PreviewImage;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritablePixelFormat;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;

/**
 * Controller for the UI
 */
public class UIController implements Initializable
{
  @FXML
  private BorderPane mainWindow;

  @FXML
  private VBox edgeImageContainer;

  @FXML
  private VBox mainImageContainer;

  @FXML
  private ImageView mainImageView;

  @FXML
  private ImageView edgeImageView;

  @FXML
  private HBox imageBox;

  @FXML
  private Button startBtn;

  @FXML
  private Slider qualitySlider;
  @FXML
  private Label qualityValue;

  // loaded image
  private int[] grayPixelBuffer;
  private int width;
  private int height;

  // Progress indicator
  private BorderPane progress;

  @FXML
  public void open()
  {
    DirectoryChooser dirChooser = new DirectoryChooser();
    dirChooser.setTitle("Select Gallery");
    dirChooser.setInitialDirectory(Paths.get(".").toFile());
    File file = dirChooser.showDialog(mainWindow.getScene().getWindow());

    if (file != null && file.isDirectory())
    {
      // Delete old image content
      imageBox.getChildren().clear();

      long time = System.nanoTime();
      List<ImageView> imageViews = FileIOHelper.loadPreViewImages(file);
      System.out.println("Time to load preview images : " + (System.nanoTime() - time) / 1_000_000 + " [ms]");

      for (ImageView imageView : imageViews)
      {
        imageView.setOnMouseClicked(new EventHandler<MouseEvent>()
        {
          @Override
          public void handle(MouseEvent mouseEvent)
          {
            File file = ((PreviewImage) imageView.getImage()).getFile();

            BufferedImage imageOut = loadAndGetImage(file);
            if (imageOut != null)
            {
              mainImageView.setImage(SwingFXUtils.toFXImage(imageOut, null));
              mainImageView.setPreserveRatio(true);
              mainImageView.setSmooth(true);
              mainImageView.setCache(true);
              startBtn.setDisable(false);

              edgeImageContainer.getChildren().remove(edgeImageView);
            }
          }
        });
      }

      this.imageBox.getChildren().addAll(imageViews);
    }
  }

  @FXML
  public void applyEdgeDetection()
  {
    long start = System.currentTimeMillis();
    
    if (this.mainImageView.getImage() == null)
      return;
    
    edgeImageContainer.getChildren().remove(edgeImageView);
    this.showProgressIndicator();

    double threashold = Double.parseDouble(this.qualityValue.getText().replace(',', '.'));

    CompletableFuture.supplyAsync( () -> {
      int[] edgePixelBuffer = SobelOperator.getEdgeDetectedImage(this.grayPixelBuffer, this.width, this.height, threashold);

      BufferedImage image = new BufferedImage(this.width, this.height, BufferedImage.TYPE_INT_RGB); // Create edged image
      image.setRGB(0, 0, this.width, this.height, edgePixelBuffer, 0, this.width);
      return image;
    } ).thenAcceptAsync( image -> {
      edgeImageView.setImage(SwingFXUtils.toFXImage(image, null));
      edgeImageView.setPreserveRatio(true);
      edgeImageView.setSmooth(true);
      edgeImageView.setCache(true);

      this.closeProgressIndicator();
      
      System.out.println("Processing image " + (System.currentTimeMillis() - start) + " [ms] (" + (image.getHeight()*image.getWidth()) + " pixels)" );
    }, Platform::runLater );
  }

  @FXML
  public void exit()
  {
    Platform.exit();
  }

  // --- Application initialisation ---
  @Override
  public void initialize(URL location, ResourceBundle resources)
  {

    this.startBtn.setDisable(true);

    this.qualityValue.textProperty().bind(qualitySlider.valueProperty().asString("%6.2f"));

    try
    {
      progress = FXMLLoader.load(getClass().getResource("progress.fxml"));
      progress.getStylesheets().add(getClass().getResource("ui.css").toExternalForm());
    }
    catch (IOException e)
    {
      e.printStackTrace();
    }
  }

  // -----------------------------------------------
  // --------- progress indicator handling ---------
  // -----------------------------------------------

  private void showProgressIndicator()
  {
    this.startBtn.setDisable(true);

    this.edgeImageContainer.getChildren().remove(edgeImageView);
    this.edgeImageContainer.getChildren().add(this.progress);
  }

  private Void closeProgressIndicator()
  {

    this.startBtn.setDisable(false);
    this.edgeImageContainer.getChildren().add(edgeImageView);
    this.edgeImageContainer.getChildren().remove(this.progress);

    return (Void) null;
  }

  // -----------------------------------------------
  // --------- some tools --------------------------
  // -----------------------------------------------

  private BufferedImage loadAndGetImage(File file)
  {
    try
    {
      byte[] bytes = Files.readAllBytes(Paths.get(file.getAbsolutePath()));
      InputStream iStream = new ByteArrayInputStream(bytes);
      Image image = new Image(iStream);

      PixelReader pReader = image.getPixelReader();
      this.width = (int) image.getWidth();
      this.height = (int) image.getHeight();
      int[] pixelBuffer = new int[width * height];
      WritablePixelFormat<IntBuffer> format = WritablePixelFormat.getIntArgbInstance();
      pReader.getPixels(0, 0, this.width, this.height, format, pixelBuffer, 0, this.width);
      this.grayPixelBuffer = createNewGrayScaleBuffer(pixelBuffer);

      BufferedImage imageOut = new BufferedImage(this.width, this.height, BufferedImage.TYPE_INT_RGB); // Create new image

      imageOut.setRGB(0, 0, this.width, this.height, grayPixelBuffer, 0, this.width);
      return imageOut;
    }
    catch (IOException exce)
    {
      exce.printStackTrace();
      return null;
    }
  }

  // Creates a gray scale buffer from an color buffer
  private int[] createNewGrayScaleBuffer(int[] buffer)
  {
    int[] pixelBuffer = new int[buffer.length];
    IntStream.range(0, pixelBuffer.length).parallel().forEach(i -> {
      int rgb = buffer[i];
      int red = (rgb >> 16) & 0xff;
      int green = (rgb >> 8) & 0xff;
      int blue = (rgb >> 0) & 0xff;

      int gray = (int) (red + green + blue) / 3;

      pixelBuffer[i] = (gray << 16) + (gray << 8) + gray;
    });

    return pixelBuffer;
  }

}
