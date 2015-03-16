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
package adamb;

import javax.swing.AbstractListModel;
import adamb.vorbis.CommentField;
import adamb.vorbis.VorbisCommentHeader;
import java.io.*;

public class CommentListModel extends AbstractListModel
{
	public VorbisCommentHeader comments;
		
	public CommentListModel(VorbisCommentHeader vch)
	{
		comments = vch;
	}
		
	public Object getElementAt(int index)
	{
		CommentField cf = comments.fields.get(index);
		
		//replace new lines with \
		try
		{
			BufferedReader br = new BufferedReader(new StringReader(cf.value));
			String line = br.readLine();
			StringBuilder sb = new StringBuilder(cf.name);
			sb.append('=');
			boolean first = true;
			while (line != null)
			{
				if (!first)
					sb.append(" \\ ");
				else
					first = false;
				
				sb.append(line);
				line = br.readLine();
			}
			
			//if the string is too long clip it off
			if (sb.length() > 128)
			{
				sb.setLength(125);
				sb.append("...");
			}
			
			return sb.toString();
		}
		catch (IOException ioe)
		{
			return null;
			//will never happen with StringReader
		}
	}
	
	public int getSize()
	{
		return comments.fields.size();
	}

	public void fireChanged(int idx)
	{
		fireContentsChanged(this, idx, idx);
	}
	
	public void add(CommentField cf)
	{
		int idx = comments.fields.size();
		comments.fields.add(cf);
		fireIntervalAdded(this, idx, idx);
	}	
	
	public void remove(int idx)
	{
		comments.fields.remove(idx);
		fireIntervalRemoved(this, idx, idx);
	}		
}

