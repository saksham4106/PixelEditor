package org.intellij.sdk.fileeditor;

import com.intellij.ide.DataManager;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.JBColor;
import com.intellij.util.Alarm;
import com.intellij.util.ui.UIUtil;
import org.intellij.sdk.Tool;
import org.intellij.sdk.service.BrushSelectionService;
import org.intellij.sdk.service.ToolSelectionService;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;

public class ImageEditorPanel extends JPanel implements Disposable, DataProvider {

    private float zoom = 1;
    private Graphics2D imageGraphic;
    private Graphics2D overlayGraphic;
    private final BufferedImage originalImage;
    private BufferedImage scaledImage;
    private BufferedImage overlay;
    private final Project project;
    private final VirtualFile file;
    private Tool equippedTool;
    private Point initialPoint, finalPoint;
    private Point selectionInitialPoint, selectionFinalPoint;
    private Rectangle selection = null;
    private boolean areaSelected = false;
    private Point viewPosition = new Point(0, 0);
    private Point hoveredPixel = null;

    @Override
    protected void paintComponent(Graphics g1) {
        super.paintComponent(g1);
        if (scaledImage == null) {
            scaledImage = (BufferedImage) createImage(getSize().width, getSize().height);
            imageGraphic = (Graphics2D) scaledImage.getGraphics();
            imageGraphic.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
        }

        g1.drawImage(scaledImage, 0, 0, scaledImage.getWidth(), scaledImage.getHeight() , null);

        if(selectionInitialPoint != null && selectionFinalPoint != null){
            JViewport viewport = (JViewport) SwingUtilities.getAncestorOfClass(JViewport.class, this);
            viewPosition = viewport.getViewPosition();
            int width = selectionFinalPoint.x - selectionInitialPoint.x;
            int height = selectionFinalPoint.y - selectionInitialPoint.y;

            if(width > 0 && height > 0){
                selection = new Rectangle(selectionInitialPoint.x + viewPosition.x, selectionInitialPoint.y + viewPosition.y, selectionFinalPoint.x - selectionInitialPoint.x + 1,  selectionFinalPoint.y - selectionInitialPoint.y + 1);
            }else if(width > 0 && height < 0){
                selection = new Rectangle(selectionInitialPoint.x + viewPosition.x, selectionFinalPoint.y + viewPosition.y, selectionFinalPoint.x - selectionInitialPoint.x + 1,  selectionInitialPoint.y - selectionFinalPoint.y  + 1);
            }else if(width < 0 && height > 0){
                selection = new Rectangle(selectionFinalPoint.x + viewPosition.x, selectionInitialPoint.y + viewPosition.y, selectionInitialPoint.x - selectionFinalPoint.x + 1,  selectionFinalPoint.y - selectionInitialPoint.y  + 1);
            }else{
                selection = new Rectangle(selectionFinalPoint.x + viewPosition.x, selectionFinalPoint.y + viewPosition.y, -selectionFinalPoint.x + selectionInitialPoint.x  + 1,  -selectionFinalPoint.y + selectionInitialPoint.y + 1);
            }

            selection = new Rectangle((int) ((selection.x - viewPosition.x) * zoom), (int) ((selection.y - viewPosition.y) * zoom), (int) (selection.width * zoom), (int) (selection.height * zoom) + 1);
            Color color = JBColor.BLACK;
            ((Graphics2D)g1).setPaint(new Color(color.getRed(), color.getGreen(), color.getBlue(), 128));

            g1.fillRect(selection.x, selection.y, selection.width, selection.height);
        }

        if(hoveredPixel != null && selection == null){
            ((Graphics2D)g1).setPaint(imageGraphic.getPaint());
            float w = ((BasicStroke)imageGraphic.getStroke()).getLineWidth();
            ((Graphics2D)g1).setStroke(new BasicStroke(w * zoom));
            int x = (int) ((hoveredPixel.x + 0.5f) * zoom);
            int y = (int) ((hoveredPixel.y + 0.5f) * zoom);
            g1.drawLine(x, y, x, y);
        }

        g1.drawImage(overlay, 0, 0, (int) (originalImage.getWidth() * zoom), (int) (originalImage.getHeight() * zoom), null);

        g1.dispose();

    }

    public ImageEditorPanel(BufferedImage img, Project project, VirtualFile file) {
        this.originalImage = img;
        this.project = project;
        this.file = file;

        setImage(img);
        setFocusable(true);
        setOpaque(true);
        defaultListener();


        Alarm alarm = new Alarm(Alarm.ThreadToUse.POOLED_THREAD, this);
        ImageEditorPanel panel = this;
        Runnable task = new Runnable() {
            @Override
            public void run() {
                ApplicationManager.getApplication().invokeLater(panel::forceSave);
                alarm.addRequest(this, 5000); // reschedule
            }
        };

        alarm.addRequest(task, 5000);



    }

    public void showContextMenu(int x, int y){
        ActionManager am = ActionManager.getInstance();
        ActionGroup group = (ActionGroup) am.getAction("EditorPopupMenu");
        ActionPopupMenu popupMenu = am.createActionPopupMenu("CustomEditor", group);
        popupMenu.getComponent().show(this, (int)(x), (int)(y));
    }
    public void defaultListener() {
        setDoubleBuffered(true);

        MouseListener listener = new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                if(SwingUtilities.isRightMouseButton(e)){
                    showContextMenu(e.getX(), e.getY());
                }


                ImageEditorPanel.this.requestFocusInWindow();
                initialPoint = SwingUtilities.convertPoint((Component) e.getSource(), e.getPoint(), getParent());

                if(imageGraphic != null){
                    if(equippedTool == Tool.BRUSH_TOOL){
                        JViewport viewport = (JViewport) SwingUtilities.getAncestorOfClass(JViewport.class, (Component) e.getSource());
                        viewPosition = viewport.getViewPosition();
                        drawEraseLine(imageGraphic, e, (int) (initialPoint.x / zoom) + (int)(viewPosition.x/zoom),
                                (int) (initialPoint.y / zoom)   + (int)(viewPosition.y/zoom), (int) (initialPoint.x / zoom)
                                        + (int)(viewPosition.x/zoom), (int) (initialPoint.y / zoom) + (int)(viewPosition.y/zoom), false);
                        scheduleZoom();
                    }else if(equippedTool == Tool.ERASE){
                        JViewport viewport = (JViewport) SwingUtilities.getAncestorOfClass(JViewport.class, (Component) e.getSource());
                        viewPosition = viewport.getViewPosition();
                        drawEraseLine(imageGraphic, e, (int) (initialPoint.x / zoom) + (int)(viewPosition.x/zoom),
                                (int) (initialPoint.y / zoom)   + (int)(viewPosition.y/zoom), (int) (initialPoint.x / zoom)
                                        + (int)(viewPosition.x/zoom), (int) (initialPoint.y / zoom) + (int)(viewPosition.y/zoom), true);
                        scheduleZoom();
                    }else if(equippedTool == Tool.SELECTION_TOOL){
                        JViewport viewport = (JViewport) SwingUtilities.getAncestorOfClass(JViewport.class, (Component) e.getSource());
                        viewPosition = viewport.getViewPosition();
                        if(selection == null){
                            selectionInitialPoint = handleOffset(initialPoint, viewPosition);
                        }else{
                            if(!selection.contains(initialPoint)){
                                selection = null;
                                selectionInitialPoint = null;
                                selectionFinalPoint = null;
                                areaSelected = false;
                                scheduleZoom();
                            }else{
                                areaSelected = true;
                            }
                        }
                    }
                }
            }
        };

        MouseWheelListener listener1 = e -> {
            if(e.isControlDown()){
                zoom -= e.getWheelRotation() / 20.0f;
                zoom = Math.max(0.05f, zoom);
                scheduleZoom();
                e.consume();
            }
        };

        MouseMotionListener motion = new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                JViewport viewport = (JViewport) SwingUtilities.getAncestorOfClass(JViewport.class, (Component) e.getSource());
                viewPosition = viewport.getViewPosition();
                hoveredPixel = handleOffset(SwingUtilities.convertPoint(e.getComponent(), e.getPoint(), getParent()), viewPosition);
                if(hoveredPixel.x < 0 || hoveredPixel.x > scaledImage.getWidth() / zoom || hoveredPixel.y < 0 || hoveredPixel.y > scaledImage.getHeight() / zoom){
                    hoveredPixel = null;
                }

                scheduleZoom();
            }

            public void mouseDragged(MouseEvent e) {
                if(imageGraphic == null) return;

                JViewport viewport = (JViewport) SwingUtilities.getAncestorOfClass(JViewport.class, (Component) e.getSource());
                finalPoint = SwingUtilities.convertPoint((Component) e.getSource(), e.getPoint(), getParent());

                if(SwingUtilities.isMiddleMouseButton(e)){
                    viewPosition = viewport.getViewPosition();
                    int dx = finalPoint.x - initialPoint.x;
                    int dy = finalPoint.y - initialPoint.y;

                    viewPosition.translate(-dx, -dy);
                    scrollRectToVisible(new Rectangle(viewPosition, viewport.getSize()));

                    scheduleZoom();
                }

                if(equippedTool == Tool.BRUSH_TOOL){
                    viewPosition = viewport.getViewPosition();
                    drawEraseLine(imageGraphic, e, (int) (initialPoint.x/zoom)  + (int)(viewPosition.x/zoom), (int) (initialPoint.y/zoom) + (int)(viewPosition.y/zoom), (int) (finalPoint.x/zoom)  + (int)(viewPosition.x/zoom), (int) (finalPoint.y/zoom)  + (int)(viewPosition.y/zoom), false);
                    scheduleZoom();
                }else if(equippedTool == Tool.ERASE){
                    viewPosition = viewport.getViewPosition();
                    drawEraseLine(imageGraphic, e, (int) (initialPoint.x/zoom)  + (int)(viewPosition.x/zoom), (int) (initialPoint.y/zoom) + (int)(viewPosition.y/zoom), (int) (finalPoint.x/zoom)  + (int)(viewPosition.x/zoom), (int) (finalPoint.y/zoom)  + (int)(viewPosition.y/zoom), true);
                    scheduleZoom();

                }else if(equippedTool == Tool.SELECTION_TOOL){
                    if(!areaSelected){
                        selectionFinalPoint = handleOffset(SwingUtilities.convertPoint((Component) e.getSource(), e.getPoint(), getParent()), viewPosition);
                        scheduleZoom();
                    }
//                    else{
//                        viewPosition = viewport.getViewPosition();
//                        int dx = (int)(finalPoint.x/zoom) -(int) (initialPoint.x/zoom);
//                        int dy = (int)(finalPoint.y/zoom) -(int) (initialPoint.y/zoom);
//
//                        selectionInitialPoint.translate(dx, dy);
//                        selectionFinalPoint.translate(dx, dy);
//
//                        int selX = (int) (selection.x / zoom + viewPosition.x / zoom) + 1;
//                        int selY = (int) (selection.y / zoom + viewPosition.y / zoom) + 1;
//                        int selW = (int) (selection.width / zoom);
//                        int selH = (int) (selection.height / zoom);
//
//                        if(selectionimage == null){
//                            BufferedImage copy = new BufferedImage(selW, selH, BufferedImage.TYPE_INT_ARGB);
//                            Graphics2D gCopy = copy.createGraphics();
//                            gCopy.drawImage(originalImage, 0, 0, selW, selH,
//                                    selX, selY, selX + selW, selY + selH, null);
//                            gCopy.dispose();
//                            selectionimage = copy;
//                        }
//
//
//                        Composite temp = overlayGraphic.getComposite();
//                        overlayGraphic.setComposite(AlphaComposite.Clear);
//                        overlayGraphic.fillRect(selX, selY, selW, selH);
//                        overlayGraphic.setComposite(temp);
//
//                        int newX = selX + dx;
//                        int newY = selY + dy;
////                        overlayGraphic.drawImage(copy, newX, newY, null);
//                        overlayGraphic.drawImage(selectionimage, newX, newY, null);
//                        scheduleZoom();
//                    }
                }

                initialPoint.setLocation(finalPoint);

            }
        };

        KeyListener keyListener = new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if(e.getKeyCode() == KeyEvent.VK_BACK_SPACE){
                    if(selection != null){
                        if(imageGraphic != null){
                            JViewport viewport = (JViewport) SwingUtilities.getAncestorOfClass(JViewport.class, (Component) e.getSource());
                            viewPosition = viewport.getViewPosition();

                            Composite temp = imageGraphic.getComposite();
                            imageGraphic.setComposite(AlphaComposite.Clear);
                            Point n = new Point(selection.x, selection.y);
                            n = handleOffset(n, viewPosition);

                            imageGraphic.fillRect((int) (n.x + 1 - viewPosition.x / zoom), (int) (n.y + 1 - viewPosition.y / zoom), (int) (selection.width / zoom) + 1, (int) (selection.height / zoom));
                            imageGraphic.setComposite(temp);
                            selection = null;
                            selectionInitialPoint = null;
                            scheduleZoom();

                        }
                    }
                }
            }
        };
        addMouseListener(listener);
        addMouseMotionListener(motion);
        addMouseWheelListener(listener1);
        addKeyListener(keyListener);
    }

    private Point handleOffset(Point point, Point viewPos){
        return new Point((int) (point.x / zoom + viewPos.x / zoom), (int) (point.y / zoom + viewPos.y / zoom));
    }

    private void drawEraseLine(Graphics2D g, MouseEvent e, int x1, int y1, int x2, int y2, boolean erase) {
        if(SwingUtilities.isLeftMouseButton(e)){
            if(erase){
                Composite temp = g.getComposite();
                g.setComposite(AlphaComposite.Clear);
                g.drawLine(x1, y1, x2, y2);
                g.setComposite(temp);
            }else{
                g.drawLine(x1, y1, x2, y2);
            }
        }
    }

    private boolean repaintScheduled = false;
    private boolean zoomScheduled = false;

    private void scheduleZoom(){
        if(!zoomScheduled){
            zoomScheduled = true;
            ApplicationManager.getApplication().invokeLater(() -> {
                int newHeight = (int) (originalImage.getHeight() * zoom);
                int newWidth = (int) (originalImage.getWidth() * zoom);

                BufferedImage s = new BufferedImage(newWidth, newHeight, originalImage.getType());
                Graphics2D g2d = s.createGraphics();
                g2d.drawImage(originalImage, 0, 0, newWidth, newHeight, null);
                g2d.dispose();
                this.scaledImage = s;

                zoomScheduled = false;

                scheduleRepaint();
            });
        }
        revalidate();
    }

    private void scheduleRepaint() {
        if (!repaintScheduled) {
            repaintScheduled = true;
            ApplicationManager.getApplication().invokeLater(() -> {
                repaint();
                repaintScheduled = false;
            });
        }
    }

    private void setImage(BufferedImage img) {
        imageGraphic = (Graphics2D) img.getGraphics();
        imageGraphic.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);

        this.scaledImage = img;

        this.overlay = UIUtil.createImage(scaledImage.getWidth(), scaledImage.getHeight(), BufferedImage.TYPE_INT_ARGB);
        this.overlayGraphic = (Graphics2D) this.overlay.getGraphics();
        this.overlayGraphic.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        BrushSelectionService cs = this.project.getService(BrushSelectionService.class);
        ToolSelectionService ts =  this.project.getService(ToolSelectionService.class);
        this.equippedTool = ts.getEquippedTool();
        ts.addToolChangeListener(() -> this.equippedTool = ts.getEquippedTool());

        imageGraphic.setPaint(cs.getSelectedColor());
        imageGraphic.setStroke(cs.getStroke());
        cs.addBrushChangeListener(() -> {
            imageGraphic.setPaint(cs.getSelectedColor());
            imageGraphic.setStroke(cs.getStroke());
        });

        this.zoom = (float) FileEditorManagerEx.getInstanceEx(project).getSplitters().getWidth() / (2.5f * img.getWidth());
        scheduleZoom();

        repaint();
    }

    @Override
    public void dispose() {
        removeAll();
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(scaledImage.getWidth(), scaledImage.getHeight());

    }

    public static final DataKey<ImageEditorPanel> DATA_KEY = DataKey.create("ImageEditorPanel");

    public void forceSave() {
        ApplicationManager.getApplication().runWriteAction(() -> {
            try (OutputStream out = file.getOutputStream(this)) {
                ImageIO.write(originalImage, "png", out);
                file.refresh(false, true);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

    }

    @Override
    public @Nullable Object getData(@NotNull @NonNls String s) {
        if (ImageEditorPanel.DATA_KEY.is(s)) {
            return this;
        }
        return null;
    }

}
