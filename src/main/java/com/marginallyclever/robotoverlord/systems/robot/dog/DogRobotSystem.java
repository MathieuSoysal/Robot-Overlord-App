package com.marginallyclever.robotoverlord.systems.robot.dog;

import com.marginallyclever.robotoverlord.Component;
import com.marginallyclever.robotoverlord.EntityManager;
import com.marginallyclever.robotoverlord.components.demo.DogRobotComponent;
import com.marginallyclever.robotoverlord.swinginterface.componentmanagerpanel.ComponentPanelFactory;
import com.marginallyclever.robotoverlord.swinginterface.componentmanagerpanel.ViewElementButton;
import com.marginallyclever.robotoverlord.systems.EntitySystem;
import com.marginallyclever.robotoverlord.systems.EntitySystemUtils;
import com.marginallyclever.robotoverlord.systems.robot.robotarm.EditArm6Panel;

import javax.swing.*;

/**
 * A system to manage robot dogs.
 *
 * @author Dan Royer
 * @since 2.5.7
 */
public class DogRobotSystem implements EntitySystem {
    private final EntityManager entityManager;

    public DogRobotSystem(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    /**
     * Get the Swing view of this component.
     *
     * @param view      the factory to use to create the panel
     * @param component the component to visualize
     */
    @Override
    public void decorate(ComponentPanelFactory view, Component component) {
        if( component instanceof DogRobotComponent) decorateDog(view,component);
    }

    public void decorateDog(ComponentPanelFactory view,Component component) {
        DogRobotComponent dog = (DogRobotComponent)component;

        ViewElementButton bMake = view.addButton("Edit Dog");
        bMake.addActionEventListener((evt)-> makeDog(bMake,dog,"Edit Dog"));

        view.add(dog.standingRadius);
        view.add(dog.standingHeight);
        view.add(dog.turningStrideLength);
        view.add(dog.strideLength);
        view.add(dog.strideHeight);

        view.addComboBox(dog.modeSelector, DogRobotComponent.MODE_NAMES);
        view.add(dog.speedScale);
    }

    private void makeDog(JComponent parent, DogRobotComponent dog,String title) {
        EntitySystemUtils.makePanel(new EditDogPanel(dog.getEntity(), entityManager), parent,title);
    }

    /**
     * Update the system over time.
     *
     * @param dt the time step in seconds.
     */
    @Override
    public void update(double dt) {

    }
}