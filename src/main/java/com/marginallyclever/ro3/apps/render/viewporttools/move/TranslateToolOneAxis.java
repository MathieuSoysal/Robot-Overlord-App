package com.marginallyclever.ro3.apps.render.viewporttools.move;

import com.jogamp.opengl.GL3;
import com.marginallyclever.convenience.ColorRGB;
import com.marginallyclever.convenience.Plane;
import com.marginallyclever.convenience.helpers.MatrixHelper;
import com.marginallyclever.ro3.Registry;
import com.marginallyclever.ro3.apps.render.viewporttools.SelectedItems;
import com.marginallyclever.ro3.apps.render.viewporttools.ViewportTool;
import com.marginallyclever.ro3.node.nodes.Camera;
import com.marginallyclever.ro3.node.nodes.Pose;
import com.marginallyclever.ro3.mesh.shapes.Sphere;
import com.marginallyclever.ro3.node.Node;
import com.marginallyclever.ro3.apps.render.ShaderProgram;
import com.marginallyclever.ro3.apps.render.Viewport;
import com.marginallyclever.ro3.mesh.Mesh;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.vecmath.Matrix4d;
import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.List;

/**
 * A tool for moving {@link Pose} nodes in the {@link Viewport} along a single axis.
 *
 * @author Dan Royer
 * @since 2.5.0
 */
public class TranslateToolOneAxis implements ViewportTool {
    private static final Logger logger = LoggerFactory.getLogger(TranslateToolOneAxis.class);
    private final double handleLength = 5;
    private final double gripRadius = 0.5;
    private double localScale = 1;
    private final Mesh gizmoMesh = MatrixHelper.createMesh(5.0);

    /**
     * The viewport to which this tool is attached.
     */
    private Viewport viewport;

    /**
     * The list of entities to adjust.
     */
    private SelectedItems selectedItems;

    /**
     * Is the user dragging the mouse after successfully picking the handle?
     */
    private boolean dragging = false;

    /**
     * The point on the translation plane where the handle was clicked.
     */
    private Point3d startPoint;

    /**
     * The plane on which the user is picking.
     */
    private final Plane translationPlane = new Plane();

    /**
     * The axis along which the user is translating.
     */
    private final Vector3d translationAxis = new Vector3d();

    private final Matrix4d pivotMatrix = MatrixHelper.createIdentityMatrix4();

    private boolean cursorOverHandle = false;

    private final Mesh handleLineMesh = new Mesh(GL3.GL_LINES);
    private final Sphere handleSphere = new Sphere();
    private int frameOfReference = ViewportTool.FRAME_WORLD;
    private final ColorRGB color;


    public TranslateToolOneAxis(ColorRGB color) {
        super();
        this.color = color;

        // handle line
        handleLineMesh.addVertex(0, 0, 0);
        handleLineMesh.addVertex((float)1.0, 0, 0);
    }

    @Override
    public void activate(List<Node> list) {
        this.selectedItems = new SelectedItems(list);
        if(selectedItems.isEmpty()) return;

        updatePivotMatrix();
    }


    public void setPivotMatrix(Matrix4d pivot) {
        pivotMatrix.set(pivot);
        translationPlane.set(MatrixHelper.getXYPlane(pivot));
        translationAxis.set(MatrixHelper.getXAxis(pivot));
    }

    @Override
    public void deactivate() {
        dragging = false;
        selectedItems = null;
    }

    @Override
    public void handleMouseEvent(MouseEvent event) {
        if (event.getID() == MouseEvent.MOUSE_MOVED) {
            mouseMoved(event);
        } else if (event.getID() == MouseEvent.MOUSE_PRESSED) {
            mousePressed(event);
        } else if (event.getID() == MouseEvent.MOUSE_DRAGGED && dragging) {
            mouseDragged(event);
        } else if (event.getID() == MouseEvent.MOUSE_RELEASED) {
            mouseReleased(event);
        }
    }

    @Override
    public void mouseMoved(MouseEvent event) {
        cursorOverHandle = isCursorOverHandle(event.getX(), event.getY());
    }

    @Override
    public void mousePressed(MouseEvent event) {
        if (isCursorOverHandle(event.getX(), event.getY())) {
            dragging = true;
            cursorOverHandle = true;
            startPoint = MoveUtils.getPointOnPlaneFromCursor(translationPlane,viewport,event.getX(), event.getY());
            selectedItems.savePose();
        }
    }

    @Override
    public void mouseDragged(MouseEvent event) {
        if(!dragging) return;

        Point3d currentPoint = MoveUtils.getPointOnPlaneFromCursor(translationPlane,viewport,event.getX(), event.getY());
        if(currentPoint==null) return;

        Point3d nearestPoint = getNearestPointOnAxis(currentPoint);

        Vector3d translation = new Vector3d();
        translation.sub(nearestPoint, startPoint);

        // Apply the translation to the selected items
        for (Node node : selectedItems.getNodes()) {
            if(node instanceof Pose pose) {
                Matrix4d before = selectedItems.getWorldPoseAtStart(node);
                Matrix4d m = new Matrix4d(before);
                m.m03 += translation.x;
                m.m13 += translation.y;
                m.m23 += translation.z;
                pose.setWorld(m);
            }
        }
    }

    @Override
    public void mouseReleased(MouseEvent event) {
        if(!dragging) return;

        dragging = false;
        if(selectedItems!=null) {
            MoveUtils.updateUndoState(selectedItems);
            selectedItems.savePose();
        }
    }

    /**
     * Sets the frame of reference for the tool.
     *
     * @param index 0 for world, 1 for local, 2 for camera.
     */
    @Override
    public void setFrameOfReference(int index) {
        frameOfReference = index;
        if(selectedItems!=null) {
            updatePivotMatrix();
        }
    }

    private void updatePivotMatrix() {
        setPivotMatrix(MoveUtils.getPivotMatrix(frameOfReference,selectedItems));
    }

    private Point3d getNearestPointOnAxis(Point3d currentPoint) {
        // get the cross product of the translationAxis and the translationPlane's normal
        Vector3d orthogonal = new Vector3d();
        orthogonal.cross(translationAxis, translationPlane.normal);
        orthogonal.normalize();
        Vector3d diff = new Vector3d();
        diff.sub(currentPoint,startPoint);
        double d = diff.dot(orthogonal);
        // remove the component of diff that is orthogonal to the translationAxis
        orthogonal.scale(d);
        diff.sub(orthogonal);
        diff.add(startPoint);

        return new Point3d(diff);
    }

    private boolean isCursorOverHandle(int x, int y) {
        if(selectedItems==null || selectedItems.isEmpty()) return false;

        var nc = viewport.getCursorAsNormalized();
        Point3d point = MoveUtils.getPointOnPlaneFromCursor(translationPlane,viewport,nc.x, nc.y);
        if (point == null) return false;

        // Check if the point is within the handle's bounds
        Vector3d diff = new Vector3d(translationAxis);
        diff.scaleAdd(getHandleLengthScaled(), MatrixHelper.getPosition(pivotMatrix));
        diff.sub(point);
        return (diff.lengthSquared() < getGripRadiusScaled()*getGripRadiusScaled());
    }

    @Override
    public void handleKeyEvent(KeyEvent event) {
        // Handle keyboard events, if necessary
    }

    @Override
    public void update(double deltaTime) {
        // Update the tool's state, if necessary
        if(selectedItems!=null) updatePivotMatrix();

        updateLocalScale();
    }

    private void updateLocalScale() {
        Camera cam = Registry.getActiveCamera();
        if(cam!=null) {
            Vector3d cameraPoint = cam.getPosition();
            Vector3d pivotPoint = MatrixHelper.getPosition(pivotMatrix);
            pivotPoint.sub(cameraPoint);
            localScale = pivotPoint.length() * 0.035;  // TODO * InteractionPreferences.toolScale;
        }
    }

    // Render the translation handle on the axis
    @Override
    public void render(GL3 gl, ShaderProgram shaderProgram) {
        if (selectedItems == null || selectedItems.isEmpty()) return;

        float colorScale = cursorOverHandle ? 1:0.5f;
        float red   = color.red   * colorScale / 255f;
        float green = color.green * colorScale / 255f;
        float blue  = color.blue  * colorScale / 255f;
        shaderProgram.set4f(gl,"objectColor",red, green, blue, 1.0f);

        // handle
        Matrix4d m = new Matrix4d(pivotMatrix);
        m.mul(m,MatrixHelper.createScaleMatrix4(getHandleLengthScaled()));
        m.transpose();
        shaderProgram.setMatrix4d(gl,"modelMatrix",m);
        handleLineMesh.render(gl);

        // sphere at end of handle
        Matrix4d m2 = MatrixHelper.createIdentityMatrix4();
        m2.m03 += getHandleLengthScaled();
        m2.mul(pivotMatrix,m2);
        m2.mul(m2,MatrixHelper.createScaleMatrix4(getGripRadiusScaled()));
        m2.transpose();
        shaderProgram.setMatrix4d(gl,"modelMatrix",m2);
        handleSphere.render(gl);
    }

    @Override
    public void setViewport(Viewport viewport) {
        this.viewport = viewport;
    }

    @Override
    public boolean isInUse() {
        return dragging;
    }

    @Override
    public void cancelUse() {
        dragging = false;
    }

    @Override
    public Point3d getStartPoint() {
        return startPoint;
    }


    private double getHandleLengthScaled() {
        return handleLength * localScale;
    }

    private double getGripRadiusScaled() {
        return gripRadius * localScale;
    }

    @Override
    public void init(GL3 gl3) {}

    @Override
    public void dispose(GL3 gl3) {
        handleSphere.unload(gl3);
        handleLineMesh.unload(gl3);
    }
}
