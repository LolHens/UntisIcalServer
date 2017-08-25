package org.lolhens.untisicalserver.ical4j

/**
  * Created by pierr on 17.03.2017.
  */
case class Calendar(properties: List[Property], components: List[Component]) {

}

object Calendar {
  val empty: Calendar = Calendar(Nil, Nil)
}
