package gui;

import java.awt.Font;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.LayoutStyle.ComponentPlacement;

public class PropertiesDialog extends JDialog
{
	private JPanel contentPane;
	
	private JLabel lblDirectories, lblFiles, lblTotalSize, lblWorking;
	
	private Controller controller;

	/**
	 * Create the dialog.
	 */
	public PropertiesDialog(String name, Point parentLocation)
	{
		controller = new Controller();
		
		setModal(true);
		setTitle(name);
		setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		setBounds(100, 100, 175, 175);
		setLocation(parentLocation.x + 200, parentLocation.y + 50);
		contentPane = new JPanel();
		contentPane.setBorder(null);
		setContentPane(contentPane);
		
		JButton btnCancel = new JButton("Cancel");
		btnCancel.addActionListener(controller);
		getRootPane().setDefaultButton(btnCancel);
		
		JLabel lblTextDirectories = new JLabel("Directories:");
		
		JLabel lblTextFiles = new JLabel("Files:");
		
		JLabel lblTextTotalSize = new JLabel("Total size:");
		
		lblDirectories = new JLabel("0");
		
		lblFiles = new JLabel("0");
		
		lblTotalSize = new JLabel("0 B");
		
		lblWorking = new JLabel("Working");
		lblWorking.setFont(new Font(lblWorking.getFont().getName(), Font.ITALIC, lblWorking.getFont().getSize()));
		
		GroupLayout gl_contentPane = new GroupLayout(contentPane);
		gl_contentPane.setHorizontalGroup(
			gl_contentPane.createParallelGroup(Alignment.TRAILING)
				.addGroup(gl_contentPane.createSequentialGroup()
					.addContainerGap()
					.addGroup(gl_contentPane.createParallelGroup(Alignment.LEADING)
						.addGroup(gl_contentPane.createSequentialGroup()
							.addComponent(lblTextDirectories)
							.addPreferredGap(ComponentPlacement.RELATED, 78, Short.MAX_VALUE)
							.addComponent(lblDirectories))
						.addGroup(gl_contentPane.createSequentialGroup()
							.addComponent(lblTextFiles)
							.addPreferredGap(ComponentPlacement.RELATED, 108, Short.MAX_VALUE)
							.addComponent(lblFiles))
						.addGroup(gl_contentPane.createSequentialGroup()
							.addComponent(lblTextTotalSize)
							.addPreferredGap(ComponentPlacement.RELATED, 75, Short.MAX_VALUE)
							.addComponent(lblTotalSize))
						.addGroup(Alignment.TRAILING, gl_contentPane.createSequentialGroup()
							.addComponent(lblWorking)
							.addPreferredGap(ComponentPlacement.RELATED, 28, Short.MAX_VALUE)
							.addComponent(btnCancel)))
					.addContainerGap())
		);
		gl_contentPane.setVerticalGroup(
			gl_contentPane.createParallelGroup(Alignment.LEADING)
				.addGroup(gl_contentPane.createSequentialGroup()
					.addContainerGap()
					.addGroup(gl_contentPane.createParallelGroup(Alignment.BASELINE)
						.addComponent(lblTextDirectories)
						.addComponent(lblDirectories))
					.addGap(18)
					.addGroup(gl_contentPane.createParallelGroup(Alignment.BASELINE)
						.addComponent(lblTextFiles)
						.addComponent(lblFiles))
					.addGap(18)
					.addGroup(gl_contentPane.createParallelGroup(Alignment.BASELINE)
						.addComponent(lblTextTotalSize)
						.addComponent(lblTotalSize))
					.addPreferredGap(ComponentPlacement.RELATED, 14, Short.MAX_VALUE)
					.addGroup(gl_contentPane.createParallelGroup(Alignment.BASELINE)
						.addComponent(btnCancel)
						.addComponent(lblWorking))
					.addContainerGap())
		);
		contentPane.setLayout(gl_contentPane);
	}
	
	public void updateLabels(int inputNumFolders, int inputNumFiles, String inputTotalSize)
	{
		lblDirectories.setText("" + inputNumFolders);
		lblFiles.setText("" + inputNumFiles);
		lblTotalSize.setText(inputTotalSize);
	}
	
	public void setLabelDone()
	{
		lblWorking.setText("Done");
		lblWorking.setFont(new Font(lblWorking.getFont().getName(), Font.PLAIN, lblWorking.getFont().getSize()));
	}
	
	private class Controller implements ActionListener
	{
		@Override
		public void actionPerformed(ActionEvent arg0)
		{
			dispose();
		}
	}
}
