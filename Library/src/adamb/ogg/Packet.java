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

import adamb.*;
import java.util.*;

public class Packet
{
	/**The page segments which define this packet.  A packet must have at least
	   one segment but that segment size can be zero.  From the spec:
	 
			"A packet size may well consist only of the trailing fractional segment, and a fractional segment may be zero length."
			- Ogg logical bitstream framing
	 */
	public ArrayList<Segment> segments;
	private byte[] bytes;
	
	Packet()
	{
		segments = new ArrayList<Segment>(4);  //the vorbis streams I have looked at appear to average 2 segments per packet, 4 gives a bit of breathing room
	}
	
	public byte[] getBytes()
	{
		//assemble the bytes if needed
		if (bytes == null)
		{
			//allocate the byte array
			int nBytes = 0;
			for (Segment seg: segments)
				nBytes += seg.size();
			bytes = new byte[nBytes];

			//copy in the bytes
			nBytes = 0;
			for (Segment seg: segments)
				nBytes += seg.getBytes(bytes, nBytes);
		}
			
		return bytes;
	}
	
	/**
		Get the page that this packet started on.
	 */
	public Page getStartingPage()
	{
		return segments.get(0).getSourcePage();
	}
	
	/**
		Get the page this packet belongs to (the page this packet ended on).  From the spec
	 
	"absolute granule position
		...	The position specified is the total samples encoded after including all packets
	 finished on this page (packets begun on this page but continuing on to the next page
	 do not count). The rationale here is that the position specified in the frame header
	 of the last page tells how long the data coded by the bitstream is. A truncated stream
	 will still return the proper number of samples that can be decoded fully."	 
	 
	 */
	public Page getEndingPage()
	{
		return segments.get(segments.size() - 1).getSourcePage();					
	}
	
	/**
		Determine if this packet ends evenly on a page boundary.  If so the next page
	   will begin a new packet.
	 */
	public boolean finishesOnPageBoundary()
	{
		//last segment of this packet
		Segment lastSegment = getLastSegment();
		
		//get segments from the page which ends this packet
		List<Segment> segs = lastSegment.getSourcePage().segments;
		
		/*Note: even though it is possible for a page to have zero segments
		  we know that there is at least one segment on the page or else the segment
		 could not exist.*/
		return lastSegment == segs.get(segs.size() - 1);
	}
	
	/**A convenience method to get the last segment in this packet*/
	public Segment getLastSegment()
	{
		return segments.get(segments.size() - 1);
	}
}
