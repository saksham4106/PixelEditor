package org.intellij.sdk.service;


import com.intellij.openapi.components.Service;
import com.intellij.ui.JBColor;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

@Service(Service.Level.PROJECT)
public final class BrushSelectionService {
    Color selectedColor = JBColor.BLACK;
    BasicStroke stroke = new BasicStroke(1.0f);
    List<Runnable> listeners = new ArrayList<>();

    public void setSelectedColor(Color selectedColor) {
        if(this.selectedColor != selectedColor) {
            this.selectedColor = selectedColor;
            notifyListeners();
        }
    }

    public void setStroke(BasicStroke stroke) {
        if(this.stroke != stroke){
            this.stroke = stroke;
            notifyListeners();
        }
    }

    public void addBrushChangeListener(Runnable listener) {
        listeners.add(listener);
    }

    public Color getSelectedColor() {
        return selectedColor;
    }

    public BasicStroke getStroke() {
        return stroke;
    }

    void notifyListeners(){
        for (Runnable listener : listeners) {
            listener.run();
        }
    }
}
