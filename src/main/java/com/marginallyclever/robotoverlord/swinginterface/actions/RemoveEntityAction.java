package com.marginallyclever.robotoverlord.swinginterface.actions;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;

import javax.swing.AbstractAction;
import javax.swing.Action;
import com.marginallyclever.convenience.log.Log;
import com.marginallyclever.robotoverlord.Entity;
import com.marginallyclever.robotoverlord.Removable;
import com.marginallyclever.robotoverlord.RobotOverlord;
import com.marginallyclever.robotoverlord.swinginterface.UndoSystem;
import com.marginallyclever.robotoverlord.swinginterface.translator.Translator;
import com.marginallyclever.robotoverlord.swinginterface.undoableedits.RemoveEdit;

/**
 * @author Dan Royer
 */
public class RemoveEntityAction extends AbstractAction {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	protected RobotOverlord ro;
	
	public RemoveEntityAction(RobotOverlord ro) {
		super(Translator.get("Remove Entity"));
        putValue(AbstractAction.SHORT_DESCRIPTION, Translator.get("Remove the selected entity from the world."));
        //putValue(AbstractAction.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, ActionEvent.CTRL_MASK));
        putValue(Action.MNEMONIC_KEY, KeyEvent.VK_DELETE);
        
		this.ro = ro;
	}

	@Override
	public void actionPerformed(ActionEvent event) {
		ArrayList<Entity> entityList = new ArrayList<Entity>(ro.getSelectedEntities());
		if(entityList.size()==0) {
			Log.error("RemoveEntity with no entity selected.");
			return;
		}
		for(Entity e : entityList) {
			if(e instanceof Removable) {
				UndoSystem.addEvent(this,new RemoveEdit(ro,e));
			} else {
				Log.error("Entity "+e.getFullPath()+" is not a RemovableEntity.");
			}
		}			
	}
}