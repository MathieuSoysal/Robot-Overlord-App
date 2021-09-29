package com.marginallyclever.robotOverlord.textInterfaces;

import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.BorderLayout;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JToolBar;
import javax.swing.UIManager;

import com.marginallyclever.communications.NetworkSessionEvent;
import com.marginallyclever.convenience.StringHelper;
import com.marginallyclever.convenience.log.Log;
import com.marginallyclever.robotOverlord.robots.sixi3.Sixi3Bone;
import com.marginallyclever.robotOverlord.robots.sixi3.Sixi3IK;

public class RobotUI extends JPanel {
	private static final long serialVersionUID = -6388563393882327725L;

	private Sixi3IK mySixi3 = new Sixi3IK();
	private TextInterfaceToNetworkSession chatInterface = new TextInterfaceToNetworkSession();
	private JPanel angleReport;
	private JPanel sixi3FKDrivePanel;
	private JPanel cartesianReport;
	private JPanel sixi3IKDrivePanel;
	private JToolBar toolBar = getToolBar();

	public RobotUI() {
		this(new Sixi3IK());
	}

	public RobotUI(Sixi3IK sixi3) {
		super();

		mySixi3 = sixi3;

		angleReport = new AngleReportPanel(sixi3);
		sixi3FKDrivePanel = new Sixi3FKDrivePanel(sixi3);
		cartesianReport = new CartesianReportPanel(sixi3);
		sixi3IKDrivePanel = new Sixi3IKDrivePanel(sixi3);

		JPanel interior = getInteriorPanel();

		this.setLayout(new BorderLayout());
		this.add(toolBar, BorderLayout.PAGE_START);
		this.add(interior, BorderLayout.CENTER);

		sixi3.addPropertyChangeListener((e) -> sendGoto());

		chatInterface.addActionListener((e) -> {
			switch (e.getID()) {
			case ChooseConnectionPanel.NEW_CONNECTION:
				onConnect();
				break;
			}

		});
	}

	private JPanel getInteriorPanel() {
		JPanel panel = new JPanel();

		panel.setLayout(new GridBagLayout());

		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 0;
		c.weightx = 1;
		c.weighty = 0.75;
		c.gridheight = 6;
		c.gridwidth = 1;
		c.fill = GridBagConstraints.BOTH;
		c.anchor = GridBagConstraints.NORTHWEST;

		panel.add(chatInterface, c);

		c.gridheight = 1;
		c.gridwidth = 1;
		c.gridx = 1;
		c.weightx = 0.25;
		c.weighty = 0;

		c.gridy = 0;
		panel.add(angleReport, c);
		c.gridy++;
		panel.add(sixi3FKDrivePanel, c);

		c.gridy++;
		panel.add(cartesianReport, c);
		c.gridy++;
		panel.add(sixi3IKDrivePanel, c);

		c.gridy++;
		c.weighty = 1;
		panel.add(new JPanel(), c);

		return panel;
	}

	private void onConnect() {
		setupListener();
		// you are at the position I say you are at.
		new java.util.Timer().schedule(new java.util.TimerTask() {
			@Override
			public void run() {
				sendSetHome();
			}
		}, 1000 // 1s delay
		);
	}

	private void setupListener() {
		chatInterface.getNetworkSession().addListener((evt) -> {
			if (evt.flag == NetworkSessionEvent.DATA_AVAILABLE) {
				String message = ((String) evt.data).trim();
				if (message.startsWith("X:") && message.contains("Count")) {
					System.out.println("FOUND " + message);
					processM114Reply(message);
				}
			}
		});
	}

	// format is normally X:0.00 Y:270.00 Z:0.00 U:270.00 V:180.00 W:0.00 Count X:0
	// Y:0 Z:0 U:0 V:0 W:0
	// trim everything after and including "Count", then read the angles into sixi3.
	private void processM114Reply(String message) {
		message = message.substring(0, message.indexOf("Count"));
		String[] majorParts = message.split("\b");
		double[] angles = mySixi3.getFKValues();

		for (int i = 0; i < mySixi3.getNumBones(); ++i) {
			Sixi3Bone bone = mySixi3.getBone(i);
			for (String s : majorParts) {
				String[] minorParts = s.split(":");

				if (minorParts[0].contentEquals(bone.getName())) {
					try {
						angles[i] = Double.valueOf(minorParts[1]);
					} catch (NumberFormatException e) {
					}
				}
			}
		}
		mySixi3.setFKValues(angles);
	}

	private void sendGoto() {
		String action = "G0";
		for (int i = 0; i < mySixi3.getNumBones(); ++i) {
			Sixi3Bone bone = mySixi3.getBone(i);
			action += " " + bone.getName() + StringHelper.formatDouble(bone.getTheta());
		}
		chatInterface.sendCommand(action);
	}

	private JToolBar getToolBar() {
		JToolBar bar = new JToolBar();
		bar.setRollover(true);

		JButton bESTOP = new JButton("EMERGENCY STOP");
		JButton bGetAngles = new JButton("Tell me your angles");
		JButton bSetHome = new JButton("You are at home");
		JButton bGoHome = new JButton("Go home");

		bESTOP.setFont(getFont().deriveFont(Font.BOLD));

		bESTOP.addActionListener((e) -> {
			chatInterface.sendCommand("M112");
		});

		bGetAngles.addActionListener((e) -> {
			sendGetPosition();
		});

		bSetHome.addActionListener((e) -> {
			sendSetHome();
		});

		bGoHome.addActionListener((e) -> {
			sendGoHome();
		});

		bar.add(bESTOP);
		bar.add(bGetAngles);
		bar.add(bSetHome);
		bar.add(bGoHome);

		return bar;
	}

	private void sendGetPosition() {
		chatInterface.sendCommand("M114");
	}

	private void sendSetHome() {
		String action = "G92";
		for (int i = 0; i < mySixi3.getNumBones(); ++i) {
			Sixi3Bone bone = mySixi3.getBone(i);
			action += " " + bone.getName() + StringHelper.formatDouble(bone.getTheta());
		}
		chatInterface.sendCommand(action);
	}

	private void sendGoHome() {
		String action = "G0";
		Sixi3IK temp = new Sixi3IK();
		double[] angles = temp.getFKValues();

		for (int i = 0; i < mySixi3.getNumBones(); ++i) {
			Sixi3Bone bone = mySixi3.getBone(i);
			action += " " + bone.getName() + StringHelper.formatDouble(angles[i]);
		}
		chatInterface.sendCommand(action);
	}

	// TEST

	public static void main(String[] args) {
		Log.start();

		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e) {
		}

		JFrame frame = new JFrame("RobotUI");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.add(new RobotUI());
		frame.pack();
		frame.setVisible(true);
	}
}