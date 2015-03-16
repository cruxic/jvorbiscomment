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

/**
 A page segment belonging to a packet.
 Segments can be empty.
 */
public class Segment
{
	private Page sourcePage;
	private int pageOffset;
	private int size;
	
	public Segment(Page sourcePage, int pageOffset, int size)
	{
		this.sourcePage = sourcePage;
		this.pageOffset = pageOffset;
		this.size = size;
	}
	
	public Page getSourcePage()
	{
		return sourcePage;
	}
	
	/**
	 @return true if this is the last segment in the packet (size < 255).
	 */
	public boolean isLast()
	{
		return size < 255;
	}
	
	public int size()
	{
		return size;
	}
	
	/**
	 Copy the segment bytes into the destination byte array at the specified offset
	 @return the size of the segment
	 */
	public int getBytes(byte[] dest, int offset)
	{
		System.arraycopy(sourcePage.content, pageOffset, dest, offset, size);
		return size;
	}
	
	public byte[] getBytes()
	{
		byte[] bytes = new byte[size];
		getBytes(bytes, 0);
		return bytes;
	}
}
