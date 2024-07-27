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

## Implementation

The application is entirely written in Kotlin (currently version 2.0) and targets JVM 22.

## Development

Gradle is used to build, test, execute, and package the app.

### Prequisites

Gradle depends on Java, so Java must be installed on your dev machine.

#### Windows

1. Download OpenJDK version 22 for Windows from the [official OpenJDK release page].
2. Extract the zip archive to `C:\Program Files\Java\jdk-22.0.2`
3. Go to System settings -> Advanced -> Environment variables.
4. Set or add `JAVA_HOME` to `C:\Program Files\Java\jdk-22.0.2`

#### macOS

1. Download OpenJDK version 22 for Mac from the [official OpenJDK release page].
2. Create a folder in your user directory, e.g., `~/java`, extract the downloaded tar archive and copy the resulting `jdk-22.0.2.jdk` into `~/Java/`
3. Open `~/.zshrc` in a text editor and add this line, so your JDK can be found:

```sh
export JAVA_HOME="/Users/$(whoami)/Java/jdk-22.0.2.jdk/Contents/Home"
export JDK_HOME=${JAVA_HOME}
```

### Build from command line

1. Open a terminal
2. Clone this repo locally: `git clone git@github.com:kschroeer/kilamea.git`
3. Go to project root: `cd kilamea`

#### Windows

1. Check gradle works: `gradlew.bat`
2. Build the project with `gradlew build`
3. Run the app: `gradlew run`
4. Package the app as installer for your platform: `gradlew jpackage`

#### Mac and Linux

1. Check gradle works: `./gradlew`
2. Build the project with `./gradlew build`
3. Run the app: `./gradlew run`
4. Package the app as installer for your platform: `./gradlew jpackage`

[official OpenJDK release page]: https://jdk.java.net/22/
