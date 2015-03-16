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
import java.util.*;
import adamb.*;

public class PacketStream
{
	private PacketSegmentStream segmentStream;
	private Segment prevSegment;
	
	public PacketStream(PacketSegmentStream segmentStream)
	{
		this.segmentStream = segmentStream;
	}
	
	public Packet next()
	throws IOException
	{
		Packet packet = new Packet();
		Segment seg = segmentStream.next();
		while (seg != null)
		{
			//this is not the fist segment in the stream?
			if (prevSegment != null)
			{
				//page is changing?
				if (prevSegment.getSourcePage() != seg.getSourcePage())
				{
					//if previous was not the last in the packet then this new page must be marked as a continuation
					if (!prevSegment.isLast() && !seg.getSourcePage().isContinued)
						throw new IOException("Last page did not complete the packet and the new page is not marked as a continuation!");
				}
			}
			else
			{
				//this is the first segment from the first page and thus the page must not be marked as a continuation
				if (seg.getSourcePage().isContinued)
					throw new IOException("First page marked as a continuation!");
			}
			
			prevSegment = seg;
			packet.segments.add(seg);
			if (seg.isLast())
				return packet;
			else
				seg = segmentStream.next();
		}
		
		//end of stream
		return null;
	}
}