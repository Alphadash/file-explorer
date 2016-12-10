package gui;

import java.awt.EventQueue;

import javax.swing.UIManager;

public class App
{
	public static void main(String[] args)
	{
		try
		{
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		}
		catch (Exception e)
		{
			System.out.println("Error setting look and feel: " + e.getMessage());
		}
		EventQueue.invokeLater(new Runnable() {
			public void run()
			{
				new FrameMain();
			}
		});
	}
}
