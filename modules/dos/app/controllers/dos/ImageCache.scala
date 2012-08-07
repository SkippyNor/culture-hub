package controllers.dos

import play.api.Logger
import play.api.mvc._
import play.api.mvc.Results._

import com.mongodb.casbah.Imports._
import java.util.Date
import java.io.InputStream
import org.apache.commons.httpclient.methods.GetMethod
import org.apache.commons.httpclient.Header
import extensions.HTTPClient
import com.mongodb.casbah.commons.MongoDBObject
import java.net.URLDecoder
import core.DomainConfigurationAware
import models.DomainConfiguration


/**
 *
 * @author Manuel Bernhardt <bernhardt.manuel@gmail.com>
 */

object ImageCache extends Controller with RespondWithDefaultImage with DomainConfigurationAware {
  val imageCacheService = new ImageCacheService

  def image(id: String, withDefaultFromUrl: Boolean) = DomainConfigured {
    Action {
    implicit request =>
      val result = imageCacheService.retrieveImageFromCache(request, configuration, URLDecoder.decode(id, "utf-8"), false)
      if (withDefaultFromUrl) withDefaultFromRequest(result, false, None) else result
    }
  }

  def thumbnail(id: String, width: Option[String], withDefaultFromUrl: Boolean) = DomainConfigured {
    Action {
      implicit request =>
        val result = imageCacheService.retrieveImageFromCache(request, configuration, URLDecoder.decode(id, "utf-8"), true, width)
        if (withDefaultFromUrl) withDefaultFromRequest(result, true, width) else result
    }
  }
}

class ImageCacheService extends HTTPClient with Thumbnail {

  private val log: Logger = Logger("ImageCacheService")

  def retrieveImageFromCache(request: Request[AnyContent], configuration: DomainConfiguration, url: String, thumbnail: Boolean, thumbnailWidth: Option[String] = None): Result = {
      // catch try block to harden the application and always give back a 404 for the application
      try {
        require(url != null)
        require(url != "noImageFound")
        require(!url.isEmpty)

        val sanitizedUrl = sanitizeUrl(url)

        val isAvailable = checkOrInsert(sanitizedUrl, configuration)
        if(isAvailable) {
          imageCacheStore(configuration).db.getCollection("fs.files").update(MongoDBObject("filename" -> sanitizedUrl), ($inc ("viewed" -> 1)) ++ $set ("lastViewed" -> new Date))
          ImageDisplay.renderImage(
            id = sanitizedUrl,
            thumbnail = thumbnail,
            thumbnailWidth = ImageDisplay.thumbnailWidth(thumbnailWidth),
            store = imageCacheStore(configuration)
          )(request)
        } else {
          NotFound(sanitizedUrl)
        }

      } catch {
        case ia: IllegalArgumentException =>
          log.error("Problem with processing this url: \"" + sanitizeUrl(url) + "\"", ia)
          NotFound(sanitizeUrl(url))
        case ex: Exception =>
          log.error("Unable to find image: \"" + sanitizeUrl(url) + "\"\n", ex)
          NotFound(sanitizeUrl(url))
      }
  }

  def checkOrInsert(url: String, configuration: DomainConfiguration): Boolean = {
    if(isImageCached(url, configuration)) true else {
      log.debug("Image not found, attempting to store it in the cache based on URL: '" + url + "'")
      val stored = storeImage(url, configuration)
      if(stored) {
        log.debug("Successfully cached image for URL: '" + url + "'")
        true
      } else {
        log.info("Unable to store '" + url + "'")
        false
      }
    }
  }

  private def isImageCached(url: String, configuration: DomainConfiguration): Boolean = {
    log.debug("Attempting to retrieve image for URL " + url)
    imageCacheStore(configuration).findOne(MongoDBObject("filename" -> url)) != None
  }

  /** Sanitize URLs globally. This ain't elegant but it is the most robust approach **/
  private def sanitizeUrl(url: String): String = {
    url.
      replaceAll("""\\""", "%5C").
      replaceAll("\\[", "%5B").
      replaceAll("\\]", "%5D").
      replaceAll(" ", "%20").
      replaceAll("\\+", "%2B")
  }

  private def storeImage(url: String, configuration: DomainConfiguration): Boolean = {
    val image = retrieveImageFromUrl(url)
    if (image.storable) {
      val inputFile = imageCacheStore(configuration).createFile(image.dataAsStream, image.url)
      inputFile.contentType = image.contentType
      inputFile put (IMAGE_ID_FIELD, image.url)
      inputFile put("viewed", 0)
      inputFile put("lastViewed", new Date)
      inputFile.save

      val cachedImage = imageCacheStore(configuration).findOne(image.url).getOrElse(return false)
      createThumbnails(cachedImage, imageCacheStore(configuration), Map(IMAGE_ID_FIELD -> image.url))
      true
    } else {
      false
    }
  }

  private def retrieveImageFromUrl(url: String): WebResource = {
    val method = new GetMethod(url)
    try {
      val response = getHttpClient.executeMethod(method)
      val isRedirect = Seq(300, 301, 302, 303, 307).contains(response)
      if(isRedirect) {
        val location = Option(method.getResponseHeader("Location"))
        if(location.isDefined) {
          retrieveImageFromUrl(location.get.getValue)
        } else {
          log.error("Could not retrieve Location header for redirect response returned by " + url)
          WebResource()
        }
      } else {
        WebResource(method)
      }
    } catch {
      case timeout: org.apache.commons.httpclient.ConnectTimeoutException =>
        log.error("""Could not retrieve image at URL "%s" because of connection timeout: %s""".format(url, timeout.getMessage))
        WebResource()
      case t =>
      log.error("""Error downloading image at URL "%s": %s""".format(url, t.getMessage), t)
      WebResource()
    }
  }

}

case class WebResource(url: String, dataAsStream: InputStream, storable: Boolean, contentType: String)

case object WebResource {

  def apply(): WebResource = WebResource("", null, false, "unknown/unknown")

  def apply(method: GetMethod): WebResource = {
    val contentType = method.getResponseHeader("Content-Type").getValue.toLowerCase.split(",").headOption.getOrElse("")
    // TODO sanity check on length
    val contentLength: Header = method.getResponseHeader("Content-Length")
    val mimeTypes = List("image/png", "image/jpeg", "image/jpg", "image/gif", "image/tiff", "image/pjpeg")
    WebResource(method.getURI.toString, method.getResponseBodyAsStream, mimeTypes.contains(contentType), contentType)
  }
}
