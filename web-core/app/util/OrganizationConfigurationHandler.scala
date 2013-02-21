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

package util

import core.CultureHubPlugin
import collection.immutable.HashMap
import models.OrganizationConfiguration

/**
 * Takes care of loading domain-specific configuration
 *
 * @author Sjoerd Siebinga <sjoerd.siebinga@gmail.com>
 * @author Manuel Bernhardt <bernhardt.manuel@gmail.com>
 * @since 3/9/11 3:25 PM
 */
object OrganizationConfigurationHandler {

  private var organizationConfigurationsMap: Seq[(String, OrganizationConfiguration)] = Seq.empty
  private var domainLookupCache: HashMap[String, OrganizationConfiguration] = HashMap.empty

  var organizationConfigurations: Seq[OrganizationConfiguration] = Seq.empty

  def getConfigurationNames: java.util.Set[String] = {
    val set: java.util.Set[String] = new java.util.TreeSet[String]
    organizationConfigurations.foreach(configuration => set.add(configuration.name))
    set
  }

  def startup(plugins: Seq[CultureHubPlugin]) {
    organizationConfigurations = OrganizationConfiguration.startup(plugins)
    organizationConfigurationsMap = toDomainList(organizationConfigurations)
    domainLookupCache = HashMap.empty
  }

  def getByName(name: String) = {
    organizationConfigurations.find(_.name.equalsIgnoreCase(name)).getOrElse(throw new RuntimeException("No configuration for name " + name))
  }

  def getByOrgId(orgId: String) = {
    organizationConfigurations.find(_.orgId == orgId).getOrElse(throw new RuntimeException("No configuration for orgId " + orgId))
  }

  def hasConfiguration(domain: String) = organizationConfigurations.exists(_.domains.exists(domain.startsWith(_)))

  def getByDomain(domain: String): OrganizationConfiguration = {
    // FIXME - this is, of course, vulnerable. Implement correct algorithmic solution not relying on fold.
    if (!domainLookupCache.contains(domain)) {
      // fetch by longest matching domain
      val configuration = organizationConfigurationsMap.foldLeft(("#", organizationConfigurations.head)) {
        (r: (String, OrganizationConfiguration), c: (String, OrganizationConfiguration)) =>
          {
            val rMatches = domain.startsWith(r._1)
            val cMatches = domain.startsWith(c._1)
            val rLonger = r._1.length() > c._1.length()

            if (rMatches && cMatches && rLonger) r
            else if (rMatches && cMatches && !rLonger) c
            else if (rMatches && !cMatches) r
            else if (cMatches && !rMatches) c
            else r // default
          }
      }._2
      domainLookupCache = domainLookupCache + (domain -> configuration)
    }
    domainLookupCache(domain)
  }

  private def toDomainList(domainList: Seq[OrganizationConfiguration]) = domainList.flatMap(t => t.domains.map((_, t))).sortBy(_._1.length)

}
