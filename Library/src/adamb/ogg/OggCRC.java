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

import adamb.util.Util;

/**
 Adapted from Jorbis: com.jcraft.jogg.Page
 */
public class OggCRC
{
  private int[] crc_lookup;
  private int crc_reg;
  
  public OggCRC()
  {
    crc_lookup = new int[256];
    for(int i=0; i<crc_lookup.length; i++)
      crc_lookup[i]=crc_entry(i);
    reset();
  }
  
  public void reset()
  {
    crc_reg = 0;
  }
  
  private int crc_entry(int index)
  {
    int r=index<<24;
    for(int i=0; i<8; i++)
    {
      if((r& 0x80000000)!=0)
      {
        r=(r << 1)^0x04c11db7;
      }
      else
      {
        r<<=1;
      }
    }
    
    return(r&0xffffffff);
  }
  
  public void update(byte[] bytes, int offset, int length)
  {
    for(int i=0;i < length;i++)
    {
      crc_reg=(crc_reg<<8)^crc_lookup[
        ((crc_reg>>>24)&0xff)^(bytes[i + offset]&0xff)];
    }
  }
	
	public void update(byte[] bytes)
	{
		update(bytes, 0, bytes.length);
	}  
  
  public int getValue()
  {
    byte[] bytes = new byte[4];
    
    bytes[0]=(byte)crc_reg;
    bytes[1]=(byte)(crc_reg>>>8);
    bytes[2]=(byte)(crc_reg>>>16);
    bytes[3]=(byte)(crc_reg>>>24);
    
    
    return Util.asIntLE(bytes, 0, 4);
  }
}

