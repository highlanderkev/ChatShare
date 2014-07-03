package ChatShare

import common._

import akka.actor.{Actor, ActorRef, Props}
import akka.event.Logging

import scala.collection.mutable.Map

class MessageHandler extends Actor {
  val log = Logging(context.system, this)
  val datetime = new DateTime()

  //Messages by User (key: username, value: message)
  var userMessages = Map("" -> List[String]())

  //Messages by Tag (key: tag, value; message)
  var tagMessages = Map("" -> List[String]())

  def receive = {
    case REQUESTFORTAG(tag: String) =>
      
      log.info(datetime.getDateTime + "<MESSAGEHANDLER>Request for posts with tag: " + tag)
      var messages = List[String]()
      if(tagMessages contains tag){
        messages = tagMessages getOrElse (tag, List[String]())
      }
      sender ! MESSAGES(messages)

    case REQUESTFORUSER(username: String) => 
      
      println(datetime.getDateTime + "<MESSAGEHANDLER>Request for posts by user: " + username)
      var messages = List[String]()
      if(userMessages contains username){
        messages = userMessages getOrElse (username, List[String]())
      }
      sender ! MESSAGES(messages)

    case POSTBYUSER(username: String,  message: String) =>

      println(datetime.getDateTime + "<MESSAGEHANDLER>Message: " + message + " posted by user: " + username) 
      var messages = List[String]()
      if(userMessages contains username){
          messages = userMessages getOrElse (username, List[String]())
      }
      messages = message :: messages
      userMessages += (username -> messages)

    case POSTBYTAG(tag: String, message: String) =>
      
      log.info(datetime.getDateTime + "<MESSAGEHANDLER>Message: " + message + " posted by tag: " + tag)
      var messages = List[String]()
      if(tagMessages contains tag){
        messages = tagMessages getOrElse (tag, List[String]())
      }
      messages = message :: messages
      tagMessages += (tag -> messages)

  }
}
