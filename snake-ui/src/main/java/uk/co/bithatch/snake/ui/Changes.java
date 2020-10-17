package uk.co.bithatch.snake.ui;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.ResourceBundle;
import java.util.Set;

import org.commonmark.node.IndentedCodeBlock;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.NodeRenderer;
import org.commonmark.renderer.html.HtmlNodeRendererContext;
import org.commonmark.renderer.html.HtmlRenderer;
import org.commonmark.renderer.html.HtmlRenderer.Builder;
import org.commonmark.renderer.html.HtmlWriter;

import javafx.animation.FadeTransition;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.web.WebView;
import javafx.util.Duration;

public class Changes extends AbstractDeviceController {
	final static ResourceBundle bundle = ResourceBundle.getBundle(Changes.class.getName());

	@FXML
	private WebView changes;
	@FXML
	private Label updatedIcon;
	@FXML
	private Label updated;
	@FXML
	private HBox updatesContainer;

	private String html;

	@Override
	protected void onConfigure() throws Exception {
		if (html == null) {
			Parser parser = Parser.builder().build();
			File changesFile;
			File dir = new File(System.getProperty("user.dir"));
			File pom = new File(dir, "pom.xml");
			if (pom.exists()) {
				changesFile = new File(dir.getParentFile(), "CHANGES.md");
			} else {
				changesFile = new File(dir, "docs" + File.separator + "CHANGES.md");
			}
			if (changesFile.exists()) {
				Node document = parser.parse(Files.readString(changesFile.toPath(), StandardCharsets.UTF_8));
				Builder builder = HtmlRenderer.builder();
				HtmlRenderer renderer = builder.build();
				html = "<html>";
				html += "<head>";
				html += "<link rel=\"stylesheet\" href=\"" + Changes.class.getResource("Changes.html.css") + "\">";
				html += "</head>";
				html += "<body>";
				html += renderer.render(document);
				html += "</body></html>";
			} else {
				html = "<html><body>The CHANGES file could not be found.</body></html>";
			}
		}

		try {
			changes.getEngine().loadContent(html);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

		if (PlatformService.get().isUpdated()) {
			FadeTransition anim = new FadeTransition(Duration.seconds(5));
			anim.setAutoReverse(true);
			anim.setCycleCount(FadeTransition.INDEFINITE);
			anim.setNode(updatesContainer);
			anim.setFromValue(0.5);
			anim.setToValue(1);
			anim.play();

			updated.textProperty().set(
					MessageFormat.format(bundle.getString("updated"), PlatformService.get().getInstalledVersion()));

			updatesContainer.visibleProperty().set(true);
		} else
			updatesContainer.visibleProperty().set(false);
	}

	@FXML
	void evtBack(ActionEvent evt) {
		context.pop();
	}

	class IndentedCodeBlockNodeRenderer implements NodeRenderer {

		private final HtmlWriter html;

		IndentedCodeBlockNodeRenderer(HtmlNodeRendererContext context) {
			this.html = context.getWriter();
		}

		@Override
		public Set<Class<? extends Node>> getNodeTypes() {
			// Return the node types we want to use this renderer for.
			return Collections.<Class<? extends Node>>singleton(IndentedCodeBlock.class);
		}

		@Override
		public void render(Node node) {
			// We only handle one type as per getNodeTypes, so we can just cast it here.
			IndentedCodeBlock codeBlock = (IndentedCodeBlock) node;
			html.line();
			html.tag("pre");
			html.text(codeBlock.getLiteral());
			html.tag("/pre");
			html.line();
		}
	}
}
