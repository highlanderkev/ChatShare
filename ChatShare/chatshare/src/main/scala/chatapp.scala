package ChatShare

import org.mashupbots.socko.events.HttpResponseStatus
import org.mashupbots.socko.events.WebSocketHandshakeEvent
import org.mashupbots.socko.infrastructure.Logger
import org.mashupbots.socko.routes._
import org.mashupbots.socko.webserver.WebServer
import org.mashupbots.socko.webserver.WebServerConfig

import akka.actor.{Actor, ActorRef, ActorSystem, Props, actorRef2Scala}
import akka.dispatch.OnComplete

object ChatApp extends Logger {
  
  val actorSystem = ActorSystem("ChatShareAppSystem")
  val router = actorSystem.actorOf(Props[RouterSupervisor], name="RouterSupervisor")
  
  val routes = Routes({
    case HttpRequest(httpRequest) => httpRequest match {
      case GET(Path("/")) => {
        // Return HTML page to establish web socket
        actorSystem.actorOf(Props[ChatHandler]) ! httpRequest
      }
      case Host("localhost:8888") => {
        // Return HTML page to establish web socket
        actorSystem.actorOf(Props[ChatHandler]) ! httpRequest
      }
      case Path("/favicon.ico") => {
        // if favicon.ico, just return a 404 because we dont have one
        httpRequest.response.write(HttpResponseStatus.NOT_FOUND)
      }
    }

    case WebSocketHandshake(wsHandshake) => wsHandshake match {
      case Path("/websocket/") => {
        //Store WebStocket ID For future use
        
        // To start web socket processing, authorize handshake
        wsHandshake.authorize( onComplete = Some(onWebSocketHandshakeComplete),
                               onClose = Some(onWebSocketClose))
      }
    }

    case WebSocketFrame(wsFrame) => {
      // Once handshaking has taken place, we can now process frames sent from client
      actorSystem.actorOf(Props[ChatHandler]) ! (wsFrame, router)
    }

  })

  val webServer = new WebServer(WebServerConfig(), routes, actorSystem)

  def main(args: Array[String]) {
    Runtime.getRuntime.addShutdownHook(new Thread {
      override def run { 
        webServer.stop()
      }
    })
    webServer.start()

    System.out.println("Open a few browsers and navigate to http://localhost:8888/ To Start Using ChatShare!")
  }
  
  def onWebSocketHandshakeComplete(webSocketId: String) {
    System.out.println(s"Web Socket $webSocketId connected")
  }
  
  def onWebSocketClose(webSocketId: String) {
    System.out.println(s"Web Socket $webSocketId closed")
  }
      
}
