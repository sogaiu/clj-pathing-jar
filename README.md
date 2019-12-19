# clj-pathing-jar

A work around for long-classpath issues on Windows, plus alpha.

## Background

Tim Gilbert reported and addressed long-classpath issues for Windows
in [TDEPS-120](https://dev.clojure.org/jira/browse/TDEPS-120).

Originally, this repository was created as an interim solution until
official tooling got updated.  It was a slight reworking of Tim
Gilbert's efforts.  All of the good code is due to him :)

It turned out that Graal's native-image didn't seem to support pathing
jars in quite the same way that the java executable did, so code was
added to cover this case.

## Technical Background

Most Clojure tooling run programs by constructing a classpath out of
the dependency tree and then passing that as a command-line argument
to the java executable (that which starts a JVM).

On Windows this can be problematic when the classpath is very long,
since the length of Windows command-line arguments is extremely
constrained when compare to most Unix-based OSes. Depending on various
factors, the maximum length can range from 8191 characters to 32K
characters, but even 32K may not be enough for some applications with
a large dependency tree.

One approach to this problem for JVM-based Windows development
is to create a "pathing jar" - that is, a jar which only contains a
Manifest that points to the various jar files that make up the entire
classpath of the program. Java can then be invoked via `java -cp
pathing.jar clojure.main` and the resulting JVM will have the proper
classpath.

Other approaches include classpath wildcards, @-files (Java >= 9),
building an uberjar, shortening file and directory path lengths, and
time travelling to get rid of the length limitation :)

More information can be found in the tools.deps ticket 
[TDEPS-120](https://dev.clojure.org/jira/browse/TDEPS-120).

When using Graal's native-image binary with the `-jar` option, the
required pathing jar appeared to have slightly different needs.

* A Main-Class: attribute in the pathing jar manifest appears necessary

* The space-separated file: URI approach in the original code didn't
  appear to work, so this was replaced with ordinary paths separated
  by spaces

N.B. Because ordinary paths are used, there might be the usual sorts
of issues with spaces in file paths.  No testing has been done on that
end.  To avoid potential issues, don't use spaces in file paths :)

## What this does

This repository is a tweaked version of Tim Gilbert's
[lein-classpath-jar](https://github.com/timgilbert/lein-classpath-jar)
code, though without the Leiningen bits, but with some code and
documentation changes.

Creates one of two types of pathing jars for a project:

* `make-classpath-jar!`

  The original type Tim Gilbert implemented for use with clj /
  clojure, though the bits wiring it to be used with clj / clojure's
  .cpcache have been removed to make it more generic

* `make-native-image-pathing-jar!`

  A type for use with native-image.  See the Technical Background section
  (and/or source code) for details on how this differs from the original
  type of jar.

## make-classpath-jar! Usage

TODO - for the moment, see the cljr-pathing-jar.main namespace and the
docs below for hints

## make-native-image-pathing-jar! Usage

### Command Line

The current situation with respect to quoting when using the official
clj on Windows is...unfortunate.  This manifests itself as too many
double quotes.  See the [Escaping Quotes](https://github.com/clojure/tools.deps.alpha/wiki/clj-on-Windows#escaping-quotes) section of the tools.deps.alpha Wiki
for some examples.

From a project directory in need of a pathing jar for use with native-image:

cmd.exe:

```
C:\> powershell -command clj -Sdeps '{:deps {clj-pathing-jar {:git/url """"""https://github.com/sogaiu/clj-pathing-jar"""""" :sha """"""73b65b4fc63f5b9782128c9530680125240eb9d0""""""}}}' -m clj-pathing-jar.native-image
```

powershell:

```
PS C:\> clj -Sdeps '{:deps {clj-pathing-jar {:git/url ""https://github.com/sogaiu/clj-pathing-jar"" :sha ""73b65b4fc63f5b9782128c9530680125240eb9d0""}}}' -m clj-pathing-jar.native-image
```

### Programmatic

To create a pathing jar for use with native-image, run the following
code like the following from a project's top-level directory:

```
(require '[clj-pathing-jar.core :as cc])

(cc/make-native-image-pathing-jar! "main.ns" "C:\\Users\\user\\.m2\\repository\\org\\clojure\\clojure\\1.9.0\\clojure-1.9.0.jar;C:\\Users\\user\\.m2\\repository\\org\\clojure\\spec.alpha\\0.1.143\\spec.alpha-0.1.143.jar;C:\\Users\\user\\.m2\\repository\\org\\clojure\\core.specs.alpha\\0.1.24\\core.specs.alpha-0.1.24.jar" "classes" "my-pathing.jar")
```

* First argument - name of the entry point namespace

* Second argument - a classpath (n.b. usually windows uses ; and \ in
  its classpaths -- and backslashes typically get doubled within
  doubled-quoted strings)

* Third argument - a directory name to additionally put on the
  classpath, typically containing class files for the entry point
  namespace and its interior-to-project dependencies (these files are
  typically created using Clojure's `compile` function)

* Last argument - a name for the pathing jar

Note, due to the mechanism involved in resolving file paths, the code
should be run from the project directory for which a pathing jar is
being created.  Otherwise, relative paths will likely be resolved
relative to an unintended directory.

## License

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.

## Acknowledgments

Thanks to (at least):

* ajoberstar
* alexmiller
* borkdude
* dominicm
* litteli
* nickgieschen
* seancorfield
* taylorwood
* tebeka
* timgilbert
