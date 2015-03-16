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

import adamb.util.*;
import adamb.ogg.*;
import java.io.*;
import java.util.*;
import org.archive.io.*;

//For unit testing
import org.testng.annotations.*;
import static org.testng.Assert.*;

/**
 Common operations on Ogg Vorbis audio files.
 The most common being {@link #readComments(File) readComments} and
 {@link #writeComments(File, VorbisCommentHeader) writeComments}
 <h2>Examples</h2>
 <h3>1 - Reading comments</h3>
 <code><pre>
		import adamb.vorbis.*;
		import java.io.*;

		public class ReadVorbis
		{
			public static void main(String[] args)
			{
				try
				{
					File oggFile = new File(args[0]);
					VorbisCommentHeader comments = VorbisIO.readComments(oggFile);
					comments.print();
				}
				catch (IOException ioe)
				{
					ioe.printStackTrace();
				}
			}
		} 
 </pre></code>
 
 <h3>2 - Writing comments (overwriting)</h3>
 <code><pre>
		import adamb.vorbis.*;
		import java.io.*;

		public class WriteVorbis
		{
			public static void main(String[] args)
			{
				try
				{
					File oggFile = new File(args[0]);			

 					VorbisCommentHeader comments = new VorbisCommentHeader();
					comments.vendor = "Me";
					comments.fields.add(new CommentField("TITLE", "Song sung blue"));
					comments.fields.add(new CommentField("ARTIST", "Neil Diamond"));
					comments.fields.add(new CommentField("HOW_IT_MAKES_YOU_FEEL", "Better than before"));

					VorbisIO.writeComments(oggFile, comments);
				}
				catch (IOException ioe)
				{
					ioe.printStackTrace();
				}
			}
		} 
 
 </pre></code>
	
 <h3>3 - Updating comments efficiently</h3>
 <p>
 One very common need is to modify a few comment fields but leave the others
 intact.  Your intuition might lead you write to something like this:
 <code><pre>writeComments(file, readComments(file))</pre></code>
 Although this is clean and simple, it is not the most efficient way.  This is because the
 file must be opened and parsed twice, once to read the comments and again to write them.
 The following method is more efficient and thus more well suited to bulk comment
 editing.
 </p>
 <code><pre>
	import adamb.vorbis.*;
	import java.io.*;
	
	public class VorbisUpdate
		implements CommentUpdater
	{
		public static void main(String[] args)
		{
			try
			{
				VorbisUpdate updater = new VorbisUpdate();
				File oggFile = new File(args[0]);

				VorbisIO.writeComments(oggFile, updater);
			}
			catch (IOException ioe)
			{
				ioe.printStackTrace();
			}
		}

		public boolean updateComments(VorbisCommentHeader comments)
		{
			//abort the update if it already has the ENCODED_BY field
			for (CommentField field: comments.fields)
			{
				if (field.name.equals("ENCODED_BY"))
					return false;
			}

			//it doesn't have the field so add it and update the file
			comments.fields.add(new CommentField("ENCODED_BY", "Mr. Ripper"));
			return true;
		}
	}
 
 </pre></code>
 */
public class VorbisIO
{
	/**
	 Equivalent to {@link #readComments(File,boolean) readComments(f, true)}.
	 */
	public static VorbisCommentHeader readComments(File f)
	throws IOException
	{
		return readComments(f, true);
	}
	
	/**
	 Read comments from an Ogg Vorbis file.
	 @param f the ogg vorbis file to read from.
	 @param ignoreAllRecoverableErrorsAndWarnings pass true to ignore forgivable problems with the file.  False will cause an exception to be thrown upon any problems.
	 @return the comment structure
	 @throws IOException if there is an error reading the comment structure
	 */
	public static VorbisCommentHeader readComments(File f, boolean ignoreAllRecoverableErrorsAndWarnings)
	throws IOException
	{
		FileInputStream fis = new FileInputStream(f);
		
		try
		{
			PhysicalPageStream pps = new PhysicalPageStream(new BufferedInputStream(fis, 256 * 1024));
			LogicalPageStream lps;
			
			if (ignoreAllRecoverableErrorsAndWarnings)
				lps = new StreamSerialFilter(new ErrorTolerantPageStream(pps), false);
			else
				lps = new ValidatingPageStream(new StreamSerialFilter(pps, true));
			
			return readComments(lps);
		}
		finally
		{
			fis.close();
		}
	}
	
	private static VorbisCommentHeader readComments(LogicalPageStream lps)
	throws IOException
	{
		VorbisPacketStream vps = new VorbisPacketStream(new PacketStream(new PacketSegmentStream(lps)));
		//read id and comments
		vps.next();
		//read comments
		vps.next();
		
		return vps.getCommentHeader();
	}
	
	/**
	 Replace the comments in an Ogg Vorbis file.
	 */
	public static void writeComments(File f, VorbisCommentHeader newComments)
	throws IOException
	{
		writeComments(f, newComments, null);
	}
	
	/**
	 Read and, optionally, update the comments in an Ogg Vorbis file.<p>This is slightly
	 more efficient than <code>writeComments(f, readComments(f))</code> because the file
	 is only opened and parsed once.</p>
	 
	 @param f the ogg vorbis file
	 @param commentUpdater a hook interface allowing the caller to modify the existing comments or abort the update without making changes.
	 */
	public static void writeComments(File f, CommentUpdater commentUpdater)
	throws IOException
	{
		writeComments(f, null, commentUpdater);
	}
	
	private static void writeComments(File f, VorbisCommentHeader newComments, CommentUpdater commentUpdater)
	throws IOException
	{
		assert newComments != null || commentUpdater != null;
		//System.out.println("writeComments: " + f.getPath());
		
		///open the file for read/write
		//throw an exception if the file does not exist otherwise RandomAccessFile will create it
		if (!f.exists())
			throw new FileNotFoundException(f.getPath() + " does not exist!");
		RandomAccessFile raf = new RandomAccessFile(f, "rw");
		RandomAccessInputStream rais = new RandomAccessInputStream(raf);
		
		//create a logical page stream that tolerates corruption and skips pages from foreign Ogg streams
		LogicalPageStream lps = new StreamSerialFilter(
			new ErrorTolerantPageStream(
			new PhysicalPageStream(rais)),
			false);
		
		//create a VorbisPacketStream
		VorbisPacketStream vps = new VorbisPacketStream(
			new PacketStream(
			new PacketSegmentStream(lps)));
		
		try
		{
			//read id packet
			Packet idPacket = vps.next();
			
			//read comment packet
			long commentPagePos = raf.getFilePointer();
			Packet comments = vps.next();
			int commentPageNum = comments.getStartingPage().sequence;
			
			boolean attemptUpdate = true;
			if (commentUpdater != null)
			{
				//parse the comment packet
				newComments = new VorbisCommentHeader(comments);
				
				//ask the comment updater if we should change anything
				if (!commentUpdater.updateComments(newComments))
					attemptUpdate = false;
			}
			//else assume newComments was given
			
			if (attemptUpdate)
			{
				/*build the new comments.  Ideally we will keep the exact same comment
				 size so that the entire file does not have to be re-written.  Therefore
				 we will use spaces at the end of the vendor string to give breathing room.*/
				byte[] newCommentPacket = newComments.toPacket();
				{
					int sizeDiff = comments.getBytes().length - newCommentPacket.length;
					
				/* The maxium amount of padding characters to use. Since we are
				 padding with spaces after the vendor string it is best to keep this
				 small.  That way, those programs that read the vendor string,
				 without first trimming the whitespace, will not be flooded with
				 an unexpectedly large number of characters.*/
					final int RESIZE_THRESHOLD = 128;
					
					int fillAmount;
					//if new comments are just a bit smaller then use filler to make up the difference
					if (sizeDiff >= 0 && sizeDiff <= RESIZE_THRESHOLD)
						fillAmount = sizeDiff;
					//else, since we are going to have to grow the file we might as well add a little breathing room
					else
						fillAmount = RESIZE_THRESHOLD;
					
					if (fillAmount > 0)
					{
						StringBuilder sb = new StringBuilder(newComments.vendor.length() + fillAmount);
						sb.append(newComments.vendor);
						while (fillAmount > 0)
						{
							sb.append(' ');
							fillAmount--;
						}
						
						String oldVendor = newComments.vendor;
						newComments.vendor = sb.toString();
						newCommentPacket = newComments.toPacket();
						newComments.vendor = oldVendor;
					}
				}
				
				//only continue with the update if the new comments are different at all
				if (!Arrays.equals(newCommentPacket, comments.getBytes()))
				{
					//read the setup packet
					Packet setup = vps.next();
					long firstAudioPagePos = raf.getFilePointer();
					int setupLastPageNum = setup.getLastSegment().getSourcePage().sequence;
					
					//System.out.println("writeComments: preparing to write");
					
				/*number of pages used for the comment and setup packets.  we will try
					to match this so that we can avoid changing the page sequence numbers
					for the entire stream*/
					int oldNumPagesUsed = setupLastPageNum - commentPageNum + 1;

					ArrayList<Page> pages = pagify(new byte[][]{newCommentPacket, setup.getBytes()}, true);					
					
					/*
						This is the old way I used to pagify.  This didn't work for my iAudio U2
					 because the setup packet is almost always > 4kb and thus, after adding
					 the nominal flag to pagify it would have created too many pages.
					 
					ArrayList<Page> pages = new ArrayList<Page>(4);
					if (oldNumPagesUsed == 1)
						pages.addAll(pagify(new byte[][]{newCommentPacket, setup.getBytes()}));
					else
					{
						pages.addAll(pagify(new byte[][]{newCommentPacket}));
						pages.addAll(pagify(new byte[][]{setup.getBytes()}));
					}*/
					
					//System.out.println("writeComments: nPages_old=" + oldNumPagesUsed + " nPages_new=" + pages.size());
					
					//fill out the page header values
					{
						Page firstPage = idPacket.getStartingPage();
						int lastSequenceNumber = firstPage.sequence;
						for (Page page: pages)
						{
							page.streamSerialNumber = firstPage.streamSerialNumber;
							/*Vorbis I specification: "The granule position of these first pages containing only headers is zero."*/
							page.absGranulePos = 0;
							page.sequence = ++lastSequenceNumber;
							//currently this will always be zero but just to be flexible...
							page.streamStructureVersion = firstPage.streamStructureVersion;
							page.isFirst = false;
							page.isLast = false;
							//isContinued was filled out by pagify
						}
					}
					
					//create the page data array
					ByteArrayOutputStream bbos = new ByteArrayOutputStream(1024 * 8);
					OggCRC oggCRC = new OggCRC();
					for (Page p: pages)
						OggIO.writePageToStream(p, bbos, oggCRC);
					byte[] data = bbos.toByteArray();
					bbos = null; //free the memory
					
					//the amount to increase or decrease the page sequence number
					int pageSequenceAdjust = pages.size() - oldNumPagesUsed;
					
					//System.out.println("writeComments: inserting " + data.length);
					
					//replace the old comment and setup pages with the new ones
					FileInsert fileInsert = new FileInsert(1024 * 512);
					fileInsert.insert(raf, commentPagePos, firstAudioPagePos, data, 0, data.length);
					
					//System.out.println("writeComments: page sequence adjust=" + pageSequenceAdjust);
					
					//need to adjust?
					if (pageSequenceAdjust != 0)
					{
						//the audio packet page may have shifted from the insert operation
						firstAudioPagePos = commentPagePos + data.length;
						raf.seek(firstAudioPagePos);
						
						RandomAccessOutputStream raos = new RandomAccessOutputStream(raf);
						
					/*IMPORTANT: remember that the page stream might not
					 be continguous in the file if there is corruption or the
					 stream is multiplexed*/
						Page page = lps.next();
						while (page != null)
						{
							//adjust the page sequence number
							page.sequence += pageSequenceAdjust;
							
							//seek back to the start of the page and rewrite the page
							raf.seek(raf.getFilePointer() - page.size());
							OggIO.writePageToStream(page, raos, oggCRC);  //todo: would it be faster to simply write the fixed header and skip over the content?
							
							//next page
							page = lps.next();
						}
					}
				}
			}
		}
		finally
		{
			raf.close();
		}
		//System.out.println("writeComments: done");
	}
	
	/**put one or more packets on the minimum number of required pages
	 
	 @param nominalPageSize pass true if page sizes should be limited to ~4kb.
	 False means pages hold as much data as possible (~64kb).
	 The "Ogg Logical bitstream framing" document says:
	 "Ogg bitstream specification strongly recommends nominal page size of approximately 4-8kB".
	 I found that my portable music player (iAudio U2) cannot handle page sizes larger than 4kb.
	 
	 */
	private static ArrayList<Page> pagify(byte[][] packets, boolean nominalPageSize)
	{
		assert packets.length > 0;
		
		int maxSegmentsPerPage = 255;
		if (nominalPageSize)
			maxSegmentsPerPage = 17;
		
		ArrayList<Page> pages = new ArrayList<Page>(8);
		ByteArrayOutputStream content = new ByteArrayOutputStream(1024 * 16);
		
		int packetIdx = 0;
		int packetOffset = 0;
		int lastSegmentSize = 0;
		
		int pageSegOffset = 0;
		Page page = new Page();
		page.isContinued = false;  //we know for a fact that the first page will not continue any packets
		
		while (packetIdx < packets.length)
		{
			//work on an individual packet
			while (true)
			{
				//have room for another segment on this page?
				if (page.segments.size() < maxSegmentsPerPage)
				{
					//any packet data left to copy?
					if (packetOffset < packets[packetIdx].length)
					{
						//write the segment data
						int remainder = packets[packetIdx].length - packetOffset;
						lastSegmentSize = Math.min(255, remainder);
						content.write(packets[packetIdx], packetOffset, lastSegmentSize);
						
						//add the segment object
						page.segments.add(new Segment(page, pageSegOffset, lastSegmentSize));
						
						packetOffset += lastSegmentSize;
						pageSegOffset += lastSegmentSize;
					}
					else
					{
						/*we are done with the packet.  If the last segment was 255 then we
						 need to write a 0 segment to denote the end of the packet.  From the spec:
						 
						"Note that a lacing value of 255 implies that a second lacing value follows
						 in the packet, and a value of < 255 marks the end of the packet after that
						 many additional bytes. A packet of 255 bytes (or a multiple of 255 bytes)
						 is terminated by a lacing value of 0"
						 */
						if (lastSegmentSize == 255)
							page.segments.add(new Segment(page, pageSegOffset, 0));
						
						//move on to the next packet
						break;
					}
				}
				//page is full.  start another page
				else
				{
					//copy in the content
					page.content = content.toByteArray();
					assert page.content.length == page.calculateContentSizeFromSegments();
					content.reset();
					pages.add(page);
					
					//prepare another page
					pageSegOffset = 0;
					Segment lastSegment = page.segments.get(page.segments.size() - 1);
					page = new Page();
					//this new page continues a packet if the previous page finished all it's packets
					page.isContinued = !lastSegment.isLast();
				}
			}
			
			packetIdx++;
			packetOffset = 0;
		}
		
		//add the remaining page only if it's not empty
		if (page.segments.size() > 0)
		{
			page.content = content.toByteArray();
			pages.add(page);
		}
		
		return pages;
	}

	/**For unit testing*/
	public static class Tester
	{
		
		private String buildMissingHdrMessage(int n)
		{
			return "Incomplete Vorbis stream!  Missing " + n + " of the 3 required header packets.";
		}
		
		
		@Test
		public void commentIOTest()
		throws IOException
		{
			final File dir = new File("test oggs");
			
			//final String FAILED_TO_CAPTURE_MSG = "Failed to capture Vorbis header packets due to stream corruption";
			
			//an error free ogg vorbis file
			File f = new File(dir, "error free.ogg");
			hammerTestFile(f);
			
			//a file that does not exist
			try
			{
				f = new File(dir, "does not exist.ogg");
				readComments(f);
				writeComments(f, new VorbisCommentHeader());
				assertTrue(false);
			}
			catch (FileNotFoundException fnf)
			{
				assertTrue(true);
			}
			
			//an empty file
			try
			{
				f = new File(dir, "zero bytes.ogg");
				hammerTestFile(f);
				assertTrue(false);
			}
			catch (EOFException eof)
			{
				assertTrue(eof.getMessage().equals(buildMissingHdrMessage(3)));
			}
			
			//a non Ogg stream that contains the "OggS" capture pattern
			try
			{
				f = new File(dir, "not really ogg.ogg");
				hammerTestFile(f);
				assertTrue(false);
			}
			catch (EOFException eof)
			{
				//are eof exceptions actually warnings?  what if the ogg file has something tacked onto the end?  how is that any different than having garbage data mid stream?
				//also I need to add a warning about last page not marked as last
				//	or should stream parsing stop when the last page is found?
				assertTrue(eof.getMessage().equals(buildMissingHdrMessage(3)));
			}
			
			//a file with only the vorbis ID header
			try
			{
				f = new File(dir, "id header only.ogg");
				hammerTestFile(f);
				assertTrue(false);
			}
			catch (EOFException eof)
			{
				assertTrue(eof.getMessage().equals(buildMissingHdrMessage(2)));
			}
			
			//a file with the 3 required vorbis headers (id, comment, and setup)
			f = new File(dir, "vorbis headers only.ogg");
			hammerTestFile(f);
			
			
			//the 3 headers and a single audio page
			f = new File(dir, "1 audio page.ogg");
			hammerTestFile(f);
			
			
			//second page containing comment and setup packets is corrupt
			try
			{
				f = new File(dir, "corrupt page 2.ogg");
				hammerTestFile(f);
				assertTrue(false);
			}
			catch (EOFException eof)
			{
				assertTrue(eof.getMessage().equals(buildMissingHdrMessage(2)));
			}
			
			//the 3 headers and a single audio page that is corrupt
			f = new File(dir, "1 corrupt audio page.ogg");
			hammerTestFile(f);
			
			
			f = new File(dir, "2 audio pages.ogg");
			hammerTestFile(f);
			
			//first audio page has checksum mismatch
			f = new File(dir, "2 audio pages, 1st is corrupt.ogg");
			hammerTestFile(f);
			
			//second audio page has checksum mismatch
			f = new File(dir, "2 audio pages, 2nd is corrupt.ogg");
			hammerTestFile(f);
			
			
			//both pages are valid (even CRC) but pages have wrong revision number
			try
			{
				f = new File(dir, "headers only but wrong stream revision.ogg");
				hammerTestFile(f);
				assertTrue(false);
			}
			catch (EOFException eof)
			{
				assertTrue(eof.getMessage().equals(buildMissingHdrMessage(3)));
			}
			
			//garbage data has been inserted after the first page and after the second page
			f = new File(dir, "vorbis headers with garbage after both pages.ogg");
			hammerTestFile(f);
			
			//6 audio pages 2 of which are have are from a foreign stream (different stream serial number)
			f = new File(dir, "pages from foreign stream.ogg");
			hammerTestFile(f);
			
			//2 completely empty pages (zero segments), one mid stream and one at the end
			f = new File(dir, "some empty pages.ogg");
			hammerTestFile(f);
		}
		
		//java -enableassertions -cp /home/cruxic/tmp/emma-2.0.5312/lib/emma.jar emmarun -r html -sp src -cp dist/JVorbisComment.jar:dist/lib/junit-4.1.jar org.junit.runner.JUnitCore adamb.vorbis.VorbisIO
		
		
		private void hammerTestFile(File originalFile)
		throws IOException
		{
			System.out.println(originalFile.getName());
			
			//copy the original file into a temporary file
			File tmp = new File("deleteme.ogg");
			tmp.delete();
			Util.copyFile(originalFile, tmp);
			
			//read the entire stream into memory so we can detect corruption after it is modified
			ArrayList<Page> origPages = readOggIntoMemory(tmp);
			
			final int[] changes = {0, 2, 1, 3, 0, 3};
			final int[] scales = {1, 2, 60, 1111, 1024 * 125};
			
			for (int scale: scales)
			{
				for (int change: changes)
				{
					runTestOnFile(tmp, scale * change, origPages);
				}
			}
		}
		
		private void runTestOnFile(File f, int commentSize, ArrayList<Page> origPages)
		throws IOException
		{
			//System.out.println(f.getName() + "\t" + commentSize);
			
			String str = makeRandomString(commentSize);
			
			VorbisCommentHeader vch = new VorbisCommentHeader();
			vch.fields.add(new CommentField("T", str));
			
			writeComments(f, vch);
			
			ArrayList<Page> newPages = readOggIntoMemory(f);
			
			vch = comparePagesAfterCommentChange(origPages, newPages);
			
			assertTrue(vch.vendor.length() == 0);
			assertTrue(vch.fields.size() == 1);
			assertTrue(vch.fields.get(0).name.equals("T"));
			assertTrue(vch.fields.get(0).value.equals(str));
		}
		
		private ArrayList<Page> readOggIntoMemory(File f)
		throws IOException
		{
			FileInputStream fis = new FileInputStream(f);
			
			try
			{
				ArrayList<Page> pages = new ArrayList<Page>(128);
				//I am ignoring warnings on purpose here
				PageStream ps = new ErrorTolerantPageStream(new PhysicalPageStream(new BufferedInputStream(fis, 256 * 1024)));
				Page p = ps.next();
				while (p != null)
				{
					pages.add(p);
					p = ps.next();
				}
				
				return pages;
			}
			finally
			{
				fis.close();
			}
		}
		
		private VorbisCommentHeader comparePagesAfterCommentChange(ArrayList<Page> pagesBefore, ArrayList<Page> pagesAfter)
		throws IOException
		{
			//first page must be unchanged because it is the ID header
			assertTrue(pagesBefore.get(0).equals(pagesAfter.get(0), false, false));
			
			CollectionPageStream origCPS = new CollectionPageStream(pagesBefore);
			CollectionPageStream newCPS = new CollectionPageStream(pagesAfter);
			
			///make sure the id and setup packets are unchanged
			VorbisPacketStream vps = new VorbisPacketStream(new PacketStream(new PacketSegmentStream(origCPS)));
			vps.next();  //skip id
			vps.next();  //skip comments
			Packet origSetup = vps.next();
			
			vps = new VorbisPacketStream(new PacketStream(new PacketSegmentStream(newCPS)));
			vps.next();  //skip id
			vps.next();  //skip comments
			Packet newSetup = vps.next();
			
			//setup packets must be unchanged
			assertTrue(Arrays.equals(newSetup.getBytes(), origSetup.getBytes()));
			
			///remaining pages must be unchanged except for sequence number and CRC
			Page origPage = origCPS.next();
			Page newPage = newCPS.next();
			
			boolean first = true;
			boolean ignore = true;
			
			while (origPage != null)
			{
				assertTrue(newPage != null);
				
				assertTrue(origPage.equals(newPage, ignore, ignore));
				
			//if this is the first, determine if there was a sequence change at all.
			// because if not we should look for exact equality rather than ignoring
			// sequence and CRC
				if (first)
				{
					first = false;
					
					if (origPage.sequence == newPage.sequence)
					{
						ignore = false;
						
						//re-compare the pages more strictly
						assertTrue(origPage.equals(newPage, ignore, ignore));
					}
				}
				
				origPage = origCPS.next();
				newPage = newCPS.next();
			}
			
			//both streams must be exhausted at the same time
			assertTrue(newPage == null);
			
			//return the parsed comments
			return vps.getCommentHeader();
		}
		
		
		
		private String makeRandomString(int numChars)
		{
			byte[] numbers = new byte[numChars];
			new Random().nextBytes(numbers);
			StringBuilder sb = new StringBuilder(numChars);
			for (byte b: numbers)
			{
				char c = (char)Math.abs((int)b);
				if (c < ' ')
					c = ' ';
				else if (c > '~')
					c = ' ';
				
				sb.append(c);
			}
			
			return sb.toString();
		}
		
		@Test
		public void pagifyTest()
		{
			//sanity test for makePackets
			{
				byte[][] packets = makePackets(0,1,2,3,399);
				assertTrue(packets[0].length == 0);
				assertTrue(packets[1].length == 1);
				assertTrue(packets[2].length == 2);
				assertTrue(packets[3].length == 3);
				assertTrue(packets[4].length == 399);
			}
			
			//1 packet: size 1
			assertTrue(pagifyTestHelper(makePackets(1), new int[]{1}, 1));
			
			//1 packet: 255
			assertTrue(pagifyTestHelper(makePackets(255), new int[]{255}, 2));
			
			//1 packet: 256
			assertTrue(pagifyTestHelper(makePackets(256), new int[]{256}, 2));
			
			//2 packets: 1, 1
			assertTrue(pagifyTestHelper(makePackets(1,1), new int[]{2}, 2));
			
			//2 packets: 255, 1
			assertTrue(pagifyTestHelper(makePackets(255,1), new int[]{256}, 3));
			
			//2 packets: 1, 255
			assertTrue(pagifyTestHelper(makePackets(1,255), new int[]{256}, 3));
			
			//2 packets: 255, 255
			assertTrue(pagifyTestHelper(makePackets(255,255), new int[]{510}, 4));
			
			//3 packets: 1,1,1
			assertTrue(pagifyTestHelper(makePackets(1,1,1), new int[]{3}, 3));
			
			//4 packets: 1,256,255,1
			assertTrue(pagifyTestHelper(makePackets(1,256,255,1), new int[]{513}, 6));
			
			//1 page can hold 255 packets if they are of size 254
			{
				int[] sizes = new int[255];
				for (int i = 0; i < sizes.length; i++)
					sizes[i] = 254;
				assertTrue(pagifyTestHelper(makePackets(sizes), new int[]{254*255}, 255));
			}
			
			//254 packets size 254 and one size 255 requires 2 pages, the second will have no data though
			{
				int[] sizes = new int[255];
				for (int i = 0; i < sizes.length; i++)
					sizes[i] = 254;
				sizes[254] = 255;
				assertTrue(pagifyTestHelper(makePackets(sizes), new int[]{254*254+255, 0}, 255, 1));
			}
			
			//same as above except the size 255 is in the middle causing the second page to have data
			{
				int[] sizes = new int[255];
				for (int i = 0; i < sizes.length; i++)
					sizes[i] = 254;
				sizes[100] = 255;
				assertTrue(pagifyTestHelper(makePackets(sizes), new int[]{253*254+255, 254}, 255, 1));
			}
			
			//1 big packet that fills exactly 1 page
			assertTrue(pagifyTestHelper(makePackets(255*255 - 1), new int[]{255*255 - 1}, 255));
			
			//maximum content size requires a second, empty page
			assertTrue(pagifyTestHelper(makePackets(255*255), new int[]{255*255, 0}, 255, 1));
			
			//greater than maximum content size requires 2 pages
			assertTrue(pagifyTestHelper(makePackets(255*255 + 99), new int[]{255*255, 99}, 255, 1));
			
			//2 packets one 1 page where both packets are the largest size possible without needing another page
			assertTrue(pagifyTestHelper(makePackets(32384, 32639), new int[]{255*255-2}, 255));
			
			//2 packets each maximum content size requires 3 pages
			assertTrue(pagifyTestHelper(makePackets(255 * 255, 255 * 255), new int[]{255*255, 254*255, 255}, 255, 255, 2));
			
			///1MB packet
			{
				int[] sizes = new int[17];
				
				//first 16 pages will be completely full
				for (int i = 0; i < sizes.length; i++)
					sizes[i] = 255*255;
				//last page will have 8176
				sizes[16] = 8176;
				
				int[] nSegs = new int[17];
				//first 16 pages will have 255 segments
				for (int i = 0; i < nSegs.length; i++)
					nSegs[i] = 255;
				//last page will have 33
				nSegs[16] = 33;
				
				assertTrue(pagifyTestHelper(makePackets(1024 * 1024), sizes, nSegs));
			}
		}
		
		private byte[][] makePackets(int... lengths)
		{
			byte[][] packets = new byte[lengths.length][];
			Random rnd = new Random();
			for (int i = 0; i < packets.length; i++)
			{
				packets[i] = new byte[lengths[i]];
				rnd.nextBytes(packets[i]);
			}
			return packets;
		}
		
		private boolean pagifyTestHelper(byte[][] packets, int[] contentSizeOnPages,  int... numSegmentsOnPages)
		{
			assert numSegmentsOnPages.length > 0;
			try
			{
				List<Page> pages = pagify(packets, false);
				
				assertTrue(pages.size() == numSegmentsOnPages.length);
				assertTrue(contentSizeOnPages.length == numSegmentsOnPages.length);
				
				//validate the correct number of segments on each page
				for (int i = 0; i < numSegmentsOnPages.length; i++)
					assertTrue(pages.get(i).segments.size() == numSegmentsOnPages[i]);
				
				//validate the correct content size on each page
				for (int i = 0; i < contentSizeOnPages.length; i++)
				{
					assertTrue(pages.get(i).content.length == contentSizeOnPages[i]);
					assertTrue(pages.get(i).calculateContentSizeFromSegments() == contentSizeOnPages[i]);
				}
				
				int lastSeqNumber = 0;
				for (Page page: pages)
					page.sequence = ++lastSeqNumber;
				
				if (pages.size() > 0)
				{
					pages.get(0).isFirst = true;
					pages.get(pages.size() - 1).isLast = true;
				}
				
				PacketStream ps = new PacketStream(new PacketSegmentStream(new CollectionPageStream(pages.iterator())));
				int i = 0;
				do
				{
					Packet packet = ps.next();
					if (packet != null)
					{
						if (Arrays.equals(packet.getBytes(), packets[i]))
							i++;
						else
							return false;
					}
					else
						return i == packets.length;
				}
				while (true);
			}
			catch (IOException ie)
			{
				//should never happen?
				assert false: ie.toString();
				return false;
			}
		}
		
		private static void repairPageCRC(File f, long pageOffset)
		throws IOException
		{
			RandomAccessFile raf = new RandomAccessFile(f, "rw");
			
			try
			{
				raf.seek(pageOffset);
				byte[] fixedHeader = new byte[Page.FIXED_HEADER_SIZE];
				raf.read(fixedHeader);
				int nSegs = Util.ubyte(fixedHeader[fixedHeader.length - 1]);
				byte[] segTable = new byte[nSegs];
				raf.read(segTable);
				int contentSize = new Page().parseSegmentTable(segTable);
				byte[] content = new byte[contentSize];
				raf.read(content);
				
				OggCRC oggCRC = new OggCRC();
				
				//zero out the checksum
				System.out.println("Old CRC bytes: " + Util.printBytes(fixedHeader, Page.HEADER_CHECKSUM_OFFSET, 4));
				System.arraycopy(new byte[4], 0, fixedHeader, Page.HEADER_CHECKSUM_OFFSET, 4);
				
				oggCRC.update(fixedHeader);
				oggCRC.update(segTable);
				oggCRC.update(content);
				
				int newCRC = oggCRC.getValue();
				
				raf.seek(pageOffset + Page.HEADER_CHECKSUM_OFFSET);
				
				byte[] crcBytes = new byte[4];
				java.nio.ByteBuffer bb = java.nio.ByteBuffer.wrap(crcBytes);
				bb.order(java.nio.ByteOrder.LITTLE_ENDIAN);  //Ogg values are little endian
				bb.putInt(newCRC);
				raf.write(crcBytes);
				
				//System.out.println("Old CRC " + Integer.toHexString(page.checksum));
				System.out.println("Wrote new CRC " + Integer.toHexString(newCRC).toUpperCase() + " to " + (pageOffset + Page.HEADER_CHECKSUM_OFFSET));
			}
			finally
			{
				raf.close();
			}
		}
		
		private class CollectionPageStream implements LogicalPageStream
		{
			private Iterator<Page> pages;
			
			public CollectionPageStream(Iterator<Page> pages)
			{
				this.pages = pages;
				
			}
			
			public CollectionPageStream(Collection<Page> pages)
			{
				this(pages.iterator());
			}
			
			public Page next()
			throws java.io.IOException
			{
				if (pages.hasNext())
					return pages.next();
				else
					return null;
			}
		}
	}
}
