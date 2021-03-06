/*
 * Copyright 2011 Delving B.V.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package processors

import util.{ OrganizationConfigurationHandler, Logging }
import models.dos.{ Task }
import play.api.Play
import play.api.Play.current
import models.OrganizationConfiguration

/**
 *
 * @author Manuel Bernhardt <bernhardt.manuel@gmail.com>
 */

trait Processor extends Logging {

  /**
   * Does its thing given a path and optional parameters. The path may or may not exist on the file system.
   */
  def process(task: Task, processorParams: Map[String, AnyRef] = Map.empty[String, AnyRef])(implicit configuration: OrganizationConfiguration)

  def isImage(name: String) = name.contains(".") && !name.startsWith(".") && (
    name.split("\\.").last.toLowerCase match {
      case "jpg" | "tif" | "tiff" | "jpeg" => true
      case _ => false
    })

  def parameterList(task: Task) = task.params.map(p => s"${p._1}:${p._2}").mkString(", ")

  /** image name without extension **/
  def getImageName(name: String) = if (name.indexOf(".") > 0) name.substring(0, name.lastIndexOf(".")) else name

  protected def getStore(orgId: String) = {
    import controllers.dos.fileStore
    fileStore(OrganizationConfigurationHandler.getByOrgId(orgId))
  }

}