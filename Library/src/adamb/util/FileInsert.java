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
package adamb.util;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

//#ifdef TEST
import org.testng.annotations.Test;
import static org.testng.Assert.*;
import java.io.*;
//#endif

public class FileInsert
{
	private byte[] chunk;
	
	public FileInsert(int readChunkSize)
	{
		assert readChunkSize > 0;
		chunk = new byte[readChunkSize];
	}
	
	/**
	 Insert, append, overwrite, or delete an interval of the given file.
	 
	 The simplest way to view this operation is:
	 1) the specified interval is deleted from the file (leaving no holes)
	 2) the new data is inserted at the start of the interval (extending the file length as needed)
	 The algorithm used is more efficient, however.
	 
	 All bytes on the interval [from, to) will be lost (unless from == to in which case no data will be lost).
	 
	 @param file an open file.  The file will <b>not</b> be closed.
	 @param from the file byte position to start insertion at.  Must be <= to the file length. (Equal implies an append)
	 @param to file position of the last byte to overwrite (exclusive).  If from == to then the operation will be purely an insert (no deleted data).  Must not be greater than the file size.
	 @param newData byte array containing the new data
	 @param offset the offset within newData to begin reading from
	 @param len the number of bytes to read from newData.  Pass 0 to delete the file interval
	 */
	public void insert(RandomAccessFile file, long from, long to, byte[] newData, int offset, int len)
	throws IOException
	{
		/*make sure the given interval is acceptable.
		 
			It is important to make sure that everything is in order before we
			start modifying the file because the changes being made are not reversable!
		 */
		long fileLen = file.length();
		if
			(
			//unacceptable interval?
			from < 0 || from > to
			//from and to cant be greater than the file length!
			|| from > fileLen || to > fileLen
			)
			throw new IllegalArgumentException("Invalid insertion interval!: from=" + from + " to=" + to + " (file size=" + fileLen + ")");
		
		//make sure the given data is valid ahead of time
		if (len > 0)
		{
			//the following will throw ArrayIndexOutOfBoundsException if invalid
			byte b = newData[offset];
			b = newData[offset + len - 1];
		}
		
		//the number of bytes to delete
		long intervalSize = to - from;
		
		//the amount the file must be extended (positive) or shrunken (negative)
		long excess = len - intervalSize;
		
		//case 1: file must grow
		if (excess > 0)
		{
			int nRead;
			boolean stop = false;
			int chunkSize = chunk.length;
			long lastPos = fileLen;
			
			while (!stop)
			{
				lastPos -= chunkSize;
				
				//will this be the last chunk
				if (lastPos <= to)
				{
					chunkSize = (int)((lastPos + chunkSize) - to);
					lastPos = to;
					stop = true;
				}
				
				file.seek(lastPos);
				
				//read the entire chunk
				nRead = readCompletely(file, chunk, chunkSize);
				if (nRead != chunkSize)
					throw new IOException("Unexpected read shortage: " + nRead + " bytes instead of " + chunkSize + '!');
				
				file.seek(lastPos + excess);
				file.write(chunk, 0, chunkSize);
			}
		}
		//case 2: file must shrink
		else if (excess < 0)
		{
			long lastPos = to;
			int nRead;
			while (true)
			{
				file.seek(lastPos);
				nRead = file.read(chunk);
				//are we done?
				if (nRead == -1)
					break;
				file.seek(lastPos + excess);  //since excess is negative this will seek backwards from lastPos
				file.write(chunk, 0, nRead);
				lastPos += nRead;
			}
			
			//truncate the file
			file.setLength(fileLen + excess);  //since excess is negative the file will be truncated
		}
		//case 3: file size does not change (no special action necessary)
		
		//finally write in the new data
		file.seek(from);
		file.write(newData, offset, len);
	}
	
	
	/**block until the given byte array can be filled from the stream
	 @return the number bytes actually read.  This will only be less than the length
	 of the byte array if the end of the stream is reached.
	 */
	private int readCompletely(RandomAccessFile is, byte[] bytes, int numToRead)
	throws IOException
	{
		int nRead = is.read(bytes, 0, numToRead);
		while (nRead < numToRead)
		{
			if (nRead == -1)
				return 0;
			else
				nRead += is.read(bytes, nRead, numToRead - nRead);
		}
		
		return nRead;
	}
	
//#ifdef TEST	
	public static class Tester
	{
		private final File tstFile = new File("delete_this_file.bin");
		
		
		
		/**tests for bad argument exceptions*/
		@Test
		public void testArgExceptions()
		throws IOException
		{
			FileInsert fi = new FileInsert(1);
			makeTestFile(tstFile, 10);
			RandomAccessFile raf = new RandomAccessFile(tstFile, "rw");
			byte[] data = new byte[5];
			
			//from negative!
			try
			{
				fi.insert(raf, -1, 1, data, 0, data.length);
				assertTrue(false);  //if the test succeeds this should never happen
			}
			catch (IllegalArgumentException ia)
			{assertTrue(true);}
			
			//from after to!
			try
			{
				fi.insert(raf, 2, 1, data, 0, data.length);
				assertTrue(false);
			}
			catch (IllegalArgumentException ia)
			{assertTrue(true);}
			
			//to negative! (and from after to)
			try
			{
				fi.insert(raf, 0, -1, data, 0, data.length);
				assertTrue(false);
			}
			catch (IllegalArgumentException ia)
			{assertTrue(true);}
			
			//from > file length
			try
			{
				fi.insert(raf, 11, 5, data, 0, data.length);
				assertTrue(false);
			}
			catch (IllegalArgumentException ia)
			{assertTrue(true);}
			
			//to > file length
			try
			{
				fi.insert(raf, 5, 11, data, 0, data.length);
				assertTrue(false);
			}
			catch (IllegalArgumentException ia)
			{assertTrue(true);}
			
			//both from and to > file length
			try
			{
				fi.insert(raf, 11, 11, data, 0, data.length);
				assertTrue(false);
			}
			catch (IllegalArgumentException ia)
			{assertTrue(true);}
			
			//invalid data offset
			try
			{
				fi.insert(raf, 1, 2, data, -1, data.length);
				assertTrue(false);
			}
			catch (ArrayIndexOutOfBoundsException e)
			{
				//file was not modified
				assertTrue(isTestFileGood(tstFile, "", 0, 0));
			}
			
			//data too short
			try
			{
				fi.insert(raf, 1, 2, data, 0, data.length + 1);
				assertTrue(false);
			}
			catch (ArrayIndexOutOfBoundsException e)
			{
				//file was not modified
				assertTrue(isTestFileGood(tstFile, "", 0, 0));
			}
			
			//data too short considering given offset
			try
			{
				fi.insert(raf, 1, 2, data, 1, data.length);
				assertTrue(false);
			}
			catch (ArrayIndexOutOfBoundsException e)
			{
				//file was not modified
				assertTrue(isTestFileGood(tstFile, "", 0, 0));
			}
			
			raf.close();
		}
		
		
		@Test
		public void insertTest()
		{
			try
			{
				//some special cases
				{
					//overwrite an empty file with empty
					FileInsert fi = new FileInsert(1);
					makeTestFile(tstFile, 0);
					RandomAccessFile raf = new RandomAccessFile(tstFile, "rw");
					fi.insert(raf, 0, 0, new byte[0], 0, 0);
					raf.close();
					assertTrue(getFileBytes(tstFile).length == 0);
					
					//overwrite an empty file with 1
					makeTestFile(tstFile, 0);
					raf = new RandomAccessFile(tstFile, "rw");
					fi.insert(raf, 0, 0, new byte[]{Byte.MIN_VALUE}, 0, 1);
					raf.close();
					byte[] bytes = getFileBytes(tstFile);
					assertTrue(bytes.length == 1 && bytes[0] == Byte.MIN_VALUE);
				}
				
				int[] chunkSizes = new int[] {1, 2, 3, 10, 100, 1000, 4096};
				int[] fileSizes = new int[] {9, 10, 100, 1000, 4096, 10001};
				System.out.println("Testing " + (chunkSizes.length * fileSizes.length * 3) + " combinations of FileInsert");
				
			/*try multiple file sizes
			 for each file size...*/
				for (int fileSize: fileSizes)
				{
				/*try multiple chunk (buffer) sizes
				 for each chunk size...*/
					for (int chunkSize: chunkSizes)
					{
						//System.out.println("fileSize " + fileSize + "\tchunkSize " + chunkSize);
						
						FileInsert fi = new FileInsert(chunkSize);
						
					/*Do the same insertion type at different sections of the file
					 (beginning, middle, or end)
					 for each file section...*/
						for (FileSection section: java.util.EnumSet.allOf(FileSection.class))
						{
							//System.out.println("fileSize " + fileSize + "\tchunkSize " + chunkSize + "\tFileSection " + section);
							
							///smaller (overwrite an interval with a smaller interval)
							{
								//overwrite 3 with 2 (cant use size of 1 because that will not be unique in the file!)
								helper(fi, fileSize, section, 3, "CB");
								
								//overwrite first third of the file with 3.  (Strictly speaking this will only be smaller if the file size is larger than 9)
								helper(fi, fileSize, section, fileSize / 3, "CBA");
							}
							
							///equal (overwrite an interval with an equal size interval)
							{
								//overwrite 2 with 2
								helper(fi, fileSize, section, 2, "BA");
								
								//overwrite 3 with 3
								helper(fi, fileSize, section, 2, "CBA");
							}
							
							///larger (overwrite an interval with a larger interval)
							{
								//overwrite 0 with 2
								helper(fi, fileSize, section, 0, "CB");
								
								//overwrite 1 with 2
								helper(fi, fileSize, section, 1, "BA");
								
								//overwrite 1 with 3
								helper(fi, fileSize, section, 1, "CBA");
								
								//overwrite 2 with 3
								helper(fi, fileSize, section, 2, "CBA");
							}
						}
						
						///special cases
						{
							//overwrite entire file with 0
							makeTestFile(tstFile, fileSize);
							RandomAccessFile raf = new RandomAccessFile(tstFile, "rw");
							fi.insert(raf, 0, fileSize, new byte[0], 0, 0);
							raf.close();
							assertTrue(tstFile.length() == 0);
							
							//overwrite entire file with 1
							makeTestFile(tstFile, fileSize);
							raf = new RandomAccessFile(tstFile, "rw");
							fi.insert(raf, 0, fileSize, new byte[]{65}, 0, 1);
							raf.close();
							byte[] bytes = getFileBytes(tstFile);
							assertTrue(bytes.length == 1 && bytes[0] == 65);
							
							//overwrite entire file with different bytes
							makeTestFile(tstFile, fileSize);
							raf = new RandomAccessFile(tstFile, "rw");
							bytes = new byte[fileSize];
							for (int i = 0; i < fileSize; i++)
								bytes[i] = Util.ubyte((255 - (i % 255)));
							fi.insert(raf, 0, fileSize, bytes, 0, bytes.length);
							raf.close();
							bytes = getFileBytes(tstFile);
							for (int i = 0; i < fileSize; i++)
								assertTrue(bytes[i] == Util.ubyte(255 - (i % 255)));
						}
					}
				}
			}
			catch (IOException ex)
			{
				ex.printStackTrace();
			}
		}
		
		enum FileSection
		{
			BEG,
			MID,
			END;
		}
		
		private void helper(FileInsert fi, int fileSize, FileSection section, int amountToOverwrite, String tokenSubstring)
		throws IOException
		{
			File f = tstFile;
			makeTestFile(f, fileSize);
			RandomAccessFile raf = new RandomAccessFile(f, "rw");
			
			int start;
			if (section == FileSection.BEG)
				start = 0;
			else if (section == FileSection.MID)
				start = fileSize / 3;
			else
				start = fileSize - amountToOverwrite;
			
			byte[] tokenData = "CBA".getBytes("UTF-8");
			int tokenOffset = "CBA".indexOf(tokenSubstring);
			
			fi.insert(raf, start, start + amountToOverwrite, tokenData, tokenOffset, tokenSubstring.length());
			raf.close();
			assertTrue(isTestFileGood(f, tokenSubstring, start, amountToOverwrite));
		}
		
		private boolean isTestFileGood(File file, String tokenStr,
			int expectedPosition, int numOverwritten)
			throws IOException
		{
			byte[] token = tokenStr.getBytes();
			int size = (int)file.length();
			BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file), 2048);
			try
			{
				//find the token in the file
				int start = Util.streamFind(bis, token);
				if (start != -1 && start == expectedPosition)
				{
					//reset the stream
					bis.close();
					bis = new BufferedInputStream(new FileInputStream(file), 2048);
					
					//make sure all bytes up to the token are correct
					int pos = 0;
					for (int i = 0; i < start; i++)
					{
						int val = bis.read();
						int expectedVal = pos % 255;
						if (val != expectedVal)
							return false;
						else
							pos++;
					}
					
					//skip over the bytes that we overwrote
					bis.skip(token.length);
					pos += numOverwritten;
					
					
					//make sure all bytes afte the token are correct
					for (int i = start + token.length; i < size; i++)
					{
						int val = bis.read();
						int expectedVal = pos % 255;
						if (val != expectedVal)
							return false;
						else
							pos++;
					}
					
					return true;
				}
				else
					return false;
			}
			finally
			{
				bis.close();
			}
		}
		
		private byte[] getFileBytes(File f)
		throws IOException
		{
			BufferedInputStream bis = new BufferedInputStream(new FileInputStream(f), 2048);
			ByteArrayOutputStream baos = new ByteArrayOutputStream(2048);
			int b;
			while ((b = bis.read()) != -1)
				baos.write(b);
			bis.close();
			return baos.toByteArray();
		}
		
		private void makeTestFile(File file, int size)
		throws IOException
		{
			//create the original file
			file.delete();
			file.createNewFile();
			BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(file), 2048);
			for (int i = 0; i < size; i++)
				bos.write(i % 255);
			
			bos.close();
		}
	}
//#endif
}