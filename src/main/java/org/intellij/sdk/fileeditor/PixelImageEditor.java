package org.intellij.sdk.fileeditor;

import com.intellij.openapi.actionSystem.DataProvider;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorState;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.components.JBScrollPane;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.io.InputStream;

public class PixelImageEditor implements FileEditor, DataProvider {
    private final VirtualFile virtualFile;
    private final ImageEditorPanel iPanel;
    private final JScrollPane panel;

    @Override
    public @NotNull JComponent getComponent() {
        return panel;
    }

    @Override
    public @Nullable JComponent getPreferredFocusedComponent() {
        return iPanel;
    }

    @Override
    public @Nls(capitalization = Nls.Capitalization.Title) @NotNull String getName() {
        return "Pixel Editor";
    }

    @Override
    public void setState(@NotNull FileEditorState fileEditorState) {

    }

    @Override
    public boolean isModified() {
        return virtualFile.exists();
    }


    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public void addPropertyChangeListener(@NotNull PropertyChangeListener propertyChangeListener) {

    }

    @Override
    public void removePropertyChangeListener(@NotNull PropertyChangeListener propertyChangeListener) {

    }

    @Override
    public void dispose() {

    }

    @Override
    public <T> @Nullable T getUserData(@NotNull Key<T> key) {
        return null;
    }

    @Override
    public <T> void putUserData(@NotNull Key<T> key, @Nullable T t) {

    }

    @Override
    public VirtualFile getFile() {
        return virtualFile;
    }

    public PixelImageEditor(VirtualFile virtualFile, Project project) {
        BufferedImage image = readImage(virtualFile);

        iPanel =  new ImageEditorPanel(image, project, virtualFile);
        panel = new JBScrollPane(iPanel);
        panel.setPreferredSize(new Dimension(800, 600));

        Disposer.register(this, iPanel);

        this.virtualFile = virtualFile;
    }

    public BufferedImage readImage(VirtualFile virtualFile){
        try (InputStream in = virtualFile.getInputStream()) {
            return ImageIO.read(in);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public @Nullable Object getData(@NotNull @NonNls String dataId) {
        if (ImageEditorPanel.DATA_KEY.is(dataId)) {
            return iPanel;
        }
        return null;
    }


}
