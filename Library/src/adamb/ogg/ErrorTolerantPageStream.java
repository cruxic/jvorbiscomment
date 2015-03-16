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
import adamb.util.Util;

public class ErrorTolerantPageStream
	implements PageStream
{
	private PhysicalPageStream pps;
	
	public ErrorTolerantPageStream(PhysicalPageStream pps)
	{
		//error recovery requires the mark and reset operations
		if (!pps.getInputStream().markSupported())
			throw new UnsupportedOperationException("Given input stream does not support mark and reset operations!");
		
		this.pps = pps;
	}
	
  public Page next()
		throws IOException
  {
		//assume that the next page begins directly at the current stream position
		pps.setCaptured(false);
		
		while (true)
		{
			//mark the stream before attempting to read the next page
			pps.getInputStream().mark(Page.MAX_PAGE_SIZE + 8);  //8 is a fudge factor, I don't think I need it but just in case...
			
			//try read another page but catch exceptions that are "tolerable"
			try
			{
				return pps.next();
			}
			catch (ChecksumMismatchException cme)
			{
				/*we were able to parse the entire page but it is corrupt.*/
			}
			catch (InvalidHeaderException cpe)
			{
				/*this usually means we have a bad capture (we started parsing a 
				 page from a location that wasn't really a page*/
			}
			catch (EOFException eof)
			{
				/*this will happen when the stream does not contain enough data
				 to construct a page.  This will happen if there is non-ogg (junk) data
				 at the end of the stream or if we have a bad capture and content size
				 was computed to be larger than the remainder of the stream*/				
			}
			
			/*this point will only be reached if the next page was corrupt.
			 search the stream for the next page by searching for the capture pattern.
			 first we must reset the stream to the place where we got a bad capture*/
			InputStream is = pps.getInputStream();
			is.reset();
			//skip 1 byte so the search does not match immediately and cause an infinite loop
			boolean skipped1 = is.read() != -1;
			assert skipped1;  //there should never be no problem skipping 1 byte because we know we the stream contains at least 4 bytes or at least a page

			//if we can find the capture pattern tell the physical stream that we have already read the capture pattern from the input stream
			if (Util.streamFind(is, PhysicalPageStream.OGG_STREAM_CAPTURE_PATTERN) != -1)
				pps.setCaptured(true);
			else
				return null;
		}
  }

}
