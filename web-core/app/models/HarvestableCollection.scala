package models

import com.mongodb.casbah.Imports._
import core.Constants._
import core.{OrganizationCollection, Harvestable}

/**
 * Harvestable Collection. This covers both real collections (DataSets) and virtual ones (VirtualCollections)
 *
 * @author Manuel Bernhardt <bernhardt.manuel@gmail.com>
 */

case class HarvestableCollection(spec: String,
                      orgId: String,
                      name: String,
                      namespaces: Map[String, String]) {

  def getRecords(metadataFormat: String, position: Int, limit: Int): (List[MetadataItem], Long) = {
    val dataSet = DataSet.findBySpecAndOrgId(spec, orgId)
    val cache = MetadataCache.get(orgId, spec, ITEM_TYPE_MDR)
    if(dataSet.isDefined) {
      val records = cache.list(position, Some(limit)).filter(_.xml.contains(metadataFormat))
      val totalSize = cache.count()
      (records, totalSize)
    } else {
      VirtualCollection.findBySpecAndOrgId(spec, orgId) match {
        case Some(vc) =>
          val references = VirtualCollection.children.find(MongoDBObject("parentId" -> vc._id, "validOutputFormats" -> metadataFormat) ++ ("idx" $gt position)).sort(MongoDBObject("idx" -> 1)).limit(limit)
          val totalSize = VirtualCollection.children.count(MongoDBObject("parentId" -> vc._id, "validOutputFormats" -> metadataFormat) ++ ("idx" $gt position))
          val records = references.toList.groupBy(_.collection).map {
            grouped =>
              val cache = MetadataCache.get(orgId, grouped._1, ITEM_TYPE_MDR)
              cache.list()
          }.flatten.toList
          (records, totalSize.toInt)
        case None => (List.empty, 0)
      }
    }
  }

  def getMetadataFormats(accessKey: Option[String]): Seq[RecordDefinition] = {
    val dataSets = DataSet.findAll(orgId)
    val virtualCollections = VirtualCollection.findAll(orgId)

    val formats = if (dataSets.exists(_.spec == spec)) {
      DataSet.getMetadataFormats(spec, orgId, accessKey)
    } else if (virtualCollections.exists(_.spec == spec)) {
      VirtualCollection.findBySpecAndOrgId(spec, orgId).map {
        // TODO check this
        vc => vc.getVisibleMetadataFormats(accessKey)
      }.getOrElse(Seq())
    } else {
      Seq()
    }

    formats.distinct
  }


}

object HarvestableCollection {

  def findAllNonEmpty(orgId: String, format: Option[String], accessKey: Option[String] = None): List[HarvestableCollection] = {

    // TODO implement accessKey lookup
    val dataSets: List[HarvestableCollection] ={
      val sets = DataSet.findAll(orgId).filterNot(_.state != DataSetState.ENABLED)
      if(format.isDefined) {
        sets.filter(ds => ds.getVisibleMetadataSchemas(accessKey).exists(_.prefix == format.get))
      } else {
        sets
      }
    }

    val virtualCollections: List[HarvestableCollection] = {
      val vcs = VirtualCollection.findAllNonEmpty(orgId)
      if(format.isDefined) {
        vcs.filter(vc => vc.getVisibleMetadataFormats(accessKey).exists(_.prefix == format.get))
      } else {
        vcs
      }
    }

    dataSets ++ virtualCollections
  }

  def findBySpecAndOrgId(spec: String, orgId: String): Option[HarvestableCollection] = {
    val maybeDataSet = DataSet.findBySpecAndOrgId(spec, orgId)
    if(maybeDataSet.isDefined) {
      val collection: HarvestableCollection = maybeDataSet.get
      Some(collection)
    } else {
      val maybeVirtualCollection = VirtualCollection.findBySpecAndOrgId(spec, orgId)
      if(maybeVirtualCollection.isDefined) {
        val collection: HarvestableCollection = maybeVirtualCollection.get
        Some(collection)
      } else {
        None
      }
    }
  }

  /**
   * Gets all publicly available formats out there, plus the ones available via the accessKey.
   */
  def getAllMetadataFormats(orgId: String, accessKey: Option[String]): List[RecordDefinition] = {
    DataSet.getAllVisibleMetadataFormats(orgId, accessKey).distinct
  }


  

  private implicit def dataSetToCollection(dataSet: DataSet): HarvestableCollection = HarvestableCollection(dataSet.spec, dataSet.orgId, dataSet.details.name, dataSet.namespaces)

  private implicit def dataSetListToCollectoinList(dataSets: List[DataSet]): List[HarvestableCollection] = dataSets.map(dataSetToCollection(_))

  private implicit def virtualCollectionToCollection(vc: VirtualCollection): HarvestableCollection = HarvestableCollection(vc.spec, vc.orgId, vc.name, vc.namespaces)

  private implicit def virtualCollectionListToCollectionList(vcs: List[VirtualCollection]): List[HarvestableCollection] = vcs.map(virtualCollectionToCollection(_))

  

}
