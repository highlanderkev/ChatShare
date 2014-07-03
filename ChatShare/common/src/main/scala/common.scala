package common

import java.text.SimpleDateFormat
import java.util.GregorianCalendar

class DateTime {

  def getDateTime():String = {
    val dateFormatter = new SimpleDateFormat("HH:mm:ss")
    val time = new GregorianCalendar()
    return dateFormatter.format(time.getTime())
  }

}
