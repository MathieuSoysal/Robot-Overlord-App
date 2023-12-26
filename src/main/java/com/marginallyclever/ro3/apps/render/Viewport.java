package com.marginallyclever.ro3.apps.render;

import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLEventListener;
import com.marginallyclever.convenience.Ray;
import com.marginallyclever.convenience.helpers.MatrixHelper;
import com.marginallyclever.ro3.Registry;
import com.marginallyclever.ro3.apps.render.renderpasses.*;
import com.marginallyclever.ro3.listwithevents.ListWithEvents;
import com.marginallyclever.ro3.node.Node;
import com.marginallyclever.ro3.node.nodes.Camera;
import com.marginallyclever.ro3.raypicking.RayPickSystem;
import com.marginallyclever.ro3.raypicking.RayHit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.vecmath.Matrix3d;
import javax.vecmath.Matrix4d;
import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * {@link Viewport} is an {@link OpenGLPanel} that uses a set of {@link RenderPass}es to draw the
 * {@link Registry#getScene()} from the perspective of a {@link Registry#getActiveCamera()}.
 */
public class Viewport extends OpenGLPanel implements GLEventListener {
    private static final Logger logger = LoggerFactory.getLogger(Viewport.class);
    public ListWithEvents<RenderPass> renderPasses = new ListWithEvents<>();
    private Camera camera;
    private final JToolBar toolBar = new JToolBar();
    private final DefaultComboBoxModel<Camera> cameraListModel = new DefaultComboBoxModel<>();
    private final JPopupMenu renderPassMenu = new JPopupMenu();
    private final List<Boolean> buttonPressed = new ArrayList<>();
    private int mx, my;
    private double orbitRadius = 50;
    private double orbitChangeFactor = 1.1;  // must always be greater than 1
    private int canvasWidth, canvasHeight;


    public Viewport() {
        super();
        add(toolBar, BorderLayout.NORTH);
        toolBar.setLayout(new FlowLayout(FlowLayout.LEFT,5,1));
        addRenderPasses();
        addCameraSelector();
        addRenderPassSelection();
        addCopyCameraAction();
        allocateButtonMemory();
    }

    private void addRenderPasses() {
        renderPasses.add(new DrawBackground());
        renderPasses.add(new DrawGroundPlane());
        renderPasses.add(new DrawMeshes());
        renderPasses.add(new DrawBoundingBoxes());
        renderPasses.add(new DrawCameras());
        renderPasses.add(new DrawDHParameters());
        renderPasses.add(new DrawPoses());
        renderPasses.add(new DrawHingeJoints());
    }

    private void allocateButtonMemory() {
        // initialize mouse button states
        for(int i=0;i<MouseInfo.getNumberOfButtons();++i) {
            buttonPressed.add(false);
        }
    }

    private void addCopyCameraAction() {
        JButton button = new JButton(new AbstractAction() {
            {
                putValue(Action.NAME,"Copy to Scene");
                putValue(Action.SMALL_ICON, new ImageIcon(Objects.requireNonNull(getClass().getResource("icons8-add-16.png"))));
                putValue(Action.SHORT_DESCRIPTION,"Copy the current camera to the root of the scene.");
            }
            @Override
            public void actionPerformed(ActionEvent e) {
                logger.debug("copy camera");
                Camera newCamera = new Camera();
                newCamera.fromJSON(camera.toJSON());
                newCamera.witnessProtection();
                Registry.getScene().addChild(newCamera);
            }
        });
        toolBar.add(button);
    }

    private void addRenderPassSelection() {
        JButton button = new JButton("Render");
        toolBar.add(button);

        renderPassMenu.removeAll();

        // Add an ActionListener to the JButton to show the JPopupMenu when clicked
        button.addActionListener(e -> renderPassMenu.show(button, button.getWidth()/2, button.getHeight()/2));

        for(RenderPass renderPass : renderPasses.getList()) {
            addRenderPass(renderPass);
        }
    }

    @Override
    public void addNotify() {
        super.addNotify();
        Registry.cameras.addItemAddedListener(this::addCamera);
        Registry.cameras.addItemRemovedListener(this::removeCamera);
        renderPasses.addItemAddedListener(this::addRenderPass);
        renderPasses.addItemRemovedListener(this::removeRenderPass);
    }

    @Override
    public void removeNotify() {
        super.removeNotify();
        Registry.cameras.removeItemAddedListener(this::addCamera);
        Registry.cameras.removeItemRemovedListener(this::removeCamera);
        renderPasses.removeItemAddedListener(this::addRenderPass);
        renderPasses.removeItemRemovedListener(this::removeRenderPass);
    }

    private void addRenderPass(RenderPass renderPass) {
        addRenderPassInternal(renderPass);
        addGLEventListener(renderPass);
    }

    private void removeRenderPass(RenderPass renderPass) {
        removeRenderPassInternal(renderPass);
        removeGLEventListener(renderPass);
    }

    private void addRenderPassInternal(RenderPass renderPass) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(2,2,0,2));
        JButton button = new JButton();
        setRenderPassButtonLabel(button, renderPass.getActiveStatus());
        button.addActionListener(e -> {
            renderPass.setActiveStatus((renderPass.getActiveStatus() + 1) % RenderPass.MAX_STATUS );
            setRenderPassButtonLabel(button, renderPass.getActiveStatus());
        });
        panel.add(button, BorderLayout.WEST);
        JLabel label = new JLabel(renderPass.getName());
        label.setBorder(BorderFactory.createEmptyBorder(0,5,0,0));
        panel.add(label, BorderLayout.CENTER);
        renderPassMenu.add(panel);
    }

    void setRenderPassButtonLabel(JButton button, int status) {
        switch(status) {
            case RenderPass.NEVER -> button.setText("N");
            case RenderPass.SOMETIMES -> button.setText("S");
            case RenderPass.ALWAYS -> button.setText("A");
        }
    }

    private void removeRenderPassInternal(RenderPass renderPass) {
        for(Component c : renderPassMenu.getComponents()) {
            if(c instanceof JCheckBox checkBox) {
                if(checkBox.getText().equals(renderPass.getName())) {
                    renderPassMenu.remove(c);
                    return;
                }
            }
        }
    }

    private void addCamera(Camera camera) {
        if(cameraListModel.getIndexOf(camera) == -1) {
            cameraListModel.addElement(camera);
        }
    }

    private void removeCamera(Camera camera) {
        cameraListModel.removeElement(camera);
    }

    private void addCameraSelector() {
        JComboBox<Camera> cameraSelector = new JComboBox<>();
        cameraSelector.setModel(cameraListModel);
        cameraSelector.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof Camera) {
                    setText(((Camera) value).getName());
                }
                return this;
            }
        });

        cameraListModel.addAll(Registry.cameras.getList());

        cameraSelector.addItemListener(e -> {
            camera = (Camera) e.getItem();
            Registry.setActiveCamera(camera);
        });
        if(cameraListModel.getSize()>0) cameraSelector.setSelectedIndex(0);
        toolBar.add(cameraSelector);
    }

    @Override
    public void init(GLAutoDrawable glAutoDrawable) {
        super.init(glAutoDrawable);
    }

    @Override
    public void dispose(GLAutoDrawable glAutoDrawable) {
        super.dispose(glAutoDrawable);
    }

    @Override
    public void reshape(GLAutoDrawable glAutoDrawable, int x, int y, int width, int height) {
        super.reshape(glAutoDrawable,x,y,width,height);
        canvasWidth = width;
        canvasHeight = height;
    }

    @Override
    public void display(GLAutoDrawable glAutoDrawable) {
        double dt = 0.03;
        updateAllNodes(dt);
        renderAllPasses();
    }

    private void renderAllPasses() {
        // renderPasses that are always on
        for(RenderPass pass : renderPasses.getList()) {
            if(pass.getActiveStatus()==RenderPass.ALWAYS) {
                pass.draw(this);
            }
        }
    }

    private void updateAllNodes(double dt) {
        Registry.getScene().update(dt);
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        super.mouseClicked(e);
        // Ray pick to select node

        Node hit = findNodeUnderCursor();
        if(hit==null) {
            logger.debug("hit nothing.");
        } else {
            logger.debug("hit {}", hit.getAbsolutePath());
        }
    }

    @Override
    public void mousePressed(MouseEvent e) {
        buttonPressed.set(e.getButton(),true);
        mx = e.getX();
        my = e.getY();
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        buttonPressed.set(e.getButton(),false);
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        int px = e.getX();
        double dx = px - mx;
        mx = px;

        int py = e.getY();
        double dy = py - my;
        my = py;

        // scale based on orbit distance - smaller orbits need smaller movements
        dx *= orbitRadius / 50d;
        dy *= orbitRadius / 50d;

        boolean shift = (e.getModifiersEx() & MouseEvent.SHIFT_DOWN_MASK) != 0;

        //if(buttonPressed.get(MouseEvent.BUTTON1)) {}
        if(buttonPressed.get(MouseEvent.BUTTON2)) {  // middle button
            if(!shift) {
                panTiltCamera(dx, dy);
            } else {
                camera.dolly(dy);
            }
        }
        if(buttonPressed.get(MouseEvent.BUTTON3)) {  // right button
            if(!shift) {
                orbitCamera(dx,dy);
            } else {
                camera.truck(-dx);
                camera.pedestal(dy);
            }
        }
    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        super.mouseWheelMoved(e);
        int dz = e.getWheelRotation();
        changeOrbitRadius(dz);
    }

    /**
     * Change the distance from the camera to the orbit point.  The orbit point does not move.  In effect the camera
     * is performing a dolly in/out.
     * @param dz mouse wheel movement
     */
    private void changeOrbitRadius(int dz) {
        Matrix4d local = camera.getLocal();
        Vector3d orbitPoint = getOrbitPoint();

        orbitRadius = dz > 0 ? orbitRadius * orbitChangeFactor : orbitRadius / orbitChangeFactor;
        orbitRadius = Math.max(1,orbitRadius);
        //logger.debug("wheel "+dz + " orbitRadius=" + orbitRadius);

        Vector3d orbitVector = MatrixHelper.getZAxis(local);
        orbitVector.scaleAdd(orbitRadius,orbitPoint);
        local.setTranslation(orbitVector);
    }

    private void panTiltCamera(double dx, double dy) {
        Matrix4d local = camera.getLocal();
        Vector3d t = new Vector3d();
        local.get(t);
        double [] panTiltAngles = getPanTiltFromMatrix(local);
        panTiltAngles[0] = (panTiltAngles[0] + dx+360) % 360;
        panTiltAngles[1] = Math.max(0,Math.min(180,panTiltAngles[1] + dy));
        Matrix3d panTilt = buildPanTiltMatrix(panTiltAngles);
        local.set(panTilt);
        local.setTranslation(t);
        camera.setLocal(local);
    }

    /**
     * Orbit the camera around a point orbitRadius ahead of the camera.
     * @param dx change in x
     * @param dy change in y
     */
    void orbitCamera(double dx,double dy) {
        Matrix4d local = camera.getLocal();
        Vector3d orbitPoint = getOrbitPoint();
        //logger.debug("before {}",orbitPoint);
        double [] panTiltAngles = getPanTiltFromMatrix(local);
        panTiltAngles[0] = (panTiltAngles[0] + dx+360) % 360;
        panTiltAngles[1] = Math.max(0,Math.min(180,panTiltAngles[1] + dy));
        Matrix3d panTilt = buildPanTiltMatrix(panTiltAngles);
        Matrix4d newLocal = new Matrix4d();
        newLocal.set(panTilt);
        Vector3d orbitVector = MatrixHelper.getZAxis(newLocal);
        orbitVector.scaleAdd(orbitRadius,orbitPoint);
        newLocal.setTranslation(orbitVector);
        camera.setLocal(newLocal);
        //logger.debug("after {}",getOrbitPoint());
    }

    /**
     * @return the point that the camera is orbiting around.
     */
    Vector3d getOrbitPoint() {
        Matrix4d local = camera.getLocal();
        Vector3d position = MatrixHelper.getPosition(local);
        // z axis points away from the direction the camera is facing.
        Vector3d zAxis = MatrixHelper.getZAxis(local);
        zAxis.scale(-orbitRadius);
        position.add(zAxis);
        return position;
    }

    double[] getPanTiltFromMatrix(Matrix4d matrix) {
        Vector3d v = MatrixHelper.matrixToEuler(matrix);
        double pan = Math.toDegrees(-v.z);
        double tilt = Math.toDegrees(v.x);
        return new double[]{ pan, tilt };
    }

    /**
     * @param panTiltAngles [0] = pan, [1] = tilt
     * @return a matrix that rotates the camera by the given pan and tilt angles.
     */
    Matrix3d buildPanTiltMatrix(double [] panTiltAngles) {
        Matrix3d a = new Matrix3d();
        a.rotZ(Math.toRadians(panTiltAngles[0]));

        Matrix3d b = new Matrix3d();
        b.rotX(Math.toRadians(-panTiltAngles[1]));

        Matrix3d c = new Matrix3d();
        c.mul(b,a);
        c.transpose();
        return c;
    }

    public double getOrbitChangeFactor() {
        return orbitChangeFactor;
    }

    /**
     * @param amount a value greater than one.
     */
    public void setOrbitChangeFactor(double amount) {
        if( amount <= 1 ) throw new InvalidParameterException("orbit change factor must be greater than 1.");
        orbitChangeFactor = amount;
    }

    /**
     * Use ray tracing to find the Entity at the cursor position closest to the camera.
     * @return the name of the item under the cursor, or -1 if nothing was picked.
     */
    private Node findNodeUnderCursor() {
        if(camera==null) return null;

        Ray ray = getRayThroughCursor();
        RayPickSystem rayPickSystem = new RayPickSystem();
        RayHit rayHit = rayPickSystem.getFirstHit(ray);
        if(rayHit == null || rayHit.target()==null) {
            return null;
        }

        return rayHit.target();
    }

    /**
     * Return the ray coming through the viewport in the current projection.
     * @return the ray coming through the viewport in the current projection.
     */
    public Ray getRayThroughCursor() {
        return getRayThroughPoint(camera,mx,my);
    }

    /**
     * Return the ray coming through the viewport in the current projection.
     * @param x the cursor position in screen coordinates [-1,1]
     * @param y the cursor position in screen coordinates [-1,1]
     * @return the ray coming through the viewport in the current projection.
     */
    public Ray getRayThroughPoint(Camera c,double x,double y) {
        // OpenGL camera: -Z=forward, +X=right, +Y=up
        // get the ray coming through the viewport in the current projection.
        Point3d origin;
        Vector3d direction;

        if(c.getDrawOrthographic()) {
            // orthographic projection
            origin = new Point3d(
                    x*canvasWidth/10,
                    y*canvasHeight/10,
                    0);
            direction = new Vector3d(0,0,-1);
            Matrix4d m2 = c.getWorld();
            m2.transform(direction);
            m2.transform(origin);
        } else {
            // perspective projection
            Vector3d cursorUnitVector = getCursorAsNormalized();
            double t = Math.tan(Math.toRadians(c.getFovY()/2));
            direction = new Vector3d((cursorUnitVector.x)*t*getAspectRatio(),
                                    (cursorUnitVector.y)*t,
                                    -1);
            // adjust the ray by the camera world pose.
            Matrix4d m2 = c.getWorld();
            m2.transform(direction);
            origin = new Point3d(c.getPosition());
            //logger.debug("origin {} direction {}",origin,direction);
        }

        return new Ray(origin,direction);
    }

    /**
     * @return the cursor position as values from -1...1.
     */
    public Vector3d getCursorAsNormalized() {
        return new Vector3d((2.0*mx/canvasWidth)-1.0,
                            1.0-(2.0*my/canvasHeight),
                            0);
    }

    public double getAspectRatio() {
        return (double)canvasWidth/(double)canvasHeight;
    }
}