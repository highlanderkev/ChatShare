package server

import common._
import akka.actor.{Actor, ActorRef, Props}
import akka.remote.routing.RemoteRouterConfig
import akka.routing.{Broadcast, RoundRobinPool}
import com.typesafe.config.ConfigFactory
import scala.collection.mutable.Map


class MasterActor extends Actor {
  
  // Clients Map (key: username, value: ActorRef)
  var clients = Map("" -> self)
  
  // List of users by username
  var users = List[String]()

  // Followers Map (key: username, value: followers by username)
  var followers = Map("" -> List[String]())

  // Tweets Tagged By HashTag (key: HashTag, value: Tweet)
  var hashTweets = Map("" -> List[String]())

  // Tweets by Users (key: username, value: Tweet)
  var userTweets = Map("" -> List[String]())

  // Tweets Queue Pending Request (key: username, value: List of Tweets)
  var pending = Map("" -> List[String]())

  //publish tweets to followers -> put in queue for request
  def publishTweet(username: String, tweet: String) = {
    //grab all followers of this user
    var followersList = List[String]()
    if(followers contains username){
      followersList = followers getOrElse (username, List[String]())
    }
    for(follower <- followersList){
      var pendingTweets = List[String]()
      if(pending contains follower){
        pendingTweets = pending getOrElse (follower, List[String]())
      }
      pendingTweets = (username + " just tweeted " + tweet) :: pendingTweets
      pending += (follower -> pendingTweets)
      println("Added pending tweet " + tweet + " for " + username) 
    }
  }
 
  def receive = {
    
    case CONNECT(username: String) =>
      
      //check if new user
      if(clients.contains(username) && users.contains(username)){
        println("Client: " + sender.toString + " connected with username: " + username)
        clients += (username -> sender)
        sender ! CONNECTED("Welcome back to Twitter!")
      }else if(users.contains(username)){
        //user connects from different client
        println("Client: " + sender.toString + " connected with username: " + username)
        clients += (username -> sender)
        sender ! CONNECTED("Welcome back to Twitter!")
      }else{
        //New Client and New Username
       println("New Client: " + sender.toString + " connected with username: " + username)
        clients += (username ->  sender)
        users = username :: users
        sender ! CONNECTED("Welcome to Twitter!, post a new tweet or see a list of all users.")
      }

    case GETUSERS =>
      
      println("Client: " + sender.toString + " requested list of users.")
      sender ! USERS(users)

    case GETTWEETS(username: String) =>
      
      println("Client: " + sender.toString + " requested pending Tweets for " + username)
      var pendingTweets = List[String]()
      if(pending contains username){
          pendingTweets = pending getOrElse (username, List[String]())
      }
      pending -= username 
      sender ! TWEETS(pendingTweets)

    case REQUESTBYTAG(hashtag: String) =>
      
      println("Client: " + sender.toString + " requested tweets by hashtag #" + hashtag)
      var tweets = List[String]()
      if(hashTweets contains hashtag){
          tweets = hashTweets getOrElse (hashtag, List[String]())
      }
      sender ! TWEETS(tweets)
 
    case REQUESTBYUSER(username: String) =>
      println("Client: " + sender.toString + " requested tweets for user " + username)
      var tweets = List[String]()
      if(userTweets contains username){
          tweets = userTweets getOrElse (username, List[String]())
      }
      sender ! TWEETS(tweets)

    case FOLLOW(thisUser: String, userToFollow: String) =>
      
      println("Client: " + sender.toString + ", username: " + thisUser + " requests to follow: " + userToFollow)
      //Check that user exists
      if(users.contains(userToFollow)){
        var followersList = List[String]()
        if(followers contains userToFollow){
          followersList = followers getOrElse (userToFollow, List[String]())
        }
        followersList = thisUser :: followersList
        followers += (userToFollow -> followersList)
        sender ! FOLLOWING("Success, you are now following " + userToFollow)
      }
      else{
        //follower doesn't exist error
        println("Error trying to follow: " + userToFollow)
        sender ! FOLLOWING("Error, no users known as " + userToFollow + " exist.")
      }

    case TWEET(hashtag: String, tweet: String, username: String) =>

      println("Hashtag #" + hashtag + "with Tweet: " + tweet + ", posted by " + username)
      //check if hashtag exists 
      if(hashtag != null && !(hashtag.isEmpty)){       
        var tagTweets = List[String]()
        if(hashTweets contains hashtag){
          tagTweets = hashTweets getOrElse (hashtag, List[String]())
        }
        tagTweets = tweet :: tagTweets  
        hashTweets += (hashtag -> tagTweets)
        var tweets = List[String]()
        if(userTweets contains username){
          tweets = userTweets getOrElse (username, List[String]())
        }
        tweets = tweet :: tweets
        userTweets += (username -> tweets)
        sender ! POSTED("Success! Tweet was posted.")
        publishTweet(username, tweet)
      }else{
        //no hashtag
        var tweets = List[String]()
        if(userTweets contains username){
          tweets = userTweets getOrElse (username, List[String]())
        }
        tweets = tweet :: tweets
        userTweets += (username -> tweets)
        sender ! POSTED("Success! Tweet was posted.") 
        publishTweet(username, tweet)
      }
    
    case DISCONNECT(username: String) =>
      
      println("Client: " + sender + " is disconnecting as username: " + username)
      if(clients contains username){
        clients -= username
      }
      sender ! DISCONNECTED  
      
    case _ => 
      
      println("Unknown message received from " + sender.toString)

  }
}
