package com.marginallyclever.robotOverlord.entity.modelInWorld;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.Iterator;
import java.util.ServiceLoader;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.filechooser.FileNameExtensionFilter;

import com.marginallyclever.convenience.PanelHelper;
import com.marginallyclever.robotOverlord.RobotOverlord;
import com.marginallyclever.robotOverlord.engine.model.Model;
import com.marginallyclever.robotOverlord.engine.model.ModelLoadAndSave;
import com.marginallyclever.robotOverlord.engine.undoRedo.commands.UserCommandSelectFile;
import com.marginallyclever.robotOverlord.engine.undoRedo.commands.UserCommandSelectNumber;
import com.marginallyclever.robotOverlord.engine.undoRedo.commands.UserCommandSelectVector3d;
import com.marginallyclever.robotOverlord.entity.Entity;

public class ModelInWorldPanel extends JPanel implements ChangeListener {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private ModelInWorld model;
	private UserCommandSelectFile userCommandSelectFile;
	private UserCommandSelectNumber setScale;
	private UserCommandSelectVector3d setOrigin;
	private UserCommandSelectVector3d setRotation;
	
	public ModelInWorldPanel(RobotOverlord gui,ModelInWorld model) {
		super();
		
		this.model = model;

		this.setName("Model");
		this.setBorder(new EmptyBorder(5,5,5,5));
		this.setLayout(new GridBagLayout());

		GridBagConstraints con1 = PanelHelper.getDefaultGridBagConstraints();

		userCommandSelectFile = new UserCommandSelectFile(gui,"Filename",model.getFilename());
		// Find all the serviceLoaders for loading files.
		ServiceLoader<ModelLoadAndSave> loaders = ServiceLoader.load(ModelLoadAndSave.class);
		Iterator<ModelLoadAndSave> i = loaders.iterator();
		while(i.hasNext()) {
			ModelLoadAndSave loader = i.next();
			FileNameExtensionFilter filter = new FileNameExtensionFilter(loader.getEnglishName(), loader.getValidExtensions());
			userCommandSelectFile.addChoosableFileFilter(filter);
		}
		userCommandSelectFile.addChangeListener(this);
		con1.gridy++;
		this.add(userCommandSelectFile,con1);

		con1.gridy++;
		setScale = new UserCommandSelectNumber(gui,"Scale",model.getModelScale());
		setScale.addChangeListener(this);
		this.add(setScale,con1);

		con1.gridy++;
		setOrigin = new UserCommandSelectVector3d(gui,"Origin",model.getModelOrigin());
		setOrigin.addChangeListener(this);
		this.add(setOrigin,con1);

		con1.gridy++;
		setRotation = new UserCommandSelectVector3d(gui,"Rotation",model.getModelRotation());
		setRotation.addChangeListener(this);
		this.add(setRotation,con1);

		Model m = this.model.getModel();
		if(m!=null) {
			con1.gridy++;
			this.add(new JLabel("Triangles: "+m.getNumTriangles(),SwingConstants.LEFT),con1);
			con1.gridy++;
			this.add(new JLabel("Normals: "+(m.hasNormals?"yes":"no"),SwingConstants.LEFT),con1);
			con1.gridy++;
			this.add(new JLabel("Colors: "+(m.hasColors?"yes":"no"),SwingConstants.LEFT),con1);
			con1.gridy++;
			this.add(new JLabel("Texture coordinates: "+(m.hasTextureCoordinates?"yes":"no"),SwingConstants.LEFT),con1);
		}
		PanelHelper.ExpandLastChild(this, con1);
	}

	@Override
	public void stateChanged(ChangeEvent e) {
		if(e.getSource()==userCommandSelectFile) {
			model.setModelFilename(userCommandSelectFile.getFilename());
		}
		if(e.getSource()==setScale) {
			model.setModelScale(setScale.getValue());
		}
		if(e.getSource()==setOrigin) {
			model.setModelOrigin(setOrigin.getValue());
		}
		if(e.getSource()==setRotation) {
			model.setModelRotation(setRotation.getValue());
		}
	}
	
	/**
	 * Call by an {@link Entity} when it's details change so that they are reflected on the panel.
	 * This might be better as a listener pattern.
	 */
	public void updateFields() {
		setScale.setValue(model.scale);
		setOrigin.setValue(model.getModelOrigin());
		setRotation.setValue(model.getModelRotation());
	}
}