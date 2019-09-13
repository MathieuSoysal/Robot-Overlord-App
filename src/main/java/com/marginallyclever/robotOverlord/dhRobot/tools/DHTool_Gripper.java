package com.marginallyclever.robotOverlord.dhRobot.tools;

import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

import javax.vecmath.Matrix3d;
import javax.vecmath.Matrix4d;
import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;

import com.jogamp.opengl.GL2;
import com.marginallyclever.convenience.StringHelper;
import com.marginallyclever.robotOverlord.InputManager;
import com.marginallyclever.robotOverlord.camera.Camera;
import com.marginallyclever.robotOverlord.dhRobot.DHTool;
import com.marginallyclever.robotOverlord.entity.Entity;
import com.marginallyclever.robotOverlord.light.Light;
import com.marginallyclever.robotOverlord.model.Model;
import com.marginallyclever.robotOverlord.model.ModelFactory;
import com.marginallyclever.robotOverlord.physicalObject.PhysicalObject;
import com.marginallyclever.robotOverlord.world.World;


/**
 * DHTool is a model that has a DHLink equivalence.
 * In this way it can perform transforms and have sub-links.
 * @author Dan Royer
 */
public class DHTool_Gripper extends DHTool {
	/**
	 * 
	 */
	private static final long serialVersionUID = 127023987907031123L;

	/**
	 * A PhysicalObject, if any, being held by the tool.  Assumes only one object can be held.
	 */
	private transient PhysicalObject subjectBeingHeld;
	private transient Matrix4d heldRelative;
	
	private double gripperServoAngle;
	public static final double ANGLE_MAX=55;
	public static final double ANGLE_MIN=10;
	
	private double interpolatePoseT;
	private double startT,endT;
	private double startD,endD;
	private double startR,endR;
	
	private Model linkage;
	private Model finger;
	
	private transient boolean wasGripping;
	
	public DHTool_Gripper() {
		super();
		setDisplayName("Gripper");
		dhLinkEquivalent.d=11.9082;  // cm
		dhLinkEquivalent.refreshPoseMatrix();
		
		heldRelative = new Matrix4d();
		
		gripperServoAngle=90;
		interpolatePoseT=1;
		startT=endT=gripperServoAngle;
		startD=endD=dhLinkEquivalent.d;
		startR=endR=dhLinkEquivalent.r;
		
		setFilename("/Sixi2/beerGripper/base.stl");
		setScale(0.1f);
		
		setPosition(new Vector3d(0.91,0,4.1));

		Matrix3d r = new Matrix3d();
		r.setIdentity();
		r.rotX(Math.toRadians(180));
		Matrix3d r2 = new Matrix3d();
		r2.setIdentity();
		r2.rotZ(Math.toRadians(90));
		r.mul(r2);
		this.setRotation(r);
		
		wasGripping=false;
	}
	
	public void render(GL2 gl2) {
		super.render(gl2);
		
		if(linkage==null) {
			try {
				linkage= ModelFactory.createModelFromFilename("/Sixi2/beerGripper/linkage.stl",0.1f);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		if(finger==null) {
			try {
				finger= ModelFactory.createModelFromFilename("/Sixi2/beerGripper/finger.stl",0.1f);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}			
		}

		//render gripper model
		if(model==null) return;
		if(linkage==null) return;
		if(finger==null) return;

		material.render(gl2);
		
		gl2.glPushMatrix();
		gl2.glRotated(90, 0, 0, 1);
			// model rendered by super
		
			gl2.glPushMatrix();
			gl2.glTranslated(2.7/2, 0, 4.1);
			gl2.glRotated(this.gripperServoAngle, 0, 1, 0);
			linkage.render(gl2);
			gl2.glPopMatrix();
			
			gl2.glPushMatrix();
			gl2.glTranslated(1.1/2, 0, 5.9575);
			gl2.glRotated(this.gripperServoAngle, 0, 1, 0);
			linkage.render(gl2);
			gl2.glPopMatrix();
			
			gl2.glPushMatrix();
			gl2.glTranslated(-2.7/2, 0, 4.1);
			gl2.glRotated(-this.gripperServoAngle, 0, 1, 0);
			linkage.render(gl2);
			gl2.glPopMatrix();
			
			gl2.glPushMatrix();
			gl2.glTranslated(-1.1/2, 0, 5.9575);
			gl2.glRotated(-this.gripperServoAngle, 0, 1, 0);
			linkage.render(gl2);
			
			gl2.glPopMatrix();

			double c=Math.cos(Math.toRadians(gripperServoAngle));
			double s=Math.sin(Math.toRadians(gripperServoAngle));
			gl2.glPushMatrix();
			gl2.glTranslated(-2.7/2-s*4.1, 0, 4.1+c*4.1);
			gl2.glScaled(1,1,-1);
			//gl2.glTranslated(s*4.1, 2.7/2, 4.1+c*4.1);
			finger.render(gl2);
			gl2.glPopMatrix();
			
			gl2.glPushMatrix();
			gl2.glTranslated(2.7/2+s*4.1, 0, 4.1+c*4.1);
			gl2.glScaled(-1,1,-1);
			finger.render(gl2);
			gl2.glPopMatrix();
		
		gl2.glPopMatrix();
	}

	/**
	 * Read HID device to move target pose.  Currently hard-coded to PS4 joystick values. 
	 * @return true if targetPose changes.
	 */
	@Override
	public boolean directDrive() {
		boolean isDirty=false;
		final double scaleGrip=1.8;
		
		if(InputManager.isOn(InputManager.STICK_CIRCLE) && !wasGripping) {
			wasGripping=true;
			// grab release
			if(subjectBeingHeld==null) {
				// get the object at the targetPos
				// attach it to this object
				//System.out.println("Grab");
				subjectBeingHeld = findObjectNear(10);
				if(subjectBeingHeld != null) {
					// find the relative transform so that its angle doesn't change?
					Matrix4d held = new Matrix4d(subjectBeingHeld.getMatrix());
					Matrix4d iEnd = new Matrix4d(dhLinkEquivalent.poseCumulative);
					iEnd.invert();
					heldRelative.mul(iEnd,held);
					//heldRelative.transform(dp);
					//heldRelative.m03=dp.x;
					//heldRelative.m13=dp.y;
					//heldRelative.m23=dp.z;
				}
			} else {
				//System.out.println("Release");
				// release the object being held, somehow.
				subjectBeingHeld=null;
			}
		}
		if(InputManager.isOff(InputManager.STICK_CIRCLE)) wasGripping=false;
		
        if(InputManager.isOn(InputManager.STICK_OPTIONS)) {
			if(gripperServoAngle<ANGLE_MAX) {
				gripperServoAngle+=scaleGrip;
				if(gripperServoAngle>ANGLE_MAX) gripperServoAngle=ANGLE_MAX;
				isDirty=true;
			}
        }
        if(InputManager.isOn(InputManager.STICK_SHARE)) {
			if(gripperServoAngle>ANGLE_MIN) {
				gripperServoAngle-=scaleGrip;
				if(gripperServoAngle<ANGLE_MIN) gripperServoAngle=ANGLE_MIN;
				isDirty=true;
			}
        }

        return isDirty;
	}
	
	public void refreshPose(Matrix4d endMatrix) {
		super.refreshPose(endMatrix);
		if(subjectBeingHeld!=null) {
			Matrix4d finalPose = new Matrix4d();
			finalPose.mul(endMatrix,heldRelative);
			subjectBeingHeld.setMatrix(finalPose);
		}
	}
	
	public PhysicalObject findObjectNear(double radius) {
		//System.out.println("Finding world...");
		Entity p=parent;
		while(p!=null) {
			//System.out.println("\t"+p.getDisplayName());
			if(p instanceof World) {
				break;
			}
			p=p.getParent();
		}
		if(p==null || !(p instanceof World)) {
			//System.out.println("World not found");
			return null;
		}

		//System.out.println("Asking world...");
		
		// World, please tell me who is near my grab point.
		World world = (World)p;
		Point3d target = new Point3d(dhLinkEquivalent.poseCumulative.m03,
									 dhLinkEquivalent.poseCumulative.m13,
									 dhLinkEquivalent.poseCumulative.m23);
		List<PhysicalObject> list = world.findPhysicalObjectsNear(target,radius);

		// Check the list for anything that is not this tool and not this robot.
		Iterator<PhysicalObject> iter = list.iterator();
		while(iter.hasNext()) {
			PhysicalObject po = iter.next();
			if(po==parent) continue;
			if(po==this) continue;
			if(po instanceof Light) continue;
			if(po instanceof Camera) continue;
			//System.out.println("  selected "+po.getDisplayName());
			return po;  // found!
		}
		return null;
	}

	public String generateGCode() {
		String message = " T"+StringHelper.formatDouble(this.gripperServoAngle)
						+ " R"+StringHelper.formatDouble(this.dhLinkEquivalent.r)
						+ " S"+StringHelper.formatDouble(this.dhLinkEquivalent.d);
		return message;
	}
	
	public void parseGCode(String str) {
		StringTokenizer tok = new StringTokenizer(str);
		while(tok.hasMoreTokens()) {
			String token = tok.nextToken();
			try {
				if(token.startsWith("T")) {
					startT = gripperServoAngle;
					endT = Double.parseDouble(token.substring(1));
				}
				if(token.startsWith("R")) {
					startR = dhLinkEquivalent.r;
					endR = Double.parseDouble(token.substring(1));
				}
				if(token.startsWith("S")) {
					startD = dhLinkEquivalent.d;
					endD = Double.parseDouble(token.substring(1));
				}
			} catch(NumberFormatException e) {
				e.printStackTrace();
			}
		}
		interpolatePoseT=0;
	}
	
	public void interpolate(double dt) {
		if(interpolatePoseT<1) {
			interpolatePoseT+=dt;
			if(interpolatePoseT>=1) {
				interpolatePoseT=1;
			}
			gripperServoAngle  = (endT-startT)*interpolatePoseT + startT;
			dhLinkEquivalent.r = (endR-startR)*interpolatePoseT + startR;
			dhLinkEquivalent.d = (endD-startD)*interpolatePoseT + startD;
			dhLinkEquivalent.refreshPoseMatrix();
		}
	}
}