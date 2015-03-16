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
package adamb.ogg;

import java.io.*;
import adamb.util.Util;

/**
 Iterates over all successive pages in an Ogg bitstream.  Each page may
 belong to a different logical Ogg stream (which is why PhysicalPageStream does
 not implement {@link LogicalPageStream}).
 This class does not tolerate any corruption in the bitstream.  If error tolerance is
 required wrap this with an {@link ErrorTolerantPageStream}.
 */
public class PhysicalPageStream
	implements PageStream
{
  private InputStream is;
  private OggCRC pageCRC;
	/**true if the stream is currently positioned directly after the Ogg stream capture pattern*/
	private boolean haveCapture;
  
  /**
   the 4 byte Ogg stream capture pattern "OggS"
   */
  static final byte[] OGG_STREAM_CAPTURE_PATTERN = {(byte)'O', (byte)'g', (byte)'g',(byte)'S'};
  
  public PhysicalPageStream(InputStream inputStream)
		throws UnsupportedOperationException
  {
    this.is = inputStream;
    pageCRC = new OggCRC();
		haveCapture = false;
  }
	
  public Page next()
		//Even though these are all IOExceptions I will declare them explicitly because the first 3 are recoverable (see implementation of ErrorTolerantPageStream)
		throws InvalidHeaderException, ChecksumMismatchException, EOFException, IOException
  {
		//try read a page
		Page page = new Page();
		if (readPageFromStream(page))
		{
			//only return the page if it passes the CRC check
			if (page.checksum == pageCRC.getValue())
				return page;
			else
				throw new ChecksumMismatchException();
		}
		//graceful end of stream
		else
			return null;
  }
	
	private boolean readPageFromStream(Page page)
		throws IOException, InvalidHeaderException
	{
		byte[] fixedHeaderBytes;
		if (haveCapture)
			fixedHeaderBytes = new byte[Page.FIXED_HEADER_SIZE - PhysicalPageStream.OGG_STREAM_CAPTURE_PATTERN.length];
		else
			fixedHeaderBytes = new byte[Page.FIXED_HEADER_SIZE];
				
		int nRead = Util.readCompletely(is, fixedHeaderBytes);
		if (nRead == fixedHeaderBytes.length)
		{
			int segmentCount = page.parseFixedHeaderValues(fixedHeaderBytes);
			
			byte[] segmentTable = new byte[segmentCount];

			if (Util.readCompletely(is, segmentTable) == segmentTable.length)
			{
				int contentSize = page.parseSegmentTable(segmentTable);

				/*read the page contents*/
				page.content = new byte[contentSize];
				if (Util.readCompletely(is, page.content) == page.content.length)
				{
					///compute the CRC
					pageCRC.reset();

					//include the capture pattern if we don't already have it in the header data
					if (haveCapture)
						pageCRC.update(PhysicalPageStream.OGG_STREAM_CAPTURE_PATTERN);

					//zero out the checksum
					{
						int checksumOffset = Page.HEADER_CHECKSUM_OFFSET;
						if (haveCapture)
							checksumOffset -= PhysicalPageStream.OGG_STREAM_CAPTURE_PATTERN.length;
						System.arraycopy(new byte[4], 0, fixedHeaderBytes, checksumOffset, 4);
					}

					pageCRC.update(fixedHeaderBytes);
					pageCRC.update(segmentTable);
					pageCRC.update(page.content);

					return true;
				}
				else
					throw new EOFException("partial page content due to eos");
			}
			else
				throw new EOFException("partial segment table due to eos");
		}
		//graceful end of stream?
		else if (nRead == 0)
			return false;
		//unexpected end of stream
		else
			throw new EOFException("partial header due to eos");		
	}
	
	/**
	 @return the InputStream given in the constructor
	 */
	public InputStream getInputStream()
	{
		return is;
	}
	
	/**
		Should the next page read assume that the capture pattern ("OggS") was already
		read from the input stream.
	 @param captured true if the stream is currently positioned directly after the Ogg stream capture pattern, false otherwise*/
	public void setCaptured(boolean captured)
	{
		haveCapture	= captured;	
	}
}



