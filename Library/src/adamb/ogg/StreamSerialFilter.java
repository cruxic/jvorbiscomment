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
package adamb.ogg;

import java.io.*;

/**Converts a {@link PageStream} into a {@link LogicalPageStream}
 by returning only pages with the correct serial number*/
public class StreamSerialFilter
	implements LogicalPageStream
{
	private PageStream ps;
	private boolean haveSerialNumber;
	private int correctSerialNumber;
	private boolean throwUponForeignPages;
	
	public StreamSerialFilter(PageStream ps, boolean throwUponForeignPages)
	{
		this(ps, throwUponForeignPages, null);
	}
	
	public StreamSerialFilter(PageStream ps, boolean throwUponForeignPages, Integer streamSerialNumber)
	{
		this.ps = ps;
		
		if (streamSerialNumber != null)
		{
			haveSerialNumber = true;
			correctSerialNumber = streamSerialNumber.intValue();
		}
		else
			haveSerialNumber = false;
		
		this.throwUponForeignPages = throwUponForeignPages;
	}	
	
  public Page next()
		throws ForeignPageException, IOException
	{
		while (true)
		{
			Page page = ps.next();

			if (page != null)
			{
				//do I already know the correct serial number?
				if (haveSerialNumber)
				{
					//correct serial number?
					if (page.streamSerialNumber == correctSerialNumber)
						return page;
					//shall I throw an exception?
					else if (throwUponForeignPages)
						throw new ForeignPageException("Found Ogg page from a different stream, #" + page.streamSerialNumber + "; expected #" + correctSerialNumber + "!");
					//else silently skip the foreign page
				}
				else
				{
					correctSerialNumber = page.streamSerialNumber;
					haveSerialNumber = true;
					return page;
				}
			}
			else
				return null;
		}
	}
}
