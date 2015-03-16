/* (The MIT License)
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

import javax.swing.filechooser.FileFilter;
import java.io.File;

public class ExtensionFileFilter extends FileFilter
	implements java.io.FileFilter
{
	private String[] extensions;
	private boolean alwaysAcceptDirectories;
	
	/**
		Filter based on file name suffix and, optionally, directory name suffixes
		@param alwaysAcceptDirectories pass true to always accept directories.  Pass false if directories should be subjected to the same suffix filter as files.
	 */
	public ExtensionFileFilter(boolean alwaysAcceptDirectories, String... lowerCaseExtensions)
	{
		this.alwaysAcceptDirectories = alwaysAcceptDirectories;
		this.extensions = lowerCaseExtensions;		
	}
	
	/**
	 Filter based on file name suffix.  Directories will always be accepted.
	 todo: to avoid bugs perhaps I should ensure that the given strings are lower case
	 */
	public ExtensionFileFilter(String... lowerCaseExtensions)
	{
		this(true, lowerCaseExtensions);
	}

	public boolean accept(File f)
	{
		/*
			File.isDirectory() is a native call.  Native calls are slow so I
		 will try to avoid making the call if not required.
		 
		 On the average hard drive I think it's safe to say that there are more files
		 than directories.  The directory is the rare case.  Therefore I will assume
		 that the file in question, f, is not a directory.  If it happens to match
		 our extension filter than we have saved ourselves a call to f.isDirectory()
		 */
		String name = f.getName().toLowerCase();
		for (String extension: extensions)
		{
			//suffix match?
			if (name.endsWith(extension))
				return true;
		}

		return alwaysAcceptDirectories && f.isDirectory();  //short circuit is important
	}
	
	public String getDescription()
	{
		StringBuilder sb = new StringBuilder();
		boolean first = true;
		for (String ext: extensions)
		{
			if (!first)
				sb.append(',');
			else
				first = false;
				
			sb.append(ext);			
		}
		
		return sb.toString();
	}
}
