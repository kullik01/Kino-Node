package io.github.kullik01.kinonode.util;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;

public class CustomAlert {
    
    public static void showInformation(Window owner, String title, String header, String content) {
        Stage stage = new Stage();
        if (owner instanceof Stage ownerStage) {
            stage.initOwner(ownerStage);
            stage.getIcons().addAll(ownerStage.getIcons());
        }
        stage.initModality(Modality.WINDOW_MODAL);
        stage.initStyle(StageStyle.UNDECORATED);
        
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: -color-bg-default; -fx-border-color: -color-border-default; -fx-border-width: 1px; -fx-border-radius: 8px; -fx-background-radius: 8px;");
        root.getStylesheets().add(CustomAlert.class.getResource("/io/github/kullik01/kinonode/css/theme.css").toExternalForm());
        
        // Title Bar
        HBox titleBar = new HBox();
        titleBar.getStyleClass().add("title-bar");
        titleBar.setAlignment(Pos.CENTER_LEFT);
        
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("title-bar-label");
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        Button closeBtn = new Button("✕");
        closeBtn.getStyleClass().addAll("window-button", "close-button");
        closeBtn.setOnAction(e -> stage.close());
        
        titleBar.getChildren().addAll(titleLabel, spacer, closeBtn);
        
        // Content
        VBox contentBox = new VBox(15);
        contentBox.setPadding(new Insets(20));
        contentBox.setStyle("-fx-background-radius: 0 0 8px 8px;");
        
        Label headerLabel = new Label(header);
        headerLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: -color-fg-default;");
        
        Label textLabel = new Label(content);
        textLabel.setWrapText(true);
        textLabel.setStyle("-fx-text-fill: -color-fg-muted;");
        
        Button okBtn = new Button("OK");
        okBtn.getStyleClass().add("button-primary");
        okBtn.setOnAction(e -> stage.close());
        HBox btnBox = new HBox(okBtn);
        btnBox.setAlignment(Pos.CENTER_RIGHT);
        
        contentBox.getChildren().addAll(headerLabel, textLabel, btnBox);
        
        root.setTop(titleBar);
        root.setCenter(contentBox);
        
        // Drag logic
        final double[] xOffset = {0};
        final double[] yOffset = {0};
        titleBar.setOnMousePressed(event -> {
            xOffset[0] = stage.getX() - event.getScreenX();
            yOffset[0] = stage.getY() - event.getScreenY();
        });
        titleBar.setOnMouseDragged(event -> {
            stage.setX(event.getScreenX() + xOffset[0]);
            stage.setY(event.getScreenY() + yOffset[0]);
        });
        
        StackPane wrapper = new StackPane(root);
        wrapper.setStyle("-fx-background-color: transparent; -fx-padding: 0px;");
        
        Scene scene = new Scene(wrapper);
        scene.setFill(Color.TRANSPARENT);
        stage.setScene(scene);
        
        stage.setWidth(450);
        stage.setHeight(250);
        
        stage.showAndWait();
    }
}
