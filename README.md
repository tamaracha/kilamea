# Kilamea
@author Kay Schröer (acsf.dev@gmail.com)

Kilamea is a messaging software developed specifically for the needs of blind and visually impaired users, i.e. the focus is on a slim user interface (e.g. with a sidebar, classic menus, separate dialog windows) and keyboard operation (e.g. using shortcuts).

Of course, anyone not affected can also use the software with pleasure. Everything can be easily controlled with a mouse as well. I have simply refrained from elaborate layouts in the design.

## Main features

- Reading, writing, replying, and forwarding messages
- Plain text editor
- Easy handling of file attachments
- Passing application arguments as file attachments
- Managing email accounts and folders
- Gmail support
- Address book

## Future goals

- Lazy loading of message content and file attachments
- Integration of rich text editor
- Spellcheck capability
- Implementation of spam filter
- Backup tools

## Technical notes

The application is entirely written in Kotlin (currently version 1.9). For compilation, I use the command line tools. The generated .class files are then packaged into an executable JAR file along with the Kotlin runtime, the resources and the manifest.  
I automate this process using Apache Ant. Therefore, modifications to the build.xml might be necessary for your own development environment.

For Windows users, I also offer a way to wrap the executable JAR file into an EXE using Launch4j. I include the OpenJDK (currently version 22) as the VM. The configuration can be found in the launch4j.xml.
