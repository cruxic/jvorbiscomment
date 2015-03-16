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
package adamb.vorbis;

import adamb.util.Util;
import adamb.ogg.*;
import java.util.*;
import java.io.*;

/**
 A representation of the comments (user meta-data) stored in an Ogg Vorbis stream.
 <p>
 See <a href="http://xiph.org/vorbis/doc/v-comment.html">xiph.org/vorbis/doc/v-comment.html</a>
 for a more detailed and low level description of the comment header.  For instance
 the document covers the standard set of fields:
	<ul>
	<li>TITLE</li>
	<li>VERSION</li>
	<li>ALBUM</li>
	<li>TRACKNUMBER</li>
	<li>ARTIST</li>
	<li>PERFORMER</li>
	<li>COPYRIGHT</li>
	<li>LICENSE</li>
	<li>ORGANIZATION</li>
	<li>DESCRIPTION</li>
	<li>GENRE</li>
	<li>DATE</li>
	<li>LOCATION</li>
	<li>CONTACT</li>
	<li>ISRC</li>
	</ul>
 </p>
 */
public class VorbisCommentHeader
	implements Serializable
{
	/**
	 Vendor name.  This value is mandatory in the comment header.
	 */
	public String vendor;
	
	/**
	 The comment fields (name/value pairs).  Order is significant and duplcates are allowed.
	 For instance, a song could have a primary and alternate title which would be represented
	 by two TITLE fields, the first being the primary title.
	 */
	public List<CommentField> fields;
	
	/**
	 Create a comment header with empty vendor and fields.
	 */
	public VorbisCommentHeader()
	{
		vendor = "";
		fields = new ArrayList<CommentField>(32);
	}
	
	VorbisCommentHeader(Packet packet)
	throws IOException
	{
		this();
		VorbisPacketStream.validateHeaderPacket(packet, VorbisPacketStream.COMMENT_HEADER_TYPE);
		byte[] data = packet.getBytes();
		
		int i = 1 + VorbisPacketStream.VORBIS.length;
		
		//vendor length
		ensureData(i, 4, data);
		int len = Util.asIntLE(data, i, 4);
		i += 4;
		
		//vendor string
		ensureData(i, len, data);
		vendor = Util.asUTF8(data, i, len).trim();  //trim is used here because of the white-space buffering scheme used in VorbisIO.writeComments
		if (vendor == null)
			throw new IOException("Invalid UTF-8 in vendor string");
		i += len;
		
		//number of user fields
		ensureData(i, 4, data);
		int nFields = Util.asIntLE(data, i, 4);
		i += 4;
		
		//read user fields
		for (int j = 0; j < nFields; j++)
		{
			//field length
			ensureData(i, 4, data);
			len = Util.asIntLE(data, i, 4);
			i += 4;
			
			//field string
			ensureData(i, len, data);
			String str = Util.asUTF8(data, i, len);
			i += len;
			if (str != null)
				fields.add(new CommentField(str));
			else
				throw new IOException("Invalid UTF-8 in vendor string");
		}
		
		//skip framing byte
		//i++;

		//bugfix: the spec does not require the comment-structure to fill the packet.  In fact some applications leverage
		//this and write "padding" into the packet so that the comment structure can change without having to rewrite the whole stream.
		//if (i != data.length)
		//	throw new IOException("Vorbis comment structure does not fill comment packet!");
	}

	/**Return the offset of the end of the comment structure.  Only the minimal amount of parsing is done.
	 @return -1 if the comment structure is corrupt*/
	static int getCommentStructureLength(Packet packet)
	{
		byte[] data = packet.getBytes();

		int i = 1 + VorbisPacketStream.VORBIS.length;

		//vendor length
		if (i + 4 > data.length) return -1;
		int len = Util.asIntLE(data, i, 4);
		i += 4;

		//skip vendor string
		if (i + len > data.length) return -1;
		i += len;

		//number of user fields
		if (i + 4 > data.length) return -1;
		int nFields = Util.asIntLE(data, i, 4);
		i += 4;

		//read user fields
		for (int j = 0; j < nFields; j++)
		{
			//field length
			if (i + 4 > data.length) return -1;
			len = Util.asIntLE(data, i, 4);
			i += 4;

			//field string
			if (i + len > data.length) return -1;
			i += len;
		}

		return i;
	}
	
	public byte[] toPacket()
	{
		ByteArrayOutputStream s = new ByteArrayOutputStream(2048);
		
		try
		{
			//type and "vorbis"
			s.write(VorbisPacketStream.COMMENT_HEADER_TYPE);
			s.write(VorbisPacketStream.VORBIS);
			
			//vendor
			byte[] utf8 = vendor.getBytes("UTF-8");
			s.write(Util.intLE(utf8.length));
			s.write(utf8);
			
			//number of fields
			s.write(Util.intLE(fields.size()));
			
			//fields
			StringBuilder sb = new StringBuilder(128);
			for (CommentField field: fields)
			{
				sb.setLength(0);
				sb.append(field.name);
				sb.append('=');
				sb.append(field.value);
				
				utf8 = sb.toString().getBytes("UTF-8");
				s.write(Util.intLE(utf8.length));
				s.write(utf8);
			}
			
			//framing flag
			s.write(1);
		}
		catch (IOException iex)  //shouldn't happen with ByteArrayOutputStream
		{
			throw new RuntimeException(iex);
		}
		
		return s.toByteArray();
	}
	
	private static void ensureData(int offset, int amount, byte[] data)
	throws IOException
	{
		if (offset + amount > data.length)
			throw new IOException("Vorbis comment header is incomplete.");
	}
	
	/**Debugging printout of the values in this object.*/
	public void print()
	{
		System.out.println("vendor=" + vendor);
		for (CommentField cf: fields)
		{
			System.out.print(cf.name);
			System.out.print('=');
			System.out.println(cf.value);
		}
	}
}