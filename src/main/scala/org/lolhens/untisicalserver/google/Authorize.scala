package org.lolhens.untisicalserver.google

import java.io.{File, FileInputStream, InputStreamReader}

import com.google.api.client.auth.oauth2.Credential
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver
import com.google.api.client.googleapis.auth.oauth2.{GoogleAuthorizationCodeFlow, GoogleClientSecrets}
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.client.util.store.FileDataStoreFactory
import com.google.api.services.calendar.{CalendarScopes, Calendar => CalendarService}

import scala.collection.JavaConverters._
import scala.util.Try

object Authorize {
  private def jarDirPath(): File =
    new File(getClass.getProtectionDomain.getCodeSource.getLocation.toURI.getPath).getParentFile

  lazy val dataStoreDir: File = new File(jarDirPath().getParentFile, "conf")

  val dataStoreFactory = new FileDataStoreFactory(dataStoreDir)

  val httpTransport: NetHttpTransport = GoogleNetHttpTransport.newTrustedTransport()

  val jsonFactory: JacksonFactory = JacksonFactory.getDefaultInstance

  private def loadClientSecrets(): Try[GoogleClientSecrets] = Try {
    val in = new FileInputStream(dataStoreDir.toPath.resolve("client_secret.json").toFile)
    GoogleClientSecrets.load(jsonFactory, new InputStreamReader(in))
  }

  def apply(scopes: List[String]): Try[Credential] = for {
    clientSecrets <- loadClientSecrets()
  } yield {
    val flow = new GoogleAuthorizationCodeFlow.Builder(
      httpTransport,
      jsonFactory,
      clientSecrets,
      scopes.asJavaCollection
    )
      .setDataStoreFactory(dataStoreFactory)
      .setAccessType("offline")
      .build

    val credential = new AuthorizationCodeInstalledApp(flow, new LocalServerReceiver()).authorize("user")
    credential
  }

  def getCalendarService(applicationName: String, readonly: Boolean = true): Try[CalendarService] = for {
    credential <- Authorize(List(
      if (readonly) CalendarScopes.CALENDAR_READONLY
      else CalendarScopes.CALENDAR
    ))
  } yield {
    new CalendarService.Builder(httpTransport, jsonFactory, credential)
      .setApplicationName(applicationName)
      .build
  }
}
