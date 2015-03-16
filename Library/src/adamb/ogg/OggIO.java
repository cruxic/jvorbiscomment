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

public class OggIO
{
	//perhaps a good test would be to read in an ogg stream and immediately write
	//it back out.  If the stream is 100% intact it will be an exact match if the code is correct.
	/*
	public static int validateFile(File oggFile)
		throws IOException
	{
		FileInputStream fis = new FileInputStream(oggFile);
		
		try
		{
			BufferedInputStream bis = new BufferedInputStream(fis, 1024 * 125);
			PhysicalPageStream pps = new PhysicalPageStream(bis);
			PacketSegmentStream pss = new PacketSegmentStream(pps);
			PacketStream ps = new PacketStream(pss);
			
			Packet packet = ps.next();
			int nPackets = 0;
			while (packet != null)
			{
				nPackets++;
				packet = ps.next();
			}
			
			return nPackets;
		}
		finally
		{
			fis.close();						
		}
	}*/
	
	public static void writePageToStream(Page page, OutputStream os, OggCRC oggCRC)
		throws IOException
	{
		/*
		 Assert that the segments match the content size.
		 todo: if you can structure the code to disallow invalid page segmentation
		 then this could be changed to an assert*/
		if (page.calculateContentSizeFromSegments() != page.content.length)
			throw new IOException("Incorrect Ogg page segmentation!  Computed size does not match size of content array.");

		//compute the correct CRC
		oggCRC.reset();
		page.checksum = 0;
		oggCRC.update(page.getFixedHeaderBytes());
		byte[] segmentTable = page.getSegmentTableBytes();
		oggCRC.update(segmentTable);
		oggCRC.update(page.content);
		page.checksum = oggCRC.getValue();
		
		//write out the data
		os.write(page.getFixedHeaderBytes());  //Important: must call getFixedHeaderBytes again because the checksum has changed
		os.write(segmentTable);
		os.write(page.content);		
	}
	
	public OggIO()
	{
	}

}
