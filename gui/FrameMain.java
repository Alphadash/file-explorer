package gui;

import java.awt.Desktop;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.List;

import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JFrame;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTree;
import javax.swing.ListSelectionModel;
import javax.swing.SwingWorker;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.event.TreeWillExpandListener;
import javax.swing.filechooser.FileSystemView;
import javax.swing.table.DefaultTableModel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.ExpandVetoException;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

public class FrameMain extends JFrame
{
	private JPanel contentPane;
	private JTree tree;
	private JTable table;

	private DefaultTreeModel treeModel;
	private DefaultTableModel tableModel;
	private DefaultTableModel rootTableModel;
	
	private JPopupMenu popMenu;
	private JMenuItem search;
	private JMenuItem properties;
	
	private PropertiesDialog propDialog;
	private PropWorker propWorker;
	private SearchWorker searchWorker;

	private Controller controller;

	public FrameMain()
	{
		controller = new Controller();

		setTitle("Total File Commander Explorer 3000");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 100, 800, 600);
		contentPane = new JPanel();
		contentPane.setBorder(null);
		setContentPane(contentPane);

		JSplitPane splitPane = new JSplitPane();
		splitPane.setBorder(null);
		GroupLayout gl_contentPane = new GroupLayout(contentPane);
		gl_contentPane.setHorizontalGroup(
				gl_contentPane.createParallelGroup(Alignment.LEADING)
				.addComponent(splitPane, GroupLayout.DEFAULT_SIZE, 424, Short.MAX_VALUE)
				);
		gl_contentPane.setVerticalGroup(
				gl_contentPane.createParallelGroup(Alignment.LEADING)
				.addComponent(splitPane, Alignment.TRAILING, GroupLayout.DEFAULT_SIZE, 252, Short.MAX_VALUE)
				);

		JScrollPane scrollPane = new JScrollPane();
		splitPane.setLeftComponent(scrollPane);

		tree = new JTree();
		tree.setShowsRootHandles(true);
		tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
		tree.setModel(treeModel = new DefaultTreeModel(
				new DefaultMutableTreeNode("Computer") {
					{
						add(new DefaultMutableTreeNode("placeholder")); // Required to allow root node to be expanded
					}
				}
				));
		tree.collapseRow(0);
		tree.addTreeSelectionListener(controller);
		tree.addTreeWillExpandListener(controller);
		scrollPane.setViewportView(tree);

		JScrollPane scrollPane_1 = new JScrollPane();
		splitPane.setRightComponent(scrollPane_1);

		table = new JTable();
		table.setShowGrid(false);
		table.setFillsViewportHeight(true);
		table.setAutoCreateRowSorter(true);
		table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		table.setModel(rootTableModel = new DefaultTableModel(
				new String[][] {
				},
				new String[] {
						"Name", "Type", "Free space", "Used space", "Total size"
				}
				) {
					@Override
					public boolean isCellEditable(int row, int column) { return false; } // Disable cell editing completely, would otherwise block a double-click mouseListener
				});
		tableModel = new DefaultTableModel(
				new Object[][] {
				},
				new String[] {
						"Name", "Date modified", "Type", "Size"
				}
				) {
					@Override
					public boolean isCellEditable(int row, int column) { return false; } // See above
				};
		table.addMouseListener(controller);
		scrollPane_1.setViewportView(table);
		splitPane.setDividerLocation(150);
		
		popMenu = new JPopupMenu();
		search = new JMenuItem("Search");
		properties = new JMenuItem("Properties");
		search.addActionListener(controller);
		properties.addActionListener(controller);
		popMenu.add(search);
		popMenu.add(properties);
		
		propDialog = null;
		propWorker = null;
		searchWorker = null;
		
		contentPane.setLayout(gl_contentPane);

		this.setVisible(true);
	}

	private class Controller implements TreeWillExpandListener, TreeSelectionListener, ActionListener, WindowListener, MouseListener
	{
		private String findFilePathFromNode(DefaultMutableTreeNode inputNode)
		{
			// We need to find the path to the file
			String nodePath = "";
			int i = 0;
			for (TreeNode node : inputNode.getPath())
			{
				if (i != 0) // Index 0 is always the root handle, "Computer", this is irrelevant to the path
				{
					if (i > 1) nodePath += "/"; // We only need to add a forward slash if the path object is not the root of the drive
					nodePath += node.toString();
				}
				i++;
			}
			return nodePath;
		}

		private String genSizeStringFromLength(Long fileLength)
		{
			String size = "";
			int count = 0;
			while (fileLength >= 1024) // If fileLength is larger than or equal to 1024, we can shorten it by moving up a prefix
			{
				fileLength /= 1024;
				count++;
			}
			size += fileLength + " ";

			/*
			 * count is the amount of times we divided fileLength by 1024, this denotes the prefix we must add to the string
			 * The prefixes used are from the IEC binary prefix standard
			 */
			switch (count)
			{
				case 0:
					size += "B";
					break;
				case 1:
					size += "KiB";
					break;
				case 2:
					size += "MiB";
					break;
				case 3:
					size += "GiB";
					break;
				case 4:
					size += "TiB";
					break;
				case 5:
					size += "PiB";
					break;
				case 6:
					size += "EiB";
					break;
				case 7:
					size += "ZiB";
					break;
				case 8:
					size += "YiB";
					break;
				default:
					size += "Undefined (2^" + count * 10 + " Bytes)";
					break;
			}

			return size;
		}

		@Override
		public void treeWillExpand(TreeExpansionEvent te) throws ExpandVetoException
		{	
			DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) te.getPath().getLastPathComponent();

			// Due to clean up before collapsing, we know only the "placeholder" child exists
			treeModel.removeNodeFromParent((DefaultMutableTreeNode) selectedNode.getFirstChild());

			boolean isRoot = selectedNode.isRoot();
			File[] files = (!isRoot) ? new File(findFilePathFromNode(selectedNode)).listFiles() : File.listRoots(); // If the selected node is the root (ie. "Computer"), we need to list drive letters instead

			/*
			 * If files == null, it is a protected folder (no read) or device with no mounted media (DVD drive, card reader, etc)
			 * If files.length is zero, the folder is empty
			 * The null check must come first, as if it is not true, the files.length check will not be made
			 * This is important as files.length would throw an exception if files was null
			 */
			if (files != null)
			{
				boolean empty = true;
				for (File file : files)
				{
					if (file.isDirectory() || isRoot)
					{
						DefaultMutableTreeNode nameNode = new DefaultMutableTreeNode((isRoot) ? file.toString() : file.getName());
						treeModel.insertNodeInto(nameNode, selectedNode, selectedNode.getChildCount());
						treeModel.insertNodeInto(new DefaultMutableTreeNode("placeholder"), nameNode, 0); // Required to allow node to be expanded
						empty = false;
					}
				}
				// If empty is still true, the folder contains no subfolders - but may contain files, so we check files.length
				if (empty) treeModel.insertNodeInto(new DefaultMutableTreeNode((files.length == 0) ? "Folder is empty" : "No subfolders"), selectedNode, 0);
			}
			else treeModel.insertNodeInto(new DefaultMutableTreeNode((new File(findFilePathFromNode(selectedNode)).exists()) ? "Read denied by OS" : "No mounted media"), selectedNode, 0); // File.exists() returns false if called on a device with no mounted media
		}

		@Override
		public void treeWillCollapse(TreeExpansionEvent te) throws ExpandVetoException
		{
			DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) te.getPath().getLastPathComponent();

			/*
			 * All children are deleted upon collapsing, this is the easiest way to allow for synchronising with the filesystem
			 * Upon deletion of a child from a node, the index does not seem to be immediately updated,
			 * therefore the children must be traversed in reverse order to delete them one-by-one without indexes shifting
			 */
			for (int i = selectedNode.getChildCount() - 1; i >= 0; i--) treeModel.removeNodeFromParent((DefaultMutableTreeNode) selectedNode.getChildAt(i));
			treeModel.insertNodeInto(new DefaultMutableTreeNode("placeholder"), selectedNode, 0); // Required to allow node to be expanded
			
			tree.setSelectionPath(new TreePath(selectedNode.getPath())); // We need to make sure there's always a selected node in the tree, or the mouseListener will generate errors
		}

		@Override
		public void valueChanged(TreeSelectionEvent te)
		{
			if (searchWorker != null)
			{
				searchWorker.cancel(false);
				searchWorker = null;
			}
			
			DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) te.getPath().getLastPathComponent();
			File selectedDir = new File(findFilePathFromNode(selectedNode));

			// See deletion of tree node children above, except since we have two table models, we must get the current one
			DefaultTableModel curModel = (DefaultTableModel) table.getModel();
			for (int i = curModel.getRowCount() - 1; i >= 0; i--) curModel.removeRow(i);

			if (selectedNode.getChildCount() != 0) // If the node has no children, it is an info-node, the table should just show the information
			{
				FileSystemView fsView = FileSystemView.getFileSystemView(); // For getting the type of a file object from the system
				if (selectedDir.getName() != "")
				{
					table.setModel(tableModel);

					File[] files = selectedDir.listFiles();
					if (files != null)
					{
						SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm"); // For formatting the return of File.lastmodified()
						
						boolean empty = true;
						for (File file : files)
						{
							String type = fsView.getSystemTypeDescription(file); // Required as this seems to return null on non-Windows systems
							
							String[] row = new String[] {"", "", "", ""};
							row[0] = file.getName();
							row[1] = formatter.format(file.lastModified());
							if (type != null) row[2] = type;
							else // Find a simple type to display if the FileSystemView fails
							{
								if (file.isDirectory()) row[2] = "File folder";
								else
								{
									String name = file.getName();
									if (!name.contains(".")) row[2] = "File";
									else row[2] = name.substring(name.lastIndexOf('.')).toUpperCase() + " file"; // Very simple solution, will not work right for extensions with multiple periods (eg. .tar.gz showing "GZ file")
								}
							}
							if (!file.isDirectory()) row[3] = genSizeStringFromLength(file.length());

							tableModel.addRow(row);
							empty = false;
						}
						if (empty) tableModel.addRow(new String[] {"Folder is empty", "", "", ""});
					}
					else tableModel.addRow(new String[] {(selectedDir.exists()) ? "Read denied by OS" : "No mounted media", "", "", ""}); // See tree node creation above
				}
				else
				{
					table.setModel(rootTableModel);

					for (File file : File.listRoots())
					{
						// To save on filesystem calls, we save the values to use for calculation of used space
						Long freeSpace = file.getFreeSpace();
						Long totalSpace = file.getTotalSpace();
						
						String type = fsView.getSystemTypeDescription(file); // See type-getting of file objects above

						String[] row = new String[] {"", "", "", "", ""};
						row[0] = file.toString();
						row[1] = (type != null) ? type : "Root drive"; // See above
						row[2] = genSizeStringFromLength(freeSpace);
						row[3] = genSizeStringFromLength(totalSpace - freeSpace);
						row[4] = genSizeStringFromLength(totalSpace);

						rootTableModel.addRow(row);
					}
				}
			}
			else tableModel.addRow(new String[] {(String) selectedNode.getUserObject(), "", "", ""}); // The node's object is the information we wish to display
		}
		
		@Override
		public void actionPerformed(ActionEvent ae)
		{
			String selectedName = (String) table.getModel().getValueAt(table.getSelectedRow(), 0);

			DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();
			File clickedFile = new File(findFilePathFromNode(selectedNode) + "/" + selectedName);
			
			if (ae.getSource().equals(properties))
			{	
				propDialog = new PropertiesDialog(selectedName, getLocation());
				
				propWorker = new PropWorker(clickedFile);
				propWorker.execute();
				
				propDialog.addWindowListener(controller);
				propDialog.setVisible(true);
			}
			else
			{
				String entry = (String) JOptionPane.showInputDialog(contentPane, "Enter the string to search for", "Search in: " + ((selectedNode.isRoot()) ? clickedFile.toString() : clickedFile.getName()), JOptionPane.PLAIN_MESSAGE);
				
				if (entry != null && entry.length() > 0)
				{
					tree.setSelectionPath(null);
					
					DefaultTableModel curModel = (DefaultTableModel) table.getModel();
					for (int i = curModel.getRowCount() - 1; i >= 0; i--) curModel.removeRow(i);
					
					searchWorker = new SearchWorker(clickedFile, entry);
					searchWorker.execute();
				}
				else JOptionPane.showMessageDialog(contentPane, "Invalid string entered" + System.lineSeparator() + "Please try again", "Error searching", JOptionPane.ERROR_MESSAGE);
			}
		}
		
		@Override
		public void windowClosed(WindowEvent we)
		{
			if (we.getSource().equals(propDialog))
			{
				propWorker.cancel(false);
				propWorker = null;
				propDialog = null;
			}
		}
		
		@Override
		public void windowActivated(WindowEvent arg0) {}

		@Override
		public void windowClosing(WindowEvent arg0) {}

		@Override
		public void windowDeactivated(WindowEvent arg0) {}

		@Override
		public void windowDeiconified(WindowEvent arg0) {}

		@Override
		public void windowIconified(WindowEvent arg0) {}

		@Override
		public void windowOpened(WindowEvent arg0) {}

		@Override
		public void mouseClicked(MouseEvent me)
		{
			if (me.getSource().equals(table) && tree.getSelectionPath() != null)
			{
				int row = table.rowAtPoint(me.getPoint()); // Get the row under the mouse pointer
				if (row >= 0 && row < table.getRowCount()) // Proceed only if we clicked a non-null row within the table
				{
					if (me.getClickCount() == 2) // We double-clicked a row
					{
						String selectedName = (String) table.getModel().getValueAt(table.getSelectedRow(), 0);

						DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();
						File clickedFile = new File(findFilePathFromNode(selectedNode) + "/" + selectedName);

						if (clickedFile.isDirectory())
						{
							tree.expandPath(new TreePath(selectedNode.getPath())); // Expand the selected node
							for (int i = 0; i < selectedNode.getChildCount(); i++)
							{
								if (selectedNode.getChildAt(i).toString().equals(selectedName)) // We need to find the folder we clicked in the table, that is the child with the same name as the entry in the table
								{
									tree.setSelectionPath(new TreePath(((DefaultMutableTreeNode) selectedNode.getChildAt(i)).getPath())); // By setting the selection to the child, the TreeSelectionListener updates the table with the contents of the folder
									break;
								}
							}
						}
						else if (clickedFile.canExecute())
						{
							try
							{
								Desktop.getDesktop().open(clickedFile);
							}
							catch (IOException e)
							{
								JOptionPane.showMessageDialog(contentPane, "System said:" + System.lineSeparator() + e.getMessage(), "Error opening file", JOptionPane.ERROR_MESSAGE);
							}
						}
					}
				}
				else table.clearSelection();
			}
		}


		@Override
		public void mouseEntered(MouseEvent arg0) {}

		@Override
		public void mouseExited(MouseEvent arg0) {}

		@Override
		public void mousePressed(MouseEvent me)
		{
			if (me.isPopupTrigger()) showPopupMenu(me);
		}

		@Override
		public void mouseReleased(MouseEvent me)
		{
			if (me.isPopupTrigger()) showPopupMenu(me);
		}
		
		private void showPopupMenu(MouseEvent me)
		{
			if (tree.getSelectionPath() != null)
			{
				int row = table.rowAtPoint(me.getPoint()); // See above
				if (row >= 0 && row < table.getRowCount())
				{
					table.setRowSelectionInterval(row, row); // If we right-click, the row is not automatically selected

					String selectedName = (String) table.getModel().getValueAt(table.getSelectedRow(), 0);

					DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();
					File clickedFile = new File(findFilePathFromNode(selectedNode) + "/" + selectedName);

					if (clickedFile.isDirectory()) popMenu.show(me.getComponent(), me.getX(), me.getY());
				}
				else table.clearSelection();
			}
		}
	}
	
	private class PropWorker extends SwingWorker<Long, Long>
	{
		private File folder;
		private int numFolders, numFiles;
		private long totalSize;
		
		public PropWorker(File inputFolder)
		{
			folder = inputFolder;
			numFolders = 0;
			numFiles = 0;
			totalSize = 0L;
		}
		
		protected void countFilesInFolder(File inputFolder)
		{
			File[] files = inputFolder.listFiles();
			if (files != null)
			{
				for (File file : files)
				{
					if (isCancelled()) break;
					
					if (file.isDirectory())
					{
						countFilesInFolder(file);
						numFolders++;
					}
					else
					{
						publish(file.length());
						numFiles++;
					}
				}
			}
		}

		@Override
		protected Long doInBackground() throws Exception
		{
			countFilesInFolder(folder);
			return null;
		}
		
		@Override
		protected void process(List<Long> chunks)
		{
			if (!isCancelled())
			{
				for (Long chunk : chunks) totalSize += chunk;
				propDialog.updateLabels(numFolders, numFiles, controller.genSizeStringFromLength(totalSize));
			}
		}
		
		@Override
		protected void done()
		{
			if (!isCancelled()) propDialog.setLabelDone();
		}
	}
	
	private class SearchWorker extends SwingWorker<File, File>
	{
		private File folder;
		private String name;
		
		public SearchWorker(File inputFolder, String inputName)
		{
			folder = inputFolder;
			name = inputName.toLowerCase();
			
			table.setModel(tableModel);
		}
		
		protected void findFilesInFolder(File inputFolder)
		{
			File[] files = inputFolder.listFiles();
			if (files != null)
			{
				for (File file : files)
				{
					if (isCancelled()) break;
					
					if (file.isDirectory()) findFilesInFolder(file);
					else if (file.getName().toLowerCase().contains(name)) publish(file);
				}
			}
		}

		@Override
		protected File doInBackground() throws Exception
		{
			findFilesInFolder(folder);
			return null;
		}
		
		@Override
		protected void process(List<File> chunks)
		{
			SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm"); // For formatting the return of File.lastmodified()
			
			for (File file : chunks)
			{
				if (isCancelled()) break;
				
				String type = FileSystemView.getFileSystemView().getSystemTypeDescription(file); // Required as this seems to return null on non-Windows systems
				
				String[] row = new String[] {"", "", "", ""};
				row[0] = file.toString().substring(folder.toString().length()); // Show path relative to searched folder
				row[1] = formatter.format(file.lastModified());
				if (type != null) row[2] = type;
				else // Find a simple type to display if the FileSystemView fails
				{
					if (file.isDirectory()) row[2] = "File folder";
					else
					{
						String name = file.getName();
						if (!name.contains(".")) row[2] = "File";
						else row[2] = name.substring(name.lastIndexOf('.')).toUpperCase() + " file"; // Very simple solution, will not work right for extensions with multiple periods (eg. .tar.gz showing "GZ file")
					}
				}
				if (!file.isDirectory()) row[3] = controller.genSizeStringFromLength(file.length());

				tableModel.addRow(row);
			}
		}
	}
}
