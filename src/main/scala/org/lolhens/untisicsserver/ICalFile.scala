package org.lolhens.untisicsserver

import org.lolhens.untisicsserver.ICalFile.ICalBlock

/**
  * Created by pierr on 29.08.2016.
  */
class ICalFile(val version: String,
               val prodId: String,
               val blocks: Map[String, ICalBlock])

object ICalFile {

  class ICalBlock(properties: Map[String, String])

  object ICalBlock {
    def parse(string: List[String]): Option[ICalBlock] = {

    }

    def parseAll(string: List[String]): List[ICalBlock] = {
      string.split("BEGIN:")
        .drop(1)
        .map { rawBlock =>

          rawBlock.split("END:")
        }
    }
  }

  def parse(string: String): Option[ICalFile] = {
    val lines = string.replaceAll("\r", "").split("\n")
    println(lines.mkString("\n--"))

    def trueAndDefined(e: Option[Boolean]) = e.isDefined && e.get

    if (trueAndDefined(lines.headOption.map(_ == "BEGIN:VCALENDAR")) &&
      trueAndDefined(lines.lastOption.map(_ == "BEGIN:VCALENDAR")))

  }

}