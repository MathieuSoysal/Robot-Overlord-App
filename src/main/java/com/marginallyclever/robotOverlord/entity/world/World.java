package com.marginallyclever.robotOverlord.entity.world;

import java.util.ArrayList;
import java.util.List;

import javax.swing.JPanel;
import javax.vecmath.Matrix3d;
import javax.vecmath.Matrix4d;
import javax.vecmath.Vector3d;


import com.jogamp.opengl.GL2;
import com.jogamp.opengl.util.texture.Texture;
import com.jogamp.opengl.util.texture.TextureIO;
import com.marginallyclever.convenience.Cuboid;
import com.marginallyclever.convenience.FileAccess;
import com.marginallyclever.convenience.IntersectionTester;
import com.marginallyclever.convenience.PrimitiveSolids;
import com.marginallyclever.robotOverlord.RobotOverlord;
import com.marginallyclever.robotOverlord.engine.ViewCube;
import com.marginallyclever.robotOverlord.entity.Entity;
import com.marginallyclever.robotOverlord.entity.EntityPanel;
import com.marginallyclever.robotOverlord.entity.camera.Camera;
import com.marginallyclever.robotOverlord.entity.gridEntity.GridEntity;
import com.marginallyclever.robotOverlord.entity.light.Light;
import com.marginallyclever.robotOverlord.entity.physicalObject.PhysicalObject;
import com.marginallyclever.robotOverlord.entity.physicalObject.boxObject.BoxObject;
import com.marginallyclever.robotOverlord.entity.robot.sixi2.Sixi2;

/**
 * Container for all the visible objects in a scene.
 * @author danroyer
 */
public class World extends Entity {
	public final static Matrix4d pose = new Matrix4d();
	// TODO lose these junk vectors that don't match assumptions, anyhow.
	public final static Vector3d forward = new Vector3d(0,0,1);
	public final static Vector3d right = new Vector3d(1,0,0);
	public final static Vector3d up = new Vector3d(0,1,0);
	
	protected Camera camera = new Camera();
	//protected CameraMount freeCamera;

	// background, if any
	protected transient boolean areSkyboxTexturesLoaded;
	protected transient Texture skyboxtextureXPos,
								skyboxtextureXNeg,
								skyboxtextureYPos,
								skyboxtextureYNeg,
								skyboxtextureZPos,
								skyboxtextureZNeg;

	// ray picking
	// TODO probably doesn't belong here, it's per-user?  per-camera?
	protected transient Vector3d pickForward;
	protected transient Vector3d pickRight;
	protected transient Vector3d pickUp;
	protected transient Vector3d pickRay;
	
	protected transient boolean isSetup;

	// The box in the top right of the user view that shows your orientation in the world.
	// TODO probably doesn't belong here, it's per-user?  per-camera?
	protected transient ViewCube viewCube;
	
	protected transient WorldPanel worldPanel;
	
	public World() {
		super();
		
		isSetup = false;
		
		pose.setIdentity();
		
		setName("World");
		
		areSkyboxTexturesLoaded=false;
		pickForward=new Vector3d();
		pickRight=new Vector3d();
		pickUp=new Vector3d();
		pickRay=new Vector3d();
	}
	
	public void createDefaultWorld() {
		// adjust grid
		GridEntity grid;
		addChild(grid = new GridEntity());
		grid.width = 130;
		grid.height = 70;
		grid.setPosition(new Vector3d(30,0,-0.5));
		
		// adjust default camera
		addChild(camera);
		camera.setPosition(new Vector3d(0,-100,65));
		//camera.setPan(52);
		camera.setTilt(76);

		// add some lights
    	Light light;
    	 
		addChild(light = new Light());
		light.setName("light ambient");
    	light.index=0;
    	light.setPosition(new Vector3d(0,0,30));
    	light.setAmbient(2.55f,2.55f,2.51f,1);

		addChild(light = new Light());
		light.setName("light1");
    	light.index=1;
    	light.setPosition(new Vector3d(60,-60,160));
    	light.setDiffuse (80,80,80,1);

		// add some collision bounds
		BoxObject box;
		addChild(box = new BoxObject());
		box.setName("Front wall");
		box.setSize(160,100,1);
		box.setPosition(new Vector3d(30,40,0));
		box.getMaterial().setDiffuseColor(117f/255f,169f/255f,207f/255f,1f);

		addChild(box = new BoxObject());
		box.setName("Back wall");
		box.setSize(100,100,1);
		box.setPosition(new Vector3d(-50,-10,0));
		box.setRotation(new Vector3d(0, 0, Math.toRadians(-90)));
		box.getMaterial().setDiffuseColor(117f/255f,169f/255f,207f/255f,1f);

		addChild(box = new BoxObject());
		box.setName("Table");
		box.setSize(150,1,80);
		box.setPosition(new Vector3d(30,0,-2.5));
    	
    	// add a sixi robot
		Sixi2 sixi2=new Sixi2();
		addChild(sixi2);
		//sixi2.setPosition(new Vector3d(78,-25,0));
		Matrix3d m=new Matrix3d();
		m.setIdentity();
		//m.rotZ(Math.toRadians(-90));
		sixi2.setRotation(m);
	}
	
	
	/**
	 * Get the {@link EntityPanel} for this class' superclass, then the EntityPanel for this class, and so on.
	 * 
	 * @param gui the main application instance.
	 * @return the list of EntityPanels 
	 */
	public ArrayList<JPanel> getContextPanels(RobotOverlord gui) {
		ArrayList<JPanel> list = new ArrayList<JPanel>();
		
		worldPanel = new WorldPanel(gui,this);
		list.add(worldPanel);

		return list;
	}
	

	protected void setup() {
		viewCube = new ViewCube();

		loadSkyboxTextures();
    }
    
	
	void loadSkyboxTextures() {
		if(areSkyboxTexturesLoaded) return;
		try {
			skyboxtextureXPos = TextureIO.newTexture(FileAccess.open("/images/cube-x-pos.png"), false, "png");
			skyboxtextureXNeg = TextureIO.newTexture(FileAccess.open("/images/cube-x-neg.png"), false, "png");
			skyboxtextureYPos = TextureIO.newTexture(FileAccess.open("/images/cube-y-pos.png"), false, "png");
			skyboxtextureYNeg = TextureIO.newTexture(FileAccess.open("/images/cube-y-neg.png"), false, "png");
			skyboxtextureZPos = TextureIO.newTexture(FileAccess.open("/images/cube-z-pos.png"), false, "png");
			skyboxtextureZNeg = TextureIO.newTexture(FileAccess.open("/images/cube-z-neg.png"), false, "png");
			//System.out.println(">>> All textures loaded OK");
			areSkyboxTexturesLoaded=true;
		}
		catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public void update(double dt) {
		if(!isSetup) {
			setup();
			isSetup=true;
		}

		// calls update on all entities and sub-entities
		super.update(dt);
	}
	
	/**
	 * @param gl2 render context
	 */
	public void render(GL2 gl2) {
		// Clear the screen and depth buffer

		// background color
    	//gl2.glClearColor(212.0f/255.0f, 233.0f/255.0f, 255.0f/255.0f, 1.0f);
		gl2.glClearColor(0.85f,0.85f,0.85f,1.0f);
    	// Special handling for the case where the GLJPanel is translucent
        // and wants to be composited with other Java 2D content
		/*
        if (GLProfile.isAWTAvailable() &&
            (gl2 instanceof com.jogamp.opengl.awt.GLJPanel) &&
            !((com.jogamp.opengl.awt.GLJPanel) gl2).isOpaque() &&
            ((com.jogamp.opengl.awt.GLJPanel) gl2).shouldPreserveColorBufferIfTranslucent()) {
          gl2.glClear(GL2.GL_DEPTH_BUFFER_BIT);
        } else {
          gl2.glClear(GL2.GL_DEPTH_BUFFER_BIT);
        }*/
    	gl2.glDrawBuffer(GL2.GL_BACK);
        gl2.glClear(GL2.GL_DEPTH_BUFFER_BIT | GL2.GL_COLOR_BUFFER_BIT);
		gl2.glCullFace(GL2.GL_BACK);

		// DRAW THE WORLD
        gl2.glMatrixMode(GL2.GL_MODELVIEW);
		gl2.glLoadIdentity();

		gl2.glPushMatrix();
			camera.render(gl2);
			
			gl2.glDisable(GL2.GL_LIGHTING);

			//drawSkyBox(gl2);
			
			// lights
			for( Entity obj : children ) {
				if(obj instanceof Light) {
					obj.render(gl2);
				}
			}

			// draw!
			for( Entity obj : children ) {
				if(obj instanceof Light) continue;
				if(obj instanceof Camera) continue;
				
				gl2.glPushName(obj.getPickName());
				obj.render(gl2);
				gl2.glPopName();
			}
	
			showPickingTest(gl2);

		gl2.glPopMatrix();
	
		// DRAW THE HUD
		gl2.glPushMatrix();
			camera.render(gl2);
			
	        gl2.glMatrixMode(GL2.GL_MODELVIEW);
			gl2.glLoadIdentity();
	
			viewCube.render(gl2,getCamera());
		gl2.glPopMatrix();
	}

	
	protected void showPickingTest(GL2 gl2) {
		if(pickForward == null) return;
		
		gl2.glPushMatrix();
		gl2.glDisable(GL2.GL_LIGHTING);

		Vector3d forward = new Vector3d();
		forward.set(pickForward);
		forward.scale(10);
		forward.sub(camera.getPosition());
		gl2.glColor3f(1,0,0);
		PrimitiveSolids.drawStar(gl2, forward);
		
		forward.set(pickForward);
		forward.scale(10);
		forward.add(pickRight);
		forward.sub(camera.getPosition());
		gl2.glColor3f(0,1,0);
		PrimitiveSolids.drawStar(gl2, forward);
		
		forward.set(pickForward);
		forward.scale(10);
		forward.add(pickUp);
		forward.sub(camera.getPosition());
		gl2.glColor3f(0,0,1);
		PrimitiveSolids.drawStar(gl2, forward);
		
		forward.set(pickRay);
		forward.scale(10);
		forward.sub(camera.getPosition());
		gl2.glColor3f(1,1,0);
		PrimitiveSolids.drawStar(gl2, forward);
		
		gl2.glEnable(GL2.GL_LIGHTING);
		gl2.glPopMatrix();
	}
	

	// Draw background
	protected void drawSkyBox(GL2 gl2) {
		if(!areSkyboxTexturesLoaded) return;

        //gl2.glDisable(GL2.GL_CULL_FACE);
		
		gl2.glDisable(GL2.GL_DEPTH_TEST);
		gl2.glDisable(GL2.GL_LIGHTING);
		gl2.glDisable(GL2.GL_COLOR_MATERIAL);
		gl2.glEnable(GL2.GL_TEXTURE_2D);
		gl2.glPushMatrix();
			gl2.glColor3f(1, 1, 1);
			Vector3d p = camera.getPosition();
			gl2.glTranslated(-p.x,-p.y,-p.z);

			skyboxtextureXPos.bind(gl2);
			gl2.glBegin(GL2.GL_TRIANGLE_FAN);
				gl2.glTexCoord2d(0,1);  gl2.glVertex3d(10, 10, 10);
				gl2.glTexCoord2d(1,1);  gl2.glVertex3d(10, -10, 10);
				gl2.glTexCoord2d(1,0);  gl2.glVertex3d(10, -10, -10);
				gl2.glTexCoord2d(0,0);  gl2.glVertex3d(10, 10, -10);
			gl2.glEnd();

			skyboxtextureXNeg.bind(gl2);
			gl2.glBegin(GL2.GL_TRIANGLE_FAN);
				gl2.glTexCoord2d(0,1);  gl2.glVertex3d(-10, -10, 10);
				gl2.glTexCoord2d(1,1);  gl2.glVertex3d(-10, 10, 10);
				gl2.glTexCoord2d(1,0);  gl2.glVertex3d(-10, 10, -10);
				gl2.glTexCoord2d(0,0);  gl2.glVertex3d(-10, -10, -10);
			gl2.glEnd();

			skyboxtextureYPos.bind(gl2);
			gl2.glBegin(GL2.GL_TRIANGLE_FAN);
				gl2.glTexCoord2d(0,1);  gl2.glVertex3d(-10, 10, 10);
				gl2.glTexCoord2d(1,1);  gl2.glVertex3d(10, 10, 10);
				gl2.glTexCoord2d(1,0);  gl2.glVertex3d(10, 10, -10);
				gl2.glTexCoord2d(0,0);  gl2.glVertex3d(-10, 10, -10);
			gl2.glEnd();

			skyboxtextureYNeg.bind(gl2);
			gl2.glBegin(GL2.GL_TRIANGLE_FAN);
				gl2.glTexCoord2d(0,1);  gl2.glVertex3d(10, -10, 10);
				gl2.glTexCoord2d(1,1);  gl2.glVertex3d(-10, -10, 10);
				gl2.glTexCoord2d(1,0);  gl2.glVertex3d(-10, -10, -10);
				gl2.glTexCoord2d(0,0);  gl2.glVertex3d(10, -10, -10);
			gl2.glEnd();

			skyboxtextureZPos.bind(gl2);
			gl2.glBegin(GL2.GL_TRIANGLE_FAN);
				gl2.glTexCoord2d(0,0);  gl2.glVertex3d(-10, 10, 10);
				gl2.glTexCoord2d(1,0);  gl2.glVertex3d( 10, 10, 10);
				gl2.glTexCoord2d(1,1);  gl2.glVertex3d( 10,-10, 10);
				gl2.glTexCoord2d(0,1);  gl2.glVertex3d(-10,-10, 10);
			gl2.glEnd();

			skyboxtextureZNeg.bind(gl2);
			gl2.glBegin(GL2.GL_TRIANGLE_FAN);
				gl2.glTexCoord2d(0,0);  gl2.glVertex3d(-10,-10, -10);
				gl2.glTexCoord2d(1,0);  gl2.glVertex3d( 10,-10, -10);
				gl2.glTexCoord2d(1,1);  gl2.glVertex3d( 10, 10, -10);
				gl2.glTexCoord2d(0,1);  gl2.glVertex3d(-10, 10, -10);
			gl2.glEnd();
			
		gl2.glPopMatrix();
		gl2.glEnable(GL2.GL_DEPTH_TEST);
        //gl2.glEnable(GL2.GL_CULL_FACE);
	}

	
	public Entity pickObjectWithName(int pickName) {
		Entity newObject=null;
		if(pickName==0) {
			// Hit nothing!  Default to camera controls
			newObject=camera;
		} else {
			// scan all objects in world to find the one with the pickName.
			for( Entity obj : children ) {
				if( obj.hasPickName(pickName) ) {
					// found!
					newObject=obj;
					break;
				}
			}
		}
		
		return newObject;
	}
	
	
	@Deprecated
	public List<String> namesOfAllObjects() {
		ArrayList<String> list = new ArrayList<String>();

		for( Entity obj : children ) {
			String s = obj.getName();
			list.add(s);
		}
		
		return list;
	}
	
	public Camera getCamera() {
		return camera;
	}
		
	/**
	 * Find all Entities within epsilon mm of pose.
	 * TODO Much optimization could be done here to reduce the search time.
	 * @param target the center of the cube around which to search.   
	 * @param radius the maximum distance to search for entities.
	 * @return a list of found PhysicalObjects
	 */
	public List<PhysicalObject> findPhysicalObjectsNear(Vector3d target,double radius) {
		radius/=2;
		
		//System.out.println("Finding within "+epsilon+" of " + target);
		List<PhysicalObject> found = new ArrayList<PhysicalObject>();
		
		// check all children
		for( Entity e : children ) {
			if(e instanceof PhysicalObject) {
				// is physical, therefore has position
				PhysicalObject po = (PhysicalObject)e;
				//System.out.println("  Checking "+po.getDisplayName()+" at "+pop);
				Vector3d pop = new Vector3d();
				pop.sub(po.getPosition(),target);
				if(pop.length()<=radius) {
					//System.out.println("  in range!");
					// in range!
					found.add(po);
				}
			}
		}
		
		return found;
	}


	public static Matrix4d getPose() {
		return new Matrix4d(pose);
	}


	/**
	 * @param listA all the cuboids being tested against the world.
	 * @param ignoreList all the entities in the world to ignore.
	 * @return true if any cuboid in the cuboidList intersects any cuboid in the world.
	 */
	public boolean collisionTest(PhysicalObject a) {
		ArrayList<Cuboid> listA = a.getCuboidList();
		
		// check all children
		for( Entity b : children ) {
			// we do not test collide with self.  filter for all physical objects EXCEPT a.
			if( !( b instanceof PhysicalObject ) || b==a ) continue;

			ArrayList<Cuboid> listB = ((PhysicalObject)b).getCuboidList();
			if( listB == null ) continue;
			
			// now we have both lists, test them against each other.
			for( Cuboid cuboidA : listA ) {
				for( Cuboid cuboidB : listB ) {
					if( IntersectionTester.cuboidCuboid(cuboidA,cuboidB) ) {
						System.out.println("Collision between "+
							a.getName()+"."+listA.indexOf(cuboidA)+
							" and "+
							b.getName()+"."+listB.indexOf(cuboidB));
						return true;
					}
				}
			}
		}

		// no intersection
		return false;
	}
	
	public WorldPanel getWorldPanel() {
		return worldPanel;
	}
}