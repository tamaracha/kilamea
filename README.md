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

The application is entirely written in Kotlin (currently version 2.0) and targets JVM 22.
It is built using Gradle.
If you're using IntelliJ, just import it from github and build or run the app.
Maybe you'll have to choose and install a Java version for IntelliJ.
This is not related to the target version for this project code.

If you want to build from command line, please make sure you have Java in your path.

1. Clone this repo
2. Open terminal in the root of this project.
3. Generally, Gradle is executed with `./gradlew` on Linux/Mac, and with `gradlew.bat` on Windows. Run this to let Gradle prepare.
4. Build the project with `gradle.bat build`
5. Run the app: `./gradlew run`
