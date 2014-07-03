package ChatShare

import common._

import org.mashupbots.socko.events.WebSocketFrameEvent

import akka.actor.{Actor, Props, Terminated, OneForOneStrategy}
import akka.actor.SupervisorStrategy.{Restart, Resume, Stop}
import akka.event.Logging
import akka.pattern.ask
import akka.util.Timeout

import scala.concurrent.{Future, ExecutionContext}
import scala.concurrent.duration._

class RouterSupervisor extends Actor {
  val log = Logging(context.system, this)
  val datetime = new DateTime()

  implicit val ec = ExecutionContext.Implicits.global
  implicit lazy val timeout = Timeout(200 seconds)
  
  override val supervisorStrategy = OneForOneStrategy(){
    case _: IllegalArgumentException => Restart
    case _: Exception => Restart
  }
 
  val clientHandler = context.actorOf(Props[ClientHandler], name="ClientHandler")
  context.watch(clientHandler)
  val userHandler = context.actorOf(Props[UserHandler], name="UserHandler")
  context.watch(userHandler)
  val messageHandler = context.actorOf(Props[MessageHandler], name="MessageHandler")
  context.watch(messageHandler)

  def receive = {
    case Terminated(`clientHandler`) => 
      log.info(datetime.getDateTime + "<ROUTER>Client Handler Terminated.")
      //clientHandler = context.actorOf(Props[ClientHandler], name="ClientHandler")
    
    case Terminated(`userHandler`) =>
      log.info(datetime.getDateTime + "<ROUTER>User Handler Terminated.")  
      //userHandler = context.actorOf(Props[UserHandler], name="UserHandler")
    
    case Terminated(`messageHandler`) =>
      log.info(datetime.getDateTime + "<ROUTER>Message Handler Terminated.")
      //messageHandler = context.actorOf(Props[MessageHandler], name="MessageHandler")
    
    case POSTLOGIN(event: WebSocketFrameEvent) =>
      log.info(datetime.getDateTime + "<ROUTER>POSTLOGIN: " +  event.webSocketId)
      val username = parseUserName(event.readText)
      val future = clientHandler ? CONNECT(event.webSocketId, username, userHandler)
      future.onSuccess{
        case CONNECTED(response: String) =>
          log.info(datetime.getDateTime + "<ROUTER>Success on POSTLOGIN")
          context.actorOf(Props[ChatHandler]) ! (event, response)
      }
      future.onFailure{
        case e: Exception =>
          log.info(datetime.getDateTime + "<ROUTER>Exception on POSTLOGIN")
          val response = "SERVER ERROR LOGGING IN."
          context.actorOf(Props[ChatHandler]) ! (event, response)
      }
    case POSTLOGOUT(event: WebSocketFrameEvent) =>
      log.info(datetime.getDateTime + "<ROUTER>POSTLOGOUT: " + event.webSocketId)
      val username = parseUserName(event.readText)
      val future = clientHandler ? DISCONNECT(event.webSocketId, username, userHandler)
      future.onSuccess{
        case DISCONNECTED(response: Boolean) =>
          if(response){
            log.info(datetime.getDateTime + "<ROUTER>Success on POSTLOGOUT")
          }else{
            log.info(datetime.getDateTime + "<ROUTER>Failure on POSTLOGOUT")
          }
      }
      future.onFailure{
          case e: Exception =>
          log.info(datetime.getDateTime + "<ROUTER>Exception on POSTLOGOUT")
      }   
    case POSTMESSAGE(event: WebSocketFrameEvent) =>
      log.info(datetime.getDateTime + "<ROUTER>POSTMESSAGE: " + event.webSocketId)
      val future = clientHandler ? GETUSERNAME(event.webSocketId)
      future.onSuccess{
        case USERNAME(username: String) =>
          log.info(datetime.getDateTime + "<ROUTER>Success on POSTMESSAGE")
          messageHandler ! POSTBYUSER(username, event.readText)
          postMessageToFollowers(username, event.readText)
          val response = "Message Posted: " + event.readText
          context.actorOf(Props[ChatHandler]) ! (event, response)
      }
      future.onFailure{
        case e: Exception =>
          log.info(datetime.getDateTime + "<ROUTER>Exception on POSTMESSAGE")
          val response = "SERVER ERROR POSTING MESSAGE."
          context.actorOf(Props[ChatHandler]) ! (event, response)
      }
    case POSTTAGMESSAGE(event: WebSocketFrameEvent) =>
      log.info(datetime.getDateTime + "<ROUTER>POSTTAGMESSAGE: " + event.webSocketId)
      val future = clientHandler ? GETUSERNAME(event.webSocketId)
      future.onSuccess{
        case USERNAME(username: String) =>
          log.info(datetime.getDateTime + "<ROUTER>Success on POSTTAGMESSAGE")
          messageHandler ! POSTBYUSER(username, event.readText)
          val tags = parseTags(event.readText)
          for(tag <- tags){
            messageHandler ! POSTBYTAG(tag, event.readText)
          }
          postMessageToFollowers(username, event.readText)
          val response = "Message Posted: " + event.readText
          context.actorOf(Props[ChatHandler]) ! (event, response)
      }
      future.onFailure{
        case e: Exception =>
          log.info(datetime.getDateTime + "<ROUTER>Exception on POSTTAGMESSAGE")
          val response = "SERVER ERROR POSTING MESSAGE."
          context.actorOf(Props[ChatHandler]) ! (event, response)
      }
    case GETUSERMESSAGES(event: WebSocketFrameEvent) =>
      log.info(datetime.getDateTime + "<ROUTER>GETUSERMESSAGES: " + event.webSocketId)
      val username = parseUserName(event.readText)
      val future = messageHandler ? REQUESTFORUSER(username)
      future.onSuccess{
          case MESSAGES(messages) =>
          log.info(datetime.getDateTime + "<ROUTER>Success on GETUSERMESSAGES")
          val response = formatMessages(messages)
          context.actorOf(Props[ChatHandler]) ! (event, response)
      }
      future.onFailure{
          case e: Exception =>
          log.info(datetime.getDateTime + "<ROUTER>Exception on GETUSERMESSAGES")
          val response = "SERVER ERROR GETTING MESSAGES."
          context.actorOf(Props[ChatHandler]) ! (event, response)
      }
    case GETTAGMESSAGES(event: WebSocketFrameEvent) =>
      log.info(datetime.getDateTime + "<ROUTER>GETTAGMESSAGES: " + event.webSocketId) 
      val tag = parseTag(event.readText)
      val future = messageHandler ? REQUESTFORTAG(tag)
      future.onSuccess{
        case MESSAGES(messages) =>
          log.info(datetime.getDateTime + "<ROUTER>Success on GETTAGMESSAGES")
          val response = formatMessages(messages)
          context.actorOf(Props[ChatHandler]) ! (event, response)
      }
      future.onFailure{
        case e: Exception =>
          log.info(datetime.getDateTime + "<ROUTER>Exception on GETTAGMESSAGES")
          val response = "SERVER ERROR GETTING MESSAGES."
          context.actorOf(Props[ChatHandler]) ! (event, response)
      }
    case GETFOLLOWERSPERUSER(event: WebSocketFrameEvent) =>
      log.info(datetime.getDateTime + "<ROUTER>GETFOLLOWERS: " + event.webSocketId)
      val username = parseUserName(event.readText)
      val future = userHandler ? GETFOLLOWING(username)
      future.onSuccess{
        case FOLLOWERS(followersList: List[String]) =>
          log.info(datetime.getDateTime + "<ROUTER>Success on GETFOLLOWERS")
          val response = formatFollowers(followersList)
          context.actorOf(Props[ChatHandler]) ! (event, response)
      }
      future.onFailure{
        case e: Exception =>
          log.info(datetime.getDateTime + "<ROUTER>Exception on GETFOLLOWERS")
          val response = "SERVER ERROR GETTING FOLLOWERS."
          context.actorOf(Props[ChatHandler]) ! (event, response)
      }
    case GETUSERSPERUSER(event: WebSocketFrameEvent) =>
      log.info(datetime.getDateTime + "<ROUTER>GETUSERS: " + event.webSocketId)
      val future = userHandler ? GETUSERS
      future.onSuccess{
        case USERS(users: List[String]) => 
          log.info(datetime.getDateTime + "<ROUTER>Success on GETUSERS")
          val response = formatUsers(users)
          context.actorOf(Props[ChatHandler]) ! (event, response)
      }
      future.onFailure{
        case e: Exception =>
          log.info(datetime.getDateTime + "<ROUTER>Exception on GETUSERS")
          val response = "SERVER ERROR FOR REQUEST FOR USERS."
          context.actorOf(Props[ChatHandler]) ! (event, response)
      }      
    case STARTFOLLOWING(event: WebSocketFrameEvent) =>
      log.info(datetime.getDateTime + "<ROUTER>STARTFOLLOWING: " + event.webSocketId)
      val userToFollow = parseUserName(event.readText)
      val future = clientHandler ? FORWARDFOLLOW(event.webSocketId, userToFollow, userHandler)
      future.onSuccess{
        case FOLLOWERS(followersList: List[String]) =>
          log.info(datetime.getDateTime + "<ROUTER>Success on STARTFOLLOWING")
          val response = formatFollowers(followersList)
          context.actorOf(Props[ChatHandler]) ! (event, response)
      }
      future.onFailure{
        case e: Exception =>
          log.info(datetime.getDateTime + "<ROUTER>Exception on STARTFOLLOWING")
          val response = "SERVER ERROR FOR FOLLOW USER."
          context.actorOf(Props[ChatHandler]) ! (event, response)
      }
    case STOPFOLLOWING(event: WebSocketFrameEvent) =>
      log.info(datetime.getDateTime + "<ROUTER>STOPFOLLOWING: " + event.webSocketId)
      val userToFollow = parseUserName(event.readText)
      val future = clientHandler ? FORWARDSTOPFOLLOW(event.webSocketId, userToFollow, userHandler)
      future.onSuccess{
        case FOLLOWERS(followersList: List[String]) =>
          log.info(datetime.getDateTime + "<ROUTER>Success on STOPFOLLOWING")
          val response = formatFollowers(followersList)
          context.actorOf(Props[ChatHandler]) ! (event, response)
      }
      future.onFailure{
        case e: Exception =>
          log.info(datetime.getDateTime + "<ROUTER>Exception on STOPFOLLOWING")
          val response = "SERVER ERROR FOR STOP FOLLOWING USER."
          context.actorOf(Props[ChatHandler]) ! (event, response)
      }
  }

  /**
   * Method for Posting a Message to Followers of this User.
   */ 
  private def postMessageToFollowers(username: String, message: String){
    log.info(datetime.getDateTime + "<ROUTER>postMessageToFollowers: " + message + " by " + username)
    val future = clientHandler ? GETFOLLOWERSWITHCLIENTS(userHandler, username)
    future.onSuccess{
      case FOLLOWERSWITHCLIENTS(clients: List[String]) =>
        log.info(datetime.getDateTime + "<ROUTER>Success on postMessageToFollowers")
        val postmessage = "User: " + username + " posted > " + message
        for(client <- clients){
          context.actorOf(Props[ChatHandler]) ! POST(client, postmessage)
        }
    }
    future.onFailure{
      case e: Exception =>
        log.info(datetime.getDateTime + "<ROUTER>Exception on postMessageToFollowers")
    }
  }

  /**
   * Format method for returning query of messages as a string.
   */ 
  private def formatMessages(messages: List[String]): String = {
    var sb = new StringBuilder()
    for(message <- messages){
      if(!message.isEmpty){
        sb.append('?' + message + '<' + ' ')
      }
    }
    return sb.toString
  }

  /**
   * Format method for returning list of users as a string.
   */ 
  private def formatUsers(users: List[String]): String = {
    var sb = new StringBuilder()
    for(user <- users){
      if(!user.isEmpty){
        sb.append('@' + user + '<' + ' ')
      }
    }
    return sb.toString
  }

  /**
   * Format method for returning list of followers as a string.
   */ 
  private def formatFollowers(followers: List[String]): String = {
    var sb = new StringBuilder()
    for(follower <- followers){
      if(!follower.isEmpty){
        sb.append('$' + follower + '<' + ' ')
      }
    }
    return sb.toString
  }

  /**
   * Method for parsing username "@______"
   */ 
  private def parseUserName(message: String): String = {
    var startIndex = message.indexOf("@")
    startIndex = startIndex + 1
    return message.substring(startIndex, message.length)
  }

  /**
   * Method for parsing tag "#______"
   */ 
  private def parseTag(message: String): String = {
    var startIndex = message.indexOf("#")
    startIndex = startIndex + 1
    return message.substring(startIndex, message.length) 
  }

  /**
   * Method for parsing multiple tags from a message.
   */ 
  private def parseTags(message: String): List[String] = {
    var thisMessage = message
    var tags = List[String]()
    var tagMessage = ""
    var temp = ""
    while(thisMessage.contains("#")){
      temp = thisMessage.substring(thisMessage.indexOf("#"), thisMessage.length)
      tagMessage = temp.substring(1, temp.indexOf(" "))
      tags = parseTag(tagMessage) :: tags
      thisMessage = thisMessage.substring(thisMessage.indexOf("#") + tagMessage.length, thisMessage.length)
    }
    return tags
  }

}
