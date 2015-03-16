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

import adamb.ogg.*;
import java.io.*;

import adamb.util.Util;

public class VorbisPacketStream
{
	static final byte[] VORBIS = {(byte)'v',(byte)'o',(byte)'r',(byte)'b',(byte)'i',(byte)'s'};
	static final int ID_HEADER_TYPE = 1;
	static final int COMMENT_HEADER_TYPE = 3;
	static final int SETUP_HEADER_TYPE = 5;

	private PacketStream packetStream;
	private int packetNumber;
	/**the parsed ID header*/
	private VorbisIDHeader idHeader;
	private Packet commentHeader;
	
	public VorbisPacketStream(PacketStream packetStream)
	{
		this.packetStream = packetStream;
		packetNumber = 0;
	}
	
	public Packet next()
	throws IOException
	{
		packetNumber++;
		
		//one of the first 3 packets?
		if (packetNumber < 4)
		{
			Packet packet = packetStream.next();
			if (packet != null)
			{
				//the ID header must be parsed now because decoding must stop here if it's malformed
				if (packetNumber == 1)
				{
					validateHeaderPacket(packet, ID_HEADER_TYPE);
					idHeader = new VorbisIDHeader(packet);
				}
				//the comment header parsing can be delayed
				else if (packetNumber == 2)
				{
					validateHeaderPacket(packet, COMMENT_HEADER_TYPE);
					commentHeader = packet;
				}
				else if (packetNumber == 3)
					validateHeaderPacket(packet, SETUP_HEADER_TYPE);
				
				return packet;
			}
			//unexpected end of stream - the first 3 packets are required
			else
			{
				//this is usually caused when trying to read a non Ogg stream
				throw new EOFException("Incomplete Vorbis stream!  Missing " + (4 - packetNumber) + " of the 3 required header packets.");
			}
		}
		else
			return packetStream.next();
	}
	
	static void validateHeaderPacket(Packet packet, int expectedType)
	throws IOException
	{
		byte[] bytes = packet.getBytes();
		//each header packet must at least have type, "vorbis", and the framing flag
		if (bytes.length >= (1 + VORBIS.length + 1))
		{
			int type = Util.ubyte(bytes[0]);
			if (type == expectedType)
			{
				//all headers must contain "vorbis"
				if (Util.intervalEquals(bytes, 1, VORBIS))
				{
					/*all headers MUST have the framing bit set.  For the ID and
					 comment headers this will usually be the first bit of the last byte
					 in the packet.  However for the setup header size varies on the bit
					 level rather than the byte level therefore the framing bit position in
					 the final byte will vary.  Without parsing the setup header the best
					 we can do is ensure that 1 and only 1 bit is set in the final byte.					 
					 */
					int framingFlag;

					//bugfix: the spec does not require the comment-structure to fill the packet.  In fact some applications leverage
					//this and write "padding" into the packet.  We must parse over the comment-structure to find the framing bit
					if (type == COMMENT_HEADER_TYPE)
					{
						int commentEndPos = VorbisCommentHeader.getCommentStructureLength(packet);
						if (commentEndPos != -1)
							framingFlag = bytes[commentEndPos];
						else  //else fallback and assume it's the last byte of the packet.  Vorbis spec says "End-of-packet decoding the comment header is a non-fatal error condition"
							framingFlag = Util.ubyte(bytes[bytes.length - 1]);
					}
					else
						framingFlag = Util.ubyte(bytes[bytes.length - 1]);

					if
					(
						(expectedType == SETUP_HEADER_TYPE && Integer.bitCount(framingFlag) == 1)
						|| (expectedType != SETUP_HEADER_TYPE && framingFlag == 1)
					)
					{
						//header packets shall (must?) have granule of 0 according to Vorbis spec
						long absGranulePos = packet.getStartingPage().absGranulePos;
						if (absGranulePos == 0 || absGranulePos == -1)  //bugfix: accept -1 because the Ogg Framing spec says that means "no packets finish on this page".
						{
							//comment header must be on a fresh page
							if (expectedType != COMMENT_HEADER_TYPE || !packet.getStartingPage().isContinued)
							{
								//setup header must end the page it is on
								if (expectedType == SETUP_HEADER_TYPE && !packet.finishesOnPageBoundary())
									throw new IOException("Vorbis setup header must finish on a page boundary!");
							}
							else
								throw new IOException("Vorbis comment header must be on a fresh page!");
						}
						else
							throw new IOException("Vorbis header with non-zero granule position!");
					}
					else
						throw new IOException("Header packet does not have correct framing bit set!" /* + type + " value " + (Util.ubyte(bytes[bytes.length - 1]))*/);
				}
				else
					throw new IOException("Packet does not contain \"vorbis\"!");
			}
			else
				throw new IOException("Incorrect Vorbis Header type " + type + "!  Expected " + expectedType + ".");
		}
		else
			throw new IOException("Packet is too small to be a Vorbis header!");
	}
	
	public VorbisIDHeader getIDHeader()
	{
		assert idHeader != null;
		return idHeader;
	}
	
	public VorbisCommentHeader getCommentHeader()
		throws IOException
	{
		assert commentHeader != null;
		return new VorbisCommentHeader(commentHeader);
	}
}