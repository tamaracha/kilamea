# Contributing to Kilamea

Kilamea is a non-commercial open source project.
See how you can help to improve it.
There are several options:

1. Reporting problems or missing features
2. Submitting code as a pull request

## Reporting problems

- Kilamea isn't working as expected?
- A feature is missing?
- Something should be changed?

Please share your experience and create a [new issue].
Even if you don't have the time or skills to contribute code, found problems should be documented and help developers to decide where they can make progress.
Try to give your new issue a descriptive title.
If you want to report a problem, please consider the following questions:

- Describe your environment: Kilamea version, operating system name and version. This helps others to reproduce the problem.
- What did you try to do?
- What happened that you did not expect?
- What should happen?
- Did you find a solution or a temporary workaround for the problem?

## Code submissions

If you want to contribute code, you can submit a [new pull request].
You should be familiar with [git] and its workflows.
The code is written in [kotlin] and [gradle] is used as build system, having some experience with these technologies might be helpful.
Please follow this guide to make high-quality contributions.

### Prepare the project

1. Register a GitHub account, if you haven't one already.
2. Create a [new fork] of this project in your user account. This is your working copy that you can change without changing the actual project.
3. Clone your fork to your working environment, e.g., using git via command line or an IDE.

### Dev environment

If you want to make changes to the code, you should be able to build and run the project on your machine.
Gradle runs on all major operating systems, but it needs an installed Java development kit (JDK).
Using an IDE like IntelliJ, you can select and download a JDK from there, no fiddling with the OS necessary.
You can verify that you have one on the command line with `java --version`.
If Java is found, you can proceed to the next section.
Otherwise, let's install Java now for command line usage.

#### Windows

1. Download OpenJDK version 22 for Windows from the [official OpenJDK release page].
2. Extract the zip archive to `C:\Program Files\Java\jdk-22.0.2`
3. Go to System settings -> Advanced -> Environment variables.
4. Set or add `JAVA_HOME` to `C:\Program Files\Java\jdk-22.0.2`

#### macOS

1. Download OpenJDK version 22 for Mac from the [official OpenJDK release page]. Please use the x64 binary, even if you have an M1 Mac.
2. Create a folder in your user directory, e.g., `~/java`, extract the downloaded tar archive and copy the resulting `jdk-22.0.2.jdk` into `~/Java/`
3. Open `~/.zshrc` in a text editor and add these environment variables:

```sh
export JAVA_HOME="/Users/$(whoami)/Java/jdk-22.0.2.jdk/Contents/Home"
export JDK_HOME=${JAVA_HOME}
```

### Build from command line

1. Open a terminal
2. Go to the root of your cloned project: `cd kilamea`

You don't need to install gradle globally, because gradle projects contain a small script that downloads and executes the correct gradle version.
This is called the [Gradle Wrapper] and is located at the project root.
When you invoke gradle during development, you should call this wrapper script with a relative path instead of a global gradle installation.
Replace abbreviated gradle commands reported in docs etc. as `gradle xyz` accordingly:

- Windows: `.\gradlew.bat` or `.\gradlew` on Powershell
- Mac and Linux: `./gradlew`
- Never `gradle`, except for creating a new project

Try to run the wrapper in your project's root so gradle gets ready.

1. Check gradle works: `gradle`
2. Build the project: `gradle build`
3. Run the app: `gradle run`
4. Package the app as installer for your platform: `gradle jpackage`

Now your project is ready for development.

### Making changes

1. Create a new git branch for your work with a descriptive name: `git switch -c fix_weird_problem main`
2. Make your changes and create meaningful commits with descriptive messages. A commit should be described by its impact, not by its implementation details which can be viewed in the git history. If it solves an issue on GitHub, you can also reference the issue as message: `Fix #12345`.
3. Push your commits to your fork on GitHub. You can also rebase and force-push as you like, it's your branch in your working copy.

### Finishing your work

When you're satisfied with your changes, think of how they could matter to the users of Kilamea.
Did you add or remove a feature?
Did you change some app behavior?
Then you should mention that in [CHANGELOG.md].
The changelog follows the [keep a changelog] guidelines.
This styleguide highlights the point that changelogs and release notes should be written for humans and to keep users informed about the project's progress.
Implementation details should remain in the git history and issue trackers.

### Submitting your work

Finally, it's time for a [new pull request].
Select the branch that contains your work to be merged into the base branch of the project.
As always, give it a short descriptive title and write an informative description:

1. Which problems does your PR solve? You could also write things like `fixes #123` in the description to [link to an issue].
2. What did you do? This should be a technical summary because some implementation decisions might need further discussion.
3. Maybe you will be asked to push some corrections.
4. If everything is discussed and your code is approved, your PR will be merged and closed.

[new issue]: https://github.com/kschroeer/kilamea/issues/new
[new pull request]: https://github.com/kschroeer/kilamea/pull/new
[new fork]: https://github.com/kschroeer/kilamea/fork/
[git]: https://git-scm.com/
[kotlin]: https://kotlinlang.org/
[gradle]: https://gradle.org/
[official OpenJDK release page]: https://jdk.java.net/22/
[gradle wrapper]: https://docs.gradle.org/current/userguide/gradle_wrapper_basics.html
[keep a changelog]: https://keepachangelog.com
[link to an issue]: https://docs.github.com/en/issues/tracking-your-work-with-issues/linking-a-pull-request-to-an-issue
