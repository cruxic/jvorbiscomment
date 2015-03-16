# jvorbiscomment
A java library for reading and writing Ogg Vorbis comments (tags)

## Features

- **Simple** - Two static methods: VorbisIO.readComments and VorbisIO.writeComments. 
- **In-place Updates** - Comment changes are written to the original file, not a copy of the file. This means updates will be faster and the file attirbutes, such as permissions and creation time, will remain intact. 
- **Buffered Updates** - A buffering scheme is employed to avoid re-writting the entire file when the comments are changed by only a small amount. 
- **Unicode** - Full support for Unicode text. 
- **Tested** - Many unit tests have been written in hopes of preventing file corruption. I have executed this library on my 3,500 file Ogg collection.

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
