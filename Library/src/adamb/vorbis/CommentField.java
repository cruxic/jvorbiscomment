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

import java.io.Serializable;

/**
 An individual field in an Ogg Vorbis {@link VorbisCommentHeader comment header}.
 Essentially a name/value pair.
 <p>
 See {@link VorbisCommentHeader} documentation for the list of standard field names.
 </p>
 @see VorbisCommentHeader
 */
public class CommentField
	implements Serializable
{
  /**
   Field name.  See {@link VorbisCommentHeader} documentation for the list of standard field
	 names.
	 <p>	 
	 Empty string is allowed because I have encountered files containing comment entries without "name=".
   the name is optional
	 </p>
   */
  public String name;
	/**Field value.*/
  public String value;
  
	/**
	 Create a field given the name and value.
	 */
  public CommentField(String name, String value)
  {
		assert name != null && value != null;
    this.name = name;
    this.value = value;
  }
  
  /**
   Create a CommentField by parsing a name=value string.  name= is optional.
   This method will never fail to return a CommentField so long as nameValueString is
   not null.
   */
  public CommentField(String nameValueString)
  {
    int idx = nameValueString.indexOf('=');
		if (idx < 1)
			name = "";
		else
			name = nameValueString.substring(0, idx);
		
		if (idx + 1 < nameValueString.length())
			value = nameValueString.substring(idx + 1);
		else
			value = "";
  }
	
	/**
	 Return name and value separated by '='.
	 */
	public String toString()
	{
		return name + "=" + value;
	}
}