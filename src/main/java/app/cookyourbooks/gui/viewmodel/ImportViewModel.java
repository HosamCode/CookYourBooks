package app.cookyourbooks.gui.viewmodel;

import java.nio.file.Path;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.ObservableList;

import app.cookyourbooks.model.Recipe;

public interface ImportViewModel {

  void startImport(Path imagePath);

  void cancelImport();

  void acceptImport();

  void rejectImport();

  void loadCollections();

  StringProperty stateProperty();

  DoubleProperty progressProperty();

  StringProperty statusMessageProperty();

  StringProperty errorMessageProperty();

  ObjectProperty<Recipe> importedRecipeProperty();

  ObservableList<String> availableCollectionsProperty();

  StringProperty selectedCollectionIdProperty();
}
