package org.intellij.sdk.action;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import org.intellij.sdk.fileeditor.ImageEditorPanel;
import org.jetbrains.annotations.NotNull;

public class SaveImageAction extends AnAction {

    @Override
    public void update(@NotNull AnActionEvent e) {
        ImageEditorPanel panel = e.getData(ImageEditorPanel.DATA_KEY);
        e.getPresentation().setEnabled(panel != null);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        ImageEditorPanel panel = event.getData(ImageEditorPanel.DATA_KEY);
        if (panel != null) {
            panel.forceSave();
        }
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return super.getActionUpdateThread();
    }
}
