package ChatShare

import common._

import akka.actor.{Actor, ActorRef, ActorSelection}
import akka.event.Logging
import akka.pattern.ask
import akka.util.Timeout

import scala.concurrent.duration._
import scala.concurrent.{Future, ExecutionContext}
import scala.collection.mutable.Map

class ClientHandler extends Actor {
  val log = Logging(context.system, this)
  val datetime = new DateTime()
  
  implicit val ec = ExecutionContext.Implicits.global
  implicit lazy val timeout = Timeout(200 seconds)

  //Client Map (key: username, value: clientId)
  var clients = Map("" -> "")

  def receive = {
    case CONNECT(clientId: String, username: String, userHandler: ActorRef) =>
      if(clients contains username){
        log.info("<CLIENTHANDLER>Client: " + clientId + " already connected as " + username + ".")
      }else{
        log.info("<CLIENTHANDLER>Client: " + clientId + " connnected as " + username + ".")
      }
      clients += (username -> clientId)
      val parent = sender
      val future = userHandler ? LOGIN(username)
      future.onSuccess{
        case LOGGED(username: String) =>
          log.info(datetime.getDateTime + "<CLIENTHANDLER>LOGGED SUCCESS FOR " + username)
          val response = username + " now connected to ChatShare."
          parent ! CONNECTED(response)
      }
      future.onFailure{
        case e: Exception =>
          log.info(datetime.getDateTime + "<CLIENTHANDLER>Exception on CONNECT")
          val response = "SERVER ERROR ON LOGIN"
          parent ! CONNECTED(response)
      }
    case DISCONNECT(clientId: String, username: String, userHandler: ActorRef) =>
      if(clients contains clientId){
        log.info("<CLIENTHANDLER>Client: " + clientId + " is disconnecting.")
      }else{
        log.info("<CLIENTHANDLER>Client: " + clientId + " is not connected.")
      }        
      clients = clients - username
      sender ! DISCONNECTED(true)
    case GETCLIENT(username: String) =>
      log.info(datetime.getDateTime + "<CLIENTHANDLER>GETCLIENT: " + username)
      var client = ""
      if(clients contains username){
        client = clients getOrElse (username, "")
      }
      sender ! CLIENT(client)
    case GETUSERNAME(clientId: String) =>
      log.info(datetime.getDateTime + "<CLIENTHANDLER>GETUSERNAME: " + clientId)
      var username = ""
      var users = clients.map(_.swap)
      if(users contains clientId){
        username = users getOrElse (clientId, "")
      }
      sender ! USERNAME(username)
    case FORWARDFOLLOW(clientId: String, userToFollow: String, userHandler: ActorRef) =>
      log.info(datetime.getDateTime + "<CLIENTHANDLER>FORWARDFOLLOW: " + userToFollow + " by " + clientId)
      var username = ""
      var users = clients.map(_.swap)
      if(users contains clientId){
          username = users getOrElse (clientId, "")
      }
      val parent = sender
      val future = userHandler ? FOLLOW(username, userToFollow)
      future.onSuccess{
        case FOLLOWERS(followersList: List[String]) =>
            log.info(datetime.getDateTime + "<CLIENTHANDLER>Success on FORWARDFOLLOW")
            parent ! FOLLOWERS(followersList)
      }
      future.onFailure{
        case e: Exception =>
          log.info(datetime.getDateTime + "<CLIENTHANDLER>Exception on FORWARDFOLLOW") 
          parent ! FOLLOWERS(List[String]())
      }
    case FORWARDSTOPFOLLOW(clientId: String, userToFollow: String, userHandler: ActorRef) =>
      log.info(datetime.getDateTime + "<CLIENTHANDLER>FORWARDSTOPFOLLOW: " + userToFollow + " by " + clientId)
      var username = ""
      var users = clients.map(_.swap)
      if(users contains clientId){
        username = users getOrElse (clientId, "")
      }
      val parent = sender
      val future = userHandler ? STOPFOLLOW(username, userToFollow)
      future.onSuccess{
        case FOLLOWERS(followersList: List[String]) =>
            log.info(datetime.getDateTime + "<CLIENTHANDLER>Success on FORWARDSTOPFOLLOW")
            parent ! FOLLOWERS(followersList)
      }
      future.onFailure{
        case e: Exception =>
          log.info(datetime.getDateTime + "<CLIENTHANDLER>Exception on FORWARDSTOPFOLLOW")
          parent ! FOLLOWERS(List[String]())
      }
    case GETFOLLOWERSWITHCLIENTS(userHandler: ActorRef, username: String) =>
      log.info(datetime.getDateTime + "<CLIENTHANDLER>GETFOLLOWERSWITHCLIENTS: " + username)
      val parent = sender
      val future = userHandler ? GETFOLLOWERS(username)
      future.onSuccess{
        case FOLLOWERS(followersList: List[String]) =>
          log.info(datetime.getDateTime + "<CLIENTHANDLER>Success on GETFOLLOWERSWITHCLIENTS")
          var clientList = List[String]()
          for(follower <- followersList){
            var client = ""
            if(clients contains follower){
              client = clients getOrElse (follower, "")
            }
            clientList = client :: clientList
          }
          parent ! FOLLOWERSWITHCLIENTS(clientList)
      }
      future.onFailure{
        case e: Exception =>
          log.info(datetime.getDateTime + "<CLIENTHANDLER>Exception on GETFOLLOWERSWITHCLIENTS")
          parent ! FOLLOWERSWITHCLIENTS(List[String]()) 
      }
  }
}
      
