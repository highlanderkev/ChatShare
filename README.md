ChatShare
=========

(Scala Social Applications in Akka)


1. Twitter (like) App
===

| README For TwitterApp |
---
| Project for CSC 536: Distributed Systems 2 |
| Professor Ljubomir Perkovic |
| By Student Kevin Westropp |

There are three main directories:

1. common - code compiled into jar to use as common between client and server

2. server - Twitter Server to maintain state of program

3. client - Twitter Client for connecting and interacting with server

To Compile
---

- navigate to root of both /client and /server

			$ sbt compile

To Run
---

- navigate to root of both /client and /server (start server first then clients)

			$ sbt run

Interaction
---

	1. It will prompt for username (this persists with server, just registers username -> client)

	2. Then download possible/relevant tweets

	3. Print Possible commands/options 

	4. Interface works in loop, -> select option -> provide parameters for option -> receive possible messages -> back to commands and options


Stopping
---

- client side

	Enter 0 to disconnect from server

	It will then shutdown

- server side

	Enter Ctrl-X Ctrl-C for Shutdown
	


2. ChatShare
==========

| README For ChatShare |
---
| Project for CSC 536: Distributed Systems 2 |
| Professor Ljubomir Perkovic |
| By Student Kevin Westropp |

There are two main directories:

1. common - code compiled into jar to use as common in app

2. chatshare - Web Server to maintain state of program and handle http requests

To Compile
---

- navigate to root of /server

			$ sbt compile


To Run
---

- navigate to root of /server (start server first then web browser)

			$ sbt run

Interaction
---

	1. Navigate web browser to http://localhost:8888/
	
	2. It will prompt for username (this persists with server, just registers username -> client)

	3. Can post tweets, find other users of system by username and start following their posts


Stopping
---

- client side (web browser)

	Click Logout button and/or Close browser window 

- server side

	Enter Ctrl-X Ctrl-C for Shutdown
