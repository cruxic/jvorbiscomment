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
import adamb.util.Util;
import java.io.IOException;

public class VorbisIDHeader
{
	public int audioChannels;
	public long audioSampleRate;
	public int maxBitRate;
	public int minBitRate;
	public int nominalBitRate;
	public int blockSize0;
	public int blockSize1;
	
	VorbisIDHeader(Packet packet)
		throws IOException
	{
		byte[] data = packet.getBytes();
		if (data.length == 30)
		{
			int i = 1 + VorbisPacketStream.VORBIS.length;
			//vorbis version
			int vorbisVersion = Util.asIntLE(data, i, 4);
			i += 4;
			//audio channels
			audioChannels = Util.ubyte(data[i]);
			i++;
			//audio sample rate
			audioSampleRate = Util.asLongLE(data, i, 4);
			i += 4;
			//max bit rate
			maxBitRate = Util.asIntLE(data, i, 4);
			i += 4;
			//nominal bit rate
			nominalBitRate = Util.asIntLE(data, i, 4);
			i += 4;
			//min bit rate
			minBitRate = Util.asIntLE(data, i, 4);
			i += 4;
			//block sizes
			int exponent0 = Util.lowNibble(data[i]);
			int exponent1 = Util.highNibble(data[i]);
			blockSize0 = (int)Math.pow(2D, exponent0);
			blockSize1 = (int)Math.pow(2D, exponent1);
			i++;
			//framing flag
			int framingFlag = Util.ubyte(data[i]);
			i++;
			assert i == 30: i;
			
			//vorbis version must be 0
			if (vorbisVersion == 0)
			{
				if (audioChannels > 0)
				{
					if (audioSampleRate > 0)
					{
						if
							(
							blockSize0 >= 64 && blockSize0 < 8192
							&& blockSize1 >= 64 && blockSize1 < 8192
							&& blockSize0 <= blockSize1
							)
						{
							return;		
						}
						else
							throw new IOException("Invalid block size values (" + blockSize0 + "," + blockSize1 + ")");
					}
					else
						throw new IOException("Audio sample rate must be greater than 0");
				}
				else
					throw new IOException("Audio channels must be > 0");
			}
			else
				throw new IOException("Incompatible Voribis version " + vorbisVersion);
		}
		else
			throw new IOException("Invalid packet size " + data.length + " for Vorbis ID header");
	}
	
	public void print()
	{
		System.out.println("audioChannels=" + audioChannels);
		System.out.println("audioSampleRate=" + audioSampleRate);
		System.out.println("maxBitRate=" + maxBitRate);
		System.out.println("minBitRate=" + minBitRate);
		System.out.println("nominalBitRate=" + nominalBitRate);
		System.out.println("blockSize0=" + blockSize0);
		System.out.println("blockSize1=" + blockSize1);
	}
}
