/*(The MIT License)
Copyright (c) 2006 Adam Bennett (cruxic@gmail.com)
 
Permission is hereby granted, free of charge, to any person obtaining a copy of
this software and associated documentation files (the "Software"), to deal in
the Software without restriction, including without limitation the rights to
use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
of the Software, and to permit persons to whom the Software is furnished to do
so, subject to the following conditions:
 
The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.
 
THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
 */
package adamb.ui;

import java.io.File;
import java.io.IOException;
import java.awt.Component;
import adamb.util.Util;
import javax.swing.*;
import java.awt.Window;
import java.awt.Container;

public class SwingUtil
{
	public SwingUtil()
	{
	}
	
	public static Window getParentWindow(Container container)
	{
		Container parent = container.getParent();
		while (!(parent instanceof Window))
			parent = parent.getParent();
		
		return (Window)parent;
	}
	
	public static File chooseExistingFile(Component parent, String dialogTitle, String[] extensions, File initialFileOrDir)
	{
		//if the given file does not exist then try at least to get the parent directory
		if (initialFileOrDir != null && !initialFileOrDir.exists())
		{
			try
			{
				initialFileOrDir = Util.findFirstExistingParentDirectory(initialFileOrDir);
			}
			catch (IOException ioe)
			{
				//no need to bother the user with this but at least report it
				System.err.println(ioe);
			}
		}
		
		JFileChooser fc = new JFileChooser(initialFileOrDir);
		fc.setFileFilter(new ExtensionFileFilter(extensions));
		fc.setAcceptAllFileFilterUsed(false);
		fc.setDialogType(JFileChooser.OPEN_DIALOG);
		fc.setDialogTitle(dialogTitle);
				
		if (initialFileOrDir != null && initialFileOrDir.isFile())
			fc.setSelectedFile(initialFileOrDir);
		
		if (fc.showOpenDialog(parent) == JFileChooser.APPROVE_OPTION)
			return fc.getSelectedFile();
		else
			return null;
	}	
	
	public static File chooseDirectory(Component parent, String dialogTitle, boolean allowCreate, File initialFileOrDir)
	{
		//if the given file does not exist then try at least to get the parent directory
		if (initialFileOrDir != null && !initialFileOrDir.exists())
		{
			try
			{
				initialFileOrDir = Util.findFirstExistingParentDirectory(initialFileOrDir);
			}
			catch (IOException ioe)
			{
				//no need to bother the user with this but at least report it
				System.err.println(ioe);
			}
		}
		
		JFileChooser fc = new JFileChooser(initialFileOrDir);
		fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		fc.setDialogType(allowCreate ? JFileChooser.SAVE_DIALOG : JFileChooser.OPEN_DIALOG);
		fc.setDialogTitle(dialogTitle);
				
		if (initialFileOrDir != null && initialFileOrDir.isDirectory())
			fc.setSelectedFile(initialFileOrDir);
		
		int result = allowCreate ? fc.showSaveDialog(parent) : fc.showOpenDialog(parent);
		
		if (result == JFileChooser.APPROVE_OPTION)
			return fc.getSelectedFile();
		else
			return null;
	}
	
	/**Prompt the user to choose between two courses of action.
	 @return true if the user chose the default course of action,
		false if the alternate course was chosen of the prompt closed.*/
	public static boolean prompt2(Component parent, String question, String title,
		String defaultActionText, String alternateActionText)
	{
		Object[] values = new Object[] {defaultActionText, alternateActionText};
		
		int choice = JOptionPane.showOptionDialog(parent, 
			question,
			title,
			JOptionPane.YES_NO_OPTION,
			JOptionPane.QUESTION_MESSAGE,
			null,
			values,
			values[0]);
		
		return choice == JOptionPane.YES_OPTION;
	}
}
