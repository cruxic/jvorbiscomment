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

import java.util.*;

import adamb.util.Util;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**content is defined as the bytes after the header*/
public class Page
{
	/**the size of the page header excluding the variable length segment table*/
	public static final int FIXED_HEADER_SIZE = 27;	
	
	/**
		The maximum size of one page in the page stream. (header size + max number of segments + max content size).
		The maximum number of 255 segments (255 bytes each) sets the maximum possible physical page size at 65307 bytes or just under 64kB
	 */
	public static final int MAX_PAGE_SIZE = FIXED_HEADER_SIZE + 255 + (255 * 255);
	
	/**the header offset of the checksum value*/
	public static final int HEADER_CHECKSUM_OFFSET = 22;
	
	/**Must be zero.  "The capture pattern is followed by the stream structure revision"*/
	public int streamStructureVersion;
	
	/**continued packet*/
	public boolean isContinued;
	/**first page of logical bitstream (bos)*/
	public boolean isFirst;
	/**last page of logical bitstream (eos)*/
	public boolean isLast;
	
	
	/**
	 (This is packed in the same way the rest of Ogg data is packed; LSb of LSB first. Note that the 'position' data specifies a 'sample' number (eg, in a CD quality sample is four octets, 16 bits for left and 16 bits for right; in video it would likely be the frame number. It is up to the specific codec in use to define the semantic meaning of the granule position value). The position specified is the total samples encoded after including all packets finished on this page (packets begun on this page but continuing on to the next page do not count). The rationale here is that the position specified in the frame header of the last page tells how long the data coded by the bitstream is. A truncated stream will still return the proper number of samples that can be decoded fully.
	 
	 A special value of '-1' (in two's complement) indicates that no packets finish on this page.
	 */
	public long absGranulePos;
	
	
	/**Ogg allows for separate logical bitstreams to be mixed at page granularity in a physical bitstream. The most common case would be sequential arrangement, but it is possible to interleave pages for two separate bitstreams to be decoded concurrently. The serial number is the means by which pages physical pages are associated with a particular logical stream. Each logical stream must have a unique serial number within a physical stream.*/
	public int streamSerialNumber;
	
	/*Page counter; lets us know if a page is lost (useful where packets span page boundaries).*/
	public int sequence;
	
	public int checksum;
	
	/**The segments on this page.  A page may contain from 0 to 255 segments.*/
	public ArrayList<Segment> segments;
	public byte[] content;
	
	public Page()
	{
		/*
		 "Ogg bitstream specification strongly recommends nominal page size of approximately 4-8kB"
			Thus we should pre-allocate the segment list for that size to avoid growing
		 the arraylist in the average case.*/
		segments = new ArrayList<Segment>(32);
		
		streamStructureVersion = 0;
	}
	
	public byte[] getFixedHeaderBytes()
	{
		byte[] bytes = new byte[FIXED_HEADER_SIZE];
		ByteBuffer bb = ByteBuffer.wrap(bytes);
		bb.order(ByteOrder.LITTLE_ENDIAN);  //Ogg values are little endian
		
		//capture pattern
		bb.put(PhysicalPageStream.OGG_STREAM_CAPTURE_PATTERN);
		
		bb.put(Util.ubyte(streamStructureVersion));
		
		//flags
		byte flags = 0;
		if (isContinued)
			flags |= 1;
		if (isFirst)
			flags |= 2;
		if (isLast)
			flags |= 4;
		bb.put(flags);
		
		bb.putLong(absGranulePos);
		
		bb.putInt(streamSerialNumber);
		
		bb.putInt(sequence);
		
		bb.putInt(checksum);
		
		//segment count
		assert segments.size() <= 255;
		bb.put(Util.ubyte(segments.size()));
		
		assert bb.position() == FIXED_HEADER_SIZE;

		return bytes;
	}
	
	public byte[] getSegmentTableBytes()
	{
		byte[] segmentTable = new byte[segments.size()];
		for (int i = 0; i < segmentTable.length; i++)
			segmentTable[i] = Util.ubyte(segments.get(i).size());
		
		return segmentTable;
	}
	
	public int calculateContentSizeFromSegments()
	{
		int contentSize = 0;
		for (Segment segment: segments)
			contentSize += segment.size();
		
		return contentSize;
	}	
	
	/**
	 Populate the header values from the bytes.
	 

	 @param fixedHeaderBytes raw header data.  If length of array is FIXED_HEADER_SIZE then it must begin with the capture pattern.
	 @return the segment count
	 @throws InvalidHeaderException if the bytes do not begin with the ogg
	   stream capture pattern: "OggS" or any other header values are incorrect.  The page is corrupt in some way.
	 */
	public int parseFixedHeaderValues(byte[] fixedHeaderBytes)
		throws InvalidHeaderException
	{
		//do the bytes need to begin with the capture pattern?
		boolean needCapture = fixedHeaderBytes.length == FIXED_HEADER_SIZE;
		
		if (needCapture && !Util.startsWith(fixedHeaderBytes, PhysicalPageStream.OGG_STREAM_CAPTURE_PATTERN))
			throw new InvalidHeaderException("Ogg page does not begin with \"OggS\"!");
		
		ByteBuffer bb = ByteBuffer.wrap(fixedHeaderBytes);
		bb.order(ByteOrder.LITTLE_ENDIAN);  //Ogg values are LE

		//skip the capture pattern
		if (needCapture)
			bb.position(PhysicalPageStream.OGG_STREAM_CAPTURE_PATTERN.length);
		
		streamStructureVersion = Util.ubyte(bb.get());
		if (streamStructureVersion != 0)
			throw new InvalidHeaderException("Wrong Ogg stream structure revision " + streamStructureVersion);
		
		int flags = Util.ubyte(bb.get());
		isContinued = (flags & 1) > 0;
		isFirst = (flags & 2) > 0;
		isLast = (flags & 4) > 0;

		absGranulePos = bb.getLong();
		streamSerialNumber = bb.getInt();
		sequence = bb.getInt();
		checksum = bb.getInt();
		int segmentCount = Util.ubyte(bb.get());

		return segmentCount;
	}

	/**
	 @return the calculated content size for convenience (a by product of parsing the segment table)
	 */
	public int parseSegmentTable(byte[] segmentTable)
	{
		/*build the segment objects and calculate the content size*/
		int offset = 0;
		int lacingValue;
		for (int i = 0; i < segmentTable.length; i++)
		{
			lacingValue = Util.ubyte(segmentTable[i]);
			segments.add(new Segment(this, offset, lacingValue));
			offset += lacingValue;
		}
		
		return offset;
	}
	
	
	/**Get the total page size (as it was in the Ogg stream).
	 This includes the header and contents*/
	public int size()
	{
		return FIXED_HEADER_SIZE + segments.size() + content.length;
	}
	
	/**
	 Compare all or most members for equality.
	 */
	public boolean equals(Page p, boolean ignoreCRC, boolean ignoreSequence)
	{
		return streamStructureVersion == p.streamStructureVersion
			&& isContinued == p.isContinued
			&& isFirst == p.isFirst
			&& isLast == p.isLast
			&& absGranulePos == p.absGranulePos
			&& streamSerialNumber == p.streamSerialNumber
			&& (ignoreSequence || sequence == p.sequence)
			&& (ignoreCRC || checksum == p.checksum)
			&& Arrays.equals(content, p.content)
			&& Arrays.equals(getSegmentTableBytes(), p.getSegmentTableBytes());
	}	
}