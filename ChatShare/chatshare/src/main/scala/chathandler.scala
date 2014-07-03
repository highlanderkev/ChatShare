package ChatShare

import common._

import org.mashupbots.socko.events.HttpRequestEvent
import org.mashupbots.socko.events.WebSocketFrameEvent

import akka.actor.{Actor, ActorRef, actorRef2Scala}
import akka.event.Logging

import scala.io.Source

class ChatHandler extends Actor {
  val log = Logging(context.system, this)
  val datetime = new DateTime()

  def receive = {
    case event: HttpRequestEvent =>
      writeHTML(event)
      context.stop(self)
    case (event: WebSocketFrameEvent, router: ActorRef) =>
      processWebSocketResponse(event, router)
      context.stop(self)
    case (event: WebSocketFrameEvent, response: String) => 
      writeWebSocketResponse(event, response)
      context.stop(self)
    case POST(socketId: String, message: String) =>
      writeWebSocketMessage(socketId, message)
      context.stop(self)
    case _ => {
      log.info("received unknown message of type: ")
      context.stop(self)
    }
  }

  /**
   * Write index.html back for setting up websocket.
   */ 
  private def writeHTML(ctx: HttpRequestEvent){
    if(ctx.request.is100ContinueExpected){
      ctx.response.write100Continue()
    }
    val buf = new StringBuilder()
    //Serve Back index.html file
    Source.fromURL(getClass.getResource("/index.html")).foreach{
      case c => buf += c
    }
    ctx.response.write(buf.toString, "text/html; charset=UTF-8")
  }

  /**
   * Write back message to client.
   */ 
  private def writeWebSocketMessage(webSocketId: String, message: String){
    log.info(datetime.getDateTime + "<CHATHANDLER>Message " + message + " to websocket: " + webSocketId)
    ChatApp.webServer.webSocketConnections.writeText(datetime.getDateTime + "> " + message, webSocketId)
  }

  /**
   * Write back processed response to client.
   */ 
  private def writeWebSocketResponse(event: WebSocketFrameEvent, response: String){
    log.info(datetime.getDateTime + "<CHATHANDLER>Response: " + response + " to websocket: " + event.webSocketId)
    ChatApp.webServer.webSocketConnections.writeText(datetime.getDateTime + "> " + response, event.webSocketId)
  }

  /**
   * Parse WebSocket Frame Event and forward to router for processing.
   */ 
  private def processWebSocketResponse(event: WebSocketFrameEvent, router: ActorRef){
    log.info(datetime.getDateTime + "<CHATHANDLER>WebSocketFrameEvent: " + event.webSocketId)
    //parsing tree for frame event
    if(event.readText.contains(">")){
      postLogin(event, router)
    }else if(event.readText.contains("?")){
      searchMessages(event, router)
    }else if(event.readText.contains("$")){
      getUsersFollowers(event, router)
    }else if(event.readText.contains("%")){
      startstopfollow(event, router)
    }else{
      log.info(datetime.getDateTime + "<CHATHANDLER>ERROR INVALID FORMAT EVENT: " + event.readText)
      val response = "ERROR: INVALID FORMAT: " + event.readText
      writeWebSocketResponse(event, response)
    }
  }

  /**
   * "%" - follow - "@" - start or "!@" - stop 
   */ 
  private def startstopfollow(event: WebSocketFrameEvent, router: ActorRef){
    if(event.readText.contains("!")){
      log.info(datetime.getDateTime + "<CHATHANDLER>Stop Following: " + event.readText)
      router ! STOPFOLLOWING(event)
    }else if(event.readText.contains("@")){
      log.info(datetime.getDateTime + "<CHATHANDLER>Start Following: " + event.readText)
      router ! STARTFOLLOWING(event)
    }else{
      log.info(datetime.getDateTime + "<CHATHANDLER>ERROR For Following: " + event.readText)
      val response = "ERROR: INVALID FORMAT: " + event.readText
      writeWebSocketResponse(event, response)
    }
  }

  /**
   * "$" - get "@" - followers by username, or "!" all users
   */ 
  private def getUsersFollowers(event: WebSocketFrameEvent, router: ActorRef){
      if(event.readText.contains("@")){
        log.info(datetime.getDateTime + "<CHATHANDLER>Get Followers: " + event.readText)
        router ! GETFOLLOWERSPERUSER(event)
      }else if(event.readText.contains("!")){
        log.info(datetime.getDateTime + "<CHATHANDLER>Get Users: " + event.readText)
        router ! GETUSERSPERUSER(event)
     }else{
        log.info(datetime.getDateTime + "<CHATHANDLER>ERROR for getUsersFollowers: " + event.readText)
        val response = "ERROR: INVALID FORMAT: " + event.readText
        writeWebSocketResponse(event, response)
      }
  }

  /**
   * "?" - search then either "@" - user messages or "#" - tag messages
   */ 
  private def searchMessages(event: WebSocketFrameEvent, router: ActorRef){
    if(event.readText.contains("@")){
      log.info(datetime.getDateTime + "<CHATHANDLER>Search for username: " + event.readText)
      router ! GETUSERMESSAGES(event)
    }else if(event.readText.contains("#")){
      log.info(datetime.getDateTime + "<CHATHANDLER>Search for tag: " + event.readText)
      router ! GETTAGMESSAGES(event)
    }else{
      log.info(datetime.getDateTime + "<CHATHANDLER>ERROR for Search: " + event.readText)
      
    }
  }

  /**
   * ">" - post then either "@" - login or "#" - post tag message, or post message
   */ 
  private def postLogin(event: WebSocketFrameEvent, router: ActorRef){
    if(event.readText.contains("!@")){
      log.info(datetime.getDateTime + "<CHATHANDLER>Logout: " + event.readText)
      router ! POSTLOGOUT(event)
    }else if(event.readText.contains("@")){
      log.info(datetime.getDateTime + "<CHATHANDLER>Login: " + event.readText)
      router ! POSTLOGIN(event)
    }else if(event.readText.contains("#")){
      log.info(datetime.getDateTime + "<CHATHANDLER>Post with tag: " + event.readText)
      router ! POSTTAGMESSAGE(event)
    }else{
      log.info(datetime.getDateTime + "<CHATHANDLER>Post: " + event.readText)
      router ! POSTMESSAGE(event)
    }
  }

}
