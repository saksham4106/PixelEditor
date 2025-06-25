package org.intellij.sdk;

public enum Tool {

    BRUSH_TOOL("Brush tool"),
    SELECTION_TOOL("Selection tool"),
    ERASE("Eraser Tool");

    public final String name;
    Tool(String name){
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
