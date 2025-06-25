package org.intellij.sdk.toolwindow;

import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.ColorChooserService;
import com.intellij.ui.JBColor;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import org.intellij.sdk.Tool;
import org.intellij.sdk.service.BrushSelectionService;
import org.intellij.sdk.service.ToolSelectionService;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class ColorEditorWindowFactory implements ToolWindowFactory, DumbAware {
    public static String WINDOW_ID = "ColorEditor";

    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        WINDOW_ID = toolWindow.getId();
        ColorEditorWindowContent content = new ColorEditorWindowContent(toolWindow);
        Content con = ContentFactory.getInstance().createContent(content.getContentPanel(), "", false);
        toolWindow.getContentManager().addContent(con);
    }

    public static class ColorEditorWindowContent {
        public AtomicReference<Color> selectedColor = new AtomicReference<>(JBColor.BLACK);
        private final JPanel contentPanel = new JPanel();

        public ColorEditorWindowContent(ToolWindow toolWindow){
            contentPanel.setLayout(new BorderLayout(0, 20));
            contentPanel.setBorder(BorderFactory.createEmptyBorder(40, 0, 0, 0));
            contentPanel.add(createControlsPanel(toolWindow), BorderLayout.CENTER);
        }

        @NotNull
        private JPanel createControlsPanel(ToolWindow toolWindow) {

            JPanel controlsPanel = new JPanel();
            controlsPanel.setLayout(new BoxLayout(controlsPanel, BoxLayout.Y_AXIS));
            controlsPanel.setBackground(JBColor.PanelBackground); // Optional aesthetic tweak

            ToolSelectionService ts = toolWindow.getProject().getService(ToolSelectionService.class);
            JPanel toolButtonsPanel = new JPanel(new GridLayout(2, 2, 5, 5));
            toolButtonsPanel.setMaximumSize(new Dimension(200, 60));
            toolButtonsPanel.setAlignmentX(0.25f);


            ButtonGroup toolButtonGroup = new ButtonGroup();

            for (Tool tool : Tool.values()) {
                JToggleButton button = new JToggleButton(tool.getName());
                button.setFocusPainted(false);
                button.setSelected(ts.getEquippedTool() == tool);
                button.addActionListener(e -> ts.setEquippedTool(tool));

                toolButtonGroup.add(button);
                toolButtonsPanel.add(button);
            }

            controlsPanel.add(new JLabel("Tools:"));
            controlsPanel.add(toolButtonsPanel);
            controlsPanel.add(Box.createVerticalStrut(10));

            JButton colorButton = new JButton("Color");
            colorButton.setBackground(selectedColor.get());
            colorButton.setForeground(selectedColor.get());

            colorButton.addActionListener(e ->
                    ColorChooserService.getInstance().showPopup(toolWindow.getProject(), selectedColor.get(), ((color, o) -> {
                        selectedColor.set(color);
                        colorButton.setForeground(color);
                        colorButton.setBackground(color);
                        toolWindow.getProject().getService(BrushSelectionService.class).setSelectedColor(color);
                    }))
            );
            controlsPanel.add(colorButton);
            controlsPanel.add(Box.createVerticalStrut(10));

            controlsPanel.add(new JLabel("Brush Size:"));
            JSpinner brushSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 50, 1));
            brushSpinner.setMaximumSize(new Dimension(100, 30));

            brushSpinner.addChangeListener(e -> {
                int value = (Integer) brushSpinner.getValue();
                toolWindow.getProject().getService(BrushSelectionService.class)
                        .setStroke(new BasicStroke(value));
            });
            controlsPanel.add(brushSpinner);

            return controlsPanel;

        }

        public JPanel getContentPanel() {
            return contentPanel;
        }
    }



}
