/*
 * PageStream.java
 *
 * Created on October 7, 2006, 7:29 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package adamb.ogg;

/**A stream of Ogg Pages*/
public interface PageStream
{
  public Page next()
		throws java.io.IOException;
}
