package client 

import common._
import com.typesafe.config.ConfigFactory
import akka.actor.{Actor, ActorRef, ActorLogging, ActorSystem, Props, AddressFromURIString}
import akka.remote.routing.RemoteRouterConfig
import akka.routing.RoundRobinPool
import scala.util.control._
import scala.concurrent.duration._
import scala.concurrent.{Future, ExecutionContext}
import akka.util.Timeout
import akka.actor.ReceiveTimeout
import akka.pattern.ask

class ClientActor extends Actor {

  // Get Ref to Master Actor
  var master = context.actorSelection("akka.tcp://TwitterServer@127.0.0.1:2552/user/master")

  //Display welcome message to user
  println("Welcome to the Twitter Client!")
  println("Please enter your username or create a new one to get started.")    
  var username = readLine("username> ")
  println("Connecting to Twitter Server with username: " + username)
  master ! CONNECT(username)

  def interfaceLoop(){
    // Set Future for Tweets
    implicit val ec = ExecutionContext.Implicits.global
    implicit lazy val timeout = Timeout(10 seconds)
    val future = master.ask(GETTWEETS(username))
    future.onSuccess{
      case TWEETS(tweets: List[String]) =>
        println("Loading tweets...")
        for(tweet <- tweets){
          println(tweet)
        }
        println("Done loading tweets.")
    }

    //Main interface loop for user commands
    var loop = new Breaks;
    loop.breakable{
      //print out user options
      println("***Commands and Options***")
      println("***Enter > 1 < for User List***")
      println("***Enter > 2 < for Posting Tweet***")
      println("***Enter > 3 < to follow user***")
      println("***Enter > 4 < for all posts by hashtag***")
      println("***Enter > 5 < for all posts by user***")
      println("***Enter > 0 < to disconnect and shutdown app***")
      println("******************************")

      //Read in user commands
      for(ln <- io.Source.stdin.getLines){
        if(ln.contains("1")){
          
          println("List of Users loading...")
          master ! GETUSERS
          loop.break
        
        }else if(ln.contains("2")){
        
          var hashtag = ""
          var tweet = readLine("Tweet> ")
          if(tweet.contains("#")){
            //hashtag tweet
            var parsedTweet = tweet.split(" ")
            for(word <- parsedTweet){
              if(word.contains("#")) hashtag = word;
            }
            println("Posting hashtag: #" + hashtag + ", with Tweet: " + tweet)
            master ! TWEET(hashtag, tweet, username)
          }else{
            //just post tweet
            println("Posting Tweet: " + tweet)
            master ! TWEET("", tweet, username)
          }
          loop.break
        
        }else if(ln.contains("3")){
        
          var userToFollow = readLine("UserToFollow> ")
          println("Requesting to follow: " + userToFollow)
          master ! FOLLOW(username, userToFollow)
          loop.break
        
        }else if(ln.contains("4")){
        
          var tagTweets = readLine("HashTag> ")
          println("Loading Tweets with hashtag #" + tagTweets)
          master ! REQUESTBYTAG(tagTweets)
          loop.break
        
        }else if(ln.contains("5")){

          var userTweets = readLine("Username> ")
          println("Loading Tweets by user: " + userTweets)
          master ! REQUESTBYUSER(userTweets)
          loop.break

        }else if(ln.contains("0")){
          
          println("Disconnecting from server.")
          master ! DISCONNECT(username)
          loop.break

        }else{
          println("Please enter a valid number/option (1 - 4).")
        }
      }
    }
  }

  //context.setReceiveTimeout(100 milliseconds)
  def receive = {
    case USERS(users: List[String]) =>
      for(user <- users){
        println("Active User: " + user)
      }
      interfaceLoop()
    case TWEETS(tweets: List[String]) =>
      for(tweet <- tweets){
        println("Tweet: " + tweet)
      }
      interfaceLoop()
    case CONNECTED(msg: String) =>
      println(msg)
      interfaceLoop()
    case FOLLOWING(msg: String) =>
      println(msg)
      interfaceLoop()
    case POSTED(msg: String) =>
      println(msg)
      interfaceLoop()
    case REALTIMETWEET(tweet: String) =>
      println(tweet)
      interfaceLoop()
    case ReceiveTimeout =>
      interfaceLoop()
    case DISCONNECTED =>
      println("Disconnected from server, shutting down client.")
      context.system.shutdown  
    case _ =>
      println("Received unknown msg.")
      interfaceLoop()
  }
}
