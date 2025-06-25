package org.intellij.sdk.service;

import com.intellij.openapi.components.Service;
import org.intellij.sdk.Tool;

import java.util.ArrayList;
import java.util.List;

@Service(Service.Level.PROJECT)
public final class ToolSelectionService {
    Tool equippedTool = Tool.BRUSH_TOOL;
    List<Runnable> listeners = new ArrayList<>();

    public void addToolChangeListener(Runnable listener) {
        listeners.add(listener);
    }

    public void setEquippedTool(Tool equippedTool) {
        if(this.equippedTool != equippedTool){
            this.equippedTool = equippedTool;
            notifyListeners();
        }

    }

    public Tool getEquippedTool() {
        return equippedTool;
    }

    void notifyListeners(){
        for (Runnable listener : listeners) {
            listener.run();
        }
    }
}
