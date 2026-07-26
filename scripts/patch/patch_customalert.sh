#!/bin/bash
sed -i 's/tmpLogoView.setFitHeight(16);/tmpLogoView.setFitHeight(24); tmpLogoView.setSmooth(true);/' src/main/java/io/github/kullik01/kinonode/util/CustomAlert.java
sed -i 's/tmpLogoView.setFitWidth(16);/tmpLogoView.setFitWidth(24); javafx.scene.layout.HBox.setMargin(tmpLogoView, new javafx.geometry.Insets(0, 0, 0, -5));/' src/main/java/io/github/kullik01/kinonode/util/CustomAlert.java
sed -i 's/tmpStage.setHeight(250);/tmpStage.setMinHeight(250); tmpStage.sizeToScene();/' src/main/java/io/github/kullik01/kinonode/util/CustomAlert.java
