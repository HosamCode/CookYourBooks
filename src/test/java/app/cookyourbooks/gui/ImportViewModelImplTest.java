package app.cookyourbooks.gui;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.nio.file.Path;

import javafx.application.Platform;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import app.cookyourbooks.gui.viewmodel.ImportViewModelImpl;
import app.cookyourbooks.services.LibrarianService;
import app.cookyourbooks.services.ocr.FakeRecipeOcrService;

@SuppressWarnings("NullAway")
class ImportViewModelImplTest extends ViewModelTestBase {

  private ImportViewModelImpl viewModel;
  private FakeRecipeOcrService ocrService;
  private LibrarianService librarianService;

  @BeforeEach
  void setUp() throws InterruptedException {
    ocrService = new FakeRecipeOcrService(500);
    librarianService = mock(LibrarianService.class);
    Platform.runLater(() -> viewModel = new ImportViewModelImpl(ocrService, librarianService));
    waitForFxEvents();
  }

  @Test
  void startImport_transitionsToProcessing() throws InterruptedException {
    Platform.runLater(() -> viewModel.startImport(Path.of("test.jpg")));
    waitForFxEvents();
    assertThat(viewModel.stateProperty().get()).isEqualTo("processing");
  }

  @Test
  void acceptImport_savesRecipeAndTransitionsToIdle() throws InterruptedException {
    Platform.runLater(() -> viewModel.startImport(Path.of("test.jpg")));
    Thread.sleep(700); // wait for FakeRecipeOcrService (500ms delay)
    waitForFxEvents();
    Platform.runLater(() -> viewModel.acceptImport());
    waitForFxEvents();
    assertThat(viewModel.stateProperty().get()).isEqualTo("idle");
  }
}
