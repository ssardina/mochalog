 # Mochalog: A two-way bridge between Java and SWI-Prolog :coffee:

[![Travis-CI](https://img.shields.io/travis/mochalog/mochalog.svg)](https://travis-ci.org/ssardina/mochalog/builds)

**This is a FORK of the [original Mochalog](https://github.com/mochalog/mochalog) GitHub main repository.**

The current version (> 0.3.0) has been simplified significantly and migrated to Maven. 

Mochalog can be obtained automatically via Maven automatically from Github repository via JitPack: <https://jitpack.io/#ssardina/mochalog>. See below for more details for your POM.


## Overview

Mochalog is a rich bidirectional interface between the Java Runtime and the SWI-Prolog interpreter inspired and built on top of [JPL](http://jpl7.org/). 

In some sense, Mochalog is a further abstraction of JPL. Looking to stand on its own two feet, however, Mochalog is focused on achieving three core design objectives:

* **API simplicity:** Reduce code complexity and learning curve. Increase maintainability. Use modern Java and Prolog software design practices. Prolog calls to Java should look like ***Java***, Java calls to Prolog should look like ***Prolog***.
* **Performance:** Calls between the two languages should be blazingly fast (as fast or **faster** than other existing Java-Prolog interfaces).
* **Sandboxing:** Ability to execute different SWI Prolog instances, each in isolation. This is realized via SWI [modules](http://www.swi-prolog.org/pldoc/man?section=modules). This is useful for example in mult-agent applications, where each agent is meant to have its own knowledgebase.

Mochalog provides some high level API to consult, assert and retract, and a few query methods (just prove, one solution, all solutions, iterators). It also provides a mechanism to manage different Prolog sandboxes, each with its own space.


## Prerequisites

* [SWI Prolog](http://www.swi-prolog.org/) (>7.4.x) with [JPL](http://www.swi-prolog.org/pldoc/doc_for?object=section(%27packages/jpl.html%27)) Bidirectional interface with Java:
	* This is package `swi-prolog-java` in Linux.
	* In Windows, the Java-SWI interface it can be installed as part of the main install.
	* Main Page for JPL: https://jpl7.org/ 
	    * API for JPL: https://jpl7.org/doc/index.html

Also, depending on the system being used:

* If in **Windows**:
	* Tested successfully in Windows 7 with SWI 7.6.4.
	* Make sure SWI is installed with the JPL Java-SWI connectivity. You should have a `jpl.dll` (in the SWI `bin/` subdir) and a `jpl.jar` (in the SWI `lib/` subdir).
	* Define a _system_ environment variable `SWI_HOME_DIR` and set it to the root directory of your installed version of SWI-Prolog (e.g., to `C:\Program Files\swipl`).
	* Extend `Path` system environment variable with the following two components:
		* `%SWI_HOME_DIR%\bin`
		* `%SWI_HOME_DIR%\lib\jpl.jar`
	* No changes to `CLASSPATH` are needed.
* If in **Linux**:
	* Latest package versions at <http://www.swi-prolog.org/build/PPA.txt> 
	* JPL is provided via package `swi-prolog-java` (interface between Java and SWI) installed. This will include library `libjpl.so` (e.g., `/usr/lib/swi-prolog/lib/amd64/libjpl.so`)
	* Extend environment library `LD_PRELOAD` for system to pre-load `libswipl.so`: `export LD_PRELOAD=libswipl.so:$LD_PRELOAD`
		* Check [this post](https://answers.ros.org/question/132411/unable-to-load-existing-owl-in-semantic-map-editor/) and [this one](https://blog.cryptomilk.org/2014/07/21/what-is-preloading/) about library preloading.
		* Also, check [this](https://bugs.debian.org/cgi-bin/bugreport.cgi?bug=690734) and [this](https://github.com/yuce/pyswip/issues/10) posts.
	* Extend environment variable `LD_LIBRARY_PATH`  to point to the directory where `libjpl.so` is located (e.g., `export LD_LIBRARY_PATH=/usr/lib/swi-prolog/lib/amd64/`)
	* If using RUN AS configuration in ECLIPSE, remember to set up these two variables `LD_LIBRARY_PATH` and `LD_PRELOAD` too (and check "Append environment to native environment").



## How to use it: Add dependency in Maven POM file

Mochalog can be configured automatically in your application as dependency via JitPack:

```xml
    <repositories>
        <!-- JitPack used for remote installation of dependencies from Github and Bitbucket -->
        <repository>
            <id>jitpack.io</id>
            <name>JitPack Repository</name>
            <url>https://jitpack.io</url>
        </repository>
   </repositories>

   <dependencies>
        <!--  Mochalog interface for SWI-Prolog -->
        <dependency>
            <groupId>com.github.ssardina</groupId>
            <artifactId>mochalog</artifactId>
            <version>0.5.0</version>
        </dependency>
        ...
   </dependencies>
```

## A Simple Example of Mochalog in SARL

Once again, a more complete set of examples in Java can be found in the [Mochalog Unit Test Examples](src/test/java/io/mochalog/bridge/MochaTest.java). It has tests showing how to consult/load a KB, assert and retract, update facts (with new arguments), prove a query, ask for one solution, for all solutions, or iterate through solutions. 

For some tests using [JPL](https://jpl7.org/) check [this test](https://github.com/ssardina/mochalog/blob/master/src/test/java/io/mochalog/bridge/JPLTest.java) and the [JPL Examples](https://github.com/SWI-Prolog/packages-jpl/tree/master/examples) 


Here is some very simple example code of its use within a [SARL](http://www.sarl.io/) agent system:

```java
import io.mochalog.bridge.prolog.PrologContext
import io.mochalog.bridge.prolog.SandboxedPrologContext
import io.mochalog.bridge.prolog.query.MQuery

// Set-up Prolog knowledgebase
var prolog_kb : PrologContext
val beliefSpace = String.format("swiplayer")
prolog_kb = new SandboxedPrologContext(beliefSpace)
prolog_kb.importFile("src/main/prolog/masssim_coordinator.pl") // newest version

// Assert percepts in the KB
prolog_kb.assertFirst("percepts(@A, @I, @A)", agentName, agents.get(agentName).step, percepts.toString)

// Querying one solution - Tell the KB to process last percept
agents.keySet().forEach([ agentName : String |
	prolog_kb.askForSolution(MQuery.format("process_last_percepts(@S)", agentName))
])

// Querying all solutions - Report percepts available in the KB
val query = MQuery.format("percepts(Agent, Step, Percepts)")
for (solution : prolog_kb.askForAllSolutions(query))
{
	System.out.format("Information for agent %s on step %d\n", solution.get("Agent").toString(),  
                   solution.get("Step").intValue)
}
```    

## Information, Special Features, Cavets

### Goals via MQuery or String

All query methods of Mochalog can receive either an `MQuery` object (which basically falls back to a [JPL `Query` object](https://jpl7.org/doc/org/jpl7/Query.html)) or a String with possible formatting.

This is an example of building an object `MQuery` first:

```
query = MQuery.format("fullName(@A, FullName)", "nick")
solution = prolog_kb.askOnce(query)
```

and here is directly using the string:

```
solution = prolog_kb.askOnce("fullName(nick, FullName)")
solution2 = prolog_kb.askOnce("fullName(@A, FullName)", nickName)
```

### @-Formatting Support & Data Types

In the end, one builds a goal query **string** that JPL executes into the SWI engine. So, an important aspect is the construction of such string in a covenient manner.

One alternative is to build the query string as in standard Java, by conncatenation or via [`String.format`](https://dzone.com/articles/java-string-format-examples). For example:

	result = prolog_kb.prove(String.format("percepts(%s, %d, %s)", "smithagent", 34, data)))
	

An alternative is to call the proving method directly with a string using the Mochalog placeholders @A (for atomic), @I (for integers) and @S (for strings):

	result = prolog_kb.prove("percepts(@A, @I, @A)", "smithagent", 34, data)))


The understanding of the three placeholders is as follows:

* **@I**: interpret the argument as an **integer**. If a float, keep interger part.
    * `prolog.assertFirst("data(integer, @I)", 48.234))` will yield `data(integer, 48)`
* **@A**: interpret the argument as **atomic**. This is used to construct atoms, numbers (integers and floats), compound terms, variables, and lists. Most of the time the data is a Java String. The string has to be a proper legal atom. For example, "Father(X,Y)" or "hello world" are not proper atoms and will give error (use @S).
    * `prolog.assertFirst("data(integer, @A)", 48.45)`
    * `prolog.assertFirst("data(atom, @A)", "helloWorld")`
    * `prolog.assertFirst("data(compound, @A)", "father(mary,john)"))`
    * `prolog.assertFirst("data(variable, @A)", "X")`
    * `prolog.assertFirst("data(list, @A)", "[1212, pete, father(mary, john), X, 34]")`
        * this will yield clause `data(list, [1212, pete, father(mary, john), _, 34])`
* **@S**: interpret the argument as a full **atom**, whatever it is. Basically it is like adding `'...'`.
    * `prolog.assertFirst("data(atom_simple, @A)", "helloWorld")`
    * `prolog.assertFirst("data(atom_complex, @S)", "here comes the sun")`
        * would not work with @A because there are spaces. 
    * `prolog.assertFirst("data(atom_complex, @S)", "Father(John,Maria)")`
        * would not work with @A because Father is a variable and cannot be followed by parenthesis.
    * `prolog.assertFirst("data(atom_complex, @S)", 2312)`
        * this will yield clause `data(atom__complex, '2312')`


When using `String.format` one has to be careful in building the string that would be use in Prolog. For example, 

    `prolog.assertFirst(String.format("data(atom_complex, %s)", "here comes the sun"))`

would be wrong, as it will try to assert `data(atom_complex, here comes the sun)` whose second argument is wrongly formatted. Instead, write:

    `prolog.assertFirst(String.format("data(atom_complex, '%s')", "here comes the sun"))`
    
This is where the placeholder @S comes handy... :-)


To inspect how Java data types and values are mapped to Prolog, check [this link](https://jpl7.org/PrologApiOverview.jsp#representation_of_Java_values...) in JPL doc. 







### Fact Update Support

When SWI is used as a kowledge base it is very common to just update the content of some facts. For example, suppose we want to update the phone of a person, stored in a `person_info(firstName, lastName, passportNo, phoneNo, country)` fact. 

The standard Prolog way is to retract the existing clause, and assert the new updated fact.

We can compactly do this using the `<-` notation provided by Mochalog:

	query = "get_new_phone(firstName, lastName, newPhone), 
		 person_info(firstName, lastName, _, OldPhone <- newPhone, _),
		 write("Phone number removed: "), write(OldPhone)" 

This will replace all the person's `person_info/5` facts (for all passports and countries!) with the new phone number `newPhone`.



### Management of Strings

From Paul Singleton's great explanation in SWI Forum (see [post](http://www.swi-prolog.org/forum?place=msg%2Fswi-prolog%2Ff8tWomPpSyM%2FiAlKViiTCAAJ)): 

JPL only really supports the classic term model (i.e., variable, atom, integer, float, compound), but has half-baked support for SWI Prolog's string type extension.

Strings and queryText atoms (and also reserved symbols) are all brought into Java as org.jpl7.Atom instances, but with an indication of their origin: see `Atom.atomType()`.

From Java into Prolog you can't currently create a string :-( but this may just require a hack to `Atom.put()`

Also, void using `Atom.toString()` the string value, use `Atom.name()` instead.

_Summary:_ avoid strings in JPL-accessed Prolog code, perhaps by wrapping string-exposing predicates.
For example, if you have a predicate `full_name(Nickname, FullName)` where `FullName` are meant to be strings, use this interface to it, where the second argument is seen as an atom:

	full_name_v2(Nickname, Fullname1) :-
	    (   atom(Fullname1)
	    ->  atom_string(Fullname1, Fullname2),
		full_name(Nickname, Fullname2)
	    ;   var(Fullname1)
	    ->  full_name(Nickname, Fullname2),
		atom_string(Fullname1, Fullname2)
	    ).


        
                
## Contact

Sebastian Sardina - ssardina@gmail.com
