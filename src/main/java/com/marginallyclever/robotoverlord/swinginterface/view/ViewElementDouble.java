package com.marginallyclever.robotoverlord.swinginterface.view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.concurrent.locks.ReentrantLock;

import javax.swing.AbstractAction;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.undo.AbstractUndoableEdit;

import com.marginallyclever.convenience.StringHelper;
import com.marginallyclever.robotoverlord.RobotOverlord;
import com.marginallyclever.robotoverlord.swinginterface.UndoSystem;
import com.marginallyclever.robotoverlord.swinginterface.undoableedits.DoubleEdit;
import com.marginallyclever.robotoverlord.uiexposedtypes.DoubleEntity;

/**
 * Panel to alter a number parameter.  There is no way at present to limit the input options (range, step size, etc)
 * @author Dan Royer
 *
 */
public class ViewElementDouble extends ViewElement implements DocumentListener, PropertyChangeListener {
	/**
	 * 
	 */
	private static final long serialVersionUID = 6763069709436830626L;
	private JTextField field;
	private DoubleEntity e;
	private ReentrantLock lock = new ReentrantLock();
	
	public ViewElementDouble(RobotOverlord ro,DoubleEntity e) {
		super(ro);
		this.e=e;
		
		e.addPropertyChangeListener(this);
		
		field = new FocusTextField(8);
		field.addActionListener(new AbstractAction() {
			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent e) {
				conditionalChange();
			}
		});
		field.addFocusListener(new FocusListener() {
			@Override
			public void focusGained(FocusEvent e) {}

			@Override
			public void focusLost(FocusEvent e) {
				conditionalChange();
			}
		});
		field.getDocument().addDocumentListener(this);
		field.setHorizontalAlignment(SwingConstants.RIGHT);
		field.setText(StringHelper.formatDouble(e.get()));
		field.addFocusListener(this);

		JLabel label=new JLabel(e.getName(),JLabel.LEADING);
		label.setLabelFor(field);
		
		//this.setBorder(new LineBorder(Color.RED));
		this.setLayout(new BorderLayout());
		this.add(label,BorderLayout.LINE_START);
		this.add(field,BorderLayout.LINE_END);
	}
	
	protected void conditionalChange() {
		double newNumber;
		
		try {
			newNumber = Double.valueOf(field.getText());
		} catch(NumberFormatException e1) {
			field.setForeground(Color.RED);
			return;
		}

		field.setForeground(UIManager.getColor("Textfield.foreground"));
		
		if(lock.isLocked()) return;
		lock.lock();

		if(newNumber != e.get()) {
			AbstractUndoableEdit event = new DoubleEdit(e, newNumber);
			UndoSystem.addEvent(this,event);
		}
		
		lock.unlock();
	}
	
	protected void validateField() {
		try {
			Double.valueOf(field.getText());
			field.setForeground(UIManager.getColor("Textfield.foreground"));
		} catch(NumberFormatException e1) {
			field.setForeground(Color.RED);
		}
	}

	/**
	 * panel changed, poke entity
	 */
	@Override
	public void changedUpdate(DocumentEvent arg0) {
		validateField();
	}
	
	@Override
	public void insertUpdate(DocumentEvent arg0) {
		validateField();
	}

	@Override
	public void removeUpdate(DocumentEvent arg0) {
		validateField();
	}
	
	@Override
	public void setReadOnly(boolean arg0) {
		field.setEnabled(!arg0);
	}

	@Override
	public void propertyChange(PropertyChangeEvent evt) {		
		if(lock.isLocked()) return;
		lock.lock();
		field.setText(StringHelper.formatDouble((Double)evt.getNewValue()));
		lock.unlock();		
	}
}