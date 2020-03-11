package com.marginallyclever.robotOverlord.entity.robot.sixi2;

import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JPanel;
import javax.vecmath.Matrix3d;
import javax.vecmath.Matrix4d;
import javax.vecmath.Vector3d;

import com.jogamp.opengl.GL2;
import com.marginallyclever.communications.NetworkConnection;
import com.marginallyclever.convenience.Cuboid;
import com.marginallyclever.convenience.MatrixHelper;
import com.marginallyclever.convenience.StringHelper;
import com.marginallyclever.robotOverlord.RobotOverlord;
import com.marginallyclever.robotOverlord.engine.DragBall;
import com.marginallyclever.robotOverlord.engine.dhRobot.DHKeyframe;
import com.marginallyclever.robotOverlord.engine.dhRobot.DHLink;
import com.marginallyclever.robotOverlord.engine.dhRobot.DHTool;
import com.marginallyclever.robotOverlord.entity.physicalObject.PhysicalObject;
import com.marginallyclever.robotOverlord.entity.robot.Robot;
import com.marginallyclever.robotOverlord.entity.robot.RobotKeyframe;
import com.marginallyclever.robotOverlord.entity.robot.sixi2.sixi2ControlBox.Sixi2ControlBox;
import com.marginallyclever.robotOverlord.entity.world.World;
import com.marginallyclever.robotOverlord.uiElements.InputManager;

/**
 * A controller for the simulated and live Sixi robot.
 * @author Dan Royer
 * @since 1.6.0
 *
 */
public class Sixi2 extends Robot {
	public enum ControlMode {
		RECORD(0,"RECORD"),
		PLAYBACK(1,"PLAYBACK");

		private int modeNumber;
		private String modeName;
		private ControlMode(int n,String s) {
			modeNumber=n;
			modeName=s;
		}
		public int toInt() {
			return modeNumber;
		}
		public String toString() {
			return modeName;
		}
		static public String [] getAll() {
			ControlMode[] allModes = ControlMode.values();
			String[] labels = new String[allModes.length];
			for(int i=0;i<labels.length;++i) {
				labels[i] = allModes[i].toString();
			}
			return labels;
		}
	};

	public enum OperatingMode {
		LIVE(0,"LIVE"),
		SIM(1,"SIM");

		private int modeNumber;
		private String modeName;
		private OperatingMode(int n,String s) {
			modeNumber=n;
			modeName=s;
		}
		public int toInt() {
			return modeNumber;
		}
		public String toString() {
			return modeName;
		}
		static public String [] getAll() {
			OperatingMode[] allModes = OperatingMode.values();
			String[] labels = new String[allModes.length];
			for(int i=0;i<labels.length;++i) {
				labels[i] = allModes[i].toString();
			}
			return labels;
		}
	}

	protected Sixi2Panel sixi2Panel;

	public Sixi2Live live = new Sixi2Live();
	public Sixi2Sim sim = new Sixi2Sim();

	public Sixi2Recording recording = new Sixi2Recording();

	protected DragBall ball;

	// true if the skeleton should be visualized on screen. Default is false.
	protected boolean isPicked = false;

	// are we live or simulated?
	protected OperatingMode operatingMode=OperatingMode.SIM;

	// are we trying to record the robot?
	protected ControlMode controlMode=ControlMode.RECORD;

	protected boolean singleBlock = false;
	protected boolean cycleStart = false;
	protected boolean m01Break = true;

	public Sixi2() {
		super();
		setName("Sixi");

		addChild(live);
		addChild(sim);

		ball = new DragBall();
		ball.setParent(this);

		// spawn a control box as a child of the anchor.
		Sixi2ControlBox sixi2ControlBox=new Sixi2ControlBox();
		this.addChild(sixi2ControlBox);
		sixi2ControlBox.setPosition(new Vector3d(0,39,14));
		sixi2ControlBox.setRotation(new Vector3d(0, 0, Math.toRadians(90)));
		sixi2ControlBox.getMaterial().setDiffuseColor(1,217f/255f,33f/255f,1);
	}

	/**
	 * Attach the nearest tool Detach the active tool if there is one.
	 */
	public void toggleATC() {
		if (sim.dhTool != null) {
			// we have a tool, release it.
			sim.removeTool();
			return;
		}

		// we have no tool. Look out into the world...
		World world = getWorld();
		if (world != null) {
			// Request from the world "is there a tool at the position of the end effector"?
			Vector3d target = new Vector3d();
			sim.getEndEffectorMatrix().get(target);
			List<PhysicalObject> list = world.findPhysicalObjectsNear(target, 10);

			// If there is a tool, attach to it.
			for( PhysicalObject po : list ) {
				if (po instanceof DHTool) {
					// probably the only one we'll find.
					sim.setTool((DHTool) po);
				}
			}
		}
	}

	@Override
	public ArrayList<JPanel> getContextPanel(RobotOverlord gui) {
		ArrayList<JPanel> list = super.getContextPanel(gui);

		// hide the dhrobot panel because we'll replace it with our own.
		sixi2Panel = new Sixi2Panel(gui,this);
		list.add(sixi2Panel);

		return list;
	}

	@Override
	public void render(GL2 gl2) {
		for( DHLink link : sim.links ) {
			link.refreshPoseMatrix();
		}

		IntBuffer depthFunc = IntBuffer.allocate(1);
		gl2.glGetIntegerv(GL2.GL_DEPTH_FUNC, depthFunc);
		boolean isLit = gl2.glIsEnabled(GL2.GL_LIGHTING);
		gl2.glDepthFunc(GL2.GL_ALWAYS);
		gl2.glDisable(GL2.GL_LIGHTING);

		// draw the drag ball
		if(isPicked && controlMode == ControlMode.RECORD) {
			ball.render(gl2);
		}

		if (isLit) gl2.glEnable(GL2.GL_LIGHTING);
		gl2.glDepthFunc(depthFunc.get(0));

		// then draw the target pose, aka the ghost.
		super.render(gl2);
	}

	@Override
	public void dataAvailable(NetworkConnection arg0,String data) {
		live.dataAvailable(arg0, data);

		super.dataAvailable(arg0, data);
	}

	/**
	 * move the finger tip of the arm if the InputManager says so. The direction and
	 * torque of the movement is controlled by a frame of reference.
	 *
	 * @return true if targetPose changes.
	 */
	public boolean driveFromKeyState(double dt) {
		ball.setSubjectMatrix(sim.links.get(sim.getNumLinks()-1).getPoseCumulative());
		ball.setCameraMatrix(getWorld().getCamera().getPose());

		boolean isDirty = false;

		ball.update(dt);

		// move the robot by dragging the ball in live mode
		if(controlMode==ControlMode.RECORD) {
			if (InputManager.isOn(InputManager.Source.MOUSE_LEFT)) {
				if(ball.isActivelyMoving()) {
					Matrix4d worldPose = new Matrix4d(ball.getResultMatrix());

					// The ghost only accepts poses in it's frame of reference.
					// so we have to account for the robot's pose in world space.
					//Matrix4d iRoot = new Matrix4d(this.matrix);
					//iRoot.invert();
					//worldPose.mul(iRoot);

					//System.out.println("Update begins");
					sim.setPoseIK(worldPose);
					//System.out.println("Update ends");
					//System.out.println(MatrixHelper.getPosition(worldPose));
					isDirty=true;
				}
			}
		}

		if (sim.dhTool != null) {
			isDirty |= sim.dhTool.directDrive();
		}


		if(InputManager.isReleased(InputManager.Source.KEY_SPACE)) {			reset();					}
		if(InputManager.isReleased(InputManager.Source.KEY_TAB  )) {			toggleControlMode();		}
		if(InputManager.isReleased(InputManager.Source.KEY_TILDE)) {			toggleOperatingMode();		}
		if(InputManager.isReleased(InputManager.Source.KEY_S    )) {			toggleSingleBlock();		}

		if(InputManager.isReleased(InputManager.Source.KEY_DELETE)
		|| InputManager.isOn(InputManager.Source.STICK_TRIANGLE)) {
			sim.set(live);
		}

		if(InputManager.isOn(InputManager.Source.KEY_LSHIFT)
		|| InputManager.isOn(InputManager.Source.KEY_RSHIFT)) {
			if(InputManager.isReleased(InputManager.Source.KEY_ENTER)
			|| InputManager.isReleased(InputManager.Source.KEY_RETURN)) {
				if(controlMode==ControlMode.PLAYBACK) {
					toggleCycleStart();
				}
			}
		} else {
			// shift off
			if(InputManager.isReleased(InputManager.Source.KEY_ENTER)
			|| InputManager.isReleased(InputManager.Source.KEY_RETURN)) {
				if(controlMode==ControlMode.RECORD) {
					System.out.println("setCommand");
					setCommand();
				}
			}
			if(InputManager.isReleased(InputManager.Source.KEY_PLUS)) {
				System.out.println("addCommand");
				addCommand();
			}
			if(InputManager.isReleased(InputManager.Source.KEY_DELETE)) {
				System.out.println("deleteCurrentCommand");
				deleteCurrentCommand();
			}
			if(InputManager.isReleased(InputManager.Source.KEY_BACKSPACE)) {
				System.out.println("deletePreviousCommand");
				recording.deletePreviousCommand();
			}
			if(InputManager.isReleased(InputManager.Source.KEY_LESSTHAN)) {
				System.out.println("PreviousCommand");
				if(recording.hasPrev()) {
			          String line=recording.prev();
			          sim.sendCommand(line);
				}
			}
			if(InputManager.isReleased(InputManager.Source.KEY_GREATERTHAN)) {
				System.out.println("NextCommand");
		        if(recording.hasNext()) {
		            String line=recording.next();
		            sim.sendCommand(line);
		        }
			}
		}

		return isDirty;
	}

	@Override
	public void update(double dt) {
		if (isPicked) {
			driveFromKeyState(dt);
		}

		Sixi2Model activeModel = (operatingMode == OperatingMode.LIVE) ? live : sim;
		if(activeModel.readyForCommands) {
			if(controlMode == ControlMode.RECORD) {
				if( operatingMode == OperatingMode.LIVE) {
					String line = sim.getCommand();
					System.out.println(controlMode + " " + operatingMode + " send command: "+line);
					activeModel.sendCommand(line);
				}
			} else {
				if(cycleStart && recording.hasNext()) {
					String line = recording.next();
					System.out.println(controlMode + " " + operatingMode + " send command: "+line);
					activeModel.sendCommand(line);
					if(singleBlock) {
						// one block at a time
						cycleStart=false;
					}
				} else {
					// no more recording, stop.
					cycleStart=false;
				}
			}
			// active model is updated when all children are updated
		}

		super.update(dt);
	}

	@Override
	public void pick() {
		isPicked = true;
	}

	@Override
	public void unPick() {
		isPicked = false;
	}

	protected boolean canSubjectRotateX() {
		return true;
	}

	protected boolean canSubjectRotateY() {
		return true;
	}

	protected boolean canSubjectRotateZ() {
		return true;
	}

	@Override
	public RobotKeyframe createKeyframe() {
		return null;
	}

	@Override
	public String getStatusMessage() {
		Matrix4d pose=sim.getEndEffectorMatrix();
		Matrix3d m = new Matrix3d();
		pose.get(m);
		Vector3d v = MatrixHelper.matrixToEuler(m);
		String message =
				"Base @ "+this.getPosition().toString() + " Tip @ "
				+" ("+StringHelper.formatDouble(pose.m03)
				+", "+StringHelper.formatDouble(pose.m13)
				+", "+StringHelper.formatDouble(pose.m23)
				+") R("+StringHelper.formatDouble(Math.toDegrees(v.x))
				+", "+StringHelper.formatDouble(Math.toDegrees(v.y))
				+", "+StringHelper.formatDouble(Math.toDegrees(v.z))
				+")";
		if(ball.isActivelyMoving) {
			message += " "+ball.getStatusMessage();
		}
		return message;
	}

	/**
	 * @return a list of cuboids, or null.
	 */
	@Override
	public ArrayList<Cuboid> getCuboidList() {

		// get a list of all cuboids
		ArrayList<Cuboid> cuboidList = new ArrayList<Cuboid>();

		sim.refreshPose();

		for( DHLink link : this.sim.links ) {
			if(link.getCuboid() != null ) {
				cuboidList.add(link.getCuboid());
			}
		}
		cuboidList.addAll(sim.dhTool.getCuboidList());

		return cuboidList;
	}

	@Override
	public void setPose(Matrix4d arg0) {
		super.setPose(arg0);
		live.refreshPose();
		sim.refreshPose();
	}

	public double getAcceleration() {
		if(operatingMode==OperatingMode.LIVE) {
			return live.getAcceleration();
		} else {
			return sim.getAcceleration();
		}
	}

	public void setAcceleration(double v) {
		if(operatingMode==OperatingMode.LIVE) {
			live.setAcceleration(v);
		}
		sim.setAcceleration(v);
	}

	public double getFeedRate() {
		if(operatingMode==OperatingMode.LIVE) {
			return live.getFeedrate();
		} else {
			return sim.getFeedrate();
		}
	}

	public void setFeedRate(double v) {
		if(operatingMode==OperatingMode.LIVE) {
			live.setFeedRate(v);
		}
		sim.setFeedRate(v);
	}

	/**
	 * Processes a single instruction meant for the robot.
	 * @param line command to send
	 * @return true if the command is sent to the robot.
	 */
	@Override
	public boolean sendCommand(String command) {
		if(operatingMode==OperatingMode.LIVE) {
			live.sendCommand(command);
		}
		sim.sendCommand(command);

		return true;
	}

	public String getCommand() {
		if(operatingMode==OperatingMode.LIVE) {
			return live.getCommand();
		} else {
			return sim.getCommand();
		}
	}

	public void reset() {
		recording.reset();
		singleBlock = false;
		cycleStart = false;
		m01Break = true;

		System.out.println("reset "+recording.getNumCommands());
	}

	public void toggleCycleStart() {
		cycleStart = !cycleStart;
		System.out.println("cycleStart="+(cycleStart?"on":"off"));
	}

	public void setCycleStart(boolean arg0) {
		cycleStart = arg0;
		System.out.println("cycleStart="+(cycleStart?"on":"off"));
	}

	public boolean isCycleStart() {
		return cycleStart;
	}

	public void toggleSingleBlock() {
		singleBlock = !singleBlock;
		System.out.println("singleBlock="+(singleBlock?"on":"off"));
	}

	public boolean isSingleBlock() {
		return singleBlock;
	}

	public void toggleM01Break() {
		m01Break = !m01Break;
		System.out.println("m01Break="+(m01Break?"on":"off"));
	}

	public boolean isM01Break() {
		return m01Break;
	}

	public ControlMode getControlMode() {
		return controlMode;
	}

	public void toggleControlMode() {
		controlMode = (controlMode==ControlMode.RECORD) ? ControlMode.PLAYBACK : ControlMode.RECORD;
		System.out.println("controlMode="+controlMode);

		if(controlMode==ControlMode.RECORD) {
			// move the joystick to match the simulated position?
		}

		reset();
	}

	public boolean isPicked() {
		return isPicked;
	}

	public OperatingMode getOperatingMode() {
		return operatingMode;
	}

	public void toggleOperatingMode() {
		operatingMode = (operatingMode==OperatingMode.LIVE) ? OperatingMode.SIM : OperatingMode.LIVE;

		System.out.println("operatingMode="+operatingMode);
	}

	public ArrayList<String> getCommandList() {
		return recording.getCommandList();
	}

	public void addCommand() {
		recording.addCommand(sim.getCommand());
	}
	public void deleteCurrentCommand() {
		recording.deleteCurrentCommand();
	}
	public void setCommand() {
		recording.setCommand(sim.getCommand());
	}

	public void loadRecording(String filename) {
		recording.loadRecording(filename);
	}

	public void saveRecording(String filename) {
		recording.saveRecording(filename);
	}

	@Override
	public void setDrawBoundingBox(boolean shouldDrawBoundingBox) {
		super.setDrawBoundingBox(shouldDrawBoundingBox);

		for( DHLink link : this.sim.links ) {
			link.setDrawBoundingBox(shouldDrawBoundingBox);
		}
	}
	@Override
	public void setDrawLocalOrigin(boolean shouldDrawLocalOrigin) {
		super.setDrawLocalOrigin(shouldDrawLocalOrigin);
		for( DHLink link : this.sim.links ) {
			link.setDrawLocalOrigin(shouldDrawLocalOrigin);
		}
	}
	@Override
	public void setDrawConnectionToChildren(boolean shouldDrawConnectionToChildren) {
		super.setDrawConnectionToChildren(shouldDrawConnectionToChildren);
		for( DHLink link : this.sim.links ) {
			link.setDrawConnectionToChildren(shouldDrawConnectionToChildren);
		}
	}

	public void openConnection() {
		live.openConnection();
	}

	public void goHome() {
	    // the home position
		DHKeyframe homeKey = sim.getIKSolver().createDHKeyframe();
		homeKey.fkValues[0]=0;
		homeKey.fkValues[1]=0;
		homeKey.fkValues[2]=0;
		homeKey.fkValues[3]=0;
		homeKey.fkValues[4]=20;
		homeKey.fkValues[5]=0;
		sim.setPoseFK(homeKey);
	}

	public void goRest() {
	    // set rest position
		DHKeyframe restKey = sim.getIKSolver().createDHKeyframe();
		restKey.fkValues[0]=0;
		restKey.fkValues[1]=-60;
		restKey.fkValues[2]=85;
		restKey.fkValues[3]=0;
		restKey.fkValues[4]=20;
		restKey.fkValues[5]=0;
		sim.setPoseFK(restKey);
	}

	public int setCommandIndex(int newIndex) {
		return recording.setCommandIndex(newIndex);
	}
}