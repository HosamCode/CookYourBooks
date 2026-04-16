package app.cookyourbooks.cli.mode;

import java.nio.file.Path;

import javafx.fxml.FXML;
import javafx.stage.FileChooser;

import app.cookyourbooks.gui.viewmodel.ImportViewModel;

public class ImportViewController {

  @SuppressWarnings("NullAway.Init")
  @FXML
  private ImportViewModel viewModel;

  @FXML
  private void onStartImport() {
    FileChooser fileChooser = new FileChooser();
    fileChooser
        .getExtensionFilters()
        .add(new FileChooser.ExtensionFilter("Image Files", "*.jpg", "*.png"));
    Path selectedFile = fileChooser.showOpenDialog(null).toPath();
    if (selectedFile != null) {
      viewModel.startImport(selectedFile);
    }
  }

  @FXML
  private void onCancelImport() {
    viewModel.cancelImport();
  }

  @FXML
  private void onAcceptImport() {
    viewModel.acceptImport();
  }

  @FXML
  private void onRejectImport() {
    viewModel.rejectImport();
  }

  @FXML
  private void onRetry() {
    viewModel.cancelImport();
  }
}
