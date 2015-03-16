/*(The MIT License)
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

/**
	
 */
public class ValidatingPageStream
	implements LogicalPageStream
{
	public enum Warning
	{
		MISSING_FIRST_FLAG ("First page from stream was not marked as first!"),
		MISSING_LAST_FLAG ("Last page from stream was not marked as last!"),
		UNEXPECTED_FIRST_FLAG ("Page in mid stream marked as first!"),
		UNEXPECTED_AFTER_LAST_FLAG ("Found more pages after the page marked as last!"),
		OUT_OF_SEQUENCE ("Page is out of sequence or stream is missing pages!");
		
		private String message;
			
		public String getMessage()
		{
			return message;
		}
			
		private Warning(String message)
		{
			this.message = message;
		}
	}
	
	private LogicalPageStream lps;
	
	private boolean foundPageMarkedAsLast;
	private boolean firstPage;
	private EnumSet<Warning> warnings;
	
	private int expectedPageSequenceNumber;
	
	public ValidatingPageStream(LogicalPageStream lps)
	{
		this.lps = lps;
		foundPageMarkedAsLast = false;
		firstPage = true;
		warnings = EnumSet.noneOf(Warning.class);
	}

	/**
		Use this constructor if iteration is being started mid-stream
	 */
	public ValidatingPageStream(LogicalPageStream lps, int nextPageSequenceNumber)
	{
		this(lps);
		firstPage = false;
		expectedPageSequenceNumber = nextPageSequenceNumber;
	}	
	
  public Page next()
		throws IOException
	{
		//clear previous warnings
		warnings.clear();
		
		//get the next page from the stream
		Page page = lps.next();
		
		if (page != null)
		{
			//first page from the stream?
			if (firstPage)
			{
				firstPage = false;
				
				//the first page in the stream must be marked as first!
				if (!page.isFirst)
					warnings.add(Warning.MISSING_FIRST_FLAG);
			}
			else
			{
				//this is not the first page and thus it should not be marked so
				if (page.isFirst)
					warnings.add(Warning.UNEXPECTED_FIRST_FLAG);					

				//does this page have the expected sequence number?
				if (page.sequence != expectedPageSequenceNumber)
					warnings.add(Warning.OUT_OF_SEQUENCE);
			}			
			
			//set the next expected sequence number
			expectedPageSequenceNumber = page.sequence + 1;

			//there should not be any more pages after we find the one marked as last!
			if (page.isLast)
				foundPageMarkedAsLast = true;
			else if (foundPageMarkedAsLast)
				warnings.add(Warning.UNEXPECTED_AFTER_LAST_FLAG);
		}
		//since there are no more pages in the stream it's a warning if we have not found the page marked as last
		else if (!foundPageMarkedAsLast)
		{
			warnings.add(Warning.MISSING_LAST_FLAG);
		}
		
		//throw a warning exception if needed
		if (warnings.size() > 0)
		{	
			Warning w = warnings.iterator().next();
			if (!canIgnore(warnings, page))
				throw new IOException(w.getMessage());
		}
		
		return page;
	}
	
	/**
		There is was a non-fatal problem while parsing the stream.
	 @param page the page the warning(s) pertain to.  null if the warning is {@link Warning#MISSING_LAST_FLAG}.
	 @return false if stream parsing should be aborted with an exception
	 */
	protected boolean canIgnore(EnumSet<Warning> warnings, Page page)
	{
		return false;
	}	
}
