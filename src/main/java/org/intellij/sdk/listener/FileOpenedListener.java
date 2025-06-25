package org.intellij.sdk.listener;

import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.FileOpenedSyncListener;
import com.intellij.openapi.fileEditor.ex.FileEditorWithProvider;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.*;
import org.intellij.sdk.toolwindow.ColorEditorWindowFactory;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;

public class FileOpenedListener implements FileOpenedSyncListener {
    @Override
    public void fileOpenedSync(@NotNull FileEditorManager source, @NotNull VirtualFile file, @NotNull List<FileEditorWithProvider> editorsWithProviders) {
        ToolWindowManager toolWindowManager = ToolWindowManager.getInstance(source.getProject());

        ToolWindow win = toolWindowManager.getToolWindow(ColorEditorWindowFactory.WINDOW_ID);
        if(win == null){
            return;
        }
        List<String> extensions = List.of("png", "jpg");

        if(extensions.contains(Objects.requireNonNull(file.getExtension()).toLowerCase())){
            win.show();
        }else{
            win.hide();
        }


    }
}
