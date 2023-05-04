package com.marginallyclever.robotoverlord.swinginterface.actions;

import com.marginallyclever.robotoverlord.*;
import com.marginallyclever.robotoverlord.swinginterface.UnicodeIcon;
import com.marginallyclever.robotoverlord.swinginterface.UndoSystem;
import com.marginallyclever.robotoverlord.swinginterface.translator.Translator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.nio.file.Path;

/**
 * Import a {@link Project} and add it to the main Project.
 * @author Dan Royer
 * @since 2.0.0
 */
public class ProjectImportAction extends AbstractAction {
    private static final Logger logger = LoggerFactory.getLogger(ProjectImportAction.class);

    private final Project project;
    /**
     * The file chooser remembers the last path.
     */
    private static final JFileChooser fc = new JFileChooser();

    private File preselectedFile = null;

    public ProjectImportAction(Project project) {
        super(Translator.get("SceneImportAction.name"));
        this.project = project;
        fc.setFileFilter(RobotOverlord.FILE_FILTER);
        putValue(SMALL_ICON,new UnicodeIcon("🗁"));
        putValue(SHORT_DESCRIPTION, Translator.get("SceneImportAction.shortDescription"));
    }

    public ProjectImportAction(Project project, File preselectedFile) {
        this(project);
        putValue(NAME, Translator.get("SceneImportAction.namePreselected",preselectedFile.getName()));
        this.preselectedFile = preselectedFile;
    }

    public static void setLastDirectory(String s) {
        fc.setCurrentDirectory(new File(s));
    }

    public static String getLastDirectory() {
        return fc.getCurrentDirectory().getAbsolutePath();
    }

    @Override
    public void actionPerformed(ActionEvent evt) {
        JFrame parentFrame = (JFrame)SwingUtilities.getWindowAncestor((java.awt.Component) evt.getSource());

        if(preselectedFile != null) {
            loadFile(preselectedFile,parentFrame);
            return;
        }

        if (fc.showOpenDialog(parentFrame) == JFileChooser.APPROVE_OPTION) {
            loadFile(fc.getSelectedFile(),parentFrame);
        }
    }

    public boolean loadFile(File file,JFrame parentFrame) {
        if(!fc.getFileFilter().accept(file)) return false;

        try {
            Project source = new Project();
            source.load(file);
            Path path = Path.of(source.getPath());
            Path lastPath = path.subpath(path.getNameCount()-1,path.getNameCount());
            String destinationPath = project.getPath() + File.separator + lastPath;

            project.addProject(source);
            UndoSystem.reset();
        } catch(Exception e1) {
            logger.error("Failed to import. ",e1);
            JOptionPane.showMessageDialog(parentFrame,e1.getLocalizedMessage());
            e1.printStackTrace();
            return false;
        }
        return true;
    }
}