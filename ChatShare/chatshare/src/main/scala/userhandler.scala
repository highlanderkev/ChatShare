package ChatShare

import common._

import akka.actor.{Actor, ActorRef, Props}
import akka.event.Logging

import scala.collection.mutable.Map

class UserHandler extends Actor {
  val log = Logging(context.system, this)
  val datetime = new DateTime()

  // Users Map (key: username, value: following by username)
  var users = Map("" -> List[String]())
  // Followers Map (key: username, value: followers by username)
  var followers = Map("" -> List[String]())

  def receive = {
    case LOGIN(username: String) =>  
      if(users contains username){
        log.info(datetime.getDateTime + "<USERHANDLER>User: " + username + " connected.")
        sender ! LOGGED(username)
      }
      else{
        log.info(datetime.getDateTime + "<USERHANDLER>New User: " + username + " connected.")      
        users += (username -> List[String]())
        followers += (username -> List[String]())
        sender ! LOGGED(username)
      }
    case GETUSERS =>
      log.info(datetime.getDateTime + "<USERHANDLER>Request for users list.")
      sender ! USERS(users.keys.toList)
    case GETFOLLOWERS(username: String) =>
      log.info(datetime.getDateTime + "<USERHANDLER>Request for followers of user " + username)
      sender ! FOLLOWERS(getFollowers(username))
    case GETFOLLOWING(username: String) =>
      log.info(datetime.getDateTime + "<USERHANDLER>Request for following list " + username)
      sender ! FOLLOWERS(getFollowing(username))
    case FOLLOW(currentUser: String, userToFollow: String) =>
      if(followers.contains(userToFollow) && users.contains(currentUser)){
        log.info(datetime.getDateTime + "<USERHANDLER>User: " + currentUser + " is now following " + userToFollow)
        addFollower(userToFollow, currentUser)
        sender ! FOLLOWERS(getFollowing(currentUser))
      }
      else{
        log.info(datetime.getDateTime + "<USERHANDLER>Error user: " + currentUser + " is requesting to follow " + userToFollow)
        sender ! FOLLOWERS(List[String]())
      }
    case STOPFOLLOW(currentUser: String, userToFollow: String) =>
      if(followers.contains(userToFollow) && users.contains(currentUser)){
        log.info(datetime.getDateTime + "<USERHANDLER>User: " + currentUser + " is no longer following " + userToFollow)
        removeFollower(userToFollow, currentUser)
        sender ! FOLLOWERS(getFollowing(currentUser))
      }else{
        log.info(datetime.getDateTime + "<USERHANDLER>Error user: " + currentUser + " is requesting to stop following " + userToFollow)
        sender ! FOLLOWERS(List[String]())
      }
  }

  /**
   * Method to Add follow to followlist and this userToFollow to followinglist.
   */ 
  private def addFollower(userToFollow: String, currentUser: String){
    var followersList = List[String]()
    if(followers contains userToFollow){
      followersList = followers getOrElse (userToFollow, List[String]())
    }
    if(!followersList.contains(currentUser)){
      followersList = currentUser :: followersList
      followers += (userToFollow -> followersList)
    }
    var followingList = List[String]()
    if(users contains currentUser){
      followingList = users getOrElse (currentUser, List[String]())
    }
    if(!followingList.contains(userToFollow)){
      followingList = userToFollow :: followingList
      users += (currentUser -> followingList)
    }
  }

  /**
   * Method to Remove Follower from followlist and this userToFollow from followinglist.
   */ 
  private def removeFollower(userToFollow: String, currentUser: String){
    var followersList = List[String]()
    if(followers contains userToFollow){
      followersList = followers getOrElse (userToFollow, List[String]())
    }
    followersList = followersList.filter(user => (user != currentUser))
    followers += (userToFollow -> followersList)
    var followingList = List[String]()
    if(users contains currentUser){
      followingList = users getOrElse (currentUser, List[String]())
    }
    followingList = followingList.filter(user => (user != userToFollow))
    users += (currentUser -> followingList)
  }

  /**
   * Method to get list of users this user is following.
   */ 
  private def getFollowing(username: String): List[String] = {
    var followingList = List[String]()
    if(users contains username){
      followingList = users getOrElse (username, List[String]())
    }
    return followingList
  }

  /**
   * Method to get list of users that follow this user.
   */ 
  private def getFollowers(username: String): List[String] = {
    var followersList = List[String]()
    if(followers contains username){
      followersList = followers getOrElse (username, List[String]())
    }
    return followersList
  }

}
