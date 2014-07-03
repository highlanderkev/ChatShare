package common

import akka.actor.{Actor, ActorRef}

case object GETUSERS
case class USERS(users: List[String])
case class TWEET(hashtag: String, tweet: String, username: String)
case class POSTED(msg: String)
case class GETTWEETS(username: String)
case class REALTIMETWEET(tweet: String)
case class REQUESTBYTAG(hashtag: String)
case class REQUESTBYUSER(username: String)
case class TWEETS(tweets: List[String])
case class CONNECT(username: String)
case class CONNECTED(msg: String)
case class FOLLOW(thisUser:String, userToFollow: String)
case class FOLLOWING(msg: String)
case class DISCONNECT(username: String)
case object DISCONNECTED
case class MESSAGE(msg: String)

