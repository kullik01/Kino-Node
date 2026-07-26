import re

with open('src/main/java/io/github/kullik01/kinonode/util/CustomAlert.java', 'r') as f:
    content = f.read()

# Replace showInformation signature
content = content.replace(
    'public static void showInformation(Window owner, String title, String header,\n      String content) {',
    '''public static void showInformation(Window owner, String title, String header, String content) {
    showInformation(owner, title, header, content, null);
  }
  public static void showInformation(Window owner, String title, String header, String content, String iconPath) {'''
)

# Replace showConfirmation signature
content = content.replace(
    'public static boolean showConfirmation(Window owner, String title,\n      String header, String content) {',
    '''public static boolean showConfirmation(Window owner, String title, String header, String content) {
    return showConfirmation(owner, title, header, content, null);
  }
  public static boolean showConfirmation(Window owner, String title, String header, String content, String iconPath) {'''
)

# Replace showError signature
content = content.replace(
    'public static void showError(Window owner, String title, String header,\n      String content) {',
    '''public static void showError(Window owner, String title, String header, String content) {
    showError(owner, title, header, content, null);
  }
  public static void showError(Window owner, String title, String header, String content, String iconPath) {'''
)

# Replace Label creation with HBox logic for icons
replacement = '''    Label tmpTextLabel = new Label(content);
    tmpTextLabel.setWrapText(true);
    tmpTextLabel.setStyle("-fx-text-fill: -color-fg-muted;");
    
    javafx.scene.Node contentNode = tmpTextLabel;
    if (iconPath != null && !iconPath.isEmpty()) {
      try {
        javafx.scene.image.ImageView tmpIconView = new javafx.scene.image.ImageView(
            new javafx.scene.image.Image(CustomAlert.class.getResourceAsStream(iconPath)));
        tmpIconView.setFitHeight(48);
        tmpIconView.setFitWidth(48);
        tmpIconView.setSmooth(true);
        HBox tmpIconBox = new HBox(15, tmpIconView, tmpTextLabel);
        tmpIconBox.setAlignment(Pos.CENTER_LEFT);
        contentNode = tmpIconBox;
      } catch (Exception ignored) {}
    }'''

content = content.replace(
    '''    Label tmpTextLabel = new Label(content);
    tmpTextLabel.setWrapText(true);
    tmpTextLabel.setStyle("-fx-text-fill: -color-fg-muted;");''',
    replacement
)

# Replace tmpContentBox.getChildren().addAll(tmpHeaderLabel, tmpTextLabel, tmpVSpacer,
content = content.replace(
    'tmpContentBox.getChildren().addAll(tmpHeaderLabel, tmpTextLabel, tmpVSpacer,',
    'tmpContentBox.getChildren().addAll(tmpHeaderLabel, contentNode, tmpVSpacer,'
)

with open('src/main/java/io/github/kullik01/kinonode/util/CustomAlert.java', 'w') as f:
    f.write(content)
