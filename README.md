# jvorbiscomment
A java library for reading and writing Ogg Vorbis comments (tags)

## Features

- **Simple** - Two static methods: VorbisIO.readComments and VorbisIO.writeComments. 
- **In-place Updates** - Comment changes are written to the original file, not a copy of the file. This means updates will be faster and the file attirbutes, such as permissions and creation time, will remain intact. 
- **Buffered Updates** - A buffering scheme is employed to avoid re-writting the entire file when the comments are changed by only a small amount. 
- **Unicode** - Full support for Unicode text. 
- **Tested** - Many unit tests have been written in hopes of preventing file corruption. I have executed this library on my 3,500 file Ogg collection.

## Usage

The following examples show how to do the most common operations on Ogg Vorbis audio files.  Detailed javadocs are included in the release zip file.

### Reading comments

```java
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
 ```

### Writing comments (overwriting)

```java
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
```

### Updating comments efficiently

One very common need is to modify a few comment fields but leave the others intact. Your intuition might lead you write to something like this:

```java
writeComments(file, changeSomeFields(readComments(file)))
```

Although this is clean and simple, it is not the most efficient way. This is because the file must be opened and parsed twice, once to read the comments and again to write them. The following method is more efficient and thus more well suited to bulk comment editing.

```java
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
```
 

## Releases
v1.0.3

    Fix exception reading vorbis comments written by the foobar2000 program.
    Cleaned up project structure to simplify ant build. 

v1.0.1

    Includes workaround for the non-compliant iAudio U2 portable ogg player.
    Library jar should be a bit smaller because unit-tests are excluded.
    Converted unit tests from JUnit to TestNG 

v1.0.0

First non-beta release!

    Comment updating is faster than v0.5.0.
    Can now handle imperfect ogg files (recoverable corruption or minor non-conformance).
    Added documentation and examples. 
